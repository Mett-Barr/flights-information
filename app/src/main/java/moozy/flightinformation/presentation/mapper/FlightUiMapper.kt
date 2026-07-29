package moozy.flightinformation.presentation.mapper

import moozy.flightinformation.domain.model.flights.FlightArrival
import moozy.flightinformation.domain.model.flights.FlightStatus
import moozy.flightinformation.presentation.state.flights.FlightArrivalItemUiModel
import java.time.format.DateTimeFormatter

private val DISPLAY_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private const val NO_TIME = "--:--"
private const val NO_VALUE = "--"

/**
 * domain → UI。這一層只做呈現：補預設字串、排版、決定顯示文案。
 * 狀態的語意正規化已經在 data 層完成，這裡只把它翻成給人看的字。
 */
fun FlightArrival.toUiModel(): FlightArrivalItemUiModel {
    val scheduledText = scheduled?.format(DISPLAY_TIME) ?: NO_TIME
    val headline = (actual ?: scheduled)?.format(DISPLAY_TIME) ?: NO_TIME

    val badge = when (status) {
        FlightStatus.Arrived -> "Arrived"
        FlightStatus.Departed -> "Departed"
        FlightStatus.ScheduleChanged -> "Schedule change"
        FlightStatus.Cancelled -> "Cancelled"
        FlightStatus.Delayed -> "Delayed"
        FlightStatus.OnTime -> "On time"
        is FlightStatus.Unknown -> "Unknown"
    }

    // 綁進區域變數才能 smart cast：status 是別的模組的 public property，
    // 編譯器無法保證兩次讀取之間它沒被換掉。
    val statusLine = when (val current = status) {
        FlightStatus.Delayed -> delayCause?.let { "Delayed · $it" } ?: "Delayed"
        is FlightStatus.Unknown -> current.raw.ifBlank { "Status unknown" }
        else -> badge
    }

    val carrier = listOfNotNull(
        airlineName.ifBlank { null },
        flightNumber.ifBlank { null },
    ).joinToString(" · ").ifBlank { NO_VALUE }

    val departure = when {
        originName.isNotBlank() && originCode.isNotBlank() -> "$originName ($originCode)"
        originName.isNotBlank() -> originName
        originCode.isNotBlank() -> originCode
        else -> NO_VALUE
    }

    return FlightArrivalItemUiModel(
        headlineTimeText = headline,
        expectedLabelText = "Expected $scheduledText",
        badgeText = badge,
        carrierLineText = carrier,
        departureText = departure,
        gateText = "Gate ${gate ?: NO_VALUE}",
        aircraftText = aircraftType ?: NO_VALUE,
        flightStatusText = statusLine,
        statusKey = status.uiKey,
        airlineLogoUrl = airlineLogoUrl,
    )
}

/** UI 用來挑顏色/圖示的鍵；這張映射表屬於呈現層，不放進 domain。 */
private val FlightStatus.uiKey: String
    get() = when (this) {
        FlightStatus.Arrived -> "ARRIVED"
        FlightStatus.Departed -> "DEPARTED"
        FlightStatus.ScheduleChanged -> "SCHEDULE_CHANGE"
        FlightStatus.Cancelled -> "CANCELLED"
        FlightStatus.Delayed -> "DELAYED"
        FlightStatus.OnTime -> "ON_TIME"
        is FlightStatus.Unknown -> "UNKNOWN"
    }

fun List<FlightArrival>.toUiModels(): List<FlightArrivalItemUiModel> = map { it.toUiModel() }
