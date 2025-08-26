package com.example.roomacoustic.screens.measure

/* ── Android / Compose ─────────────────────────────────────────── */
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.opengl.Matrix as GLM
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController

/* 성능 최적화 상태 타입 */
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf

/* ── ARCore / SceneView ────────────────────────────────────────── */
import com.google.ar.core.Config
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.core.Session
import io.github.sceneview.ar.ARSceneView

/* ── 프로젝트 모듈 ──────────────────────────────────────────────── */
import com.example.roomacoustic.tracker.SimpleTracker
import com.example.roomacoustic.util.YuvToRgbConverter
import com.example.roomacoustic.viewmodel.RoomViewModel
import com.example.roomacoustic.yolo.BoundingBox
import com.example.roomacoustic.yolo.Constants
import com.example.roomacoustic.yolo.Detector
import com.example.roomacoustic.yolo.OverlayView
import com.example.roomacoustic.model.Vec3
import com.example.roomacoustic.util.DepthUtil

/* ── 코루틴/기타 ───────────────────────────────────────────────── */
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@Composable
fun MeasureScreen(nav: NavController, vm: RoomViewModel) {

    /* ── 기본 준비 ─────────────────────────────────────────────── */
    val ctx            = LocalContext.current
    val lifecycleOwner = ctx as LifecycleOwner
    val uiScope        = rememberCoroutineScope()

    /* SceneView (카메라 + ARCore) */
    val sceneView = remember {
        ARSceneView(context = ctx, sharedLifecycle = lifecycleOwner.lifecycle).apply {
            configureSession { _, cfg ->
                cfg.depthMode        = Config.DepthMode.RAW_DEPTH_ONLY
                cfg.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            }
        }
    }

    /* YOLO / 변환기 / 스레드 */
    val camExecutor  = remember { Executors.newSingleThreadExecutor() }
    val isBusy       = remember { AtomicBoolean(false) }
    var detector     by remember { mutableStateOf<Detector?>(null) }
    val yuvConverter = remember { YuvToRgbConverter(ctx) }

    /* 탭 입력 / 오버레이 크기 */
    val tapQueue = remember { mutableStateListOf<PointF>() }
    var overlayView by remember { mutableStateOf<OverlayView?>(null) }
    var overlayW by remember { mutableIntStateOf(0) }
    var overlayH by remember { mutableIntStateOf(0) }

    /* ── 두 점 측정 상태 ─────────────────────────────────────── */
    var firstPoint by remember { mutableStateOf<Vec3?>(null) }      // A(월드 고정)
    var hoverPoint by remember { mutableStateOf<Vec3?>(null) }      // 레티클 히트(월드)
    var showLabelDialog by remember { mutableStateOf(false) }
    var lastDistanceM by remember { mutableFloatStateOf(0f) }
    val labelOptions = listOf("너비", "깊이", "높이", "기타")
    var selectedLabel by remember { mutableStateOf(labelOptions.first()) }

    /* ✅ 월드→스크린 투영 좌표(첫 점, 레티클/두번째) — Canvas에서 사용 */
    var firstScreen by remember { mutableStateOf<Offset?>(null) }
    var hoverScreen by remember { mutableStateOf<Offset?>(null) }

    /* ── 스피커 탐지 ─────────────────────────────────────────── */
    var detectionEnabled by remember { mutableStateOf(false) } // 사용자가 시작 눌러야 켜짐
    var processing by remember { mutableStateOf(true) }
    var measurementFinished by remember { mutableStateOf(false) }

    // YOLO 결과 전달용 큐 (세션 스레드에서 안전하게 처리)
    val detQueue = remember { ConcurrentLinkedQueue<List<BoundingBox>>() }

    // YOLO 입력용 재사용 버퍼
    var bmpReuse by remember { mutableStateOf<Bitmap?>(null) }

    /* ── Detector 초기화 ─────────────────────────────────────── */
    LaunchedEffect(Unit) {
        detector = Detector(
            ctx, Constants.MODEL_PATH, Constants.LABELS_PATH,
            detectorListener = null,
            message = {}
        ).apply {
            restart(useGpu = true)
            warmUp()
        }
    }

    fun finishAndGoRender() {
        if (measurementFinished) return
        measurementFinished = true
        processing = false
        uiScope.launch(Dispatchers.Main) {
            detector?.close()
            sceneView.onSessionUpdated = null
            sceneView.destroy()
            val detected = vm.speakers.isNotEmpty() // 현재까지 스피커가 있으면 true
            nav.navigate("Render?detected=$detected") {
                popUpTo("Measure") { inclusive = true }
            }
        }
    }

    /* ── UI ─────────────────────────────────────────────────── */
    Box(
        Modifier.fillMaxSize()
            .background(Color.Black)
            .wrapContentSize(Alignment.Center)
    ) {
        // 1) 카메라/AR
        AndroidView(
            factory  = { sceneView },
            modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f)
        )

        // 2) YOLO Overlay (라벨 박스는 뷰 좌표로 렌더)
        AndroidView(
            factory = { c ->
                OverlayView(c).apply {
                    addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                        overlayW = v.width
                        overlayH = v.height
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f).zIndex(1f)
        ) { overlayView = it }

        // 3) 탭 입력 레이어 (두 점 측정)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .zIndex(2f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            tapQueue.add(PointF(offset.x, offset.y))
                        }
                    )
                }
        )

        // 4) 레티클 + (월드 고정) 첫 점 + 선(첫 점 ↔ 레티클/두번째)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .zIndex(3f)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            // 레티클(가운데 십자)
            drawLine(
                color = Color.White,
                start = center.copy(x = center.x - 20f),
                end   = center.copy(x = center.x + 20f),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.White,
                start = center.copy(y = center.y - 20f),
                end   = center.copy(y = center.y + 20f),
                strokeWidth = 2f
            )

            // 월드 고정 첫 점
            firstScreen?.let { fs ->
                drawCircle(
                    color = Color.Cyan,
                    radius = 6.dp.toPx(),
                    center = fs
                )
            }

            // 첫 점 ↔ 레티클/두 번째 점(hoverScreen) 사이 선
            if (firstScreen != null && hoverScreen != null) {
                drawLine(
                    color = Color.White,
                    start = firstScreen!!,
                    end   = hoverScreen!!,
                    strokeWidth = 4f
                )
            }
        }

        // 5) 상단/하단 UI
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(12.dp)
                .zIndex(4f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val tip = if (firstPoint == null) "첫 번째 점을 탭하세요"
            else "두 번째 점을 조준(가운데 십자) 후 탭하세요"
            Text(tip, color = Color.White)

            if (firstPoint != null && hoverPoint != null) {
                val d = distanceMeters(firstPoint!!, hoverPoint!!)
                Text("현재 거리: ${"%.2f".format(d)} m", color = Color.White)
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
                .zIndex(4f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    detectionEnabled = true
                    // 평면 시각효과 숨김 + (선택) 탐지 비활성화로 가시성 확보
                    try { sceneView.planeRenderer.isVisible = false } catch (_: Exception) {}
                    try {
                        sceneView.configureSession { _, cfg ->
                            cfg.planeFindingMode = Config.PlaneFindingMode.DISABLED
                        }
                    } catch (_: Exception) {}
                },
                enabled = !detectionEnabled
            ) { Text("스피커 탐지 시작") }

            Spacer(Modifier.width(12.dp))

            Button(onClick = { finishAndGoRender() }) { Text("다음") }
        }

        // 6) 거리 라벨 선택 다이얼로그
        if (showLabelDialog) {
            AlertDialog(
                onDismissRequest = { showLabelDialog = false },
                title = { Text("거리 저장") },
                text = {
                    Column {
                        Text("측정값: ${"%.2f".format(lastDistanceM)} m")
                        Spacer(Modifier.height(8.dp))
                        listOf("너비", "깊이", "높이", "기타").forEach { label ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = (selectedLabel == label),
                                    onClick = { selectedLabel = label }
                                )
                                Text(label)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        vm.addLabeledMeasure(selectedLabel, lastDistanceM)
                        // 초기화(다음 측정을 위해)
                        firstPoint = null
                        hoverPoint = null
                        selectedLabel = labelOptions.first()
                        showLabelDialog = false
                    }) { Text("저장") }
                },
                dismissButton = {
                    TextButton(onClick = { showLabelDialog = false }) { Text("취소") }
                }
            )
        }
    }

    /* ── 프레임 루프 ─────────────────────────────────────────── */
    LaunchedEffect(sceneView, detector) {
        val det = detector ?: return@LaunchedEffect

        val updateCallback: (Session, Frame) -> Unit = update@{ _, frame ->
            // A) 탭 처리 (두 점 측정) — Depth-first
            if (tapQueue.isNotEmpty()) {
                val taps = tapQueue.toList()
                tapQueue.clear()

                taps.forEach { pt ->
                    val p = hitTestOrDepth(frame, pt.x, pt.y) ?: return@forEach
                    if (firstPoint == null) {
                        firstPoint = p
                    } else {
                        val dist = distanceMeters(firstPoint!!, p)
                        lastDistanceM = dist
                        showLabelDialog = true
                    }
                }
            }

            // B) 레티클(화면 중앙) — Depth-first 실시간 거리 + 월드→스크린 갱신
            run {
                val cx = overlayW / 2f
                val cy = overlayH / 2f
                val h = hitTestOrDepth(frame, cx, cy)
                hoverPoint = h
                hoverScreen = h?.let { worldToScreen(frame, it, overlayW.toFloat(), overlayH.toFloat()) }
            }

            // C) 첫 점도 월드→스크린 투영
            firstScreen = firstPoint?.let { worldToScreen(frame, it, overlayW.toFloat(), overlayH.toFloat()) }

            // D) 스피커 탐지 (YOLO) — 결과는 뷰 좌표 박스로 변환해 Overlay에 표시 + 월드화
            if (detectionEnabled) {
                val cpuImg = try { frame.acquireCameraImage() } catch (_: Exception) { null }
                if (cpuImg != null) {
                    if (bmpReuse == null || bmpReuse!!.width != cpuImg.width || bmpReuse!!.height != cpuImg.height) {
                        bmpReuse = Bitmap.createBitmap(cpuImg.width, cpuImg.height, Bitmap.Config.ARGB_8888)
                    }
                    val cpuW = cpuImg.width
                    yuvConverter.yuvToRgb(cpuImg, bmpReuse!!)
                    cpuImg.close()
                    val bmp = bmpReuse!!

                    val needRotate = ctx.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                    val rotatedBmp = if (needRotate) {
                        val m = Matrix().apply { postRotate(90f) }
                        Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
                    } else bmp

                    if (isBusy.compareAndSet(false, true)) {
                        val square = Bitmap.createScaledBitmap(rotatedBmp, det.inputSize, det.inputSize, false)
                        camExecutor.execute {
                            try {
                                val boxes = det.detect(square, rotatedBmp.width, rotatedBmp.height)
                                detQueue.offer(boxes)   // 회전된 이미지 좌표계 박스
                            } finally { isBusy.set(false) }
                        }
                    }

                    val polled = detQueue.poll()
                    if (polled != null) {
                        // IMAGE_PIXELS -> VIEW로 박스의 양 코너 변환
                        fun imgToView(x: Float, y: Float): Pair<Float, Float> {
                            val xo: Float; val yo: Float
                            if (needRotate) {
                                // rotated = postRotate(90°) 적용했던 좌표계를 원본 CPU 이미지로 역변환
                                xo = (cpuW - 1f) - y
                                yo = x
                            } else {
                                xo = x; yo = y
                            }
                            val inPts  = floatArrayOf(xo, yo)
                            val outPts = FloatArray(2)
                            frame.transformCoordinates2d(
                                Coordinates2d.IMAGE_PIXELS, inPts,
                                Coordinates2d.VIEW, outPts
                            )
                            return outPts[0] to outPts[1]
                        }

                        // ① VIEW 좌표 박스 리스트
                        val viewBoxes = polled.map { bb ->
                            val (x1v, y1v) = imgToView(bb.x1, bb.y1)
                            val (x2v, y2v) = imgToView(bb.x2, bb.y2)
                            val l = min(x1v, x2v); val r = max(x1v, x2v)
                            val t = min(y1v, y2v); val b = max(y1v, y2v)
                            bb.copy(x1 = l, y1 = t, x2 = r, y2 = b) // 이제 이 값은 VIEW 좌표
                        }

                        // ② 월드 좌표화 (각 박스 중심 → Depth-first)
                        viewBoxes.forEach { vb ->
                            val sx = (vb.x1 + vb.x2) * 0.5f
                            val sy = (vb.y1 + vb.y2) * 0.5f
                            val p = hitTestOrDepth(frame, sx, sy) ?: return@forEach
                            val world = floatArrayOf(p.x, p.y, p.z)
                            val id = SimpleTracker.assignId(world)
                            vm.upsertSpeaker(id, world, frame.timestamp)
                        }
                        vm.pruneSpeakers(frame.timestamp)

                        // ③ Overlay/VM 업데이트 — 오버레이도 VIEW 좌표 박스를 그대로 사용
                        uiScope.launch(Dispatchers.Main) {
                            overlayView?.setResults(viewBoxes)
                            vm.setSpeakerBoxes(viewBoxes.map { RectF(it.x1, it.y1, it.x2, it.y2) })
                        }
                    }
                }
            }
        }

        sceneView.onSessionUpdated = updateCallback
    }

    /* ── Disposable 정리 ─────────────────────────────────────── */
    DisposableEffect(Unit) {
        onDispose {
            detector?.close()
            camExecutor.shutdownNow()
            sceneView.onSessionUpdated = null
            sceneView.destroy()
        }
    }
}

