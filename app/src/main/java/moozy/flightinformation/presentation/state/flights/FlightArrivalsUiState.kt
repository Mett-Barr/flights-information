package moozy.flightinformation.presentation.state.flights

import moozy.flightinformation.domain.error.LoadError

sealed class FlightArrivalsUiState {
    data object Loading : FlightArrivalsUiState()
    data class Content(
        val items: List<FlightArrivalItemUiModel>,
        val isRefreshing: Boolean = false
    ) : FlightArrivalsUiState()
    data class Error(val error: LoadError) : FlightArrivalsUiState()
}

