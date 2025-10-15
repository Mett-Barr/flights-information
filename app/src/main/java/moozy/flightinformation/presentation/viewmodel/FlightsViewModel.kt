package moozy.flightinformation.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import moozy.flightinformation.data.repository.flights.FlightsRepository
import moozy.flightinformation.presentation.mapper.toUiModels
import moozy.flightinformation.presentation.state.flights.FlightArrivalsUiState
import javax.inject.Inject

@HiltViewModel
class FlightsViewModel @Inject constructor(
    private val flightsRepository: FlightsRepository
) : ViewModel() {
    private val _ui = MutableStateFlow<FlightArrivalsUiState>(FlightArrivalsUiState.Loading)
    val ui: StateFlow<FlightArrivalsUiState> = _ui.asStateFlow()

    init { load(initial = true) }

    /** 首次/重試載入（會顯示 Loading 畫面） */
    fun retry() = load(initial = true)

    /** 下拉刷新（保留畫面內容，顯示 isRefreshing=true） */
    fun refresh() = load(initial = false)

    private fun load(initial: Boolean) {
        viewModelScope.launch {
            if (initial) _ui.value = FlightArrivalsUiState.Loading
            else (_ui.value as? FlightArrivalsUiState.Content)?.let { _ui.value = it.copy(isRefreshing = true) }

            flightsRepository.fetchArrivals() // Result<List<Dto>>
                .mapCatching { it.toUiModels() } // DTO -> UiModel（只做 UI 格式化）
                .fold(
                    onSuccess = { items ->
                        _ui.value =
                            FlightArrivalsUiState.Content(items = items, isRefreshing = false)
                    },
                    onFailure = { e ->
                        // 若是刷新失敗，回退到原 Content 並關閉 isRefreshing；若是首次則顯示 Error。
                        val prev = _ui.value
                        _ui.value = if (!initial && prev is FlightArrivalsUiState.Content) {
                            prev.copy(isRefreshing = false)
                        } else {
                            FlightArrivalsUiState.Error(e.message ?: "Unknown error")
                        }
                    }
                )
        }
    }
}