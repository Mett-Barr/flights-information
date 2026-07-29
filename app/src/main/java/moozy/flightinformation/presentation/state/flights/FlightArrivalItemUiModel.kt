package moozy.flightinformation.presentation.state.flights


data class FlightArrivalItemUiModel(
    val headlineTimeText: String,   // 大字時間：realTime 優先，否則 expectTime，皆無則 "--:--"
    val expectedLabelText: String,  // "Expected HH:mm"（沒資料時 "Expected --:--"）
    val badgeText: String,          // 右上角徽章："On time" / "Delayed" / "Cancelled" / "Arrived" / "Departed" / "Schedule change" / "Unknown"
    val carrierLineText: String,    // "United Airlines · UA 1234"
    val departureText: String,      // "San Francisco (SFO)"
    val gateText: String,           // "Gate 42" / "Gate --"
    val aircraftText: String,       // "Boeing 737" / "--"
    val flightStatusText: String,   // 下方狀態行（可含延誤原因）例如 "On time"、"Delayed · Weather"
    val statusKey: String,          // "ARRIVED" | "DEPARTED" | "SCHEDULE_CHANGE" | "CANCELLED" | "DELAYED" | "ON_TIME" | "UNKNOWN"
    val isCancelled: Boolean = false,
    val airlineLogoUrl: String?     // Logo URL
)


val fakeFlightArrivalItem = FlightArrivalItemUiModel(
    // realTime (10:01) 優先於 expectTime (09:00)
    headlineTimeText = "10:01",
    // 預計時間
    expectedLabelText = "Expected 09:00",
    // 航班已抵達
    badgeText = "Arrived",
    // 航空公司名稱 + 航班號
    carrierLineText = "立榮航空 · B78690",
    // 起飛機場名稱 (代碼)
    departureText = "澎湖 (MZG)",
    // 原始數據 airBoardingGate 為 null，故填入預設值
    gateText = "Gate --",
    // 飛機型號，通常是縮寫 AT76 (ATR 72-600)
    aircraftText = "AT76",
    // 航班已抵達，沒有延誤原因，直接顯示狀態
    flightStatusText = "Arrived",
    // 狀態鍵值
    statusKey = "ARRIVED",
    // Logo URL
    airlineLogoUrl = "https://www.kia.gov.tw/images/ALL-square/B7.png"
)
