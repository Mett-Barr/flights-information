package moozy.flightinformation.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import moozy.flightinformation.data.repository.currency.CurrencyRepository
import moozy.flightinformation.domain.model.currency.CurrencyCode
import moozy.flightinformation.presentation.state.currency.CurrencyUiState
import javax.inject.Inject
import moozy.flightinformation.util.collection.toggle

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

    fun onCurrencySelected(currencyCode: CurrencyCode) {
        _state.update {
            when (it) {
                is CurrencyUiState.Success -> {
                    it.copy(selected = it.selected.toggle(currencyCode))
                }

                else -> it
            }
        }
    }

    fun getCurrencies() {
        viewModelScope.launch {
            _state.value = when (val currentState = state.value) {
                is CurrencyUiState.Success -> currentState.copy(isRefreshing = true)
                else -> CurrencyUiState.Loading
            }

            _state.value = repository.getLatest(
                base = null,
                codes = CurrencyCode.entries.take(15).toSet()
            ).fold(
                onSuccess = { currencies ->
                    _state.update { oldState ->
                        when (oldState) {
                            is CurrencyUiState.Success -> oldState.copy(
                                currencies = currencies,
                                isRefreshing = false
                            )

                            else -> CurrencyUiState.Success(
                                currencies = currencies,
                                isRefreshing = false
                            )
                        }
                    }
                    CurrencyUiState.Success(
                        currencies = currencies,
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