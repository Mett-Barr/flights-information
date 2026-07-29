@file:OptIn(ExperimentalMaterial3Api::class)

package moozy.flightinformation.presentation.screen

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.launch
import moozy.flightinformation.R
import moozy.flightinformation.domain.error.LoadError
import moozy.flightinformation.domain.value.CurrencyCode
import moozy.flightinformation.feature.calculator.Calculator
import moozy.flightinformation.feature.calculator.CalculatorUI
import moozy.flightinformation.presentation.mapper.messageRes
import moozy.flightinformation.presentation.model.currency.CurrencyRow
import moozy.flightinformation.presentation.model.currency.CurrencyRowPlain
import moozy.flightinformation.presentation.model.currency.CurrencyRowWithConversion
import moozy.flightinformation.presentation.state.currency.CurrencyUiState
import moozy.flightinformation.util.collection.toggle
import moozy.flightinformation.presentation.theme.FlightInformationTheme
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat

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
fun CurrencyScreen(
    state: CurrencyUiState,
    onCalculatorShow: () -> Unit,
    onCalculatorDismiss: () -> Unit,
    onMoneyInput: (
        content: CurrencyUiState.Content,
        amountText: String?,
    ) -> Unit,
    onCurrencyClick: (String) -> Unit,
    onCurrencySelect: (CurrencyCode) -> Unit,
    onSearch: (CurrencyUiState.Content) -> Unit,
    onBaseCurrencySelect: (CurrencyCode) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues = PaddingValues(0.dp)
) {
    // 此設計以 ModalBottomSheet 呈現計算機，無需隱藏導覽列。
    // 註：onCurrencyClick 在這個版面刻意不接。它在 ViewModel 的唯一效果是把該列搬到最前面，
    // 而網格裡「點一張卡」已經被指派給更有意義的動作（換基準）。簽章由 pager 統一決定，故保留。
    val layoutDirection = LocalLayoutDirection.current
    val content = state as? CurrencyUiState.Content

    // 兩張 sheet 的開關都要撐過旋轉，否則轉一下畫面就把使用者做到一半的事情關掉。
    var showCalculator by rememberSaveable { mutableStateOf(false) }
    var showPicker by rememberSaveable { mutableStateOf(false) }

    // 進場動畫只在「內容第一次出現」時跑一次。旗標存進 saveable，
    // 旋轉之後也不會再重播；每次匯率刷新更不該讓整片網格再淡入一遍。
    var entranceDone by rememberSaveable { mutableStateOf(false) }
    val hasContent = content != null
    LaunchedEffect(hasContent) {
        if (hasContent && !entranceDone) {
            delay(ENTRANCE_TOTAL_MS)
            entranceDone = true
        }
    }
    // IDE 的 preview 只畫第一格畫面，動畫不會跑；不關掉的話卡片會停在 alpha = 0。
    val inspecting = LocalInspectionMode.current
    val animateEntrance = !entranceDone && !inspecting

    // 計算機只有「已成立的金額」會送進 ViewModel；打到一半的算式不需要撐過旋轉，
    // 旋轉後直接用 state 裡的 baseAmount 重新 seed 回去，使用者看到的數字不會掉。
    val restoredAmount = (state as? CurrencyUiState.Content.WithConversion)?.baseAmount
    val calculator = remember {
        Calculator().apply {
            if (restoredAmount != null && restoredAmount.signum() != 0) {
                seedFromDisplayString(restoredAmount.stripTrailingZeros().toPlainString())
            }
        }
    }

    val currentState by rememberUpdatedState(state)
    val currentOnMoneyInput by rememberUpdatedState(onMoneyInput)

    LaunchedEffect(calculator) {
        // 空輸入時金額是 0，snapshotFlow 的首次發射會把既有的換算結果洗回 Plain，
        // 所以在使用者真的輸入前先把發射丟掉。
        // 取 lastStableAmountText 而非 equal：那是計算機已格式化好的十進位字串，
        // 內部的 Double 不會以科學記號或二進位誤差外流，匯率管線維持全程 BigDecimal。
        snapshotFlow { calculator.lastStableAmountText.value.toBigDecimalOrNull() }
            .dropWhile { amount -> amount == null || amount.signum() == 0 }
            .collect { amount ->
                val target = currentState as? CurrencyUiState.Content ?: return@collect
                currentOnMoneyInput(target, amount?.toPlainString())
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(
                start = innerPadding.calculateStartPadding(layoutDirection),
                end = innerPadding.calculateEndPadding(layoutDirection)
            )
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (val current = state) {
                CurrencyUiState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

                is CurrencyUiState.Error -> CardGridError(
                    error = current.error,
                    onRetry = onRetry
                )

                is CurrencyUiState.Content -> CardGridContent(
                    state = current,
                    bottomPadding = innerPadding.calculateBottomPadding(),
                    animateEntrance = animateEntrance,
                    onAmountClick = { showCalculator = true },
                    onBaseClick = { showPicker = true },
                    onPromote = { row ->
                        // 點目標卡＝把它變成新的基準，換算方向整個翻過來。
                        val code = CurrencyCode.fromCode(row.code)
                        if (code != null) {
                            // onBaseCurrencySelect 是 toggle：對同一個 code 再點一次會清成 null，
                            // 所以只有真的換基準時才呼叫它。
                            if (current.selectedBaseCurrency != code) onBaseCurrencySelect(code)
                            // getCurrencies 讀的是「傳進去的那份 content」而不是 ViewModel 當下的
                            // state，所以要先把新基準寫進 content 再送出，否則這一發還是用舊基準。
                            onSearch(current.withGridBase(code))
                        }
                    },
                    onRefresh = { onSearch(current) }
                )
            }
        }
    }

    if (showCalculator && content != null) {
        CardGridCalculatorSheet(
            calculator = calculator,
            baseCode = content.baseCode,
            onDismiss = { showCalculator = false }
        )
    }

    if (showPicker && content != null) {
        CardGridPickerSheet(
            state = content,
            onBaseSelect = { code ->
                if (content.selectedBaseCurrency != code) onBaseCurrencySelect(code)
                onSearch(content.withGridBase(code))
            },
            onTargetToggle = { code ->
                onCurrencySelect(code)
                onSearch(content.withGridSelection(code))
            },
            onDismiss = { showPicker = false }
        )
    }
}

