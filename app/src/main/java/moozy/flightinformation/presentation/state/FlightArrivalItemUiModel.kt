package moozy.flightinformation.presentation.state

data class FlightArrivalItemUiModel(
    val scheduleText: String,      // "預計時間 HH:mm"
    val actualText: String,        // "實際時間 HH:mm" / "--:--"
    val originCode: String,        // "MZG"
    val originName: String,        // "澎湖"
    val flightNo: String,          // "B78690"
    val gateText: String,          // "航廈/登機門：17"
    val statusText: String,        // "出發DEPARTED" / "取消CANCELLED" …
    val statusKey: String,         // 規範後的鍵：e.g. "DEPARTED","CANCELLED","SCHEDULE_CHANGE","ARRIVED","UNKNOWN"
    val airlineLogoUrl: String?    // 圖片 URL
)
