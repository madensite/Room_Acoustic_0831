package com.example.roomacoustic.screens.measure

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.roomacoustic.model.Vec3
import com.example.roomacoustic.navigation.Screen
import com.example.roomacoustic.viewmodel.RoomViewModel
import kotlin.math.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import androidx.compose.ui.graphics.Color

/** ───────────────── 변경 요약 ─────────────────
 *  - OpenGL ES 2.0 기반 3D 뷰포트(RoomViewport3DGL) 추가
 *  - 중앙 반투명 직육면체 + 스피커 점 + 오빗/줌 제스처
 *  - 하단 요약(W/D/H) + [상세정보] 모달(측정값/편집)
 *  - 월드→로컬 변환은 기존 축 규칙(W=X, D=Z, H=Y) 유지
 */

data class RoomSize(val w: Float, val d: Float, val h: Float)

/* ──────────────────────────────────────────── */
/* RenderScreen                                 */
/* ──────────────────────────────────────────── */

@Composable
fun RenderScreen(
    nav: NavController,
    vm: RoomViewModel,
    detected: Boolean
) {
    val roomId = vm.currentRoomId.collectAsState().value
    if (roomId == null) {
        Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
        return
    }

    val labeled = vm.labeledMeasures.collectAsState().value
    val speakers = vm.speakers
    val frame3D  = vm.measure3DResult.collectAsState().value

    val bannerColor = if (detected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.error

    // 자동 추론 + 수동 입력 우선
    val autoRoomSize: RoomSize? = remember(labeled) { inferRoomSizeFromLabels(labeled) }
    var manualRoomSize by rememberSaveable { mutableStateOf<RoomSize?>(null) }
    val roomSize = manualRoomSize ?: autoRoomSize

    // 월드→로컬(W, H, D) 변환 람다
    // 기존 (문제): val m = frame3D ?: return@remember null
    val toLocal: (FloatArray) -> Triple<Float, Float, Float>? = remember(frame3D) {
        { p ->
            val m = frame3D
            if (m == null) {
                null
            } else {
                val origin = m.frame.origin
                val vx = m.frame.vx
                val vy = m.frame.vy
                val vz = m.frame.vz
                val d = Vec3(p[0], p[1], p[2]) - origin
                Triple(d.dot(vx), d.dot(vy), d.dot(vz))
            }
        }
    }


    // 스피커 로컬 좌표 목록 (W,H,D)
    val speakersLocal = remember(speakers, frame3D) {
        speakers.mapNotNull { sp ->
            toLocal(sp.worldPos)?.let { (w, h, d) -> Vec3(w, h, d) }
        }
    }

    var showInput by rememberSaveable { mutableStateOf(false) }
    var showDetail by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        /* 중앙 3D 뷰포트 */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (roomSize != null) {
                RoomViewport3DGL(
                    room = roomSize,
                    speakersLocal = speakersLocal,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .aspectRatio(1.2f)
                )
            } else {
                Text(
                    text = "방 크기(W/D/H)가 없어 3D 미리보기를 표시할 수 없습니다. (터치하여 직접 입력)",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clickable { showInput = true }
                )
            }
        }

        /* 하단 요약 + 상세정보 버튼 */
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (roomSize != null) {
                Text(
                    "W ${"%.2f".format(roomSize.w)}m · D ${"%.2f".format(roomSize.d)}m · H ${"%.2f".format(roomSize.h)}m",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFEEEEEE) // 밝은 회색 고정
                )
            } else {
                Text("W/D/H 미지정", style = MaterialTheme.typography.titleMedium)
            }
            Row {
                TextButton(onClick = { showDetail = true }) { Text("상세정보") }
                TextButton(onClick = { showInput = true }) { Text("직접 입력/편집") }
            }
        }

        Spacer(Modifier.height(8.dp))

        /* 중앙 배너 (간결) */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text  = if (detected) "스피커 탐지 완료" else "스피커 미탐지",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = bannerColor
            )
        }

        Spacer(Modifier.height(8.dp))

        /* 하단 우측 '다음' 버튼 */
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(onClick = { nav.navigate(Screen.TestGuide.route) }) { Text("다음") }
        }
    }

    /* 상세정보 모달 */
    if (showDetail) {
        AlertDialog(
            onDismissRequest = { showDetail = false },
            title = { Text("상세정보") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("측정값")
                    if (labeled.isEmpty()) Text("저장된 길이 측정값이 없습니다.")
                    else labeled.forEach { m -> Text("• ${m.label}: ${"%.2f".format(m.meters)} m") }

                    Spacer(Modifier.height(8.dp))
                    Text("프레임/좌표계")
                    if (frame3D == null) Text("좌표 프레임 없음")
                    else {
                        val f = frame3D.frame
                        Text("origin = (${fmt(f.origin.x)}, ${fmt(f.origin.y)}, ${fmt(f.origin.z)})")
                        Text("vx = (${fmt(f.vx.x)}, ${fmt(f.vx.y)}, ${fmt(f.vx.z)})")
                        Text("vy = (${fmt(f.vy.x)}, ${fmt(f.vy.y)}, ${fmt(f.vy.z)})")
                        Text("vz = (${fmt(f.vz.x)}, ${fmt(f.vz.y)}, ${fmt(f.vz.z)})")
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("스피커(로컬)")
                    if (speakersLocal.isEmpty()) Text("감지된 스피커 없음")
                    else speakersLocal.forEachIndexed { i, p ->
                        Text("• #${i + 1} (W,D,H)=(${fmt(p.x)}, ${fmt(p.z)}, ${fmt(p.y)}) m")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDetail = false }) { Text("닫기") } }
        )
    }

    /* 수동 입력 다이얼로그 */
    if (showInput) {
        RoomSizeInputDialog(
            initial = manualRoomSize ?: autoRoomSize,
            onDismiss = { showInput = false },
            onConfirm = { w, d, h ->
                manualRoomSize = RoomSize(w, d, h)
                showInput = false
            }
        )
    }
}

