package moozy.flightinformation.presentation.mapper

import moozy.flightinformation.data.datasource.flights.dto.InstantScheduleDomesticArrivalDto.InstantScheduleDomesticArrivalDtoItem
import moozy.flightinformation.presentation.state.flights.FlightArrivalItemUiModel
import kotlin.collections.map


fun InstantScheduleDomesticArrivalDtoItem.toUiModel(): FlightArrivalItemUiModel {
    fun clean(s: String?) = s?.trim().orEmpty()
    fun nnTime(s: String?) = clean(s).ifBlank { "--:--" }

    val expected = nnTime(expectTime)
    val actualOrExpected = clean(realTime).ifBlank { expected }

    // -------- 狀態正規化（中英皆吃）--------
    val statusRaw = clean(airFlyStatus)
    val upper = statusRaw.uppercase()
    val isOnTimeWord = listOf("ON TIME", "ONTIME", "ON-TIME", "準時", "準點", "正常", "準點到達", "準點出發")
        .any { upper.contains(it) }

    val statusKey = when {
        upper == "ARRIVED" || statusRaw == "抵達" -> "ARRIVED"
        upper == "DEPARTED" || statusRaw in listOf("出發", "出登") -> "DEPARTED"
        upper.replace(" ", "_") == "SCHEDULE_CHANGE" || statusRaw == "時間更改" -> "SCHEDULE_CHANGE"
        upper == "CANCELLED" || statusRaw == "取消" -> "CANCELLED"
        upper == "DELAYED" || statusRaw == "延誤" -> "DELAYED"
        isOnTimeWord -> "ON_TIME"
        else -> "UNKNOWN"
    }

    val badgeText = when (statusKey) {
        "ARRIVED" -> "Arrived"
        "DEPARTED" -> "Departed"
        "SCHEDULE_CHANGE" -> "Schedule change"
        "CANCELLED" -> "Cancelled"
        "DELAYED" -> "Delayed"
        "ON_TIME" -> "On time"
        else -> "Unknown"
    }

    // -------- 航空公司＋班號（避免重複代碼）--------
    val code = clean(airLineCode)
    val numRaw = clean(airLineNum)
    val flightNo = when {
        numRaw.startsWith(code) -> numRaw
        code.isNotBlank() && numRaw.isNotBlank() -> "$code $numRaw"
        numRaw.isNotBlank() -> numRaw
        else -> "--"
    }
    val airline = clean(airLineName).ifBlank { code.ifBlank { "Airline" } }
    val carrierLine = "$airline · $flightNo"

    // -------- 出發地（含 IATA）--------
    val depName = clean(upAirportName)
    val depCode = clean(upAirportCode)
    val departure = when {
        depName.isNotBlank() && depCode.isNotBlank() -> "$depName ($depCode)"
        depName.isNotBlank() -> depName
        depCode.isNotBlank() -> depCode
        else -> "--"
    }

    // -------- 登機門 / 機型 --------
    val gate = clean(airBoardingGate).ifBlank { "--" }
    val aircraft = clean(airPlaneType).ifBlank { "--" }

    // -------- 下方狀態行（可帶原因）--------
    val cause = clean(airFlyDelayCause)
    val statusLine = when (statusKey) {
        "DELAYED" -> if (cause.isNotBlank()) "Delayed · $cause" else "Delayed"
        "SCHEDULE_CHANGE" -> "Schedule change"
        "CANCELLED" -> "Cancelled"
        "ARRIVED" -> if (isOnTimeWord) "Arrived · On time" else "Arrived"
        "DEPARTED" -> if (isOnTimeWord) "Departed · On time" else "Departed"
        "ON_TIME" -> "On time"
        else -> if (statusRaw.isNotBlank()) statusRaw else "Status unknown"
    }

    return FlightArrivalItemUiModel(
        headlineTimeText = actualOrExpected,
        expectedLabelText = "Expected $expected",
        badgeText = badgeText,
        carrierLineText = carrierLine,
        departureText = departure,
        gateText = "Gate $gate",
        aircraftText = aircraft,
        flightStatusText = statusLine,
        statusKey = statusKey,
        airlineLogoUrl = airLineLogo
    )
}

/** 清單版映射（方便直接丟進 UiState） */
fun List<InstantScheduleDomesticArrivalDtoItem>.toUiModels(): List<FlightArrivalItemUiModel> =
    map { it.toUiModel() }