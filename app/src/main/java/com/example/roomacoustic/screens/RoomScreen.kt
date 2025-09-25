package com.example.roomacoustic.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu      // 햄버거 아이콘
import androidx.compose.material.icons.filled.Close    // X 아이콘
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.roomacoustic.data.RoomEntity
import com.example.roomacoustic.navigation.Screen
import com.example.roomacoustic.screens.components.StatusChips
import com.example.roomacoustic.screens.dialog.EditRoomDialog
import com.example.roomacoustic.viewmodel.RoomViewModel
import com.example.roomacoustic.yolo.BoundingBox
import com.example.roomacoustic.yolo.Constants
import com.example.roomacoustic.yolo.Detector
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers


import androidx.compose.ui.Alignment
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RoomScreen(
    nav: NavController,
    vm: RoomViewModel
) {
    val appCtx = LocalContext.current.applicationContext

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {                   // BG thread
            Detector(
                context = appCtx,
                modelPath = Constants.MODEL_PATH,
                labelPath = Constants.LABELS_PATH,
                detectorListener = object : Detector.DetectorListener {
                    override fun onEmptyDetect() {}
                    override fun onDetect(b: List<BoundingBox>, i: Long) {}
                },
                message = {}
            ).apply {
                restart(useGpu = true)   // GPU delegate attach
                warmUp()                // ★ 첫 run() = shader compile
                close()                 // Interpreter 종료, delegate 캐시는 남음
            }
        }
    }

    val rooms by vm.rooms.collectAsState()

    /* UI 플래그 */
    var showCreate      by remember { mutableStateOf(false) }
    var tappedRoom      by remember { mutableStateOf<RoomEntity?>(null) } // 짧게 터치
    var longPressedRoom by remember { mutableStateOf<RoomEntity?>(null) } // 길게 터치
    var showEditDialog  by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var roomToDelete by remember { mutableStateOf<RoomEntity?>(null) }
    var showNameConflictAlert by remember { mutableStateOf(false) }

    var showDeleteAllConfirm  by remember { mutableStateOf(false) }
    var isLongPress           by remember { mutableStateOf(false) }

    var fabExpanded by remember { mutableStateOf(false) }   // 메뉴 열림 여부




    /* ───── 리스트 화면 ───── */
    Scaffold(
        floatingActionButton = {

            val fabSize = 56.dp          // 메인 FAB 크기
            val gap     = 12.dp          // 메인 ↔ Mini FAB 간격
            val density = LocalDensity.current
            val expandedOffsetPx = with(density) { -(fabSize + gap).roundToPx() }

            /* 애니메이션할 Y 오프셋: 0(겹침) ↔ -expandedOffsetPx(위로 올라감) */
            val offsetY by animateIntAsState(
                targetValue = if (fabExpanded) expandedOffsetPx else 0,
                animationSpec = tween(250),
                label = "MiniFABOffset"
            )

            Box(contentAlignment = Alignment.BottomEnd) {

                /* ── Mini FAB Column ── */
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier
                        .offset { IntOffset(0, offsetY) }     // ★ 오프셋 애니메이션
                        .alpha(if (fabExpanded) 1f else 0f)   // 접히면 완전 투명
                        .zIndex(-1f)                          // ★ 메인 FAB 뒤로
                ) {
                    FloatingActionButton(
                        onClick = {
                            fabExpanded = false
                            showCreate = true
                        }
                    ) { Icon(Icons.Default.Add, "새 방") }

                    FloatingActionButton(
                        onClick = {
                            fabExpanded = false
                            showDeleteAllConfirm = true
                        },
                    ) { Icon(Icons.Default.Close, "모든 방 삭제") }
                }

                /* ── 메인 햄버거 FAB ── */
                FloatingActionButton(
                    onClick = { fabExpanded = !fabExpanded }
                ) { Icon(Icons.Default.Menu, "메뉴") }
            }
        }




    ) { pad ->
        LazyColumn(contentPadding = pad) {
            items(rooms, key = { it.id }) { room ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .combinedClickable(
                            onClick     = { tappedRoom = room },
                            onLongClick = { longPressedRoom = room }
                        )
                ) {
                    ListItem(
                        headlineContent = { Text(room.title) },
                        trailingContent = { StatusChips(room.hasMeasure, room.hasChat) },
                        supportingContent = {
                            val sub = room.lastChatPreview
                                ?: if (room.hasMeasure) "측정 완료" else "미측정"
                            Text(sub)
                        }
                    )
                }
            }
        }
    }

    /* ───── 새 방 만들기 다이얼로그 ───── */
    if (showCreate) {
        EditRoomDialog(
            onConfirm = { title ->
                val isDuplicate = rooms.any { it.title == title }
                if (isDuplicate) false
                else {
                    vm.addRoom(title) { id -> vm.select(id) }
                    showCreate = false
                    true
                }
            },
            onDismiss = { showCreate = false }
        )
    }


    /* ───── 짧게 터치 모달 (상태 기반) ───── */
    tappedRoom?.let { room ->
        vm.select(room.id)
        ModalBottomSheet(onDismissRequest = { tappedRoom = null }) {
            if (!room.hasMeasure) {
                SheetItem("측정 시작") {
                    tappedRoom = null
                    vm.clearMeasureAndSpeakers()
                    nav.navigate(Screen.MeasureGraph.route)
                }
            } else {
                SheetItem("측정 결과 보기") {
                    tappedRoom = null
                    nav.navigate("${Screen.Render.route}?detected=true")
                }
            }
            if (!room.hasChat) {
                SheetItem("새 대화") {
                    tappedRoom = null
                    nav.navigate("${Screen.NewChat.route.replace("{roomId}", room.id.toString())}")
                }
            } else {
                SheetItem("기존 대화 이어가기") {
                    tappedRoom = null
                    nav.navigate("${Screen.ExChat.route.replace("{roomId}", room.id.toString())}")
                }
            }
        }
    }

    // 삭제 확인 Dialog 추가
    if (showDeleteConfirm && roomToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirm = false
                roomToDelete = null
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(roomToDelete!!)
                    showDeleteConfirm = false
                    roomToDelete = null
                }) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    roomToDelete = null
                }) {
                    Text("취소")
                }
            },
            title = { Text("방 삭제") },
            text = { Text("정말로 '${roomToDelete?.title ?: "이 방"}' 방을 삭제하시겠습니까?") }
        )
    }

    /* ───── 길게 터치 옵션 시트 ───── */
    longPressedRoom?.let { room ->
        vm.select(room.id)
        ModalBottomSheet(onDismissRequest = { longPressedRoom = null }) {
            SheetItem("방 이름 바꾸기") {
                longPressedRoom = null
                showEditDialog = true
            }
            SheetItem("재측정하기") {
                vm.setMeasure(room.id, false)
                longPressedRoom = null
                vm.clearMeasureAndSpeakers()
                nav.navigate(Screen.MeasureGraph.route)
            }
            SheetItem("대화 초기화") {
                vm.setChat(room.id, false)
                longPressedRoom = null
            }
            SheetItem("삭제", isDestructive = true) {
                roomToDelete = room
                showDeleteConfirm = true
                longPressedRoom = null
            }

        }
    }

    /* 이름 변경 다이얼로그 */
    if (showEditDialog) {
        val current = vm.currentRoomId.collectAsState().value
            ?.let { id -> rooms.firstOrNull { it.id == id } }

        if (current != null) {
            EditRoomDialog(
                title = "방 이름 변경",
                default = current.title,
                onConfirm = { newTitle ->
                    val isDuplicate = rooms.any { it.title == newTitle && it.id != current.id }
                    if (isDuplicate) false
                    else {
                        vm.rename(current.id, newTitle)
                        showEditDialog = false
                        true
                    }
                },
                onDismiss = { showEditDialog = false }
            )
        } else showEditDialog = false
    }


    if (showNameConflictAlert) {
        AlertDialog(
            onDismissRequest = { showNameConflictAlert = false },
            confirmButton = {
                TextButton(onClick = { showNameConflictAlert = false }) {
                    Text("확인")
                }
            },
            title = { Text("중복된 이름") },
            text = { Text("같은 이름의 방이 이미 존재합니다.") }
        )
    }

    /* ───── 모든 방 삭제 다이얼로그 ───── */
    if (showDeleteAllConfirm) {
        AlertDialog(
            onDismissRequest = {
                showDeleteAllConfirm = false
                fabExpanded = false        // ← 변수만 교체
            },
            title = { Text("모든 방 삭제") },
            text  = { Text("정말로 모든 방을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteAllRooms()
                    showDeleteAllConfirm = false
                    fabExpanded = false    // ← 여기에서도
                }) { Text("삭제") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteAllConfirm = false
                    fabExpanded = false    // ← 여기에서도
                }) { Text("취소") }
            }
        )
    }



}

/* BottomSheet 항목 공용 */
@Composable
private fun SheetItem(
    text: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text,
                color = if (isDestructive)
                    MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}
