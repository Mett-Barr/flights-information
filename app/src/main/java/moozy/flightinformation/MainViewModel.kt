package moozy.flightinformation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import moozy.flightinformation.data.remote.api.FlightApi
import moozy.flightinformation.presentation.mapper.toUiModels
import moozy.flightinformation.presentation.state.FlightArrivalsUiState
import moozy.flightinformation.presentation.state.FlightArrivalsUiState.*
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val flightApi: FlightApi
) : ViewModel() {

    private val _ui = MutableStateFlow<FlightArrivalsUiState>(Loading)
    val ui: StateFlow<FlightArrivalsUiState> = _ui.asStateFlow()

    init { load(initial = true) }

    /** 首次/重試載入（會顯示 Loading 畫面） */
    fun retry() = load(initial = true)

    /** 下拉刷新（保留畫面內容，顯示 isRefreshing=true） */
    fun refresh() = load(initial = false)

    private fun load(initial: Boolean) {
        viewModelScope.launch {
            if (initial) _ui.value = Loading
            else (_ui.value as? Content)?.let { _ui.value = it.copy(isRefreshing = true) }

            FlightApi.instantDomesticArrivals() // Result<List<Dto>>
                .mapCatching { it.toUiModels() } // DTO -> UiModel（只做 UI 格式化）
                .fold(
                    onSuccess = { items ->
                        _ui.value = Content(items = items, isRefreshing = false)
                    },
                    onFailure = { e ->
                        // 若是刷新失敗，回退到原 Content 並關閉 isRefreshing；若是首次則顯示 Error。
                        val prev = _ui.value
                        _ui.value = if (!initial && prev is Content) {
                            prev.copy(isRefreshing = false)
                        } else {
                            Error(e.message ?: "Unknown error")
                        }
                    }
                )
        }
    }
}