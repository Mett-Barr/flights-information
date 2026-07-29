package moozy.flightinformation.data.datasource.flights.dto

import moozy.flightinformation.domain.model.flights.FlightArrival
import moozy.flightinformation.domain.model.flights.FlightStatus
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")

private fun String?.cleaned(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

/** 來源給 "HH:mm"；解析不出來就當作沒有這個時間。 */
private fun String?.toLocalTimeOrNull(): LocalTime? =
    cleaned()?.let { runCatching { LocalTime.parse(it, TIME_FORMAT) }.getOrNull() }

/**
 * 狀態字串正規化。來源中英混雜且不保證窮舉，認不得的一律保留原文交給
 * [FlightStatus.Unknown]，而不是悄悄吞掉。
 */
private fun String?.toFlightStatus(): FlightStatus {
    val raw = cleaned() ?: return FlightStatus.Unknown("")
    val upper = raw.uppercase()
    return when {
        upper == "ARRIVED" || raw == "抵達" -> FlightStatus.Arrived
        upper == "DEPARTED" || raw == "出發" || raw == "出登" -> FlightStatus.Departed
        upper.replace(' ', '_') == "SCHEDULE_CHANGE" || raw == "時間更改" -> FlightStatus.ScheduleChanged
        upper == "CANCELLED" || upper == "CANCELED" || raw == "取消" -> FlightStatus.Cancelled
        upper == "DELAYED" || raw == "延誤" -> FlightStatus.Delayed
        ON_TIME_WORDS.any { upper.contains(it) } -> FlightStatus.OnTime
        else -> FlightStatus.Unknown(raw)
    }
}

private val ON_TIME_WORDS = listOf(
    "ON TIME", "ONTIME", "ON-TIME", "準時", "準點", "正常",
)

fun InstantScheduleDomesticArrivalDto.InstantScheduleDomesticArrivalDtoItem.toDomain(): FlightArrival =
    FlightArrival(
        // airLineNum 已是完整的 IATA 航班號；airLineCode 是 ICAO 代碼（航管用），
        // 兩者屬於不同編碼系統，串在一起會得到 "UIA B78690" 這種不存在的編號。
        flightNumber = airLineNum.cleaned() ?: "",
        airlineName = airLineName.cleaned() ?: "",
        airlineLogoUrl = airLineLogo.cleaned(),
        originName = upAirportName.cleaned() ?: "",
        originCode = upAirportCode.cleaned() ?: "",
        scheduled = expectTime.toLocalTimeOrNull(),
        actual = realTime.toLocalTimeOrNull(),
        status = airFlyStatus.toFlightStatus(),
        gate = airBoardingGate.cleaned(),
        aircraftType = airPlaneType.cleaned(),
        delayCause = airFlyDelayCause.cleaned(),
    )

fun List<InstantScheduleDomesticArrivalDto.InstantScheduleDomesticArrivalDtoItem>.toDomain(): List<FlightArrival> =
    map { it.toDomain() }
