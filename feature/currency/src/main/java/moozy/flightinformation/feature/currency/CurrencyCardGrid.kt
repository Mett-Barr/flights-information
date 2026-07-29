@file:OptIn(ExperimentalMaterial3Api::class)

package moozy.flightinformation.feature.currency

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import moozy.flightinformation.domain.error.LoadError
import moozy.flightinformation.domain.value.CurrencyCode
import moozy.flightinformation.core.ui.messageRes
import java.math.BigDecimal

/**
 * 方向：卡片網格（card grid）。
 *
 * 其他方案都是「一列一個幣別」的清單；這個刻意不是。理由是清單優化的是**比較**
 * ——把數字對齊成一欄才好上下掃——但真正用匯率的場合多半是「我只想看那一個幣別」。
 * 掃描單一目標時，清單的每一列長得都一樣，反而得靠讀字才找得到；卡片有面積、有位置、
 * 有自己的底紋，可以用「形狀」找而不是用「讀」找。
 *
 * 這個版本的取捨：
 * - [LazyVerticalGrid] 一個幣別一張卡。手機直向兩欄（設計就是在這個尺寸上被選中的），
 *   平板／展開的折疊機再往上加欄。密度換掉了，換來的是每張卡都認得出來。
 * - 卡片角落壓一個大的、低透明度的貨幣符號當浮水印。這是唯一不需要外部資產
 *   （國旗圖檔）就能讓每張卡在餘光裡長得不一樣的辦法。
 * - 基準幣別是跨整列的整寬卡片、實心 primaryContainer，和下面的目標卡明顯不同色：
 *   上面那張是**輸入**，下面那些是**輸出**，顏色就把這句話講完了。
 * - 點任何一張目標卡，就把那個幣別換成新的基準（換算方向反過來）。這比清單版的
 *   「點一列會把它搬到最前面」有用得多——後者只會讓使用者覺得清單無故重排。
 * - 計算機走 [ModalBottomSheet]：sheet 本來就畫在導航列之上，不需要為了騰空間去藏導航列，
 *   也就沒有「開著計算機轉螢幕，導航列回不來」的軟鎖。
 *
 * Material 3 實作紀律（版面沒有改，改的是用什麼把它做出來）：
 * - 容器一律 [Card]／[Button]／[FilterChip] 等 material3 元件，不再自己用 Surface 拼。
 * - 顏色只從 `MaterialTheme.colorScheme` 取，且用 [CardDefaults.cardColors] 只給容器色，
 *   內容色交給 `contentColorFor` 配對，避免手動配錯 on-color。
 * - 圓角只從 `MaterialTheme.shapes` 取，字級只從 `MaterialTheme.typography` 取，
 *   間距只從 [CardGridSpace] 取（4/8dp 網格）。
 */
