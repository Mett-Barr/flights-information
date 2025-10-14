package moozy.flightinformation.presentation.mapper

import moozy.flightinformation.data.datasource.flights.dto.InstantScheduleDomesticArrivalDto.InstantScheduleDomesticArrivalDtoItem
import moozy.flightinformation.presentation.state.FlightArrivalItemUiModel
import kotlin.collections.map

/** 單一函數：DTO → UiModel（不做 enum，不碰資源/網路/DB） */
fun InstantScheduleDomesticArrivalDtoItem.toUiModel(): FlightArrivalItemUiModel {
    fun t(s: String?) = if (s.isNullOrBlank()) "--:--" else s
    val statusRaw = (airFlyStatus ?: "").trim()

    // 規範化一個「狀態鍵」（全大寫，中文/英文都吃），UI 用這個鍵決定顏色/樣式
    val key = when (statusRaw.uppercase()) {
        "抵達", "ARRIVED" -> "ARRIVED"
        "出登", "出發", "DEPARTED" -> "DEPARTED"
        "時間更改", "SCHEDULE CHANGE", "SCHEDULE_CHANGE" -> "SCHEDULE_CHANGE"
        "取消", "CANCELLED" -> "CANCELLED"
        "延誤", "DELAYED" -> "DELAYED"
        else -> "UNKNOWN"
    }

    // 展示字串（直接給畫面用）
    val statusText = when (key) {
        "ARRIVED" -> "抵達ARRIVED"
        "DEPARTED" -> "出發DEPARTED"
        "SCHEDULE_CHANGE" -> "時間更改SCHEDULE CHANGE"
        "CANCELLED" -> "取消CANCELLED"
        "DELAYED" -> "延誤DELAYED"
        else -> "狀態未知"
    }

    return FlightArrivalItemUiModel(
        scheduleText = "預計時間  ${t(expectTime)}",
        actualText = "實際時間  ${t(realTime)}",
        originCode = upAirportCode.orEmpty(),
        originName = upAirportName.orEmpty(),
        flightNo = airLineNum.orEmpty(),
        gateText = "航廈/登機門：${airBoardingGate?.ifBlank { "--" } ?: "--"}",
        statusText = statusText,
        statusKey = key,
        airlineLogoUrl = airLineLogo
    )
}

/** 清單版映射（方便直接丟進 UiState） */
fun List<InstantScheduleDomesticArrivalDtoItem>.toUiModels(): List<FlightArrivalItemUiModel> =
    map { it.toUiModel() }