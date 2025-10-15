package moozy.flightinformation.presentation.viewmodel

import android.R.attr.codes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import moozy.flightinformation.data.repository.currency.CurrencyRepository
import moozy.flightinformation.domain.model.currency.CurrencyCode
import moozy.flightinformation.presentation.state.currency.CurrencyUiState
import moozy.flightinformation.presentation.state.currency.toUiState
import javax.inject.Inject

@HiltViewModel
class CurrencyViewModel @Inject constructor(
    private val repository: CurrencyRepository
): ViewModel() {
    private val _state: MutableStateFlow<CurrencyUiState> = MutableStateFlow(CurrencyUiState.Loading)
    val state: StateFlow<CurrencyUiState> = _state.asStateFlow()

    init {
        getCurrencies()
    }

    private fun getCurrencies() {
        viewModelScope.launch {
            _state.value = when (val currentState = state.value) {
                is CurrencyUiState.Success -> currentState.copy(isRefreshing = true)
                else -> CurrencyUiState.Loading
            }

            _state.value = repository.getLatest(
                base = null,
                codes = CurrencyCode.entries.take(15).toSet()
            ).toUiState()
        }
    }
}