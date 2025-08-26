package com.example.roomacoustic.yolo

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

/**  YOLOv8 TFLite Detector  (동기 리턴 버전)  */
class Detector(
    private val context: Context,
    private val modelPath: String,
    private val labelPath: String?,
    private val detectorListener: DetectorListener? = null,      // ★ nullable 로 변경
    private val message: (String) -> Unit = {}
) {

    /* ── TensorFlow 관련 필드 ─────────────────────────────── */
    private var interpreter: Interpreter
    private var labels = mutableListOf<String>()

    private var tensorWidth = 0
    private var tensorHeight = 0
    private var numChannel = 0
    private var numElements = 0

    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
        .add(CastOp(INPUT_IMAGE_TYPE))
        .build()

    private var isGpu = true

    /* MeasureScreen 에서 참조하기 위한 크기 정보 */
    var frameW = 0; private set
    var frameH = 0; private set
    val inputSize get() = tensorWidth          // 정사각형 한 변

    /* ── 초기화 ─────────────────────────────────────────── */
    init {
        // Delegate 옵션
        val compat = CompatibilityList()
        val opts = Interpreter.Options().apply {
            if (isGpu && compat.isDelegateSupportedOnThisDevice) {
                addDelegate(GpuDelegate(compat.bestOptionsForThisDevice.apply {
                    setQuantizedModelsAllowed(true)
                }))
            } else setNumThreads(4)
        }
        interpreter = Interpreter(FileUtil.loadMappedFile(context, modelPath), opts)

        /* I/O Tensor Shape */        /* input: [1, 640, 640, 3] */
        interpreter.getInputTensor(0)?.shape()?.let { shp ->
            if (shp[1] == 3) { tensorWidth = shp[2]; tensorHeight = shp[3] }
            else              { tensorWidth = shp[1]; tensorHeight = shp[2] }
        }
        interpreter.getOutputTensor(0)?.shape()?.let { shp ->
            numChannel  = shp[1]
            numElements = shp[2]
        }

        /* 라벨 로드 (메타데이터 우선) */
        labels.addAll(MetaData.extractNamesFromMetadata(FileUtil.loadMappedFile(context, modelPath)))
        if (labels.isEmpty()) {
            labelPath?.let { labels.addAll(MetaData.extractNamesFromLabelFile(context, it)) }
            if (labels.isEmpty()) {
                message("LABELS not found; temp names inserted")
                labels.addAll(MetaData.TEMP_CLASSES)
            }
        }
    }

    /** GPU/CPU 토글 재시작 */
    fun restart(useGpu: Boolean) {
        isGpu = useGpu
        interpreter.close()

        val opts = Interpreter.Options().apply {
            if (useGpu && CompatibilityList().isDelegateSupportedOnThisDevice) {
                addDelegate(GpuDelegate(CompatibilityList().bestOptionsForThisDevice))
            } else setNumThreads(4)
        }
        interpreter = Interpreter(FileUtil.loadMappedFile(context, modelPath), opts)
    }

    fun close() = interpreter.close()

    /* ****************************************************************
       1) 외부에서 호출하는 detect (원본 폭·높이 함께 전달)
       **************************************************************** */
    fun detect(squareBmp: Bitmap, origW: Int, origH: Int): List<BoundingBox> {
        frameW = origW
        frameH = origH
        return detect(squareBmp)          // ↓ 내부 동작
    }

    /* ****************************************************************
       2) 실제 추론 수행  (=정사각형 Bitmap 입력)
       **************************************************************** */
    private fun detect(img: Bitmap): List<BoundingBox> {
        if (tensorWidth == 0) return emptyList()

        /* 전처리 */
        val input = if (img.width != tensorWidth)
            Bitmap.createScaledBitmap(img, tensorWidth, tensorHeight, false)
        else img

        val tensorImg  = TensorImage(INPUT_IMAGE_TYPE).apply { load(input) }
        val processed  = imageProcessor.process(tensorImg)

        /* 추론 */
        val outputBuf = TensorBuffer.createFixedSize(
            intArrayOf(1, numChannel, numElements), OUTPUT_IMAGE_TYPE
        )
        val t0 = SystemClock.uptimeMillis()
        interpreter.run(processed.buffer, outputBuf.buffer)
        val infMs = SystemClock.uptimeMillis() - t0

        /* 후처리 */
        val boxes = postProcess(outputBuf.floatArray)
        // 콜백 호출 (옵션)
        if (boxes.isEmpty()) detectorListener?.onEmptyDetect()
        else                 detectorListener?.onDetect(boxes, infMs)
        return boxes
    }

    fun warmUp() {
        if (tensorWidth == 0) return
        detect(Bitmap.createBitmap(tensorWidth, tensorHeight, Bitmap.Config.ARGB_8888))
    }

    /* ── 후처리 (NMS 포함) ─────────────────────────────────── */
    private fun postProcess(arr: FloatArray): List<BoundingBox> {
        val raw = mutableListOf<BoundingBox>()
        for (c in 0 until numElements) {
            var maxConf = CONFIDENCE_THRESHOLD
            var clsIdx  = -1
            var j = 4; var idx = c + numElements * j
            while (j < numChannel) {
                if (arr[idx] > maxConf) { maxConf = arr[idx]; clsIdx = j - 4 }
                j++; idx += numElements
            }
            if (clsIdx < 0) continue

            val cx = arr[c]
            val cy = arr[c + numElements]
            val w  = arr[c + numElements * 2]
            val h  = arr[c + numElements * 3]
            val x1 = cx - w / 2f; val y1 = cy - h / 2f
            val x2 = cx + w / 2f; val y2 = cy + h / 2f
            if (x1 !in 0f..1f || y1 !in 0f..1f || x2 !in 0f..1f || y2 !in 0f..1f) continue

            raw.add(
                BoundingBox(x1, y1, x2, y2, cx, cy, w, h,
                    maxConf, clsIdx, labels[clsIdx])
            )
        }
        return nms(raw)
    }

    private fun nms(list: MutableList<BoundingBox>): List<BoundingBox> {
        val sorted = list.sortedByDescending { it.cnf }.toMutableList()
        val sel = mutableListOf<BoundingBox>()
        while (sorted.isNotEmpty()) {
            val first = sorted.removeAt(0)
            sel.add(first)
            val it = sorted.iterator()
            while (it.hasNext()) {
                val bb = it.next()
                if (iou(first, bb) >= IOU_THRESHOLD) it.remove()
            }
        }
        return sel
    }

    private fun iou(a: BoundingBox, b: BoundingBox): Float {
        val x1 = maxOf(a.x1, b.x1); val y1 = maxOf(a.y1, b.y1)
        val x2 = minOf(a.x2, b.x2); val y2 = minOf(a.y2, b.y2)
        val inter = maxOf(0f, x2 - x1) * maxOf(0f, y2 - y1)
        val union = a.w * a.h + b.w * b.h - inter
        return inter / union
    }

    /* ── Listener 인터페이스 (nullable 허용) ────────────────── */
    interface DetectorListener {
        fun onEmptyDetect()
        fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long)
    }

    companion object {
        private const val INPUT_MEAN = 0f
        private const val INPUT_STANDARD_DEVIATION = 255f
        private val INPUT_IMAGE_TYPE  = DataType.FLOAT32
        private val OUTPUT_IMAGE_TYPE = DataType.FLOAT32
        private const val CONFIDENCE_THRESHOLD = 0.30f
        private const val IOU_THRESHOLD = 0.50f
    }
}
