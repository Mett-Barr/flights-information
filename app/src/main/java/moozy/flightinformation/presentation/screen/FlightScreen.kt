package moozy.flightinformation.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import moozy.flightinformation.presentation.state.FlightArrivalItemUiModel
import moozy.flightinformation.presentation.state.FlightArrivalsUiState
import moozy.flightinformation.presentation.theme.FlightInformationTheme

@Composable
fun FlightScreen(
    flightArrivalsUiState: FlightArrivalsUiState,
    modifier: Modifier = Modifier
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(flightArrivalsUiState.items) { item ->
                            FlightArrivalItem(item = item)
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
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // TODO: Replace with an actual image loader like Coil
                // e.g. AsyncImage(model = item.airlineLogoUrl, ...)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = Color.LightGray,
                            shape = MaterialTheme.shapes.small
                        )
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${item.originName} (${item.originCode})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.flightNo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoColumn(title = "Scheduled", value = item.scheduleText)
                InfoColumn(title = "Actual", value = item.actualText)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoColumn(title = "Gate", value = item.gateText)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item.statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColorBy(item.statusKey)
                    )
                }
            }
        }
    }
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
        val sampleUiState = FlightArrivalsUiState.Content(
            items = listOf(
                FlightArrivalItemUiModel(
                    scheduleText = "14:30",
                    actualText = "14:35",
                    originCode = "TPE",
                    originName = "Taipei",
                    flightNo = "GE233",
                    gateText = "Gate 3",
                    statusText = "Delayed",
                    statusKey = "DELAYED",
                    airlineLogoUrl = ""
                ),
                FlightArrivalItemUiModel(
                    scheduleText = "15:00",
                    actualText = "15:00",
                    originCode = "KHH",
                    originName = "Kaohsiung",
                    flightNo = "GE567",
                    gateText = "Gate 1",
                    statusText = "Arrived",
                    statusKey = "ARRIVED",
                    airlineLogoUrl = ""
                ),
                FlightArrivalItemUiModel(
                    scheduleText = "16:10",
                    actualText = "--:--",
                    originCode = "RMQ",
                    originName = "Taichung",
                    flightNo = "B7899",
                    gateText = "Gate C2",
                    statusText = "Cancelled",
                    statusKey = "CANCELLED",
                    airlineLogoUrl = ""
                )
            )
        )
        FlightScreen(flightArrivalsUiState = sampleUiState)
    }
}

@Preview(showBackground = true, name = "Empty List Preview")
@Composable
fun FlightScreenEmptyPreview() {
    FlightInformationTheme {
        val emptyUiState = FlightArrivalsUiState.Content(items = emptyList())
        FlightScreen(flightArrivalsUiState = emptyUiState)
    }
}

@Preview(showBackground = true, name = "Error State Preview")
@Composable
fun FlightScreenErrorPreview() {
    FlightInformationTheme {
        val errorUiState = FlightArrivalsUiState.Error(
            message = "Failed to load flight information. Please try again later."
        )
        FlightScreen(flightArrivalsUiState = errorUiState)
    }
}

@Preview(showBackground = true, name = "Loading State Preview")
@Composable
fun FlightScreenLoadingPreview() {
    FlightInformationTheme {
        val loadingUiState = FlightArrivalsUiState.Loading
        FlightScreen(flightArrivalsUiState = loadingUiState)
    }
}