/* ── 거리 계산 ─────────────────────────────────────────── */
private fun distanceMeters(a: Vec3, b: Vec3): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    val dz = a.z - b.z
    return sqrt(dx*dx + dy*dy + dz*dz)
}

/* ── Depth-first 보강 hitTest ───────────────────────────
   1) frame.hitTest(viewX,viewY) 성공 시 사용
   2) 실패 시 RawDepth 16bit + IMAGE_PIXELS 역투영 (DepthUtil 재사용)
*/
private fun hitTestOrDepth(frame: Frame, viewX: Float, viewY: Float): Vec3? {
    frame.hitTest(viewX, viewY).firstOrNull()?.let { h ->
        val p = h.hitPose
        return Vec3(p.tx(), p.ty(), p.tz())
    }
    val depth = try { frame.acquireRawDepthImage16Bits() } catch (_: Exception) { null } ?: return null
    return try {
        val inPts  = floatArrayOf(viewX, viewY)
        val outImg = FloatArray(2)
        frame.transformCoordinates2d(
            Coordinates2d.VIEW, inPts,
            Coordinates2d.IMAGE_PIXELS, outImg
        )
        val rect = RectF(outImg[0], outImg[1], outImg[0], outImg[1])
        val world = DepthUtil.bboxCenterToWorld(rect, depth, frame) ?: return null
        Vec3(world[0], world[1], world[2])
    } finally { try { depth.close() } catch (_: Exception) {} }
}

