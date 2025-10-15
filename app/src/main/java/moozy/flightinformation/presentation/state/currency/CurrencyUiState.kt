package moozy.flightinformation.presentation.state.currency

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import moozy.flightinformation.domain.model.currency.Currencies
import moozy.flightinformation.domain.model.currency.CurrencyCode

sealed class CurrencyUiState {
    data object Loading : CurrencyUiState()

    data class Success(
        val currencies: Currencies,
        val selected: PersistentSet<CurrencyCode> = persistentSetOf(),
        val isRefreshing: Boolean = false,
        val error: String? = null
    ) : CurrencyUiState()

    data class Error(val message: String?) : CurrencyUiState()
}


data class SelectableCurrency(
    val currencyCode: CurrencyCode,
    val isSelected: Boolean
)

sealed class CurrencyModel {
    data class Rate(
        val currencyCode: CurrencyCode,
        val rate: String
    ) : CurrencyModel()

    data class RateWithBase(
        val currencyCode: CurrencyCode,
        val rate: String,
        val conversion: String
    ) : CurrencyModel()
}