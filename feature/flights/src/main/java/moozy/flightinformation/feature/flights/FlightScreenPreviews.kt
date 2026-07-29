package moozy.flightinformation.feature.flights

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import moozy.flightinformation.domain.error.LoadError
import moozy.flightinformation.core.ui.FlightInformationTheme
import java.time.LocalDateTime

private fun previewItem(
    time: String,
    scheduledTime: String,
    badge: FlightStatusText,
    statusLevel: FlightStatusLevel,
    carrier: String,
    from: String,
    gate: String?,
    aircraft: String,
    statusLine: FlightStatusText = badge,
    logoUrl: String? = null
) = FlightArrivalItemUiModel(
    headlineTimeText = time,
    scheduledTimeText = scheduledTime,
    badgeText = badge,
    carrierLineText = carrier,
    departureText = from,
    gate = gate,
    aircraftText = aircraft,
    flightStatusText = statusLine,
    statusLevel = statusLevel,
    isCancelled = statusLevel == FlightStatusLevel.Cancelled,
    airlineLogoUrl = logoUrl
)

/** 最後一筆刻意放一個讀不出來的時間，讓「時間未定」那一組在預覽裡看得到。 */
private val timelinePreviewItems = listOf(
    previewItem(
        time = "08:40",
        scheduledTime = "08:35",
        badge = FlightStatusText.Resource(R.string.flight_status_arrived),
        statusLevel = FlightStatusLevel.Completed,
        carrier = "中華航空 · CI 152",
        from = "東京成田 (NRT)",
        gate = "3",
        aircraft = "B738",
        // 有 URL 的那一筆：預覽不發網路請求，看到的正好是首字退路。
        logoUrl = "https://www.kia.gov.tw/images/ALL-square/CI.png"
    ),
    previewItem(
        time = "09:05",
        scheduledTime = "09:05",
        badge = FlightStatusText.Resource(R.string.flight_status_arrived),
        statusLevel = FlightStatusLevel.Completed,
        carrier = "長榮航空 · BR 106",
        from = "首爾仁川 (ICN)",
        gate = "5",
        aircraft = "A333"
    ),
    previewItem(
        time = "09:50",
        scheduledTime = "09:20",
        badge = FlightStatusText.Resource(R.string.flight_status_delayed),
        statusLevel = FlightStatusLevel.Attention,
        carrier = "星宇航空 · JX 722",
        from = "檳城 (PEN)",
        gate = "7",
        aircraft = "A321",
        statusLine = FlightStatusText.Resource(R.string.flight_status_delayed_with_cause, "Weather")
    ),
    previewItem(
        time = "10:15",
        scheduledTime = "10:15",
        badge = FlightStatusText.Resource(R.string.flight_status_on_time),
        statusLevel = FlightStatusLevel.OnTime,
        carrier = "立榮航空 · B7 8690",
        from = "澎湖 (MZG)",
        gate = null,
        aircraft = "AT76"
    ),
    previewItem(
        time = "10:40",
        scheduledTime = "10:40",
        badge = FlightStatusText.Resource(R.string.flight_status_cancelled),
        statusLevel = FlightStatusLevel.Cancelled,
        carrier = "國泰航空 · CX 564",
        from = "香港 (HKG)",
        gate = null,
        aircraft = "A359",
        statusLine = FlightStatusText.Resource(R.string.flight_status_cancelled_with_cause, "Typhoon")
    ),
    previewItem(
        time = "11:05",
        scheduledTime = "11:05",
        badge = FlightStatusText.Resource(R.string.flight_status_schedule_change),
        statusLevel = FlightStatusLevel.Attention,
        carrier = "日本航空 · JL 802",
        from = "東京羽田 (HND)",
        gate = "2",
        aircraft = "B788"
    ),
    previewItem(
        time = "--:--",
        scheduledTime = "--:--",
        badge = FlightStatusText.Resource(R.string.flight_status_unknown),
        statusLevel = FlightStatusLevel.Neutral,
        carrier = "酷航 · TR 898",
        from = "新加坡 (SIN)",
        gate = null,
        aircraft = "--"
    )
).toImmutableList()

private val timelinePreviewState = FlightArrivalsUiState.Content(
    items = timelinePreviewItems,
    // 10:03：三筆已經過去、10:15 是下一班、三筆還沒到、一筆時間未定。
    updatedAt = LocalDateTime.of(2026, 7, 26, 10, 3, 12)
)

@Composable
private fun TimelinePreviewHost(
    state: FlightArrivalsUiState,
    darkTheme: Boolean
) {
    FlightInformationTheme(darkTheme = darkTheme, dynamicColor = false) {
        FlightsScreen(
            flightArrivalsUiState = state,
            onRefresh = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(name = "Timeline · light", showBackground = true, widthDp = 400, heightDp = 900)
@Composable
fun FlightsScreenTimelineLightPreview() {
    TimelinePreviewHost(state = timelinePreviewState, darkTheme = false)
}

@Preview(name = "Timeline · dark", showBackground = true, widthDp = 400, heightDp = 900)
@Composable
fun FlightsScreenTimelineDarkPreview() {
    TimelinePreviewHost(state = timelinePreviewState, darkTheme = true)
}

@Preview(name = "Timeline · empty", showBackground = true, widthDp = 400, heightDp = 420)
@Composable
fun FlightsScreenTimelineEmptyPreview() {
    TimelinePreviewHost(
        state = FlightArrivalsUiState.Content(
            items = persistentListOf(),
            updatedAt = LocalDateTime.of(2026, 7, 26, 10, 3, 12)
        ),
        darkTheme = false
    )
}

@Preview(name = "Timeline · error", showBackground = true, widthDp = 400, heightDp = 420)
@Composable
fun FlightsScreenTimelineErrorPreview() {
    TimelinePreviewHost(
        state = FlightArrivalsUiState.Error(LoadError.NoNetwork),
        darkTheme = true
    )
}

@Preview(name = "Timeline · loading", showBackground = true, widthDp = 400, heightDp = 320)
@Composable
fun FlightsScreenTimelineLoadingPreview() {
    TimelinePreviewHost(state = timelinePreviewState.copy(items = persistentListOf()).let {
        FlightArrivalsUiState.Loading
    }, darkTheme = false)
}
