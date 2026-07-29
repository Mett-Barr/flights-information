package moozy.flightinformation.feature.flights

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds

/* ============================================================
 *  尺寸與字串常數
 *
 *  版面間距一律走 8dp 系統（4 / 8 / 12 / 16 / 24 / 48）。
 *  「軌道」那幾個數字是繪圖座標，不是間距，另外標註。
 * ============================================================ */

internal val ScreenHorizontalPadding = 16.dp

/**
 * 單欄時間軸的最大寬度。卡片內時間靠左、狀態靠右，兩者距離一拉開就不易對應；
 * 600dp 讓橫向的行長貼近直向（約 411dp）的比例，超出的寬度留白而不是攤平卡片。
 */
internal val TimelineContentMaxWidth = 600.dp

/**
 * 軌道車道寬度。軌道線與節點都畫在這條車道的中線上，
 * 整點膠囊也以這條中線為中心。車道要夠寬，膠囊往左溢出時才不會被螢幕邊緣切掉。
 */
internal val RailLaneWidth = 36.dp

/** 車道與卡片之間的空隙，同時也是節點連到卡片那段短線的長度來源。 */
internal val RailGutterWidth = 8.dp

/** 軌道線的 x 座標（相對於已扣掉左右邊距的內容區）。繪圖座標，不是間距。 */
internal val RailCenterX = RailLaneWidth / 2

/** 卡片左緣的 x 座標。節點的連接線就是畫到這裡。繪圖座標。 */
internal val CardStartX = RailLaneWidth + RailGutterWidth

internal val RailLineWidth = 2.dp
internal val NodeDiameter = 12.dp
internal val NextNodeDiameter = 16.dp
internal val NodeBorderWidth = 2.5.dp

/** 「下一班」節點外圈那層柔光的厚度。 */
internal val NextNodeHaloWidth = 6.dp

/** 每一列上下各留這麼多，卡片之間就是兩倍。軌道會穿過這段空白，所以線不會斷。 */
internal val RowVerticalGap = 4.dp

internal val CardHorizontalPadding = 12.dp
internal val CardVerticalPadding = 8.dp
internal val CardTextSpacing = 2.dp

/** 整點標記上下的留白：時間軸要能「呼吸」，一天的節奏才看得出來。 */
internal val HourMarkerVerticalGap = 16.dp
internal val HourPillHorizontalPadding = 10.dp
internal val HourPillVerticalPadding = 4.dp
internal val HourPillBorderWidth = 1.dp

internal val NowRuleWidth = 2.dp
internal val NowMarkerHeight = 32.dp
internal val NowLabelHorizontalPadding = 8.dp

/** 整點膠囊釘在軌道上、左右都會溢出車道，附註文字要從這個距離之後才開始。 */
internal val HourNoteGutter = 32.dp

/** 軌道頭尾的淡出段：告訴讀者這條線是被截斷的，不是到此為止。 */
internal val RailHeadHeight = 12.dp
internal val RailTailHeight = 28.dp

internal val LogoSize = 32.dp
internal val LogoSpacing = 8.dp
internal val StatusBadgeMaxWidth = 132.dp
internal val HeadlineBadgeSpacing = 8.dp
internal val FreshnessIndicatorSize = 32.dp

/** 可點擊元素的最小高度，符合觸控目標規範。 */
internal val MinTouchTarget = 48.dp

/** 刷新指示器最短的可見時間，避免快速完成時只閃一下。 */
internal val MIN_REFRESH_VISIBLE = 700.milliseconds

/** 萬一主題把行高設成非 sp（Em 或未指定），節點高度就退回這個值。 */
internal val FallbackHeadlineLineHeight = 32.dp

internal const val UNKNOWN_TIME_TEXT = "--:--"

internal const val RAIL_HEAD_KEY = "timeline-rail-head"
internal const val RAIL_TAIL_KEY = "timeline-rail-tail"
internal const val UNSCHEDULED_MARKER_KEY = "timeline-unscheduled-marker"
internal const val EMPTY_STATE_KEY = "timeline-empty"

internal const val MINUTES_PER_HOUR = 60

/** 首次定位時，「下一班」上方保留幾列當作前情提要。 */
internal const val PRE_NEXT_CONTEXT_ROWS = 2

/** 「下一班」節點外圈柔光的透明度：夠亮到看得出來，又不至於變成第二個節點。 */
internal const val NEXT_NODE_HALO_ALPHA = 0.18f

internal const val DisabledContainerOpacity = 0.12f
internal const val DisabledContentOpacity = 0.38f

/** 等寬數字。時間是要被上下比較的東西，位數不對齊就白費了整條時間軸。 */
internal const val TABULAR_NUMBERS = "tnum"

internal val updatedAtFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

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
 * 2. **NOW 是跨過內容的時間分界。** 現在是一個瞬間，不該佔據軌道的一段；
 *    因此用 tertiary 的橫線穿過軌道與卡片欄，讓已過去與尚未到達的班次一眼分開。
 *    標籤放在末端、以 surface 墊底，既不會遮住軌道，又能在規則線上維持足夠對比。
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
internal fun TimelineRailCap(
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
internal fun TimelineHourMarker(
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

@Composable
internal fun TimelineNowMarker(
    now: LocalTime,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val label = stringResource(R.string.flights_timeline_now)
    val time = "${twoDigits(now.hour)}:${twoDigits(now.minute)}"
    val description = stringResource(R.string.flights_timeline_now_description, time)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenHorizontalPadding)
            .semantics { contentDescription = description }
            .height(NowMarkerHeight)
            .drawBehind {
                val x = RailCenterX.toPx()
                val y = size.height / 2
                drawLine(
                    color = scheme.outlineVariant,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = RailLineWidth.toPx()
                )
                drawLine(
                    color = scheme.tertiary,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = NowRuleWidth.toPx()
                )
            }
    ) {
        Surface(
            color = scheme.surface,
            contentColor = scheme.tertiary,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Text(
                text = "$label $time",
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = NowLabelHorizontalPadding)
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
