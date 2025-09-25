package com.example.roomacoustic.viewmodel

import android.app.Application
import android.graphics.RectF
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomacoustic.data.AppDatabase
import com.example.roomacoustic.data.RoomEntity
import com.example.roomacoustic.model.Speaker3D
import com.example.roomacoustic.repo.RoomRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.roomacoustic.model.Vec3


import com.example.roomacoustic.model.Measure3DResult

class RoomViewModel(app: Application) : AndroidViewModel(app) {

    /* ------------------------------------------------------------
       1) 데이터베이스 / 레포지터리  &  방 리스트  (기존 코드)
       ------------------------------------------------------------ */
    private val repo = RoomRepository(AppDatabase.get(app).roomDao())

    val rooms = repo.rooms.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    val currentRoomId = MutableStateFlow<Int?>(null)
    fun select(roomId: Int) { currentRoomId.value = roomId }

    fun addRoom(title: String, onAdded: (Int) -> Unit) = viewModelScope.launch {
        onAdded(repo.add(title))
    }
    fun rename(id: Int, newTitle: String) = viewModelScope.launch { repo.rename(id, newTitle) }
    fun setMeasure(id: Int, flag: Boolean) = viewModelScope.launch { repo.setMeasure(id, flag) }
    fun setChat(id: Int, flag: Boolean)    = viewModelScope.launch { repo.setChat(id, flag) }
    fun delete(room: RoomEntity)           = viewModelScope.launch { repo.delete(room) }

    /* ------------------------------------------------------------
       2) MiDaS 측정값  (기존 코드)
       ------------------------------------------------------------ */
    private val _measuredDimensions = MutableStateFlow<Triple<Float, Float, Float>?>(null)
    val measuredDimensions: StateFlow<Triple<Float, Float, Float>?> = _measuredDimensions.asStateFlow()
    fun setMeasuredRoomDimensions(w: Float, h: Float, d: Float) {
        _measuredDimensions.value = Triple(w, h, d)
    }

    /* ------------------------------------------------------------
       3) YOLOv8 결과 (기존 코드)
       ------------------------------------------------------------ */
    private val _inferenceTime = MutableStateFlow<Long?>(null)
    val inferenceTime: StateFlow<Long?> = _inferenceTime.asStateFlow()
    fun setInferenceTime(ms: Long) { _inferenceTime.value = ms }

    private val _speakerBoxes = MutableStateFlow<List<RectF>>(emptyList())
    val speakerBoxes: StateFlow<List<RectF>> = _speakerBoxes.asStateFlow()
    fun setSpeakerBoxes(boxes: List<RectF>) { _speakerBoxes.value = boxes }

    /* ------------------------------------------------------------
       4) 종합 측정 결과 (기존 코드)
       ------------------------------------------------------------ */
    data class MeasureResult(
        val width: Float?,
        val height: Float?,
        val depth: Float?,
        val speakerBoxes: List<RectF>
    )
    private val _roomResult = MutableStateFlow<MeasureResult?>(null)
    val roomResult: StateFlow<MeasureResult?> = _roomResult.asStateFlow()
    fun setMeasureResult(w: Float?, h: Float?, d: Float?, boxes: List<RectF>) {
        _roomResult.value = MeasureResult(w, h, d, boxes)
    }

    fun deleteAllRooms() = viewModelScope.launch {
        repo.deleteAll()
        currentRoomId.value = null
    }

    /* ------------------------------------------------------------
       5) 3-D 스피커 좌표 관리  (기존 + 유지)
       ------------------------------------------------------------ */

    /** 실시간 추적 스피커 리스트 (Compose 에서 직접 관찰 가능) */
    private val _speakers = mutableStateListOf<Speaker3D>()
    val speakers: List<Speaker3D> get() = _speakers

    /**
     * YOLO + Depth 한 번 호출마다 실행.
     * 같은 id 가 이미 있으면 좌표·타임스탬프만 갱신, 없으면 새로 추가.
     */
    fun upsertSpeaker(id: Int, pos: FloatArray, frameNs: Long) {
        _speakers.firstOrNull { it.id == id }?.apply {
            worldPos   = pos
            lastSeenNs = frameNs
        } ?: _speakers.add(Speaker3D(id, pos, frameNs))
    }

    /**
     * 지정 시간(timeoutSec) 이상 보이지 않은 스피커를 삭제.
     * Camera Frame 타임스탬프(ns)와 비교.
     */
    fun pruneSpeakers(frameNs: Long, timeoutSec: Int = 3) {
        _speakers.removeAll { (frameNs - it.lastSeenNs) / 1e9 > timeoutSec }
    }

    /* ------------------------------------------------------------
       6) 🔵 AR 6점 측정 결과 (새로 추가)
          - 기존 API(setMeasureResult(w,h,d,boxes))와 별도로 운용
          - Compose/화면에서 수월히 구독하도록 StateFlow 사용
       ------------------------------------------------------------ */
    private val _measure3DResult = MutableStateFlow<Measure3DResult?>(null)
    val measure3DResult: StateFlow<Measure3DResult?> = _measure3DResult.asStateFlow()

    fun setMeasure3DResult(result: Measure3DResult) {
        _measure3DResult.value = result
    }

    fun clearMeasure3DResult() {
        _measure3DResult.value = null
    }

    // ---------- 라벨링된 길이 측정값 ----------
    data class LabeledMeasure(val label: String, val meters: Float)

    private val _labeledMeasures = MutableStateFlow<List<LabeledMeasure>>(emptyList())
    val labeledMeasures = _labeledMeasures.asStateFlow()
    fun addLabeledMeasure(label: String, meters: Float) {
        _labeledMeasures.value = _labeledMeasures.value + LabeledMeasure(label, meters)
    }
    fun clearLabeledMeasures() { _labeledMeasures.value = emptyList() }

    // (선택) 스피커 수동 2점(좌/우) 지정 저장
    private val _manualSpeakerPair = MutableStateFlow<Pair<Vec3, Vec3>?>(null)
    val manualSpeakerPair = _manualSpeakerPair.asStateFlow()
    fun setManualSpeakerPair(left: Vec3, right: Vec3) { _manualSpeakerPair.value = left to right }
    fun clearManualSpeakerPair() { _manualSpeakerPair.value = null }

    fun clearMeasureAndSpeakers() {
        // 라벨-값(폭/깊이/높이) 저장 구조가 무엇이든 여기서 초기화
        // 예) _labeledMeasures.value = emptyMap()
        // 혹은 _measure3DResult.value = null 등 프로젝트 구조에 맞게
        // 스피커 리스트도 초기화
        _speakers.clear()
        _speakerBoxes.value = emptyList()
    }

}