@Composable
internal fun CardGridContent(
    state: CurrencyUiState.Content,
    bottomPadding: Dp,
    animateEntrance: Boolean,
    onAmountClick: () -> Unit,
    onBaseClick: () -> Unit,
    onPromote: (CurrencyRow) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseCode = state.baseCode
    val baseAmount = (state as? CurrencyUiState.Content.WithConversion)?.baseAmount

    // 基準幣別本身就是最上面那張整寬卡片，留在網格裡只會讓同一個數字出現兩次。
    val targets = remember(state.rows, baseCode) {
        state.rows.filter { row -> row.code != baseCode.code }
    }

    // MD3 的 window size class（不自己量 BoxWithConstraints）。
    // compact 維持兩欄——設計是在手機直向被挑中的，那個尺寸下的樣子不能動；
    // medium／expanded 才往上加欄，否則卡片會被拉成又寬又空的長條。
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val atLeastMedium =
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    val atLeastExpanded =
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
    val columns = when {
        atLeastExpanded -> 4
        atLeastMedium -> 3
        else -> 2
    }
    // MD3 版面邊界：compact 16dp、medium 以上 24dp。
    val horizontalMargin = if (atLeastMedium) CardGridSpace.lg else CardGridSpace.md
    // 欄距：medium 以上用 MD3 建議的 16dp；compact 保留設計挑中的 12dp（仍在 4dp 網格上）。
    val gutter = if (atLeastMedium) CardGridSpace.md else CardGridSpace.sm

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            // MD3：大螢幕不要把內容拉滿，超過就置中留白。
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = CARD_GRID_MAX_CONTENT_WIDTH)
                .fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(gutter),
            verticalArrangement = Arrangement.spacedBy(gutter),
            contentPadding = PaddingValues(
                start = horizontalMargin,
                end = horizontalMargin,
                top = CardGridSpace.xs,
                // 導航列高度由宿主傳進來，最後一排卡片不能被它蓋住。
                bottom = bottomPadding + CardGridSpace.lg
            )
        ) {
            item(key = "base", span = { GridItemSpan(maxLineSpan) }) {
                CardGridBaseTile(
                    baseCode = baseCode,
                    amount = baseAmount,
                    onAmountClick = onAmountClick,
                    onBaseClick = onBaseClick
                )
            }

            item(key = "label", span = { GridItemSpan(maxLineSpan) }) {
                CardGridLabel(
                    text = if (baseAmount == null) {
                        stringResource(R.string.currency_per_one, baseCode.code)
                    } else {
                        stringResource(R.string.currency_converted_from, baseCode.code)
                    }
                )
            }

            if (targets.isEmpty()) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    CardGridEmpty(onManageClick = onBaseClick)
                }
            }

            itemsIndexed(
                items = targets,
                key = { _, row -> row.code }
            ) { index, row ->
                CardGridTile(
                    row = row,
                    baseCode = baseCode.code,
                    index = index,
                    animateEntrance = animateEntrance,
                    onSelect = { onPromote(row) },
                    // fade 交給進場動畫自己處理，這裡只保留位置變動的動畫，
                    // 兩套淡入疊在一起會互相打架。
                    modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
                )
            }
        }
    }
}

/**
 * 基準卡片：跨滿整列、實心底色，讀起來就是「這是輸入」。
 *
 * 金額與幣別是兩個各自獨立、但連在一起讀的點擊區——「1,250.00 USD」正是下面每張卡的來源。
 * 金額欄是可點的 [Card]（點了開計算機），幣別是真的 [Button]（MD3 的按鈕就是 full 圓角）。
 */
