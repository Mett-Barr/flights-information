package moozy.flightinformation.feature.flights

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import java.time.LocalTime

internal class TimelineTimedItem(
    val minuteOfDay: Int,
    val item: FlightArrivalItemUiModel
)

internal class TimelineEntry(
    val item: FlightArrivalItemUiModel,
    val key: String,
    val isPast: Boolean,
    val isNext: Boolean
)

/**
 * 清單裡的一列。
 *
 * 先把整條時間軸攤平成這個型別的清單，再交給 `LazyColumn` 逐列畫。
 * 這樣「下一班在第幾列」是查出來的（[List.indexOfFirst]），不是一邊組畫面一邊手算索引——
 * 前一版就是在手算索引時把標記塞錯位置的。
 */
internal sealed class TimelineRow(val key: String) {
    object Head : TimelineRow(RAIL_HEAD_KEY)
    class Marker(markerKey: String, val label: String, val note: String?) : TimelineRow(markerKey)
    class Now(val now: LocalTime) : TimelineRow("timeline-now")
    class Flight(val entry: TimelineEntry) : TimelineRow(entry.key)
    object Tail : TimelineRow(RAIL_TAIL_KEY)
}

internal class Timeline(
    val rows: List<TimelineRow>,
    val nextRowIndex: Int,
    val nextLabel: String?,
    val pastCount: Int,
    val upcomingCount: Int,
    val unscheduledCount: Int
)

/**
 * 把一份班次清單整理成時間軸要的形狀。
 *
 * 規則：
 * 1. 能讀成一天之內時刻的排在前面，依時間升冪；讀不出來的整組收在最後（見 [parseMinuteOfDay]）。
 * 2. 依整點分組，每組前面放一個整點標記。
 * 3. 第一筆「時刻 >= 現在」的就是**下一班**，它自己就是那個「你在這裡」的標記；
 *    全部都已經過去時就沒有下一班，不會憑空生出一列。
 * 4. 頭尾各補一段軌道淡出，讓線從畫面上緣進來、往下緣出去。
 *
 * 已知限制：排序用的是「一天之內的分鐘數」，所以跨午夜的看板（23:50 與 00:10 同時在清單裡）
 * 會把 00:10 排到最前面並判成已過去。model 只給了 "HH:mm" 字串、沒有日期，
 * 要正確處理就得先讓資料層帶上日期，不是這一層猜得出來的。
 */
// 一次走訪就要同時決定整點分隔、現在的插入位置與下一班的索引，三者互相依賴：
// 拆開就得走訪三次並在函式之間傳遞游標狀態，那比現在難讀。
@Suppress("LongMethod")
internal fun buildTimeline(
    items: List<FlightArrivalItemUiModel>,
    now: LocalTime,
    unscheduledLabel: String
): Timeline {
    // model 沒有 id，key 只能從內容推。同一家航空、同一個時刻真的重複出現時補流水號：
    // LazyColumn 撞 key 會直接丟例外，寧可讓 key 稍微不穩，也不能讓它重複。
    // 換句話說，這個 key 的唯一性只到資料本身允許的程度。
    val usedKeys = mutableMapOf<String, Int>()
    fun keyOf(item: FlightArrivalItemUiModel): String {
        val base = "${item.carrierLineText}@${item.headlineTimeText}"
        val seen = usedKeys.getOrElse(base) { 0 }
        usedKeys[base] = seen + 1
        return if (seen == 0) base else "$base#$seen"
    }

    val timed = mutableListOf<TimelineTimedItem>()
    val untimed = mutableListOf<FlightArrivalItemUiModel>()
    items.forEach { item ->
        val minute = parseMinuteOfDay(item.headlineTimeText)
        if (minute == null) {
            untimed += item
        } else {
            timed += TimelineTimedItem(minuteOfDay = minute, item = item)
        }
    }

    // sortedBy 是穩定排序，同一分鐘內維持原本的順序。
    val sorted = timed.sortedBy { it.minuteOfDay }
    val nowMinute = now.hour * MINUTES_PER_HOUR + now.minute
    val firstUpcoming = sorted.indexOfFirst { it.minuteOfDay >= nowMinute }
    val nextIndex = if (firstUpcoming < 0) sorted.size else firstUpcoming

    val rows = mutableListOf<TimelineRow>()
    rows += TimelineRow.Head

    var cursor = 0
    while (cursor < sorted.size) {
        val hourOfDay = sorted[cursor].minuteOfDay / MINUTES_PER_HOUR
        rows += TimelineRow.Marker(
            markerKey = "hour-$hourOfDay",
            label = "${twoDigits(hourOfDay)}:00",
            note = null
        )
        while (cursor < sorted.size && sorted[cursor].minuteOfDay / MINUTES_PER_HOUR == hourOfDay) {
            val timedItem = sorted[cursor]
            if (cursor == nextIndex) rows += TimelineRow.Now(now)
            rows += TimelineRow.Flight(
                TimelineEntry(
                    item = timedItem.item,
                    key = keyOf(timedItem.item),
                    isPast = cursor < nextIndex,
                    isNext = cursor == nextIndex
                )
            )
            cursor++
        }
    }

    // 時間未定的班次在時間軸上沒有位置，硬塞進某個小時只是說謊，所以整組放在最後面。
    if (nextIndex == sorted.size) rows += TimelineRow.Now(now)

    if (untimed.isNotEmpty()) {
        rows += TimelineRow.Marker(
            markerKey = UNSCHEDULED_MARKER_KEY,
            label = UNKNOWN_TIME_TEXT,
            note = unscheduledLabel
        )
        untimed.forEach { item ->
            rows += TimelineRow.Flight(
                TimelineEntry(
                    item = item,
                    key = keyOf(item),
                    // 時間未定的班次沒有「過去或未來」可言，不做降級也不搶下一班的位置。
                    isPast = false,
                    isNext = false
                )
            )
        }
    }

    rows += TimelineRow.Tail

    return Timeline(
        rows = rows.toList(),
        nextRowIndex = rows.indexOfFirst { it is TimelineRow.Flight && it.entry.isNext },
        nextLabel = sorted.getOrNull(nextIndex)?.item?.headlineTimeText,
        pastCount = nextIndex,
        upcomingCount = sorted.size - nextIndex,
        unscheduledCount = untimed.size
    )
}

