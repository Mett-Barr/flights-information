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
@Composable
fun CurrencyScreen(
    state: CurrencyUiState,
    onMoneyInput: (
        content: CurrencyUiState.Content,
        amountText: String?,
    ) -> Unit,
    onCurrencySelect: (CurrencyCode) -> Unit,
    onSearch: (CurrencyUiState.Content) -> Unit,
    onBaseCurrencySelect: (CurrencyCode) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues = PaddingValues(0.dp)
) {
    // 此設計以 ModalBottomSheet 呈現計算機，無需隱藏導覽列。
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
            delay(ENTRANCE_TOTAL)
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

