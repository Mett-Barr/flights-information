package moozy.flightinformation.feature.flights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun TimelineLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
internal fun TimelineError(
    errorMessageRes: Int,
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
                text = stringResource(errorMessageRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.heightIn(min = MinTouchTarget)
            ) {
                Text(text = stringResource(moozy.flightinformation.core.ui.R.string.action_retry))
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
internal fun TimelineEmpty(
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
internal fun TimelineContent(
    content: FlightArrivalsUiState.Content,
    onRefresh: () -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    if (content.items.isEmpty()) {
        TimelineEmpty(
            isRefreshing = refreshIndicatorVisible(content.isRefreshing),
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
            isRefreshing = refreshIndicatorVisible(content.isRefreshing),
            onRefresh = onRefresh,
            modifier = modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = TimelineContentMaxWidth)
                    .fillMaxSize(),
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

                        is TimelineRow.Now -> TimelineNowMarker(now = row.now)

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