@Composable
private fun CardGridBaseTile(
    baseCode: CurrencyCode,
    amount: BigDecimal?,
    onAmountClick: () -> Unit,
    onBaseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 還沒輸入時用 1 當佔位值：此時每張卡顯示的就是「1 基準幣別換得到多少」，也就是匯率本身。
    val amountText = (amount ?: BigDecimal.ONE).toGridMoneyText()
    // 位數太多就換小一級的字級，長金額才不會在窄卡片裡被切掉。
    val amountStyle = when {
        amountText.length > 15 -> MaterialTheme.typography.titleLarge
        amountText.length > 10 -> MaterialTheme.typography.headlineSmall
        else -> MaterialTheme.typography.headlineMedium
    }

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        // 只給容器色，內容色由 contentColorFor 配成 onPrimaryContainer。
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            CardGridWatermark(
                glyph = baseCode.symbol.ifBlank { baseCode.code.take(1) },
                color = MaterialTheme.colorScheme.onPrimaryContainer
                    .copy(alpha = CARD_GRID_WATERMARK_ALPHA),
                alignment = Alignment.CenterEnd,
                offsetX = CardGridSpace.lg,
                offsetY = 0.dp
            )

            Column(modifier = Modifier.padding(CardGridSpace.md)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Card(
                        onClick = onAmountClick,
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .semantics { role = Role.Button }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = CARD_GRID_ACTION_HEIGHT)
                                .padding(
                                    horizontal = CardGridSpace.md,
                                    vertical = CardGridSpace.xs
                                )
                        ) {
                            Text(
                                text = baseCode.symbol,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(CardGridSpace.xs))
                            Text(
                                text = amountText,
                                style = amountStyle.copy(fontFeatureSettings = CARD_GRID_TABULAR),
                                fontWeight = FontWeight.SemiBold,
                                color = if (amount == null) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = rememberGridKeypadIcon(),
                                contentDescription = stringResource(R.string.currency_open_keypad),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(CardGridSpace.sm))

                    // 56dp 是 MD3 中型按鈕的容器高度；1.4.0 還沒把尺寸專用的
                    // ButtonDefaults 常數開放出來，所以高度自己給，其餘（形狀、
                    // 顏色、內距、圖示尺寸）全走 ButtonDefaults。
                    Button(
                        onClick = onBaseClick,
                        modifier = Modifier.heightIn(min = CARD_GRID_ACTION_HEIGHT)
                    ) {
                        Text(
                            text = baseCode.code,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                        Icon(
                            imageVector = rememberGridChevronDownIcon(),
                            contentDescription = stringResource(R.string.currency_change_base_currency),
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    }

                }

                Spacer(modifier = Modifier.height(CardGridSpace.xs))

                Text(
                    text = if (amount == null) {
                        stringResource(R.string.currency_tap_amount_hint, baseCode.fullName)
                    } else {
                        baseCode.fullName
                    },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 目標卡片：一個幣別一張。
 *
 * 排版順序就是閱讀順序——先看到幣別代碼（我在找哪一個），再看到換算後的金額（答案），
 * 最後才是全名與單位匯率（佐證）。背景角落的大符號是裝飾，但它讓每張卡在餘光裡
 * 有不同的形狀，不必讀字就能定位。
 *
 * 「點一下＝換基準」在畫面上沒有可見的提示，所以用 semantics 的 onClick label
 * 把這件事講給 TalkBack 聽。
 */
@Composable
private fun CardGridTile(
    row: CurrencyRow,
    baseCode: String,
    index: Int,
    animateEntrance: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Plain（還沒輸入金額）時 rate 就是「1 基準幣別」的換算結果，直接拿來當金額顯示。
    val converted = (row as? CurrencyRowWithConversion)?.convertedAmount ?: row.rate
    val amountText = converted.toGridMoneyText()
    val amountStyle = when {
        amountText.length > 13 -> MaterialTheme.typography.titleMedium
        amountText.length > 9 -> MaterialTheme.typography.titleLarge
        else -> MaterialTheme.typography.headlineSmall
    }

    val entrance = rememberCardGridEntrance(index = index, animate = animateEntrance)

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // 按下去縮一點點：這張卡按了會換基準，是個有後果的動作，手感上要先確認得到。
    val pressScale = animateFloatAsState(
        targetValue = if (pressed) CARD_GRID_PRESS_SCALE else 1f,
        animationSpec = tween(
            durationMillis = PRESS_DURATION.inWholeMilliseconds.toInt(),
            easing = CardGridStandardEasing
        ),
        label = "cardGridPressScale"
    )

    val selectLabel = stringResource(R.string.currency_use_as_base, row.code)

    Card(
        onClick = onSelect,
        interactionSource = interactionSource,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = modifier
            .fillMaxWidth()
            // 動畫值只在 layer 這一層讀，不會讓整張卡每一格畫面重組。
            .graphicsLayer {
                val progress = entrance.value
                alpha = progress
                translationY = (1f - progress) * CardGridSpace.lg.toPx()
                scaleX = pressScale.value
                scaleY = pressScale.value
            }
            .semantics {
                role = Role.Button
                onClick(label = selectLabel, action = null)
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = CARD_GRID_TILE_MIN_HEIGHT)
        ) {
            CardGridWatermark(
                glyph = row.symbol.ifBlank { row.code.take(1) },
                color = MaterialTheme.colorScheme.onSurfaceVariant
                    .copy(alpha = CARD_GRID_WATERMARK_ALPHA),
                alignment = Alignment.BottomEnd,
                offsetX = CardGridSpace.md,
                offsetY = CardGridSpace.lg
            )

            Column(modifier = Modifier.padding(CardGridSpace.md)) {
                Text(
                    text = row.code,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(CardGridSpace.md))

                Text(
                    text = amountText,
                    style = amountStyle.copy(fontFeatureSettings = CARD_GRID_TABULAR),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = row.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(CardGridSpace.xxs))

                Text(
                    text = stringResource(
                        R.string.currency_rate_per_base,
                        row.rate.toGridRateText(),
                        baseCode
                    ),
                    style = MaterialTheme.typography.labelSmall
                        .copy(fontFeatureSettings = CARD_GRID_TABULAR),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 卡片角落的貨幣符號浮水印。
 *
 * 用 `matchParentSize` 疊在內容底下，所以它不參與卡片的量測——字級再大也不會把卡撐高。
 * `wrapContentSize(unbounded = true)` 讓字自己量成完整大小再溢出，多出來的部分由 [Card]
 * 的圓角裁掉，得到「被切了一角的大字」這個效果。
 *
 * 字級不是憑空的 sp：從 `displayLarge`（MD3 型階最大的一級）等比放大，
 * 換掉主題的型階時浮水印會跟著動。它是裝飾，所以 semantics 直接清掉，
 * TalkBack 不會把「$」念出來。
 */
@Composable
private fun BoxScope.CardGridWatermark(
    glyph: String,
    color: Color,
    alignment: Alignment,
    offsetX: Dp,
    offsetY: Dp
) {
    val display = MaterialTheme.typography.displayLarge
    val watermarkStyle = display.copy(
        fontSize = display.fontSize * CARD_GRID_WATERMARK_SCALE,
        lineHeight = display.lineHeight * CARD_GRID_WATERMARK_SCALE,
        fontWeight = FontWeight.Bold
    )

    Box(
        modifier = Modifier
            .matchParentSize()
            .clearAndSetSemantics { }
    ) {
        Text(
            text = glyph,
            style = watermarkStyle,
            color = color,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
            modifier = Modifier
                .align(alignment)
                .wrapContentSize(align = Alignment.Center, unbounded = true)
                .offset(x = offsetX, y = offsetY)
        )
    }
}

/** 網格裡的分段標籤：把「輸入」和「輸出」之間切開來。 */
@Composable
private fun CardGridLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .padding(start = CardGridSpace.xs, top = CardGridSpace.xs)
            .semantics { heading() }
    )
}

@Composable
private fun CardGridEmpty(
    onManageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CardGridSpace.md),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CardGridSpace.lg, vertical = CardGridSpace.xxl)
    ) {
        Text(
            text = stringResource(R.string.currency_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        // Button 內建 minimumInteractiveComponentSize()，觸控目標已經是 48dp，
        // 不需要再自己撐高度。
        Button(onClick = onManageClick) {
            Text(text = stringResource(R.string.currency_choose_currencies))
        }
    }
}

@Composable
internal fun CardGridError(
    error: LoadError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CardGridSpace.md),
            modifier = Modifier.padding(CardGridSpace.xl)
        ) {
            Text(
                text = stringResource(error.messageRes()),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRetry) {
                Text(text = stringResource(moozy.flightinformation.core.ui.R.string.action_retry))
            }
        }
    }
}

/* ============================================================
 *  設計 token：間距、尺寸、動態
 *
 *  MD3 的間距是 4／8dp 網格，所以整個檔案只從 [CardGridSpace] 取值，
 *  不再散落 10dp、14dp、20dp 這種對不上格線的數字。
 * ============================================================ */

/** 4／8dp 間距階梯。padding、gap、間隔一律從這裡取。 */