/* ── 월드 → 스크린 투영 ───────────────────────────────────
   ARCore 카메라의 View/Projection 행렬로 3D 점을 VIEW(픽셀) 좌표로 변환
*/
private fun worldToScreen(frame: Frame, p: Vec3, viewW: Float, viewH: Float): Offset? {
    val proj = FloatArray(16)
    val view = FloatArray(16)
    frame.camera.getProjectionMatrix(proj, 0, 0.1f, 100f)
    frame.camera.getViewMatrix(view, 0)

    // world -> view space
    val world = floatArrayOf(p.x, p.y, p.z, 1f)
    val viewV = FloatArray(4)
    GLM.multiplyMV(viewV, 0, view, 0, world, 0)

    // 카메라 뒤쪽 점 필터
    if (viewV[2] > -0.1f) return null

    // view -> clip
    val clip = FloatArray(4)
    GLM.multiplyMV(clip, 0, proj, 0, viewV, 0)

    val w = clip[3]
    if (abs(w) < 1e-5f) return null

    val ndcX = clip[0] / w
    val ndcY = clip[1] / w

    // NDC(-1..1) -> VIEW 픽셀
    val sx = (ndcX * 0.5f + 0.5f) * viewW
    val sy = (1f - (ndcY * 0.5f + 0.5f)) * viewH
    return Offset(sx, sy)
}
