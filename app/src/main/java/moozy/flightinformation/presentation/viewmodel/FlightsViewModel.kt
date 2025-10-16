package moozy.flightinformation.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import moozy.flightinformation.data.repository.flights.FlightsRepository
import moozy.flightinformation.presentation.mapper.toUiModels
import moozy.flightinformation.presentation.state.flights.FlightArrivalsUiState
import moozy.flightinformation.util.time.ReTimer
import javax.inject.Inject

@HiltViewModel
class FlightsViewModel @Inject constructor(
    private val flightsRepository: FlightsRepository
) : ViewModel() {
    private val _state = MutableStateFlow<FlightArrivalsUiState>(FlightArrivalsUiState.Loading)
    val state: StateFlow<FlightArrivalsUiState> = _state.asStateFlow()

    private val _refreshEvent: MutableSharedFlow<Unit> = MutableSharedFlow()
    val refreshEvent = _refreshEvent.asSharedFlow()

    private var allowAutoRefresh = false

    private val refreshReTimer =
        ReTimer(
            scope = viewModelScope,
            intervalMillis = 10000,
            onStartOrRestart = {
                load()
            },
            onTimeout = {
                if (allowAutoRefresh) restart()
            }
        )

    init {
        load()
    }

    /** 下拉刷新（保留畫面內容，顯示 isRefreshing=true） */
    fun refresh() = refreshReTimer.restart()

    fun enableAutoRefresh() {
        allowAutoRefresh = true
        if (!refreshReTimer.isCountingDown.value) refreshReTimer.restart()
    }

    fun disableAutoRefresh() {
        allowAutoRefresh = false
    }

    private fun load() {
        viewModelScope.launch {
            _state.update {
                when (it) {
                    is FlightArrivalsUiState.Content -> {
                        it.copy(isRefreshing = true)
                    }

                    else -> FlightArrivalsUiState.Loading
                }
            }

            // 演示 loading 動畫用的
            delay(1500)

            flightsRepository.fetchArrivals() // Result<List<Dto>>
                .mapCatching { it.toUiModels() } // DTO -> UiModel（只做 UI 格式化）
                .fold(
                    onSuccess = { items ->
                        _state.value =
                            FlightArrivalsUiState.Content(items = items, isRefreshing = false)
                        delay(500)
                        _refreshEvent.emit(Unit)
                    },
                    onFailure = { e ->
                        // 若是刷新失敗，回退到原 Content 並關閉 isRefreshing；若是首次則顯示 Error。
                        val prev = _state.value
                        _state.value = if (prev is FlightArrivalsUiState.Content) {
                            prev.copy(isRefreshing = false)
                        } else {
                            FlightArrivalsUiState.Error(e.message ?: "Unknown error")
                        }
                    }
                )
        }
    }
}