/* ──────────────────────────────────────────── */
/* 강건한 레이블 매칭                          */
/* ──────────────────────────────────────────── */

private fun normalizeLabel(s: String): String =
    s.lowercase().replace("\\s+".toRegex(), "")
        .replace("[()\\[\\]{}:：=~_\\-]".toRegex(), "")

private val W_KEYS = setOf("w", "width", "가로", "폭", "넓이")
private val D_KEYS = setOf("d", "depth", "세로", "길이", "방길이", "방깊이", "전장", "장변")
private val H_KEYS = setOf("h", "height", "높이", "천장", "층고")

private fun inferRoomSizeFromLabels(
    labeled: List<RoomViewModel.LabeledMeasure>
): RoomSize? {
    if (labeled.isEmpty()) return null
    fun pick(keys: Set<String>): Float? =
        labeled.firstOrNull { m ->
            val norm = normalizeLabel(m.label)
            keys.any { k -> norm.contains(k) || k.contains(norm) }
        }?.meters
    val w = pick(W_KEYS); val d = pick(D_KEYS); val h = pick(H_KEYS)
    return if (w != null && d != null && h != null) RoomSize(w, d, h) else null
}

/* ──────────────────────────────────────────── */
/* 수동 입력                                   */
/* ──────────────────────────────────────────── */

