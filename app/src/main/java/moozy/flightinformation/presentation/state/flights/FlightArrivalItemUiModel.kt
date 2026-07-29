package moozy.flightinformation.presentation.state.flights

import androidx.annotation.StringRes
import moozy.flightinformation.R

sealed interface FlightStatusText {
    data class Resource(
        @get:StringRes
        val id: Int,
        val formatArgument: String? = null,
    ) : FlightStatusText

    data class Raw(val value: String) : FlightStatusText
}

data class FlightArrivalItemUiModel(
    val headlineTimeText: String,   // 大字時間：realTime 優先，否則 expectTime，皆無則 "--:--"
    val scheduledTimeText: String,  // 預計時間未交給 mapper 組成文案，讓畫面依語系決定格式。
    val badgeText: FlightStatusText,
    val carrierLineText: String,    // "United Airlines · UA 1234"
    val departureText: String,      // "San Francisco (SFO)"
    val gate: String?,              // null 代表沒有登機門資料，避免以文案作為狀態判斷。
    val aircraftText: String,       // "Boeing 737" / "--"
    val flightStatusText: FlightStatusText,
    val statusKey: String,          // "ARRIVED" | "DEPARTED" | "SCHEDULE_CHANGE" | "CANCELLED" | "DELAYED" | "ON_TIME" | "UNKNOWN"
    val isCancelled: Boolean = false,
    val airlineLogoUrl: String?     // Logo URL
)


val fakeFlightArrivalItem = FlightArrivalItemUiModel(
    // realTime (10:01) 優先於 expectTime (09:00)
    headlineTimeText = "10:01",
    // 預計時間
    scheduledTimeText = "09:00",
    // 航班已抵達
    badgeText = FlightStatusText.Resource(R.string.flight_status_arrived),
    // 航空公司名稱 + 航班號
    carrierLineText = "立榮航空 · B78690",
    // 起飛機場名稱 (代碼)
    departureText = "澎湖 (MZG)",
    // 原始數據 airBoardingGate 為 null，故不顯示登機門
    gate = null,
    // 飛機型號，通常是縮寫 AT76 (ATR 72-600)
    aircraftText = "AT76",
    // 航班已抵達，沒有延誤原因，直接顯示狀態
    flightStatusText = FlightStatusText.Resource(R.string.flight_status_arrived),
    // 狀態鍵值
    statusKey = "ARRIVED",
    // Logo URL
    airlineLogoUrl = "https://www.kia.gov.tw/images/ALL-square/B7.png"
)
