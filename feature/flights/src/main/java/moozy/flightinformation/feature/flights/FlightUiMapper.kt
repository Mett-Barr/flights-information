package moozy.flightinformation.feature.flights

import moozy.flightinformation.domain.model.flights.FlightArrival
import moozy.flightinformation.domain.model.flights.FlightStatus
import java.time.format.DateTimeFormatter

private val DISPLAY_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private const val NO_TIME = "--:--"
private const val NO_VALUE = "--"

/**
 * domain → UI。這一層只做呈現：補預設字串、排版、決定顯示文案。
 * 狀態的語意正規化已經在 data 層完成，這裡只把它翻成給人看的字。
 */
// 分支數等於來源欄位數：每個欄位各有「有值」與「缺值」兩種呈現。
// 拆成多個小函式不會減少分支，只會把同一張對照表攤到多個檔案。
@Suppress("CyclomaticComplexMethod")
fun FlightArrival.toUiModel(): FlightArrivalItemUiModel {
    val scheduledText = scheduled?.format(DISPLAY_TIME) ?: NO_TIME
    val headline = (actual ?: scheduled)?.format(DISPLAY_TIME) ?: NO_TIME

    val badge = when (status) {
        FlightStatus.Arrived -> FlightStatusText.Resource(R.string.flight_status_arrived)
        FlightStatus.Departed -> FlightStatusText.Resource(R.string.flight_status_departed)
        FlightStatus.ScheduleChanged -> FlightStatusText.Resource(R.string.flight_status_schedule_change)
        FlightStatus.Cancelled -> FlightStatusText.Resource(R.string.flight_status_cancelled)
        FlightStatus.Delayed -> FlightStatusText.Resource(R.string.flight_status_delayed)
        FlightStatus.OnTime -> FlightStatusText.Resource(R.string.flight_status_on_time)
        is FlightStatus.Unknown -> FlightStatusText.Resource(R.string.flight_status_unknown)
    }

    // 綁進區域變數才能 smart cast：status 是別的模組的 public property，
    // 編譯器無法保證兩次讀取之間它沒被換掉。
    val statusLine = when (val current = status) {
        FlightStatus.Delayed -> delayCause?.let {
            FlightStatusText.Resource(R.string.flight_status_delayed_with_cause, it)
        } ?: badge
        is FlightStatus.Unknown -> current.raw.takeIf { it.isNotBlank() }?.let(FlightStatusText::Raw) ?: badge
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
        scheduledTimeText = scheduledText,
        badgeText = badge,
        carrierLineText = carrier,
        departureText = departure,
        gate = gate,
        aircraftText = aircraftType ?: NO_VALUE,
        flightStatusText = statusLine,
        statusLevel = status.statusLevel,
        isCancelled = status == FlightStatus.Cancelled,
        airlineLogoUrl = airlineLogoUrl,
    )
}

/** UI 用來挑顏色/圖示的鍵；這張映射表屬於呈現層，不放進 domain。 */
private val FlightStatus.statusLevel: FlightStatusLevel
    get() = when (this) {
        FlightStatus.Arrived,
        FlightStatus.Departed -> FlightStatusLevel.Completed
        FlightStatus.ScheduleChanged,
        FlightStatus.Delayed -> FlightStatusLevel.Attention
        FlightStatus.Cancelled -> FlightStatusLevel.Cancelled
        FlightStatus.OnTime -> FlightStatusLevel.OnTime
        is FlightStatus.Unknown -> FlightStatusLevel.Neutral
    }

fun List<FlightArrival>.toUiModels(): List<FlightArrivalItemUiModel> = map { it.toUiModel() }
