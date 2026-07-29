package moozy.flightinformation.feature.flights

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import moozy.flightinformation.domain.error.LoadError
import moozy.flightinformation.core.ui.FlightInformationTheme
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlightsScreenTest {
    @get:Rule
    val composeTestRule: ComposeContentTestRule = createComposeRule()

    @Test
    fun loadingState_showsProgressIndicator() {
        setFlightsScreenContent(FlightArrivalsUiState.Loading)

        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
    }

    @Test
    fun contentState_showsFlightArrivalDetails() {
        setFlightsScreenContent(
            FlightArrivalsUiState.Content(
                items = listOf(fakeFlightArrivalItem),
                updatedAt = LocalDateTime.of(2026, 7, 26, 10, 11, 12),
            )
        )

        composeTestRule.onNodeWithText(fakeFlightArrivalItem.carrierLineText).assertIsDisplayed()
    }

    @Test
    fun noNetworkError_showsNetworkErrorMessage() {
        setFlightsScreenContent(FlightArrivalsUiState.Error(LoadError.NoNetwork))

        composeTestRule
            .onNodeWithText(string(moozy.flightinformation.core.ui.R.string.error_no_network))
            .assertIsDisplayed()
    }

    @Test
    fun emptyContent_showsEmptyStateMessage() {
        setFlightsScreenContent(
            FlightArrivalsUiState.Content(
                items = emptyList(),
                updatedAt = LocalDateTime.of(2026, 7, 26, 10, 11, 12),
            )
        )

        composeTestRule.onNodeWithText(string(R.string.flights_empty)).assertIsDisplayed()
    }

    /** 從資源讀取，才不會在測試裡再抄一份文案。 */
    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setFlightsScreenContent(uiState: FlightArrivalsUiState) {
        composeTestRule.setContent {
            FlightInformationTheme {
                FlightsScreen(flightArrivalsUiState = uiState)
            }
        }
    }
}