/* ============================================================
 *  網格本體
 * ============================================================ */

@Composable
private fun CardGridContent(
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
            durationMillis = PRESS_DURATION_MS,
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
private fun CardGridError(
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
                Text(text = stringResource(R.string.action_retry))
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
private object CardGridSpace {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

/** 基準卡片上兩個控制項的高度＝MD3 中型按鈕的容器高度。 */
private val CARD_GRID_ACTION_HEIGHT = 56.dp

/** 目標卡片的最小高度（8dp 網格上的 18 格）。 */
private val CARD_GRID_TILE_MIN_HEIGHT = 144.dp

/** 觸控目標下限；chip 被撐到這個高度，網格裡才好按。 */
private val CARD_GRID_MIN_TOUCH_TARGET = 48.dp

/** MD3：大螢幕的內容最大寬度（建議 840–1040dp），超過就置中留白。 */
private val CARD_GRID_MAX_CONTENT_WIDTH = 1040.dp

/** 浮水印相對 `displayLarge` 的放大倍率（57sp × 1.9 ≒ 108sp）。 */
private const val CARD_GRID_WATERMARK_SCALE = 1.9f

/** 浮水印透明度：MD3 狀態層的 0.12。 */
private const val CARD_GRID_WATERMARK_ALPHA = 0.12f

/** 按下時縮到幾成。 */
private const val CARD_GRID_PRESS_SCALE = 0.955f

/** MD3 emphasized decelerate：元素「進場」用的曲線。 */
private val CardGridEnterEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

/** MD3 standard：小型、來回都在畫面內的狀態轉換（按下回饋）用的曲線。 */
private val CardGridStandardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/* ============================================================
 *  進場動畫
 * ============================================================ */

/** 每張卡之間錯開的間隔（MD3 duration short1）。 */
private const val ENTRANCE_STEP_MS = 50L

/** 最多錯開幾階。幣別可以有三十幾個，不設上限的話最後一張要等一秒多才進來。 */
private const val ENTRANCE_MAX_STEPS = 11

/** 單張卡的淡入時間（MD3 duration medium4：元素進場）。 */
private const val ENTRANCE_DURATION_MS = 400

/** 整段進場的長度：最後一階的延遲（11 × 50）加上一張卡的淡入（400）。 */
private const val ENTRANCE_TOTAL_MS = 950L

/** 按下回饋的長度（MD3 duration short2）。 */
private const val PRESS_DURATION_MS = 100

/**
 * 單張卡的進場進度（0 → 1）。
 *
 * 回傳 [Animatable] 而不是 `Float`，呼叫端才能在 `graphicsLayer` 的 lambda 裡讀值——
 * 那是延遲讀取，動畫期間只會重畫，不會每一格畫面都重組。
 */
@Composable
private fun rememberCardGridEntrance(
    index: Int,
    animate: Boolean
): Animatable<Float, AnimationVector1D> {
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(animate, index) {
        if (animate) {
            delay(index.coerceAtMost(ENTRANCE_MAX_STEPS) * ENTRANCE_STEP_MS)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = ENTRANCE_DURATION_MS,
                    easing = CardGridEnterEasing
                )
            )
        } else {
            // 進場已經跑完（或根本不該跑）時直接到位：捲出去再捲回來的卡片不會重新淡入。
            progress.snapTo(1f)
        }
    }
    return progress
}

/* ============================================================
 *  計算機：modal bottom sheet
 * ============================================================ */

/**
 * 計算機放在 modal bottom sheet 裡。
 *
 * Sheet 本來就畫在導航列之上，所以沒有任何東西需要被藏起來，也就不會有
 * 「轉螢幕之後導航列回不來」的軟鎖。內容加了 verticalScroll，橫置時鍵盤高度
 * 超過可用高度也只會變成可捲動，不會被裁掉。
 */
@Composable
private fun CardGridCalculatorSheet(
    calculator: Calculator,
    baseCode: CurrencyCode,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        val expression = calculator.infixString.ifBlank { "0" }
        val stable = calculator.lastStableAmountText.value

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CardGridSpace.md)
                .padding(bottom = CardGridSpace.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = baseCode.code,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(CardGridSpace.xs))
                Text(
                    text = expression,
                    style = MaterialTheme.typography.headlineSmall
                        .copy(fontFeatureSettings = CARD_GRID_TABULAR),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(CardGridSpace.xs))
                // 算式可無限延長，讓它先截斷才能保留結果與確認動作的可讀性。
                Text(
                    text = stringResource(R.string.currency_calculator_result, stable),
                    style = MaterialTheme.typography.titleMedium
                        .copy(fontFeatureSettings = CARD_GRID_TABULAR),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 120.dp)
                )
                TextButton(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onDismiss()
                        }
                    }
                ) {
                    Text(text = stringResource(R.string.action_done))
                }
            }

            Spacer(modifier = Modifier.height(CardGridSpace.xs))

            CalculatorUI(
                modifier = Modifier.fillMaxWidth(),
                calculator = calculator
            )
        }
    }
}

