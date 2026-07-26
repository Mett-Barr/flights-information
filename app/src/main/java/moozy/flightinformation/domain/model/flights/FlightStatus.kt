package moozy.flightinformation.domain.model.flights

/**
 * 航班狀態。來源 API 以中英混雜的自由字串表達（"抵達"、"ARRIVED"、"延誤"…），
 * 在 data 層正規化成這組有限狀態，UI 不再需要認得那些原始字串。
 *
 * 顏色、圖示、文案屬於呈現決策，不在這裡。
 */
sealed interface FlightStatus {
    data object OnTime : FlightStatus
    data object Arrived : FlightStatus
    data object Departed : FlightStatus
    data object Delayed : FlightStatus
    data object Cancelled : FlightStatus
    data object ScheduleChanged : FlightStatus

    /** 來源出現沒見過的狀態時保留原文，方便除錯，也讓 UI 至少能照原樣顯示。 */
    data class Unknown(val raw: String) : FlightStatus
}
