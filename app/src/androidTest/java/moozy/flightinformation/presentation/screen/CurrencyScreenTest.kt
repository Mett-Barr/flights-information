package moozy.flightinformation.presentation.screen

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.math.BigDecimal
import moozy.flightinformation.domain.value.CurrencyCode
import moozy.flightinformation.presentation.model.currency.CurrencyRowPlain
import moozy.flightinformation.presentation.model.currency.CurrencyRowWithConversion
import moozy.flightinformation.presentation.state.currency.CurrencyUiState
import moozy.flightinformation.presentation.theme.FlightInformationTheme
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
    fun errorState_showsErrorMessage() {
        setCurrencyScreenContent(CurrencyUiState.Error(message = "Request failed"))

        composeTestRule.onNodeWithText("Error").assertIsDisplayed()
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
        setCurrencyScreenContent(
            contentState(),
            onCurrencySelect = { selectedCurrency = it },
        )

        composeTestRule.onNodeWithText("Search").performClick()
        composeTestRule.onNodeWithText("USD").performClick()

        assertEquals(CurrencyCode.USD, selectedCurrency)
    }

    @Test
    fun selectingBaseCurrency_callsBaseCurrencySelectAndShowsSelectedState() {
        var selectedBaseCurrency: CurrencyCode? = null
        setCurrencyScreenContent(
            contentState(selectedBaseCurrency = CurrencyCode.EUR),
            onBaseCurrencySelect = { selectedBaseCurrency = it },
        )

        composeTestRule.onNodeWithText("Search").performClick()
        composeTestRule.onNodeWithText("select targets").performClick()
        composeTestRule.onNodeWithText("EUR").assertIsSelected().performClick()

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

    private fun setCurrencyScreenContent(
        state: CurrencyUiState,
        onCurrencySelect: (CurrencyCode) -> Unit = {},
        onBaseCurrencySelect: (CurrencyCode) -> Unit = {},
    ) {
        composeTestRule.setContent {
            FlightInformationTheme {
                CurrencyScreen(
                    state = state,
                    onCalculatorShow = {},
                    onCalculatorDismiss = {},
                    onMoneyInput = { _, _ -> },
                    onCurrencyClick = {},
                    onCurrencySelect = onCurrencySelect,
                    onSearch = {},
                    onBaseCurrencySelect = onBaseCurrencySelect,
                )
            }
        }
    }
}
