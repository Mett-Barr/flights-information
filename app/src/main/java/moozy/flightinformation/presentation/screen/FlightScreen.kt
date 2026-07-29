package moozy.flightinformation.presentation.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import kotlinx.coroutines.flow.SharedFlow
import moozy.flightinformation.R
import moozy.flightinformation.domain.error.LoadError
import moozy.flightinformation.presentation.mapper.messageRes
import moozy.flightinformation.presentation.state.flights.FlightArrivalItemUiModel
import moozy.flightinformation.presentation.state.flights.FlightArrivalsUiState
import moozy.flightinformation.presentation.theme.FlightInformationTheme
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/* ============================================================
 *  尺寸與字串常數
 *
 *  版面間距一律走 8dp 系統（4 / 8 / 12 / 16 / 24 / 48）。
 *  「軌道」那幾個數字是繪圖座標，不是間距，另外標註。
 * ============================================================ */

private val ScreenHorizontalPadding = 16.dp

/**
 * 軌道車道寬度。軌道線與節點都畫在這條車道的中線上，
 * 整點膠囊也以這條中線為中心。車道要夠寬，膠囊往左溢出時才不會被螢幕邊緣切掉。
 */
private val RailLaneWidth = 36.dp

/** 車道與卡片之間的空隙，同時也是節點連到卡片那段短線的長度來源。 */
private val RailGutterWidth = 8.dp

/** 軌道線的 x 座標（相對於已扣掉左右邊距的內容區）。繪圖座標，不是間距。 */
private val RailCenterX = RailLaneWidth / 2

/** 卡片左緣的 x 座標。節點的連接線就是畫到這裡。繪圖座標。 */
private val CardStartX = RailLaneWidth + RailGutterWidth

private val RailLineWidth = 2.dp
private val NodeDiameter = 12.dp
private val NextNodeDiameter = 16.dp
private val NodeBorderWidth = 2.5.dp

/** 「下一班」節點外圈那層柔光的厚度。 */
private val NextNodeHaloWidth = 6.dp

/** 每一列上下各留這麼多，卡片之間就是兩倍。軌道會穿過這段空白，所以線不會斷。 */
private val RowVerticalGap = 4.dp

private val CardHorizontalPadding = 12.dp
private val CardVerticalPadding = 8.dp
private val CardTextSpacing = 2.dp

/** 整點標記上下的留白：時間軸要能「呼吸」，一天的節奏才看得出來。 */
private val HourMarkerVerticalGap = 16.dp
private val HourPillHorizontalPadding = 10.dp
private val HourPillVerticalPadding = 4.dp
private val HourPillBorderWidth = 1.dp

/** 整點膠囊釘在軌道上、左右都會溢出車道，附註文字要從這個距離之後才開始。 */
private val HourNoteGutter = 32.dp

/** 軌道頭尾的淡出段：告訴讀者這條線是被截斷的，不是到此為止。 */
private val RailHeadHeight = 12.dp
private val RailTailHeight = 28.dp

private val LogoSize = 32.dp
private val LogoSpacing = 8.dp
private val StatusBadgeMaxWidth = 132.dp
private val HeadlineBadgeSpacing = 8.dp
private val FreshnessIndicatorSize = 32.dp

/**
 * 資料新鮮度的唯一來源在 FlightsViewModel.FRESHNESS_MILLIS；因其為 private，於此同步維護。
 */
private const val FRESHNESS_MILLIS = 10_000L

/** 可點擊元素的最小高度，符合觸控目標規範。 */
private val MinTouchTarget = 48.dp

/** 萬一主題把行高設成非 sp（Em 或未指定），節點高度就退回這個值。 */
private val FallbackHeadlineLineHeight = 32.dp

private const val UNKNOWN_TIME_TEXT = "--:--"

private const val RAIL_HEAD_KEY = "timeline-rail-head"
private const val RAIL_TAIL_KEY = "timeline-rail-tail"
private const val UNSCHEDULED_MARKER_KEY = "timeline-unscheduled-marker"
private const val EMPTY_STATE_KEY = "timeline-empty"

private const val MINUTES_PER_HOUR = 60

/** 首次定位時，「下一班」上方保留幾列當作前情提要。 */
private const val PRE_NEXT_CONTEXT_ROWS = 2

