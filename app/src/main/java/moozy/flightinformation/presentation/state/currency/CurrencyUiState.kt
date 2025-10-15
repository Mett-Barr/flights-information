package moozy.flightinformation.presentation.state.currency

import moozy.flightinformation.domain.model.currency.Currencies

sealed class CurrencyUiState {
    data object Loading : CurrencyUiState()

    data class Success(
        val currencies: Currencies,
        val isRefreshing: Boolean = false
    ) : CurrencyUiState()

    data class Error(val message: String?) : CurrencyUiState()
}

fun Result<Currencies>.toUiState(): CurrencyUiState = fold(
    onSuccess = { CurrencyUiState.Success(it) },
    onFailure = { CurrencyUiState.Error(it.message) }
)