package moozy.flightinformation.presentation.state.flights

import moozy.flightinformation.domain.error.LoadError
import java.time.LocalDateTime

sealed class FlightArrivalsUiState {
    data object Loading : FlightArrivalsUiState()
    data class Content(
        val items: List<FlightArrivalItemUiModel>,
        val updatedAt: LocalDateTime,
        val isRefreshing: Boolean = false
    ) : FlightArrivalsUiState()
    data class Error(val error: LoadError) : FlightArrivalsUiState()
}