@Composable
private fun RoomSizeInputDialog(
    initial: RoomSize?,
    onDismiss: () -> Unit,
    onConfirm: (Float, Float, Float) -> Unit
) {
    var wText by rememberSaveable { mutableStateOf(initial?.w?.toString() ?: "") }
    var dText by rememberSaveable { mutableStateOf(initial?.d?.toString() ?: "") }
    var hText by rememberSaveable { mutableStateOf(initial?.h?.toString() ?: "") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("방 크기 직접 입력 (미터)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = wText, onValueChange = { wText = it },
                    label = { Text("가로 W (m)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number, imeAction = ImeAction.Next
                    )
                )
                OutlinedTextField(
                    value = dText, onValueChange = { dText = it },
                    label = { Text("세로 D (m)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number, imeAction = ImeAction.Next
                    )
                )
                OutlinedTextField(
                    value = hText, onValueChange = { hText = it },
                    label = { Text("높이 H (m)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number, imeAction = ImeAction.Done
                    )
                )
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val w = wText.toFloatOrNull()
                val d = dText.toFloatOrNull()
                val h = hText.toFloatOrNull()
                if (w == null || d == null || h == null || w <= 0 || d <= 0 || h <= 0) {
                    error = "모든 값을 0보다 큰 숫자로 입력하세요."
                } else onConfirm(w, d, h)
            }) { Text("확인") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

/* ──────────────────────────────────────────── */
/* 3D Viewport (OpenGL ES 2.0)                 */
/* ──────────────────────────────────────────── */

@Composable
private fun RoomViewport3DGL(
    room: RoomSize,
    speakersLocal: List<Vec3>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 오빗 상태
    var yaw by rememberSaveable { mutableStateOf(30f) }    // 좌우
    var pitch by rememberSaveable { mutableStateOf(20f) }  // 상하
    var zoom by rememberSaveable { mutableStateOf(1.0f) }  // 0.5 ~ 3.0

    // GLSurfaceView + Renderer
    AndroidView(
        modifier = modifier
            .pointerInput(Unit) {
                // 제스처: 회전(드래그), 줌(핀치)
                detectTransformGestures { _, pan, zoomChange, _ ->
                    yaw += pan.x * 0.3f
                    pitch = (pitch + (-pan.y * 0.3f)).coerceIn(-80f, 80f)
                    zoom = (zoom * zoomChange).coerceIn(0.5f, 3.0f)
                }
            },
        factory = {
            GLSurfaceView(context).apply {
                setEGLContextClientVersion(2)
                val r = GLRoomRenderer()
                setRenderer(r)
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                tag = r
            }
        },
        update = { view ->
            val r = view.tag as GLRoomRenderer
            r.setRoom(room)
            r.setSpeakers(speakersLocal)
            r.setOrbit(yaw, pitch, zoom)
            view.requestRender()
        }
    )
}

/* ──────────────────────────────────────────── */
/* OpenGL ES 2.0 Renderer                      */
/* ──────────────────────────────────────────── */

private class GLRoomRenderer : GLSurfaceView.Renderer {

    // 쉐이더
    private val vsh = """
        attribute vec3 aPos;
        uniform mat4 uMVP;
        uniform float uPointSize;
        void main(){
            gl_Position = uMVP * vec4(aPos, 1.0);
            gl_PointSize = uPointSize;
        }
    """.trimIndent()

    private val fsh = """
        precision mediump float;
        uniform vec4 uColor;
        void main(){ gl_FragColor = uColor; }
    """.trimIndent()

    // GL 핸들
    private var prog = 0
    private var aPos = 0
    private var uMVP = 0
    private var uColor = 0
    private var uPointSize = 0

    // 매트릭스
    private val proj = FloatArray(16)
    private val view = FloatArray(16)
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)

    // 룸/스피커
    @Volatile private var room: RoomSize? = null
    @Volatile private var speakers: FloatArray = floatArrayOf()  // 연속 xyz

    // 오빗 상태
    @Volatile private var yaw = 30f
    @Volatile private var pitch = 20f
    @Volatile private var zoom = 1.0f

    // 버텍스(박스 면/엣지)
    private var roomTriangles = floatArrayOf()
    private var roomEdges = floatArrayOf()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        prog = linkProgram(vsh, fsh)
        aPos = GLES20.glGetAttribLocation(prog, "aPos")
        uMVP = GLES20.glGetUniformLocation(prog, "uMVP")
        uColor = GLES20.glGetUniformLocation(prog, "uColor")
        uPointSize = GLES20.glGetUniformLocation(prog, "uPointSize")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / max(height, 1)
        Matrix.perspectiveM(proj, 0, 45f, aspect, 0.05f, 100f)
    }

    override fun onDrawFrame(unused: javax.microedition.khronos.opengles.GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val room = room ?: return
        ensureRoomGeometry(room)

        // 카메라: 방 크기에 따라 거리 자동 산정
        val radius = 0.5f * sqrt(room.w*room.w + room.h*room.h + room.d*room.d)
        val camDist = (radius * 2.2f) / zoom

        // 오빗 카메라 위치
        val yawRad = Math.toRadians(yaw.toDouble()).toFloat()
        val pitchRad = Math.toRadians(pitch.toDouble()).toFloat()
        val cx = (camDist * cos(pitchRad) * sin(yawRad))
        val cy = (camDist * sin(pitchRad))
        val cz = (camDist * cos(pitchRad) * cos(yawRad))

        // 룸을 원점에 두기 위해 중심 이동
        Matrix.setLookAtM(
            view, 0,
            cx, cy, cz,
            0f, 0f, 0f,
            0f, 1f, 0f
        )
        Matrix.setIdentityM(model, 0)

        // 공통 셰이더 설정
        GLES20.glUseProgram(prog)

        // ---- 반투명 면(삼각형) ----
        Matrix.multiplyMM(mvp, 0, view, 0, model, 0)
        Matrix.multiplyMM(mvp, 0, proj, 0, mvp, 0)
        GLES20.glUniformMatrix4fv(uMVP, 1, false, mvp, 0)
        GLES20.glUniform4f(uColor, 1f, 1f, 1f, 0.18f)  // 반투명 흰색
        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 0, roomTriangles.toBuffer())
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, roomTriangles.size / 3)

        // ---- 엣지(라인) ----
        GLES20.glUniform4f(uColor, 1f, 1f, 1f, 0.65f)
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 0, roomEdges.toBuffer())
        GLES20.glLineWidth(2f)
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, roomEdges.size / 3)

        // ---- 스피커(포인트) ----
        if (speakers.isNotEmpty()) {
            GLES20.glUniform4f(uColor, 1f, 0.6f, 0.2f, 1f)
            GLES20.glUniform1f(uPointSize, 18f)
            GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 0, speakers.toBuffer())
            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, speakers.size / 3)
        }

        GLES20.glDisableVertexAttribArray(aPos)
    }

    fun setRoom(room: RoomSize) {
        this.room = room
        // 지오메트리 갱신은 다음 프레임에 ensureRoomGeometry에서 처리
    }

    fun setSpeakers(list: List<Vec3>) {
        // 로컬 좌표(W,H,D)를 원점 중심으로 변환
        val r = room ?: return
        val cx = r.w * 0.5f
        val cy = r.h * 0.5f
        val cz = r.d * 0.5f
        val arr = FloatArray(list.size * 3)
        list.forEachIndexed { i, p ->
            arr[i*3 + 0] = p.x - cx
            arr[i*3 + 1] = p.y - cy
            arr[i*3 + 2] = p.z - cz
        }
        this.speakers = arr
    }

    fun setOrbit(yaw: Float, pitch: Float, zoom: Float) {
        this.yaw = yaw
        this.pitch = pitch
        this.zoom = zoom
    }

    private fun ensureRoomGeometry(room: RoomSize) {
        if (roomTriangles.isNotEmpty()) return

        // 중심 원점 기준 half-extent
        val hx = room.w * 0.5f
        val hy = room.h * 0.5f
        val hz = room.d * 0.5f

        // 8 꼭짓점
        val v000 = floatArrayOf(-hx, -hy, -hz)
        val v100 = floatArrayOf( hx, -hy, -hz)
        val v110 = floatArrayOf( hx,  hy, -hz)
        val v010 = floatArrayOf(-hx,  hy, -hz)
        val v001 = floatArrayOf(-hx, -hy,  hz)
        val v101 = floatArrayOf( hx, -hy,  hz)
        val v111 = floatArrayOf( hx,  hy,  hz)
        val v011 = floatArrayOf(-hx,  hy,  hz)

        // 12개 삼각형(각 면 2개)
        roomTriangles = floatArrayOf(
            // -Z (뒷면)
            *v000, *v100, *v110,   *v000, *v110, *v010,
            // +Z (앞면)
            *v001, *v101, *v111,   *v001, *v111, *v011,
            // -X
            *v000, *v001, *v011,   *v000, *v011, *v010,
            // +X
            *v100, *v101, *v111,   *v100, *v111, *v110,
            // -Y (바닥)
            *v000, *v100, *v101,   *v000, *v101, *v001,
            // +Y (천장)
            *v010, *v110, *v111,   *v010, *v111, *v011
        )

        // 엣지(라인)
        roomEdges = floatArrayOf(
            // 아래 사각
            *v000, *v100,  *v100, *v101,  *v101, *v001,  *v001, *v000,
            // 위 사각
            *v010, *v110,  *v110, *v111,  *v111, *v011,  *v011, *v010,
            // 기둥
            *v000, *v010,  *v100, *v110,  *v101, *v111,  *v001, *v011
        )
    }

    /* ── GL 유틸 ── */
    private fun linkProgram(vSrc: String, fSrc: String): Int {
        fun compile(type: Int, src: String): Int {
            val s = GLES20.glCreateShader(type)
            GLES20.glShaderSource(s, src)
            GLES20.glCompileShader(s)
            val ok = IntArray(1)
            GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0)
            if (ok[0] != GLES20.GL_TRUE) {
                val log = GLES20.glGetShaderInfoLog(s)
                GLES20.glDeleteShader(s)
                throw RuntimeException("Shader compile error: $log")
            }
            return s
        }
        val vs = compile(GLES20.GL_VERTEX_SHADER, vSrc)
        val fs = compile(GLES20.GL_FRAGMENT_SHADER, fSrc)
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, vs)
        GLES20.glAttachShader(p, fs)
        GLES20.glLinkProgram(p)
        val ok = IntArray(1)
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0)
        if (ok[0] != GLES20.GL_TRUE) {
            val log = GLES20.glGetProgramInfoLog(p)
            GLES20.glDeleteProgram(p)
            throw RuntimeException("Program link error: $log")
        }
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)
        return p
    }
}

/* ──────────────────────────────────────────── */
/* 확장/수학 유틸                               */
/* ──────────────────────────────────────────── */

private fun fmt(v: Float) = String.format("%.2f", v)
private operator fun Vec3.minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
private fun Vec3.dot(o: Vec3) = x * o.x + y * o.y + z * o.z

// FloatArray → NIO Buffer (간단 헬퍼)
private fun FloatArray.toBuffer(): java.nio.FloatBuffer =
    java.nio.ByteBuffer.allocateDirect(this.size * 4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer().apply {
        put(this@toBuffer); position(0)
    }