/* ============================================================
 *  幣別挑選：modal bottom sheet
 * ============================================================ */

/**
 * 幣別挑選。
 *
 * 兩種選取都立即生效，避免使用者誤以為還要確認尚未提交的變更。
 */
@Composable
private fun CardGridPickerSheet(
    state: CurrencyUiState.Content,
    onBaseSelect: (CurrencyCode) -> Unit,
    onTargetToggle: (CurrencyCode) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var mode by remember { mutableStateOf(CardGridPickerMode.Base) }
    // selectedBaseCurrency 首次載入是 null，此時真正生效的基準是回應裡的 baseCode。
    val base = state.selectedBaseCurrency ?: state.baseCode

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.currency_currencies),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = CardGridSpace.lg,
                        end = CardGridSpace.md,
                        bottom = CardGridSpace.sm
                    )
                    .semantics { heading() }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = CardGridSpace.lg)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    CardGridPickerMode.entries.forEachIndexed { index, item ->
                        SegmentedButton(
                            selected = mode == item,
                            onClick = { mode = item },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = CardGridPickerMode.entries.size
                            ),
                            label = {
                                Text(
                                    text = stringResource(
                                        if (item == CardGridPickerMode.Base) {
                                            R.string.currency_base_currency
                                        } else {
                                            R.string.currency_shown_in_grid
                                        }
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(CardGridSpace.sm))

                Text(
                    text = if (mode == CardGridPickerMode.Base) {
                        stringResource(R.string.currency_base_currency_description)
                    } else {
                        stringResource(R.string.currency_selected_description, state.selected.size)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(CardGridSpace.sm))

                CardGridCodeChips(
                    isSelected = { code ->
                        if (mode == CardGridPickerMode.Base) {
                            code == base
                        } else {
                            state.selected.any { it.code == code.code }
                        }
                    },
                    onClick = { code ->
                        if (mode == CardGridPickerMode.Base) onBaseSelect(code) else onTargetToggle(code)
                    }
                )

                Spacer(modifier = Modifier.height(CardGridSpace.xl))
            }
        }
    }
}

