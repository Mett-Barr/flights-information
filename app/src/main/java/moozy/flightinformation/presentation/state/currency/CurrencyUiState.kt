package moozy.flightinformation.presentation.state.currency

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import moozy.flightinformation.domain.error.LoadError
import moozy.flightinformation.domain.value.CurrencyCode
import moozy.flightinformation.presentation.model.currency.CurrencyRow
import java.math.BigDecimal

sealed class CurrencyUiState {
    data object Loading : CurrencyUiState()

    sealed class Content : CurrencyUiState() {
        abstract val rows: List<CurrencyRow>
        abstract val selected: PersistentSet<CurrencyCode>
        abstract val isRefreshing: Boolean
        abstract val baseCode: CurrencyCode
        abstract val selectedBaseCurrency: CurrencyCode?

        /** 無使用者輸入的基底金額/貨幣 */
        data class Plain(
            override val rows: List<CurrencyRow>,
            override val selected: PersistentSet<CurrencyCode> = persistentSetOf(),
            override val isRefreshing: Boolean = false,
            override val baseCode: CurrencyCode,
            override val selectedBaseCurrency: CurrencyCode? = null
        ) : Content()

        /** 有使用者輸入的基底金額/貨幣（用於換算展示） */
        data class WithConversion(
            override val rows: List<CurrencyRow>,
            val baseAmount: BigDecimal,
            override val baseCode: CurrencyCode,
            override val selected: PersistentSet<CurrencyCode> = persistentSetOf(),
            override val isRefreshing: Boolean = false,
            override val selectedBaseCurrency: CurrencyCode? = null
        ) : Content()
    }
    data class Error(val error: LoadError) : CurrencyUiState()
}

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
