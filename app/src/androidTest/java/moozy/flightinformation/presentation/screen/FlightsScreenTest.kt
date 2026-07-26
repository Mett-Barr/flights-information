package moozy.flightinformation.presentation.screen

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import moozy.flightinformation.domain.error.LoadError
import moozy.flightinformation.presentation.state.flights.FlightArrivalsUiState
import moozy.flightinformation.presentation.state.flights.fakeFlightArrivalItem
import moozy.flightinformation.presentation.theme.FlightInformationTheme
import org.junit.Rule
import org.junit.Test
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
            FlightArrivalsUiState.Content(items = listOf(fakeFlightArrivalItem))
        )

        composeTestRule.onNodeWithText(fakeFlightArrivalItem.carrierLineText).assertIsDisplayed()
    }

    @Test
    fun noNetworkError_showsNetworkErrorMessage() {
        setFlightsScreenContent(FlightArrivalsUiState.Error(LoadError.NoNetwork))

        composeTestRule
            .onNodeWithText("No internet connection. Check your connection and try again.")
            .assertIsDisplayed()
    }

    @Test
    fun emptyContent_showsEmptyStateMessage() {
        setFlightsScreenContent(FlightArrivalsUiState.Content(items = emptyList()))

        composeTestRule.onNodeWithText("No flight information available.").assertIsDisplayed()
    }

    private fun setFlightsScreenContent(uiState: FlightArrivalsUiState) {
        composeTestRule.setContent {
            FlightInformationTheme {
                FlightsScreen(flightArrivalsUiState = uiState)
            }
        }
    }
}
