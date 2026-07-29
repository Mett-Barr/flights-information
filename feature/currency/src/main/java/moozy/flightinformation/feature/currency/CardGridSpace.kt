@file:OptIn(ExperimentalMaterial3Api::class)

package moozy.flightinformation.feature.currency

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
import androidx.compose.ui.res.pluralStringResource
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
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.launch
import moozy.flightinformation.feature.currency.R
import moozy.flightinformation.domain.error.LoadError
import moozy.flightinformation.domain.value.CurrencyCode
import moozy.flightinformation.feature.calculator.Calculator
import moozy.flightinformation.feature.calculator.CalculatorUI
import moozy.flightinformation.core.ui.messageRes
import moozy.flightinformation.feature.currency.CurrencyRow
import moozy.flightinformation.feature.currency.CurrencyRowPlain
import moozy.flightinformation.feature.currency.CurrencyRowWithConversion
import moozy.flightinformation.feature.currency.CurrencyUiState
import moozy.flightinformation.feature.currency.toggle
import moozy.flightinformation.core.ui.FlightInformationTheme
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
internal object CardGridSpace {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

/** 基準卡片上兩個控制項的高度＝MD3 中型按鈕的容器高度。 */
internal val CARD_GRID_ACTION_HEIGHT = 56.dp

/** 目標卡片的最小高度（8dp 網格上的 18 格）。 */
internal val CARD_GRID_TILE_MIN_HEIGHT = 144.dp

/** 觸控目標下限；chip 被撐到這個高度，網格裡才好按。 */
internal val CARD_GRID_MIN_TOUCH_TARGET = 48.dp

/** MD3：大螢幕的內容最大寬度（建議 840–1040dp），超過就置中留白。 */
internal val CARD_GRID_MAX_CONTENT_WIDTH = 1040.dp

/** 浮水印相對 `displayLarge` 的放大倍率（57sp × 1.9 ≒ 108sp）。 */
internal const val CARD_GRID_WATERMARK_SCALE = 1.9f

/** 浮水印透明度：MD3 狀態層的 0.12。 */
internal const val CARD_GRID_WATERMARK_ALPHA = 0.12f

/** 按下時縮到幾成。 */
internal const val CARD_GRID_PRESS_SCALE = 0.955f

/** MD3 emphasized decelerate：元素「進場」用的曲線。 */
internal val CardGridEnterEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

/** MD3 standard：小型、來回都在畫面內的狀態轉換（按下回饋）用的曲線。 */
internal val CardGridStandardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/* ============================================================
 *  進場動畫
 * ============================================================ */

/** 每張卡之間錯開的間隔（MD3 duration short1）。 */
internal val ENTRANCE_STEP = 50.milliseconds

/** 最多錯開幾階。幣別可以有三十幾個，不設上限的話最後一張要等一秒多才進來。 */
internal const val ENTRANCE_MAX_STEPS = 11

/** 單張卡的淡入時間（MD3 duration medium4：元素進場）。 */
internal val ENTRANCE_DURATION = 400.milliseconds

/** 整段進場的長度：最後一階的延遲（11 × 50）加上一張卡的淡入（400）。 */
internal val ENTRANCE_TOTAL = 950.milliseconds

/** 按下回饋的長度（MD3 duration short2）。 */
internal val PRESS_DURATION = 100.milliseconds

/**
 * 單張卡的進場進度（0 → 1）。
 *
 * 回傳 [Animatable] 而不是 `Float`，呼叫端才能在 `graphicsLayer` 的 lambda 裡讀值——
 * 那是延遲讀取，動畫期間只會重畫，不會每一格畫面都重組。
 */
@Composable
internal fun rememberCardGridEntrance(
    index: Int,
    animate: Boolean
): Animatable<Float, AnimationVector1D> {
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(animate, index) {
        if (animate) {
            delay(ENTRANCE_STEP * index.coerceAtMost(ENTRANCE_MAX_STEPS))
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = ENTRANCE_DURATION.inWholeMilliseconds.toInt(),
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
internal fun CardGridCalculatorSheet(
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
internal fun CardGridPickerSheet(
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
                        pluralStringResource(
                            R.plurals.currency_selected_description,
                            state.selected.size,
                            state.selected.size,
                        )
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
internal const val CARD_GRID_TABULAR = "tnum"

/** 主要金額：千分位 + 固定兩位小數。 */
private val cardGridMoneyFormat = DecimalFormat("#,##0.00")

/** 兩位小數會被捨成 0 的極小金額（例：1 KRW 換 USD）改用有效位數，否則整片網格都是 0.00。 */
private val cardGridSmallMoneyFormat = DecimalFormat("#,##0.00######")
private const val MONEY_DISPLAY_SCALE = 2
private const val RATE_DISPLAY_SCALE = 4

/** 匯率是配角，最多四位小數，尾數的 0 不補。 */
private val cardGridRateFormat = DecimalFormat("#,##0.####")

private val cardGridSmallRateFormat = DecimalFormat("#,##0.########")

internal fun BigDecimal.toGridMoneyText(): String {
    val rounded = setScale(MONEY_DISPLAY_SCALE, RoundingMode.HALF_UP)
    return if (rounded.signum() == 0 && signum() != 0) {
        cardGridSmallMoneyFormat.format(round(MathContext(RATE_DISPLAY_SCALE, RoundingMode.HALF_UP)))
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
internal fun BigDecimal.toGridRateText(): String {
    val rounded = setScale(RATE_DISPLAY_SCALE, RoundingMode.HALF_UP)
    return if (rounded.signum() == 0 && signum() != 0) {
        cardGridSmallRateFormat.format(round(MathContext(RATE_DISPLAY_SCALE, RoundingMode.HALF_UP)))
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
internal fun CurrencyUiState.Content.withGridBase(
    code: CurrencyCode
): CurrencyUiState.Content = when (this) {
    is CurrencyUiState.Content.Plain -> copy(selectedBaseCurrency = code)
    is CurrencyUiState.Content.WithConversion -> copy(selectedBaseCurrency = code)
}

internal fun CurrencyUiState.Content.withGridSelection(
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

