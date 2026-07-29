package moozy.flightinformation.feature.currency

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import moozy.flightinformation.domain.error.LoadError
import moozy.flightinformation.domain.value.CurrencyCode
import java.math.BigDecimal

sealed class CurrencyUiState {
    data object Loading : CurrencyUiState()

    @Immutable
    sealed class Content : CurrencyUiState() {
        abstract val rows: ImmutableList<CurrencyRow>
        abstract val selected: PersistentSet<CurrencyCode>
        abstract val isRefreshing: Boolean
        abstract val baseCode: CurrencyCode
        abstract val selectedBaseCurrency: CurrencyCode?

        /** 無使用者輸入的基底金額/貨幣 */
        data class Plain(
            override val rows: ImmutableList<CurrencyRow>,
            override val selected: PersistentSet<CurrencyCode> = persistentSetOf(),
            override val isRefreshing: Boolean = false,
            override val baseCode: CurrencyCode,
            override val selectedBaseCurrency: CurrencyCode? = null
        ) : Content()

        /** 有使用者輸入的基底金額/貨幣（用於換算展示） */
        data class WithConversion(
            override val rows: ImmutableList<CurrencyRow>,
            val baseAmount: BigDecimal,
            override val baseCode: CurrencyCode,
            override val selected: PersistentSet<CurrencyCode> = persistentSetOf(),
            override val isRefreshing: Boolean = false,
            override val selectedBaseCurrency: CurrencyCode? = null
        ) : Content()
    }
    data class Error(val error: LoadError) : CurrencyUiState()
}

@Immutable
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
