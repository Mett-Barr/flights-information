package moozy.flightinformation.presentation.state.flights

sealed class FlightArrivalsUiState {
    data object Loading : FlightArrivalsUiState()
    data class Content(
        val items: List<FlightArrivalItemUiModel>,
        val isRefreshing: Boolean = false
    ) : FlightArrivalsUiState()
    data class Error(val message: String) : FlightArrivalsUiState()
}