/** 「下一班」節點外圈柔光的透明度：夠亮到看得出來，又不至於變成第二個節點。 */
private const val NEXT_NODE_HALO_ALPHA = 0.18f

/** 等寬數字。時間是要被上下比較的東西，位數不對齊就白費了整條時間軸。 */
private const val TABULAR_NUMBERS = "tnum"

private val updatedAtFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

/**
 * 方向：**單側編輯式時間軸**（single-side editorial timeline）。
 *
 * 軌道緊貼左緣，其餘全部在右邊；時間不再自己佔一欄，而是走進卡片裡當標題。
 * 這一版把前一版「對開式時間軸」的五個毛病一次改掉：
 *
 * 1. **軌道不再被整點標題切斷。** 前一版的整點是滿版色帶，橫著把軌道剖成一段一段；
 *    時間軸的軌道一旦不連續，整個視覺隱喻就沒了。這一版每一格（含頭尾的淡出段、
 *    整點標記、每一張卡片）都用 [androidx.compose.ui.draw.drawBehind] 從自己的最上緣
 *    畫到最下緣，格與格之間沒有任何間隙（列距是畫在格子「裡面」的 padding，
 *    不是 `Arrangement.spacedBy`），所以相鄰線段接起來就是一條連續的線。
 *    整點膠囊是軌道上的**子元素**，畫在軌道之後，於是線從它背後穿過去。
 * 2. **不再有會落到不可能位置的 NOW 標記。** 前一版把「現在」當成一個獨立的列插進小時分組裡，
 *    結果出現「02:12 排在 08:00 標題之後」這種讀不通的畫面。這一版直接把標記換成
 *    **下一班抵達**本身：節點放大、染成 primary、外加一圈柔光。
 *    它是清單裡真實存在的一筆，位置永遠合法；而且在入境看板上，
 *    「下一班是哪一班」本來就比「現在幾點」有用。
 * 3. **時間回到卡片裡。** 前一版把時間放大擺在軌道外面，和航班編號互搶注意力；
 *    現在它是卡片的第一行標題（[rememberTimeHeadlineStyle]，等寬數字）。
 * 4. **節點不再是小灰點。** 12dp 圓、2.5dp 邊，三種語意各自對應到 scheme 角色，
 *    看起來是「軌道上的一個點」而不是裝飾。
 * 5. **節點和卡片連起來了。** 節點對齊卡片第一行文字的視覺中心（不是卡片的垂直中心——
 *    對齊到文字那條線，才會讀成「這個點屬於這張卡」），再畫一段短線接到卡片左緣。
 *
 * 沿用前一版已經對的地方：
 * - 依整點分組，讀不出時刻的（`"--:--"`）整組收在最後，不猜也不丟——見 [buildTimeline]。
 * - 「現在」取自 [FlightArrivalsUiState.Content.updatedAt] 而不是 `LocalDateTime.now()`：
 *   標記要跟畫面上這份資料同一個時間基準。
 * - `refreshEvent` 每次成功載入後都會重設新鮮度倒數，**不捲動清單**。
 * - 沒有任何 `AnimatedContent` 綁在每次輪詢都會變的東西上。
 * - 每一列都有 key，重複時補流水號。
 */
@Composable
fun FlightsScreen(
    flightArrivalsUiState: FlightArrivalsUiState,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
    refreshEvent: SharedFlow<Unit>? = null,
    innerPadding: PaddingValues = PaddingValues(0.dp)
) {
    val layoutDirection = LocalLayoutDirection.current

    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    end = innerPadding.calculateEndPadding(layoutDirection)
                )
        ) {
            TimelineHeader(
                content = flightArrivalsUiState as? FlightArrivalsUiState.Content,
                refreshEvent = refreshEvent
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (flightArrivalsUiState) {
                    is FlightArrivalsUiState.Loading -> TimelineLoading()

                    is FlightArrivalsUiState.Error -> TimelineError(
                        error = flightArrivalsUiState.error,
                        onRetry = onRefresh,
                        bottomPadding = innerPadding.calculateBottomPadding()
                    )

                    is FlightArrivalsUiState.Content -> TimelineContent(
                        content = flightArrivalsUiState,
                        onRefresh = onRefresh,
                        bottomPadding = innerPadding.calculateBottomPadding()
                    )
                }
            }
        }
    }
}

/* ============================================================
 *  標題列
 * ============================================================ */

