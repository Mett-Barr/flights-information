package moozy.flightinformation.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import moozy.flightinformation.domain.repository.currency.CurrencyRepository
import moozy.flightinformation.domain.value.CurrencyCode
import moozy.flightinformation.domain.value.MoneyCode
import moozy.flightinformation.presentation.mapper.mapCurrenciesToRows
import moozy.flightinformation.presentation.mapper.nextContent
import moozy.flightinformation.presentation.model.currency.CurrencyRow
import moozy.flightinformation.presentation.state.currency.CurrencyUiState
import moozy.flightinformation.util.collection.toggle
import javax.inject.Inject

@HiltViewModel
class CurrencyViewModel @Inject constructor(
    private val repository: CurrencyRepository
) : ViewModel() {
    private val _state: MutableStateFlow<CurrencyUiState> =
        MutableStateFlow(CurrencyUiState.Loading)
    val state: StateFlow<CurrencyUiState> = _state.asStateFlow()
    private var hasLoaded = false

    /**
     * Starts the initial request when the currency screen is actually displayed.
     *
     * This remains separate from [_state] because later user actions update that state directly.
     */
    fun load() {
        if (hasLoaded) return
        hasLoaded = true

        viewModelScope.launch {
            getLatestCurrencies(
                base = CurrencyCode.USD,
                codes = CurrencyCode.entries.shuffled().take(15).toSet()
            )
        }
    }

    fun onCurrencySelect(currencyCode: CurrencyCode) {
        _state.update {
            when (it) {
                is CurrencyUiState.Content.Plain -> {
                    it.copy(selected = it.selected.toggle(currencyCode))
                }

                is CurrencyUiState.Content.WithConversion -> {
                    it.copy(selected = it.selected.toggle(currencyCode))
                }

                else -> it
            }
        }
    }

    fun onBaseCurrencySelect(currencyCode: CurrencyCode) {
        _state.update {
            when (it) {
                is CurrencyUiState.Content.Plain -> {
                    if (it.selectedBaseCurrency == currencyCode) {
                        it.copy(selectedBaseCurrency = null)
                    } else {
                        it.copy(selectedBaseCurrency = currencyCode)
                    }
                }

                is CurrencyUiState.Content.WithConversion -> {
                    if (it.selectedBaseCurrency == currencyCode) {
                        it.copy(selectedBaseCurrency = null)
                    } else {
                        it.copy(selectedBaseCurrency = currencyCode)
                    }
                }

                else -> it
            }
        }
    }

    fun onCurrencyClick(currencyCode: String) {
        _state.update {
            when (it) {
                is CurrencyUiState.Content.Plain -> {
                    it.copy(rows = it.rows.toMutableList().apply {
                        find { r -> r.code == currencyCode }?.let { row ->
                            remove(row)
                            add(0, row)
                        }
                    })
                }

                is CurrencyUiState.Content.WithConversion -> {
                    it.copy(rows = it.rows.toMutableList().apply {
                        find { r -> r.code == currencyCode }?.let { row ->
                            remove(row)
                            add(0, row)
                        }
                    })
                }

                else -> it
            }
        }
    }

    fun getCurrencies(state: CurrencyUiState.Content) {
        viewModelScope.launch {
            _state.value = when (state) {
                is CurrencyUiState.Content.Plain -> state.copy(isRefreshing = true)
                is CurrencyUiState.Content.WithConversion -> state.copy(isRefreshing = true)
            }

            getLatestCurrencies(state.selectedBaseCurrency, state.selected)
        }
    }

    private suspend fun getLatestCurrencies(
        base: CurrencyCode?,
        codes: Set<CurrencyCode>
    ) {
        _state.value = repository.getLatest(
            base = base,
            codes = codes
        ).fold(
            onSuccess = { currencies ->
                val rows: List<CurrencyRow> = mapCurrenciesToRows(
                    currencies = currencies,
                    conversion = null // 初始/未輸入 → 全 Plain（純資料）
                )

                val selectedFromResponse =
                    currencies.list.mapNotNull { r -> (r.code as? MoneyCode.Known)?.code }
                        .toSet()
                        .toPersistentSet()

                CurrencyUiState.Content.Plain(
                    rows = rows,
                    selected = selectedFromResponse,
                    isRefreshing = false,
                    baseCode = currencies.base
                )
            },
            onFailure = {
                CurrencyUiState.Error(it.message)
            }
        )
    }

    fun inputMoney(
        content: CurrencyUiState.Content,
        amountText: String?
    ) {
        viewModelScope.launch {
            val r = nextContent(content, content.selectedBaseCurrency, amountText)
            _state.value = r
        }
    }
}
