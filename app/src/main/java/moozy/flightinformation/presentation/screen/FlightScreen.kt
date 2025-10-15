package moozy.flightinformation.presentation.screen

import android.R.attr.contentDescription
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import moozy.flightinformation.presentation.component.FlightArrivalCard
import moozy.flightinformation.presentation.component.UrlImageWithShimmer
import moozy.flightinformation.presentation.state.flights.FlightArrivalItemUiModel
import moozy.flightinformation.presentation.state.flights.FlightArrivalsUiState
import moozy.flightinformation.presentation.state.flights.fakeFlightArrivalItem
import moozy.flightinformation.presentation.theme.FlightInformationTheme

@Composable
fun FlightsScreen(
    flightArrivalsUiState: FlightArrivalsUiState,
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues = PaddingValues(0.dp)
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (flightArrivalsUiState) {
            is FlightArrivalsUiState.Loading -> {
                CircularProgressIndicator()
            }

            is FlightArrivalsUiState.Error -> {
                Text(
                    text = flightArrivalsUiState.message,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }

            is FlightArrivalsUiState.Content -> {
                if (flightArrivalsUiState.items.isEmpty()) {
                    Text(
                        text = "No flight information available.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    val lazyListState = rememberLazyListState()

                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = innerPadding,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(flightArrivalsUiState.items) { item ->
                            FlightArrivalItem(item = item)
                        }

                        if (innerPadding.calculateBottomPadding() == 0.dp) {
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlightArrivalItem(
    item: FlightArrivalItemUiModel,
    modifier: Modifier = Modifier
) {
    FlightArrivalCard(item, modifier)
//    Card(modifier = modifier.fillMaxWidth()) {
//        Column(
//            modifier = Modifier.padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(8.dp)
//        ) {
//            Row(verticalAlignment = Alignment.CenterVertically) {
//                Log.d("!!!", "airlineLogoUrl: ${item.airlineLogoUrl}")
//                UrlImageWithShimmer(
//                    url = item.airlineLogoUrl,
//                    contentDescription = "air line Logo",
//                    modifier = modifier.size(48.dp),
//                )
//                Spacer(Modifier.width(16.dp))
//                Row(modifier = Modifier.weight(1f)) {
//                    Text(
//                        text = "${item.originName} (${item.originCode})",
//                        style = MaterialTheme.typography.titleLarge,
//                        fontWeight = FontWeight.Bold
//                    )
//                    Column {
//                        Text(
//                            text = "(${item.originCode})",
//                            style = MaterialTheme.typography.bodyLarge,
//                            fontWeight = FontWeight.Bold
//                        )
//                        Text(
//                            text = item.flightNo,
//                            style = MaterialTheme.typography.bodyMedium,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                    }
//                }
//                Column(modifier = Modifier.weight(1f)) {
//                    Text(
//                        text = "${item.originName} (${item.originCode})",
//                        style = MaterialTheme.typography.titleLarge,
//                        fontWeight = FontWeight.Bold
//                    )
//                    Text(
//                        text = item.flightNo,
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//            }
//
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                InfoColumn(title = "Scheduled", value = item.scheduleText)
//                InfoColumn(title = "Actual", value = item.actualText)
//            }
//
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                InfoColumn(title = "Gate", value = item.gateText)
//                Column(horizontalAlignment = Alignment.End) {
//                    Text(
//                        text = "Status",
//                        style = MaterialTheme.typography.labelMedium,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                    Text(
//                        text = item.statusText,
//                        style = MaterialTheme.typography.bodyMedium,
//                        fontWeight = FontWeight.Bold,
//                        color = statusColorBy(item.statusKey)
//                    )
//                }
//            }
//        }
//    }
}

@Preview
@Composable
fun FlightArrivalItemPreview() {
    FlightArrivalItem(fakeFlightArrivalItem)
}


@Preview
@Composable
fun CoilSimpleTest(
    imageUrl: String = "https://example.com/china_airlines_logo.png"
//    imageUrl: String = "https://www.kia.gov.tw/images/ALL-square/B7.png"
) {
    val context = LocalContext.current
    // 方案 A：最簡 AsyncImage
    AsyncImage(
        model = imageUrl,
        contentDescription = "Test Image",
        modifier = Modifier.fillMaxSize(),
        onError = { errorState ->
            // 顯示錯誤時可以放 fallback UI
            Log.d("!!!", "errorState: $errorState")
        },
        onSuccess = { successState ->
            // 成功後可以拿到 image 檔案資訊
            Log.d("!!!", "successState: $successState")
        }
    )
}

@Composable
private fun InfoColumn(title: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun statusColorBy(key: String): Color = when (key) {
    "ARRIVED", "DEPARTED" -> Color(0xFF2E7D32)      // Green
    "SCHEDULE_CHANGE" -> Color(0xFFF9A825)      // Yellow
    "CANCELLED", "DELAYED" -> Color(0xFFC62828)      // Red
    else -> Color.Gray
}


@Preview(showBackground = true, name = "Flight List Preview")
@Composable
fun FlightScreenPreview() {
    FlightInformationTheme {
//        val sampleUiState = FlightArrivalsUiState.Content(
//            items = listOf(
//                FlightArrivalItemUiModel(
//                    scheduleText = "14:30",
//                    actualText = "14:35",
//                    originCode = "TPE",
//                    originName = "Taipei",
//                    flightNo = "GE233",
//                    gateText = "Gate 3",
//                    statusText = "Delayed",
//                    statusKey = "DELAYED",
//                    airlineLogoUrl = ""
//                ),
//                FlightArrivalItemUiModel(
//                    scheduleText = "15:00",
//                    actualText = "15:00",
//                    originCode = "KHH",
//                    originName = "Kaohsiung",
//                    flightNo = "GE567",
//                    gateText = "Gate 1",
//                    statusText = "Arrived",
//                    statusKey = "ARRIVED",
//                    airlineLogoUrl = ""
//                ),
//                FlightArrivalItemUiModel(
//                    scheduleText = "16:10",
//                    actualText = "--:--",
//                    originCode = "RMQ",
//                    originName = "Taichung",
//                    flightNo = "B7899",
//                    gateText = "Gate C2",
//                    statusText = "Cancelled",
//                    statusKey = "CANCELLED",
//                    airlineLogoUrl = ""
//                )
//            )
//        )
//        FlightsScreen(flightArrivalsUiState = sampleUiState)
    }
}

@Preview(showBackground = true, name = "Empty List Preview")
@Composable
fun FlightScreenEmptyPreview() {
    FlightInformationTheme {
        val emptyUiState = FlightArrivalsUiState.Content(items = emptyList())
        FlightsScreen(flightArrivalsUiState = emptyUiState)
    }
}

@Preview(showBackground = true, name = "Error State Preview")
@Composable
fun FlightScreenErrorPreview() {
    FlightInformationTheme {
        val errorUiState = FlightArrivalsUiState.Error(
            message = "Failed to load flight information. Please try again later."
        )
        FlightsScreen(flightArrivalsUiState = errorUiState)
    }
}

@Preview(showBackground = true, name = "Loading State Preview")
@Composable
fun FlightScreenLoadingPreview() {
    FlightInformationTheme {
        val loadingUiState = FlightArrivalsUiState.Loading
        FlightsScreen(flightArrivalsUiState = loadingUiState)
    }
}