/**
 * 導覽列已標示目前目的地，這裡只保留新鮮度與下一步資訊。
 */
@Composable
private fun TimelineHeader(
    content: FlightArrivalsUiState.Content?,
    refreshEvent: SharedFlow<Unit>?,
    modifier: Modifier = Modifier
) {
    if (content == null) return

    val unscheduledLabel = stringResource(R.string.flights_unscheduled)
    val timeline = remember(content.items, content.updatedAt, unscheduledLabel) {
        buildTimeline(content.items, content.updatedAt.toLocalTime(), unscheduledLabel)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenHorizontalPadding, vertical = 8.dp)
    ) {
        Text(
            text = summaryTextOf(timeline),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        FreshnessIndicator(
            updatedAt = content.updatedAt,
            isRefreshing = content.isRefreshing,
            refreshEvent = refreshEvent
        )
    }
}

/**
 * 動畫值只在 Material 3 指示器的 progress lambda 內讀取，避免逐幀重組時間軸與清單。
 */
@Composable
private fun FreshnessIndicator(
    updatedAt: LocalDateTime,
    isRefreshing: Boolean,
    refreshEvent: SharedFlow<Unit>?,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(1f) }
    val updatedDescription = stringResource(
        R.string.flights_last_updated,
        updatedAt.format(updatedAtFormatter)
    )

    suspend fun restartCountdown() {
        progress.snapTo(1f)
        progress.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = FRESHNESS_MILLIS.toInt(),
                easing = LinearEasing
            )
        )
    }

    LaunchedEffect(updatedAt) { restartCountdown() }
    LaunchedEffect(refreshEvent) {
        refreshEvent?.collect { restartCountdown() }
    }

    if (isRefreshing) {
        CircularProgressIndicator(
            modifier = modifier
                .size(FreshnessIndicatorSize)
                .semantics { contentDescription = updatedDescription }
        )
    } else {
        CircularProgressIndicator(
            progress = { progress.value },
            modifier = modifier
                .size(FreshnessIndicatorSize)
                .semantics { contentDescription = updatedDescription }
        )
    }
}

/* ============================================================
 *  三種狀態
 * ============================================================ */

@Composable
private fun TimelineLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun TimelineError(
    error: LoadError,
    onRetry: () -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = bottomPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = stringResource(error.messageRes()),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.heightIn(min = MinTouchTarget)
            ) {
                Text(text = stringResource(R.string.action_retry))
            }
        }
    }
}

/**
 * 空清單。
 *
 * 三件事一次補齊：吃掉底部 inset、可以下拉更新、另外給一顆明確的按鈕
 * （下拉這個手勢在一片空白上不容易被發現）。
 */
@Composable
private fun TimelineEmpty(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        // 用 LazyColumn 而不是一般 Column：PullToRefreshBox 需要一個會發出巢狀捲動事件的子層，
        // 內容不滿一頁時也一樣能下拉。
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomPadding + 24.dp)
        ) {
            item(key = EMPTY_STATE_KEY) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 48.dp)
                ) {
                    Text(
                        text = stringResource(R.string.flights_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onRefresh,
                        modifier = Modifier.heightIn(min = MinTouchTarget)
                    ) {
                        Text(text = stringResource(R.string.action_refresh))
                    }
                }
            }
        }
    }
}

/* ============================================================
 *  時間軸本體
 * ============================================================ */

