package moozy.flightinformation.feature.currency

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import moozy.flightinformation.feature.currency.R
import moozy.flightinformation.domain.error.LoadError
import java.math.BigDecimal
import moozy.flightinformation.domain.value.CurrencyCode
import moozy.flightinformation.feature.currency.CurrencyRowPlain
import moozy.flightinformation.feature.currency.CurrencyRowWithConversion
import moozy.flightinformation.feature.currency.CurrencyUiState
import moozy.flightinformation.core.ui.FlightInformationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CurrencyScreenTest {
    @get:Rule
    val composeTestRule: ComposeContentTestRule = createComposeRule()

    @Test
    fun loadingState_showsProgressIndicator() {
        setCurrencyScreenContent(CurrencyUiState.Loading)

        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
    }

    @Test
    fun noNetworkError_showsCategorizedMessage() {
        setCurrencyScreenContent(CurrencyUiState.Error(LoadError.NoNetwork))

        composeTestRule
            .onNodeWithText(string(moozy.flightinformation.core.ui.R.string.error_no_network))
            .assertIsDisplayed()
    }

    @Test
    fun errorState_retry_callsRetryCallback() {
        var retries = 0
        setCurrencyScreenContent(
            state = CurrencyUiState.Error(LoadError.NoNetwork),
            onRetry = { retries++ },
        )

        composeTestRule.onNodeWithTag("currency_retry").performClick()

        assertEquals(1, retries)
    }

    @Test
    fun withConversionContent_showsCurrencyAndConvertedAmount() {
        setCurrencyScreenContent(
            CurrencyUiState.Content.WithConversion(
                rows = listOf(
                    CurrencyRowWithConversion(
                        code = "JPY",
                        name = "Japanese Yen",
                        symbol = "¥",
                        rate = BigDecimal("156.25"),
                        convertedAmount = BigDecimal("15625.00"),
                        baseAmount = BigDecimal("100"),
                        baseCode = "USD",
                    ),
                ),
                baseAmount = BigDecimal("100"),
                baseCode = CurrencyCode.USD,
            )
        )

        composeTestRule.onNodeWithText("JPY").assertIsDisplayed()
        composeTestRule.onNodeWithText("15625.00").assertIsDisplayed()
    }

    @Test
    fun selectingTargetCurrency_callsCurrencySelectWithSelectedCode() {
        var selectedCurrency: CurrencyCode? = null
        var searchedContent: CurrencyUiState.Content? = null
        setCurrencyScreenContent(
            contentState(),
            onCurrencySelect = { selectedCurrency = it },
            onSearch = { searchedContent = it },
        )

        composeTestRule
            .onNodeWithContentDescription(string(R.string.currency_change_base_currency))
            .performClick()
        composeTestRule.onNodeWithText(string(R.string.currency_shown_in_grid)).performClick()
        composeTestRule.onNodeWithTag("currency_picker_code_USD").performClick()

        assertEquals(CurrencyCode.USD, selectedCurrency)
        assertEquals(true, searchedContent?.selected?.contains(CurrencyCode.USD))
    }

    @Test
    fun selectingBaseCurrency_callsBaseCurrencySelectAndShowsSelectedState() {
        var selectedBaseCurrency: CurrencyCode? = null
        setCurrencyScreenContent(
            contentState(selectedBaseCurrency = CurrencyCode.EUR),
            onBaseCurrencySelect = { selectedBaseCurrency = it },
        )

        composeTestRule
            .onNodeWithContentDescription(string(R.string.currency_change_base_currency))
            .performClick()
        composeTestRule
            .onNodeWithTag("currency_picker_code_EUR")
            .assertIsSelected()
            .performClick()

        assertEquals(CurrencyCode.EUR, selectedBaseCurrency)
    }

    private fun contentState(
        selectedBaseCurrency: CurrencyCode? = null,
    ): CurrencyUiState.Content.Plain =
        CurrencyUiState.Content.Plain(
            rows = listOf(
                CurrencyRowPlain(
                    code = "JPY",
                    name = "Japanese Yen",
                    symbol = "¥",
                    rate = BigDecimal("156.25"),
                ),
            ),
            baseCode = CurrencyCode.USD,
            selectedBaseCurrency = selectedBaseCurrency,
        )

    /** 從資源讀取，才不會在測試裡再抄一份文案。 */
    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setCurrencyScreenContent(
        state: CurrencyUiState,
        onCurrencySelect: (CurrencyCode) -> Unit = {},
        onSearch: (CurrencyUiState.Content) -> Unit = {},
        onBaseCurrencySelect: (CurrencyCode) -> Unit = {},
        onRetry: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            FlightInformationTheme {
                CurrencyScreen(
                    state = state,
                    onMoneyInput = { _, _ -> },
                    onCurrencySelect = onCurrencySelect,
                    onSearch = onSearch,
                    onBaseCurrencySelect = onBaseCurrencySelect,
                    onRetry = onRetry,
                )
            }
        }
    }
}
