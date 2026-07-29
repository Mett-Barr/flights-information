package moozy.flightinformation.feature.flights

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.semantics.Role
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
import moozy.flightinformation.feature.flights.R
import moozy.flightinformation.domain.error.LoadError
import moozy.flightinformation.core.ui.messageRes
import moozy.flightinformation.feature.flights.FlightArrivalItemUiModel
import moozy.flightinformation.feature.flights.FlightArrivalsUiState
import moozy.flightinformation.feature.flights.FlightStatusText
import moozy.flightinformation.core.ui.FlightInformationTheme
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.clip

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
                refreshEvent = refreshEvent,
                onRefresh = onRefresh,
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
    onRefresh: () -> Unit,
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
            isRefreshing = refreshIndicatorVisible(content.isRefreshing),
            refreshEvent = refreshEvent,
            onRefresh = onRefresh,
        )
    }
}

/**
 * [active] 結束後，讓指示器繼續顯示 [tailMillis]。
 *
 * 抓取通常幾百毫秒內就結束，指示器會冒出一點點又立刻縮回去，看起來像閃爍而不是回饋。
 * 這裡只延後「結束」的時機，資料本身照樣立刻上畫面——不會為了動畫好看而讓使用者晚一點
 * 看到新資料。
 */
@Composable
internal fun refreshIndicatorVisible(active: Boolean, tailMillis: Long = MIN_REFRESH_VISIBLE_MILLIS): Boolean =
    produceState(initialValue = active, active, tailMillis) {
        if (active) {
            value = true
        } else {
            delay(tailMillis)
            value = false
        }
    }.value

/**
 * 動畫值只在 Material 3 指示器的 progress lambda 內讀取，避免逐幀重組時間軸與清單。
 */
@Composable
private fun FreshnessIndicator(
    updatedAt: LocalDateTime,
    isRefreshing: Boolean,
    refreshEvent: SharedFlow<Unit>?,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(1f) }
    val refreshDescription = stringResource(
        R.string.flights_refresh_countdown,
        updatedAt.format(updatedAtFormatter),
    )
    val refreshLabel = stringResource(R.string.action_refresh)

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

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(MinTouchTarget)
            // 先裁圓再掛點擊：漣漪會被裁切形狀框住，否則方形的觸控範圍會露出方形水波。
            .clip(CircleShape)
            .semantics { contentDescription = refreshDescription }
            .clickable(
                enabled = !isRefreshing,
                onClickLabel = refreshLabel,
                role = Role.Button,
                onClick = onRefresh,
            )
    ) {
        // 這個環表達的是「資料還剩多新鮮」，不是「正在抓取」。抓取中另外有刷新圖示，
        // 環再跟著轉就變成兩個轉圈講同一件事，而且會蓋掉倒數本來要傳達的訊息。
        CircularProgressIndicator(
            progress = { progress.value },
            modifier = Modifier.size(FreshnessIndicatorSize)
        )
    }
}

/* ============================================================
 *  三種狀態
 * ============================================================ */