private enum class CardGridPickerMode {
    Base,
    Grid
}

/**
 * 幣別代碼一律三個字母，用固定欄數的 Row 排比 flow layout 整齊，
 * 也不用為了三十幾個 chip 在 sheet 裡再開一個會跟外層捲動打架的 lazy 容器。
 *
 * 兩種模式都用 [FilterChip]：MD3 的 filter chip 單選複選都算它的用法，
 * 而這裡都是「從一組固定選項裡篩」。
 */
@Composable
private fun CardGridCodeChips(
    isSelected: (CurrencyCode) -> Boolean,
    onClick: (CurrencyCode) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 3
) {
    val rows = remember(columns) { CurrencyCode.entries.chunked(columns) }
    Column(
        verticalArrangement = Arrangement.spacedBy(CardGridSpace.xs),
        modifier = modifier.fillMaxWidth()
    ) {
        rows.forEach { chunk ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(CardGridSpace.xs),
                modifier = Modifier.fillMaxWidth()
            ) {
                chunk.forEach { code ->
                    FilterChip(
                        selected = isSelected(code),
                        onClick = { onClick(code) },
                        label = {
                            Text(
                                text = code.code,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .semantics { testTag = "currency_picker_code_${code.code}" }
                            .heightIn(min = CARD_GRID_MIN_TOUCH_TARGET)
                    )
                }
                // 最後一列補空位，chip 寬度才不會被撐開。
                repeat(columns - chunk.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/* ============================================================
 *  數值格式化
 * ============================================================ */

/** OpenType 的 tabular figures：每個數字等寬，數值變動時卡片上的金額不會左右跳動。 */
private const val CARD_GRID_TABULAR = "tnum"

/** 主要金額：千分位 + 固定兩位小數。 */
private val cardGridMoneyFormat = DecimalFormat("#,##0.00")

/** 兩位小數會被捨成 0 的極小金額（例：1 KRW 換 USD）改用有效位數，否則整片網格都是 0.00。 */
private val cardGridSmallMoneyFormat = DecimalFormat("#,##0.00######")

/** 匯率是配角，最多四位小數，尾數的 0 不補。 */
private val cardGridRateFormat = DecimalFormat("#,##0.####")

private val cardGridSmallRateFormat = DecimalFormat("#,##0.########")

private fun BigDecimal.toGridMoneyText(): String {
    val rounded = setScale(2, RoundingMode.HALF_UP)
    return if (rounded.signum() == 0 && signum() != 0) {
        cardGridSmallMoneyFormat.format(round(MathContext(4, RoundingMode.HALF_UP)))
    } else {
        cardGridMoneyFormat.format(rounded)
    }
}

/**
 * 匯率的顯示值。
 *
 * 原始值是 316.8400616188 這種計算用精度，直接印給使用者只是雜訊；
 * 四位小數足以判斷匯率，尾數的 0 也一併去掉。
 */
private fun BigDecimal.toGridRateText(): String {
    val rounded = setScale(4, RoundingMode.HALF_UP)
    return if (rounded.signum() == 0 && signum() != 0) {
        cardGridSmallRateFormat.format(round(MathContext(4, RoundingMode.HALF_UP)))
    } else {
        cardGridRateFormat.format(rounded)
    }
}

/**
 * [CurrencyUiState.Content] 是 sealed class，沒有共用的 `copy`，只能逐子型別複製。
 *
 * 需要這個是因為 `onSearch` 讀的是傳進去的那份 content，不是 ViewModel 當下的 state：
 * 換基準幣別時如果直接把舊的 content 送出去，這一發請求會用舊基準。
 */
private fun CurrencyUiState.Content.withGridBase(
    code: CurrencyCode
): CurrencyUiState.Content = when (this) {
    is CurrencyUiState.Content.Plain -> copy(selectedBaseCurrency = code)
    is CurrencyUiState.Content.WithConversion -> copy(selectedBaseCurrency = code)
}

private fun CurrencyUiState.Content.withGridSelection(
    code: CurrencyCode
): CurrencyUiState.Content = when (this) {
    is CurrencyUiState.Content.Plain -> copy(selected = selected.toggle(code))
    is CurrencyUiState.Content.WithConversion -> copy(selected = selected.toggle(code))
}

/* ============================================================
 *  圖示
 *
 *  專案沒有 material-icons 依賴，而這次只能動這一個檔案，所以這幾個 24dp 圖形
 *  直接用 ImageVector 畫在這裡，不去改 build 檔或 res。填色取 LocalContentColor，
 *  不出現任何寫死的顏色。
 * ============================================================ */

private val CARD_GRID_ICON_SIZE = 24.dp

private fun buildCardGridIcon(
    name: String,
    tint: Color,
    subpaths: List<PathBuilder.() -> Unit>
): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = CARD_GRID_ICON_SIZE,
        defaultHeight = CARD_GRID_ICON_SIZE,
        viewportWidth = 24f,
        viewportHeight = 24f
    )
    subpaths.forEach { subpath ->
        builder.path(fill = SolidColor(tint)) { subpath() }
    }
    return builder.build()
}

/** 向下箭頭：基準幣別按鈕的「可以換一個」提示。 */
@Composable
private fun rememberGridChevronDownIcon(): ImageVector {
    val tint = LocalContentColor.current
    return remember(tint) {
        buildCardGridIcon(
            name = "cardGridChevronDown",
            tint = tint,
            subpaths = listOf(
                {
                    moveTo(16.59f, 8.59f)
                    lineTo(12f, 13.17f)
                    lineTo(7.41f, 8.59f)
                    lineTo(6f, 10f)
                    lineTo(12f, 16f)
                    lineTo(18f, 10f)
                    close()
                }
            )
        )
    }
}

/** 九宮格鍵盤：金額欄位的「點我會跳出計算機」提示。 */
@Composable
private fun rememberGridKeypadIcon(): ImageVector {
    val tint = LocalContentColor.current
    return remember(tint) {
        val origins = listOf(3f, 10f, 17f)
        val keys = origins.flatMap { y ->
            origins.map { x ->
                val block: PathBuilder.() -> Unit = {
                    moveTo(x, y)
                    horizontalLineToRelative(4f)
                    verticalLineToRelative(4f)
                    horizontalLineToRelative(-4f)
                    close()
                }
                block
            }
        }
        buildCardGridIcon(name = "cardGridKeypad", tint = tint, subpaths = keys)
    }
}

/* ============================================================
 *  Preview：假資料，讓設計可以在 IDE 裡直接看
 * ============================================================ */

private val previewBaseAmount = BigDecimal("1250")

private fun previewConvertedRows(): List<CurrencyRow> = listOf(
    CurrencyRowWithConversion(
        code = "EUR",
        name = "Euro",
        symbol = "€",
        rate = BigDecimal("0.9231004512"),
        convertedAmount = BigDecimal("1153.8755640000"),
        baseAmount = previewBaseAmount,
        baseCode = "USD"
    ),
    CurrencyRowWithConversion(
        code = "JPY",
        name = "Japanese Yen",
        symbol = "¥",
        rate = BigDecimal("157.4021338800"),
        convertedAmount = BigDecimal("196752.6673500000"),
        baseAmount = previewBaseAmount,
        baseCode = "USD"
    ),
    CurrencyRowWithConversion(
        code = "GBP",
        name = "British Pound Sterling",
        symbol = "£",
        rate = BigDecimal("0.7842119900"),
        convertedAmount = BigDecimal("980.2649875000"),
        baseAmount = previewBaseAmount,
        baseCode = "USD"
    ),
    CurrencyRowWithConversion(
        code = "KRW",
        name = "South Korean Won",
        symbol = "₩",
        rate = BigDecimal("1378.5504120000"),
        convertedAmount = BigDecimal("1723188.0150000000"),
        baseAmount = previewBaseAmount,
        baseCode = "USD"
    ),
    CurrencyRowWithConversion(
        code = "CHF",
        name = "Swiss Franc",
        symbol = "Fr",
        rate = BigDecimal("0.8804112300"),
        convertedAmount = BigDecimal("1100.5140375000"),
        baseAmount = previewBaseAmount,
        baseCode = "USD"
    ),
    CurrencyRowWithConversion(
        code = "AUD",
        name = "Australian Dollar",
        symbol = "$",
        rate = BigDecimal("1.5231889400"),
        convertedAmount = BigDecimal("1903.9861750000"),
        baseAmount = previewBaseAmount,
        baseCode = "USD"
    )
)

private fun previewPlainRows(): List<CurrencyRow> = listOf(
    CurrencyRowPlain("EUR", "Euro", "€", BigDecimal("0.9231004512")),
    CurrencyRowPlain("JPY", "Japanese Yen", "¥", BigDecimal("157.4021338800")),
    CurrencyRowPlain("GBP", "British Pound Sterling", "£", BigDecimal("0.7842119900")),
    CurrencyRowPlain("THB", "Thai Baht", "฿", BigDecimal("36.4120009900"))
)

private val previewSelection = persistentSetOf(
    CurrencyCode.EUR,
    CurrencyCode.JPY,
    CurrencyCode.GBP,
    CurrencyCode.KRW,
    CurrencyCode.CHF,
    CurrencyCode.AUD
)

@Preview(name = "Card grid · converted", showBackground = true, widthDp = 400, heightDp = 880)
@Composable
private fun CurrencyScreenCardGridConvertedPreview() {
    FlightInformationTheme(darkTheme = false, dynamicColor = false) {
        CurrencyScreen(
            state = CurrencyUiState.Content.WithConversion(
                rows = previewConvertedRows(),
                baseAmount = previewBaseAmount,
                baseCode = CurrencyCode.USD,
                selected = previewSelection
            ),
            onCalculatorShow = {},
            onCalculatorDismiss = {},
            onMoneyInput = { _, _ -> },
            onCurrencyClick = {},
            onCurrencySelect = {},
            onSearch = {},
            onBaseCurrencySelect = {},
            onRetry = {}
        )
    }
}

@Preview(
    name = "Card grid · rates only, dark",
    showBackground = true,
    widthDp = 400,
    heightDp = 880,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun CurrencyScreenCardGridPlainDarkPreview() {
    FlightInformationTheme(darkTheme = true, dynamicColor = false) {
        CurrencyScreen(
            state = CurrencyUiState.Content.Plain(
                rows = previewPlainRows(),
                baseCode = CurrencyCode.USD,
                selected = previewSelection
            ),
            onCalculatorShow = {},
            onCalculatorDismiss = {},
            onMoneyInput = { _, _ -> },
            onCurrencyClick = {},
            onCurrencySelect = {},
            onSearch = {},
            onBaseCurrencySelect = {},
            onRetry = {}
        )
    }
}

@Preview(name = "Card grid · error", showBackground = true, widthDp = 400, heightDp = 440)
@Composable
private fun CurrencyScreenCardGridErrorPreview() {
    FlightInformationTheme(darkTheme = false, dynamicColor = false) {
        CurrencyScreen(
            state = CurrencyUiState.Error(LoadError.NoNetwork),
            onCalculatorShow = {},
            onCalculatorDismiss = {},
            onMoneyInput = { _, _ -> },
            onCurrencyClick = {},
            onCurrencySelect = {},
            onSearch = {},
            onBaseCurrencySelect = {},
            onRetry = {}
        )
    }
}