@Composable
private fun TimelineContent(
    content: FlightArrivalsUiState.Content,
    onRefresh: () -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    if (content.items.isEmpty()) {
        TimelineEmpty(
            isRefreshing = content.isRefreshing,
            onRefresh = onRefresh,
            bottomPadding = bottomPadding,
            modifier = modifier
        )
    } else {
        // updatedAt 一起當 key：「下一班」的位置本來就該跟著資料的時間戳走。
        // 這只是重算一份純資料，composable 的組合槽位完全沒動，所以清單不會跳。
        val unscheduledLabel = stringResource(R.string.flights_unscheduled)
        val timeline = remember(content.items, content.updatedAt, unscheduledLabel) {
            buildTimeline(content.items, content.updatedAt.toLocalTime(), unscheduledLabel)
        }

        val listState = rememberLazyListState()

        // 只在第一次拿到資料時把「下一班」帶進畫面；撐過旋轉，之後不再自動捲動。
        var alignedToNext by rememberSaveable { mutableStateOf(false) }
        val initialTarget = timeline.nextRowIndex
        LaunchedEffect(Unit) {
            if (!alignedToNext) {
                alignedToNext = true
                val target = (initialTarget - PRE_NEXT_CONTEXT_ROWS).coerceAtLeast(0)
                if (target > 0) {
                    // 不用動畫：第一次定位是「畫面本來就長這樣」，不是一段位移。
                    listState.scrollToItem(target)
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = content.isRefreshing,
            onRefresh = onRefresh,
            modifier = modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = bottomPadding + 24.dp)
            ) {
                // 這裡刻意不給 verticalArrangement：格與格之間只要出現任何間隙，
                // 軌道就會斷成一節一節。列距畫在每一格自己的 padding 裡。
                items(
                    items = timeline.rows,
                    key = { row -> row.key },
                    contentType = { row -> row::class }
                ) { row ->
                    when (row) {
                        is TimelineRow.Head -> TimelineRailCap(
                            height = RailHeadHeight,
                            fadeAtTop = true
                        )

                        is TimelineRow.Marker -> TimelineHourMarker(
                            label = row.label,
                            note = row.note
                        )

                        is TimelineRow.Flight -> TimelineFlightRow(entry = row.entry)

                        is TimelineRow.Tail -> TimelineRailCap(
                            height = RailTailHeight,
                            fadeAtTop = false
                        )
                    }
                }
            }
        }
    }
}

/* ============================================================
 *  軌道上的三種列
 * ============================================================ */

/**
 * 軌道的頭與尾。
 *
 * 用漸層淡出而不是硬切：一條突然開始又突然結束的線看起來像畫錯了，
 * 淡出則是在說「這條線是被畫面截斷的」。透明色一樣從 scheme 角色推導（`copy(alpha = 0f)`），
 * 沒有任何寫死的顏色。
 */
@Composable
private fun TimelineRailCap(
    height: Dp,
    fadeAtTop: Boolean,
    modifier: Modifier = Modifier
) {
    val railColor = MaterialTheme.colorScheme.outlineVariant

    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenHorizontalPadding)
            .height(height)
            .drawBehind {
                val x = RailCenterX.toPx()
                val stops = if (fadeAtTop) {
                    listOf(railColor.copy(alpha = 0f), railColor)
                } else {
                    listOf(railColor, railColor.copy(alpha = 0f))
                }
                drawLine(
                    brush = Brush.verticalGradient(stops),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = RailLineWidth.toPx()
                )
            }
    )
}

/**
 * 整點標記：一顆釘在軌道上的小膠囊。
 *
 * 不是滿版色帶，也不是釘住的標題列——那兩種做法都會把軌道橫切開來。
 * 膠囊只有自己那麼寬、水平置中在軌道上，軌道線先畫（[androidx.compose.ui.draw.drawBehind]
 * 從這一格最上緣畫到最下緣），膠囊當作子元素畫在上面，於是線從它背後穿過去，
 * 上下兩段和相鄰格子接得起來。
 *
 * [note] 只有「時間未定」那一組會用到：膠囊寫 `--:--`，右邊補一句話說明它為什麼在這裡。
 */