/**
 * 把 `headlineTimeText` 讀成一天之內的分鐘數。
 *
 * 這個欄位是已經格式化好的字串，沒有可用時間時會是 `"--:--"`，也不保證一定是 `HH:mm`。
 * 只有能明確讀成合法時刻的才回傳數字，其餘一律回 `null` ——
 * 呼叫端會把它們收進「時間未定」那一組，不會當掉，也不會默默把班次丟掉。
 */
private const val MAX_HOUR = 23
private const val MAX_MINUTE = 59

private fun parseMinuteOfDay(text: String): Int? {
    val separator = text.indexOf(':')
    if (separator <= 0 || separator == text.lastIndex) return null
    val hour = text.substring(0, separator).trim().toIntOrNull() ?: return null
    val minute = text.substring(separator + 1).trim().toIntOrNull() ?: return null
    if (hour !in 0..MAX_HOUR || minute !in 0..MAX_MINUTE) return null
    return hour * MINUTES_PER_HOUR + minute
}

/** 補零成兩位數。手動補而不用 `String.format`，免得在某些語系跑出非阿拉伯數字。 */
internal fun twoDigits(value: Int): String = value.toString().padStart(2, '0')

@Composable
internal fun summaryTextOf(timeline: Timeline): String = when {
    timeline.nextLabel != null && timeline.unscheduledCount > 0 -> pluralStringResource(
        R.plurals.flights_summary_next_unscheduled,
        timeline.pastCount,
        timeline.pastCount,
        timeline.upcomingCount,
        timeline.nextLabel,
        timeline.unscheduledCount
    )

    timeline.nextLabel != null -> pluralStringResource(
        R.plurals.flights_summary_next,
        timeline.pastCount,
        timeline.pastCount,
        timeline.upcomingCount,
        timeline.nextLabel
    )

    timeline.unscheduledCount > 0 -> pluralStringResource(
        R.plurals.flights_summary_unscheduled,
        timeline.pastCount,
        timeline.pastCount,
        timeline.upcomingCount,
        timeline.unscheduledCount
    )

    else -> pluralStringResource(
        R.plurals.flights_summary,
        timeline.pastCount,
        timeline.pastCount,
        timeline.upcomingCount
    )
}

/**
 * 次要資訊收斂為一行：出發地永遠保留；登機門僅在已到或下一班時才會指引行動；
 * 預計時間只在和主時間不同時呈現，避免將同一個時刻唸兩次。
 */
@Composable
internal fun supportingLineOf(
    item: FlightArrivalItemUiModel,
    showGate: Boolean
): String {
    val gateText = item.gate?.let { stringResource(R.string.flight_gate, it) }
    val expectedTimeText = stringResource(R.string.flight_expected_time, item.scheduledTimeText)

    return buildList {
        add(item.departureText)
        if (showGate && gateText != null) add(gateText)
        if (expectedTimeDiffers(item)) add(expectedTimeText)
        if (item.flightStatusText != item.badgeText) add(item.flightStatusText.resolve())
    }.joinToString(separator = " · ")
}

@Composable
internal fun FlightStatusText.resolve(): String = when (this) {
    is FlightStatusText.Resource -> formatArgument?.let { stringResource(id, it) } ?: stringResource(id)
    is FlightStatusText.Raw -> value
}

/** 已到或下一班的登機門會改變讀者現在該往哪裡走。 */
internal fun shouldShowGate(entry: TimelineEntry): Boolean =
    entry.isPast || entry.isNext || entry.item.statusLevel == FlightStatusLevel.Completed

/** 預計時間與主時間相同時不重複顯示，避免從已在地化文案解析資料。 */
private fun expectedTimeDiffers(item: FlightArrivalItemUiModel): Boolean =
    item.scheduledTimeText != item.headlineTimeText

/** Logo 載不到時墊在底下的首字。 */
internal fun monogramOf(carrierLineText: String): String =
    carrierLineText.trim().firstOrNull()?.toString().orEmpty()

/* ============================================================
 *  預覽
 * ============================================================ */

