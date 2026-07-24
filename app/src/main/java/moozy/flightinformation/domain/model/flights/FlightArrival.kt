package moozy.flightinformation.domain.model.flights

import java.time.LocalTime

/**
 * 一筆抵達航班。
 *
 * 時間用 [LocalTime]：來源只給 "HH:mm"，沒有日期也沒有時區，而看板顯示的本來就是
 * 機場當地的壁鐘時間、不做時區換算，所以 [LocalTime] 的資訊量剛好相符（用 Instant
 * 會被迫捏造日期與時區）。解析不出來就是 null。
 *
 * 只放這個 app 真的會用到的事實：來源另有 ICAO 代碼與航空公司網頁，兩者都沒有使用者。
 */
data class FlightArrival(
    /** IATA 航班號，如 "B78690"（B7＝立榮的 IATA 代碼 ＋ 班次 8690）。乘客看到的就是這個。 */
    val flightNumber: String,
    val airlineName: String,
    val airlineLogoUrl: String?,
    /** 出發地，如 "澎湖" / "MZG"。 */
    val originName: String,
    val originCode: String,
    /** 表定時間。 */
    val scheduled: LocalTime?,
    /** 實際時間，尚未發生時為 null。 */
    val actual: LocalTime?,
    val status: FlightStatus,
    val gate: String?,
    val aircraftType: String?,
    /** 延誤原因，來源常給空字串，這裡統一成 null。 */
    val delayCause: String?,
)
