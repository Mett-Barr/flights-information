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
            onMoneyInput = { _, _ -> },
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
            onMoneyInput = { _, _ -> },
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
            onMoneyInput = { _, _ -> },
            onCurrencySelect = {},
            onSearch = {},
            onBaseCurrencySelect = {},
            onRetry = {}
        )
    }
}
