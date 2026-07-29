package moozy.flightinformation.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import moozy.flightinformation.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppNavigationTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun switchingBetweenFlightsAndCurrency_keepsFlightsContent() {
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule
                .onAllNodesWithText("Last updated:", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText("Flights").assertIsDisplayed().assertIsSelected()
        composeTestRule.onNodeWithText("Last updated:", substring = true).assertIsDisplayed()

        composeTestRule.onNodeWithText("Currency").performClick()
        composeTestRule.onNodeWithText("Currency").assertIsSelected()
        composeTestRule.onNodeWithText("Flights").performClick()

        composeTestRule.onNodeWithText("Flights").assertIsDisplayed().assertIsSelected()
        composeTestRule.onNodeWithText("Last updated:", substring = true).assertIsDisplayed()
    }
}
