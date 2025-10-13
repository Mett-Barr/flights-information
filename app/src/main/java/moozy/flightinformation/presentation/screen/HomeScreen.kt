
package moozy.flightinformation.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import moozy.flightinformation.presentation.theme.FlightInformationTheme

@Composable
fun HomeScreen(onNavigateToDetails: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "這裡是主頁 (Nav3)")
        Button(onClick = onNavigateToDetails) {
            Text("前往詳情頁")
        }
    }
}

@Composable
fun DetailsScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "這裡是詳情頁 (Nav3)")
        Button(onClick = onNavigateBack) {
            Text("返回主頁")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FlightInformationTheme {
        HomeScreen(onNavigateToDetails = {})
    }
}
