package moozy.flightinformation.presentation.screen

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import moozy.flightinformation.R
import moozy.flightinformation.domain.error.LoadError
import moozy.flightinformation.presentation.mapper.messageRes
import moozy.flightinformation.presentation.state.flights.FlightArrivalItemUiModel
import moozy.flightinformation.presentation.state.flights.FlightArrivalsUiState
import moozy.flightinformation.presentation.state.flights.FlightStatusText
import moozy.flightinformation.presentation.theme.FlightInformationTheme
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlin.time.TimeMark
import kotlin.time.Duration
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.clip

/* ============================================================
 *  尺寸與字串常數
 *
 *  版面間距一律走 8dp 系統（4 / 8 / 12 / 16 / 24 / 48）。
 *  「軌道」那幾個數字是繪圖座標，不是間距，另外標註。
 * ============================================================ */

@Composable
internal fun TimelineFlightRow(
    entry: TimelineEntry,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val item = entry.item
    val isCancelled = item.isCancelled
    val badgeText = item.badgeText.resolve()

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
    val containerColor = if (isCancelled) {
        scheme.onSurface.copy(alpha = DisabledContainerOpacity)
    } else when {
        entry.isNext -> scheme.surfaceContainerHigh
        entry.isPast -> scheme.surfaceContainerLow
        else -> scheme.surfaceContainer
    }
    val contentColor = if (isCancelled) {
        scheme.onSurface.copy(alpha = DisabledContentOpacity)
    } else if (entry.isPast) {
        scheme.onSurfaceVariant
    } else {
        scheme.onSurface
    }

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
                    monogram = monogramOf(item.carrierLineText),
                    disabled = isCancelled
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
                            color = if (entry.isNext && !isCancelled) scheme.primary else contentColor,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(HeadlineBadgeSpacing))
                        Spacer(modifier = Modifier.weight(1f))
                        StatusBadge(
                            statusKey = item.statusKey,
                            text = badgeText,
                            disabled = isCancelled
                        )
                    }

                    Text(
                        text = item.carrierLineText,
                        // 下一班用 emphasized 字級角色（1.5.0-alpha 才公開的 API），
                        // 「目前作用中」在規格裡本來就是 emphasized 的用途。
                        style = if (entry.isNext && !isCancelled) {
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
                        color = if (isCancelled) contentColor else scheme.onSurfaceVariant,
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
    disabled: Boolean,
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
    val badgeContainer = if (disabled) {
        scheme.onSurface.copy(alpha = DisabledContainerOpacity)
    } else {
        container
    }
    val badgeContent = if (disabled) {
        scheme.onSurface.copy(alpha = DisabledContentOpacity)
    } else {
        content
    }

    Badge(
        containerColor = badgeContainer,
        contentColor = badgeContent,
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
    disabled: Boolean,
    modifier: Modifier = Modifier
) {
    var loaded by remember(url) { mutableStateOf(false) }
    val inspecting = LocalInspectionMode.current

    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (disabled) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContainerOpacity)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
        contentColor = if (disabled) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContentOpacity)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
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
                        .padding(4.dp)
                        .alpha(if (disabled) DisabledContentOpacity else 1f),
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