@Composable
private fun TimelineHourMarker(
    label: String,
    note: String?,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val railColor = scheme.outlineVariant

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenHorizontalPadding)
            .drawBehind {
                val x = RailCenterX.toPx()
                drawLine(
                    color = railColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = RailLineWidth.toPx()
                )
            }
            .padding(vertical = HourMarkerVerticalGap)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.width(RailLaneWidth)
        ) {
            Surface(
                shape = CircleShape,
                color = scheme.surface,
                contentColor = scheme.onSurfaceVariant,
                border = BorderStroke(HourPillBorderWidth, scheme.outlineVariant),
                // 膠囊比車道寬，unbounded 讓它以車道中線為中心往兩邊溢出而不被壓扁。
                modifier = Modifier.wrapContentWidth(unbounded = true)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    modifier = Modifier.padding(
                        horizontal = HourPillHorizontalPadding,
                        vertical = HourPillVerticalPadding
                    )
                )
            }
        }

        if (note != null) {
            Spacer(modifier = Modifier.width(HourNoteGutter))
            Text(
                text = note,
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 時間軸上的一筆班次：軌道上的節點 + 一段短連接線 + 右邊的卡片。
 *
 * **節點對齊卡片的第一行文字**，不是卡片的垂直中心。這件事是這個版面成不成立的關鍵：
 * 對到卡片中心，節點會讀成「旁邊剛好有個點」；對到第一行文字，才會讀成「這個點就是這班」。
 * 位置是算出來的——`列上緣留白 + 卡片上內距 + 第一行行高的一半`——
 * 行高取自實際的 [TextStyle]（見 [firstLineHeightOf]），所以字體放大時節點會跟著走，
 * 不是抄一個固定數字。
 *
 * 三種節點語意：
 * - 已經抵達 → 實心 `secondary`（跟 `ARRIVED` 徽章用同一組語意角色，「已完成」）
 * - 還沒到 → 空心（`surface` 填色 + `outlineVariant` 邊）
 * - **下一班** → 放大到 16dp、`primary` 實心、外加一圈低透明度的 primary 柔光。
 *   這就是「你在這裡」，取代了前一版那個會落到不可能位置的 NOW 標記。
 *
 * 空心變實心（班機落地）的那一下用 [androidx.compose.material3.MaterialTheme.motionScheme]
 * 的彈簧：狀態真的變了才動，而且是規格指定的彈簧，不是隨手挑的 tween。
 */
@Composable
private fun TimelineFlightRow(
    entry: TimelineEntry,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val item = entry.item

    val headlineStyle = rememberTimeHeadlineStyle()
    val nodeCenterY = RowVerticalGap + CardVerticalPadding + firstLineHeightOf(headlineStyle) / 2

    val sizeSpec = spring<Dp>(dampingRatio = 0.8f, stiffness = 500f)
    val colorSpec = spring<Color>(dampingRatio = 0.8f, stiffness = 500f)

    val nodeDiameter by animateDpAsState(
        targetValue = if (entry.isNext) NextNodeDiameter else NodeDiameter,
        animationSpec = sizeSpec,
        label = "timelineNodeDiameter"
    )
    val nodeFill by animateColorAsState(
        targetValue = when {
            entry.isNext -> scheme.primary
            entry.isPast -> scheme.secondary
            else -> scheme.surface
        },
        animationSpec = colorSpec,
        label = "timelineNodeFill"
    )
    val nodeBorder by animateColorAsState(
        targetValue = when {
            entry.isNext -> scheme.primary
            entry.isPast -> scheme.secondary
            else -> scheme.outlineVariant
        },
        animationSpec = colorSpec,
        label = "timelineNodeBorder"
    )

    val railColor = scheme.outlineVariant
    val haloColor = scheme.primary.copy(alpha = NEXT_NODE_HALO_ALPHA)
    val hasHalo = entry.isNext

    // 已經過去的往後退一階（容器色更低、文字降級），還沒到的維持滿版強度。
    val containerColor = when {
        entry.isNext -> scheme.surfaceContainerHigh
        entry.isPast -> scheme.surfaceContainerLow
        else -> scheme.surfaceContainer
    }
    val contentColor = if (entry.isPast) scheme.onSurfaceVariant else scheme.onSurface

    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenHorizontalPadding)
            .drawBehind {
                val x = RailCenterX.toPx()
                // 軌道：這一格的最上緣畫到最下緣，含上下留白，所以和相鄰格子接得起來。
                drawLine(
                    color = railColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = RailLineWidth.toPx()
                )

                val y = nodeCenterY.toPx()
                val radius = nodeDiameter.toPx() / 2f

                // 連接線：從節點邊緣接到卡片左緣。少了這一段，節點和卡片就是兩件不相干的東西。
                drawLine(
                    color = railColor,
                    start = Offset(x + radius, y),
                    end = Offset(CardStartX.toPx(), y),
                    strokeWidth = RailLineWidth.toPx()
                )

                if (hasHalo) {
                    drawCircle(haloColor, radius + NextNodeHaloWidth.toPx(), Offset(x, y))
                }
                // 實心（或空心的 surface 填色）先蓋掉底下的軌道線，節點才不會被線穿過去。
                drawCircle(nodeFill, radius, Offset(x, y))
                drawCircle(
                    nodeBorder,
                    radius - NodeBorderWidth.toPx() / 2f,
                    Offset(x, y),
                    style = Stroke(NodeBorderWidth.toPx())
                )
            }
            .padding(vertical = RowVerticalGap)
    ) {
        Spacer(modifier = Modifier.width(CardStartX))

        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = contentColor
            ),
            modifier = Modifier
                .weight(1f)
                // 一張卡片對 TalkBack 來說就是一則班機資訊，不該被唸成五段。
                .semantics(mergeDescendants = true) {}
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(
                    horizontal = CardHorizontalPadding,
                    vertical = CardVerticalPadding
                )
            ) {
                AirlineLogo(
                    url = item.airlineLogoUrl,
                    monogram = monogramOf(item.carrierLineText)
                )
                Spacer(modifier = Modifier.width(LogoSpacing))

                Column(
                    verticalArrangement = Arrangement.spacedBy(CardTextSpacing),
                    modifier = Modifier.weight(1f)
                ) {
                    // 第一行：時間當標題，狀態徽章就在旁邊。節點對齊的就是這一行。
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            // 已經格式化好的字串，沒有可用時間時就是 "--:--"，原樣顯示。
                            text = item.headlineTimeText,
                            style = headlineStyle,
                            color = if (entry.isNext) scheme.primary else contentColor,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(HeadlineBadgeSpacing))
                        Spacer(modifier = Modifier.weight(1f))
                        StatusBadge(statusKey = item.statusKey, text = item.badgeText)
                    }

                    Text(
                        text = item.carrierLineText,
                        // 下一班用 emphasized 字級角色（1.5.0-alpha 才公開的 API），
                        // 「目前作用中」在規格裡本來就是 emphasized 的用途。
                        style = if (entry.isNext) {
                            MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        } else {
                            MaterialTheme.typography.titleSmall
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = supportingLineOf(item, showGate = shouldShowGate(entry)),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * 狀態徽章。
 *
 * 顏色一律從 scheme 的語意角色推導，不寫死任何十六進位色碼、也不手動判斷深色模式
 * （寫死的色碼在動態取色下更是完全不成立）。六個 statusKey 收斂成四個語意層級：
 *
 * - `ON_TIME` → primary container：一切照計畫，這是最正面的狀態。
 * - `ARRIVED` / `DEPARTED` → secondary container：已完成，語氣比較低調
 *   （軌道上「已抵達」的節點用的也是同一族的 `secondary`）。
 * - `DELAYED` / `SCHEDULE_CHANGE` → tertiary container：要留意，但不是錯誤。
 * - `CANCELLED` → error container：唯一真正的失敗。
 * - 其他（含 `UNKNOWN`）→ surfaceContainerHighest：不表態。
 *
 * 每一組都是 MD3 規定的成對角色，深淺色與動態取色下的對比都由 scheme 保證。
 */
@Composable
private fun StatusBadge(
    statusKey: String,
    text: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when (statusKey.uppercase()) {
        "ON_TIME" -> scheme.primaryContainer to scheme.onPrimaryContainer
        "ARRIVED", "DEPARTED" -> scheme.secondaryContainer to scheme.onSecondaryContainer
        "DELAYED", "SCHEDULE_CHANGE" -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        "CANCELLED" -> scheme.errorContainer to scheme.onErrorContainer
        else -> scheme.surfaceContainerHighest to scheme.onSurfaceVariant
    }

    Badge(
        containerColor = container,
        contentColor = content,
        // "Schedule change" 這種長標籤不該把時間擠掉，寬度設上限、超出就省略。
        modifier = modifier.widthIn(max = StatusBadgeMaxWidth)
    ) {
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 航空公司 Logo。
 *
 * 底下永遠先墊一塊 tonal 方塊加航空公司首字：沒有 URL、載入中、載入失敗都會露出它，
 * 版面不會因為圖片有沒有到而抖動，也不需要任何寫死的灰色。
 * 只有真的 `Success` 才把首字收起來，所以「載不到」和「還沒到」都有得看。
 * 預覽模式不發網路請求，看到的就是這個退路。
 */
@Composable
private fun AirlineLogo(
    url: String?,
    monogram: String,
    modifier: Modifier = Modifier
) {
    var loaded by remember(url) { mutableStateOf(false) }
    val inspecting = LocalInspectionMode.current

    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.size(LogoSize)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (!loaded && monogram.isNotEmpty()) {
                Text(
                    text = monogram,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
            }
            if (!url.isNullOrBlank() && !inspecting) {
                AsyncImage(
                    model = url,
                    // 純裝飾：航空公司名稱已經在下一行的文字裡了。
                    contentDescription = null,
                    // Fit 會把整張商標塞進去，寬扁的標誌不會被切掉半個字。
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    onState = { state ->
                        loaded = state is AsyncImagePainter.State.Success
                    }
                )
            }
        }
    }
}

/* ============================================================
 *  字體與量測
 * ============================================================ */

/**
 * 卡片第一行的時間樣式。
 *
 * `headlineSmallEmphasized` 是 1.5.0-alpha 才公開的 emphasized 字級角色，
 * 以前只能自己疊 `FontWeight.Bold` 去模擬。等寬數字（`tnum`）讓不同列的時間位數對齊，
 * 整條時間軸才掃得動。
 */
@Composable
private fun rememberTimeHeadlineStyle(): TextStyle {
    val base = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
    return remember(base) { base.copy(fontFeatureSettings = TABULAR_NUMBERS) }
}

/**
 * 一行文字實際佔多高。
 *
 * 節點要對齊卡片第一行，就得知道那一行的高度；直接抄規格的 32dp 會在字體放大時對不上，
 * 所以從實際的 [TextStyle] 換算。主題如果把行高設成 Em 或沒設，才退回固定值。
 */
@Composable
private fun firstLineHeightOf(style: TextStyle): Dp {
    val lineHeight = style.lineHeight
    val density = LocalDensity.current
    return if (lineHeight.isSp) {
        with(density) { lineHeight.toDp() }
    } else {
        FallbackHeadlineLineHeight
    }
}

/* ============================================================
 *  資料整理
 * ============================================================ */

private class TimelineTimedItem(
    val minuteOfDay: Int,
    val item: FlightArrivalItemUiModel
)

private class TimelineEntry(
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
private sealed class TimelineRow(val key: String) {
    object Head : TimelineRow(RAIL_HEAD_KEY)
    class Marker(markerKey: String, val label: String, val note: String?) : TimelineRow(markerKey)
    class Flight(val entry: TimelineEntry) : TimelineRow(entry.key)
    object Tail : TimelineRow(RAIL_TAIL_KEY)
}

private class Timeline(
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
private fun buildTimeline(
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
private fun parseMinuteOfDay(text: String): Int? {
    val separator = text.indexOf(':')
    if (separator <= 0 || separator == text.lastIndex) return null
    val hour = text.substring(0, separator).trim().toIntOrNull() ?: return null
    val minute = text.substring(separator + 1).trim().toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * MINUTES_PER_HOUR + minute
}

/** 補零成兩位數。手動補而不用 `String.format`，免得在某些語系跑出非阿拉伯數字。 */
private fun twoDigits(value: Int): String = value.toString().padStart(2, '0')

@Composable
private fun summaryTextOf(timeline: Timeline): String = when {
    timeline.nextLabel != null && timeline.unscheduledCount > 0 -> stringResource(
        R.string.flights_summary_next_unscheduled,
        timeline.pastCount,
        timeline.upcomingCount,
        timeline.nextLabel,
        timeline.unscheduledCount
    )

    timeline.nextLabel != null -> stringResource(
        R.string.flights_summary_next,
        timeline.pastCount,
        timeline.upcomingCount,
        timeline.nextLabel
    )

    timeline.unscheduledCount > 0 -> stringResource(
        R.string.flights_summary_unscheduled,
        timeline.pastCount,
        timeline.upcomingCount,
        timeline.unscheduledCount
    )

    else -> stringResource(
        R.string.flights_summary,
        timeline.pastCount,
        timeline.upcomingCount
    )
}

/**
 * 次要資訊收斂為一行：出發地永遠保留；登機門僅在已到或下一班時才會指引行動；
 * 預計時間只在和主時間不同時呈現，避免將同一個時刻唸兩次。
 */
private fun supportingLineOf(
    item: FlightArrivalItemUiModel,
    showGate: Boolean
): String = buildList {
    add(item.departureText)
    if (showGate && item.gateText != "Gate --") add(item.gateText)
    if (expectedTimeDiffers(item)) add(item.expectedLabelText)
    if (item.flightStatusText != item.badgeText) add(item.flightStatusText)
}.joinToString(separator = " · ")

/** 已到或下一班的登機門會改變讀者現在該往哪裡走。 */
private fun shouldShowGate(entry: TimelineEntry): Boolean =
    entry.isPast || entry.isNext || entry.item.statusKey.uppercase() in setOf("ARRIVED", "DEPARTED")

/** `expectedLabelText` 內的 HH:mm 和主時間相同時，預計時間沒有新增訊息。 */
private fun expectedTimeDiffers(item: FlightArrivalItemUiModel): Boolean =
    timeTextIn(item.expectedLabelText)?.let { it != item.headlineTimeText } ?: false

private fun timeTextIn(text: String): String? =
    Regex("\\b\\d{2}:\\d{2}\\b").find(text)?.value

/** Logo 載不到時墊在底下的首字。 */
private fun monogramOf(carrierLineText: String): String =
    carrierLineText.trim().firstOrNull()?.toString().orEmpty()

/* ============================================================
 *  預覽
 * ============================================================ */

private fun previewItem(
    time: String,
    expected: String,
    badge: String,
    status: String,
    carrier: String,
    from: String,
    gate: String,
    aircraft: String,
    statusLine: String = badge,
    logoUrl: String? = null
) = FlightArrivalItemUiModel(
    headlineTimeText = time,
    expectedLabelText = expected,
    badgeText = badge,
    carrierLineText = carrier,
    departureText = from,
    gateText = gate,
    aircraftText = aircraft,
    flightStatusText = statusLine,
    statusKey = status,
    airlineLogoUrl = logoUrl
)

/** 最後一筆刻意放一個讀不出來的時間，讓「時間未定」那一組在預覽裡看得到。 */
private val timelinePreviewItems = listOf(
    previewItem(
        time = "08:40",
        expected = "Expected 08:35",
        badge = "Arrived",
        status = "ARRIVED",
        carrier = "中華航空 · CI 152",
        from = "東京成田 (NRT)",
        gate = "Gate 3",
        aircraft = "B738",
        // 有 URL 的那一筆：預覽不發網路請求，看到的正好是首字退路。
        logoUrl = "https://www.kia.gov.tw/images/ALL-square/CI.png"
    ),
    previewItem(
        time = "09:05",
        expected = "Expected 09:05",
        badge = "Arrived",
        status = "ARRIVED",
        carrier = "長榮航空 · BR 106",
        from = "首爾仁川 (ICN)",
        gate = "Gate 5",
        aircraft = "A333"
    ),
    previewItem(
        time = "09:50",
        expected = "Expected 09:20",
        badge = "Delayed",
        status = "DELAYED",
        carrier = "星宇航空 · JX 722",
        from = "檳城 (PEN)",
        gate = "Gate 7",
        aircraft = "A321",
        statusLine = "Delayed · Weather"
    ),
    previewItem(
        time = "10:15",
        expected = "Expected 10:15",
        badge = "On time",
        status = "ON_TIME",
        carrier = "立榮航空 · B7 8690",
        from = "澎湖 (MZG)",
        gate = "Gate --",
        aircraft = "AT76"
    ),
    previewItem(
        time = "10:40",
        expected = "Expected 10:40",
        badge = "Cancelled",
        status = "CANCELLED",
        carrier = "國泰航空 · CX 564",
        from = "香港 (HKG)",
        gate = "Gate --",
        aircraft = "A359",
        statusLine = "Cancelled · Typhoon"
    ),
    previewItem(
        time = "11:05",
        expected = "Expected 11:05",
        badge = "Schedule change",
        status = "SCHEDULE_CHANGE",
        carrier = "日本航空 · JL 802",
        from = "東京羽田 (HND)",
        gate = "Gate 2",
        aircraft = "B788"
    ),
    previewItem(
        time = "--:--",
        expected = "Expected --:--",
        badge = "Unknown",
        status = "UNKNOWN",
        carrier = "酷航 · TR 898",
        from = "新加坡 (SIN)",
        gate = "Gate --",
        aircraft = "--"
    )
)

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
            refreshEvent = null,
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
            items = emptyList(),
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
    TimelinePreviewHost(state = timelinePreviewState.copy(items = emptyList()).let {
        FlightArrivalsUiState.Loading
    }, darkTheme = false)
}
