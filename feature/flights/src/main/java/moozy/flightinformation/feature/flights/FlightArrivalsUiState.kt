package moozy.flightinformation.feature.flights

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import moozy.flightinformation.domain.error.LoadError
import java.time.LocalDateTime

sealed class FlightArrivalsUiState {
    data object Loading : FlightArrivalsUiState()
    @Immutable
    data class Content(
        val items: ImmutableList<FlightArrivalItemUiModel>,
        val updatedAt: LocalDateTime,
        val isRefreshing: Boolean = false,
        val refreshAttempt: Long = 0,
        val refreshHealth: RefreshHealth = RefreshHealth.NeverFailed,
    ) : FlightArrivalsUiState()
    data class Error(val error: LoadError) : FlightArrivalsUiState()
}

@Immutable
sealed interface RefreshHealth {
    data object NeverFailed : RefreshHealth
    data class Failed(val error: LoadError) : RefreshHealth
    data object Recovered : RefreshHealth
}
