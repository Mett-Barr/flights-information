package moozy.flightinformation.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import moozy.flightinformation.data.repository.currency.CurrencyRepository
import moozy.flightinformation.domain.value.CurrencyCode
import moozy.flightinformation.domain.value.MoneyCode
import moozy.flightinformation.presentation.mapper.mapCurrenciesToRows
import moozy.flightinformation.presentation.model.currency.CurrencyRow
import moozy.flightinformation.presentation.state.currency.CurrencyUiState
import javax.inject.Inject

@HiltViewModel
class CurrencyViewModel @Inject constructor(
    private val repository: CurrencyRepository
) : ViewModel() {
    private val _state: MutableStateFlow<CurrencyUiState> =
        MutableStateFlow(CurrencyUiState.Loading)
    val state: StateFlow<CurrencyUiState> = _state.asStateFlow()

    init {
        getCurrencies()
    }

//    fun onCurrencySelected(currencyCode: CurrencyCode) {
//        _state.update {
//            when (it) {
//                is CurrencyUiState.Content -> {
//                    it.copy(selected = it.selected.toggle(currencyCode))
//                }
//
//                else -> it
//            }
//        }
//    }

    fun getCurrencies() {
        viewModelScope.launch {
            _state.value = when (val currentState = state.value) {
                is CurrencyUiState.Content.Plain -> currentState.copy(isRefreshing = true)
                is CurrencyUiState.Content.WithConversion -> currentState.copy(isRefreshing = true)
                else -> CurrencyUiState.Loading
            }

            delay(1500)

            _state.value = repository.getLatest(
                base = null,
                codes = CurrencyCode.entries.take(15).toSet()
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
                        isRefreshing = false
                    )
                },
                onFailure = {
                    CurrencyUiState.Error(it.message)
                }
            )
        }
    }
}