package moozy.flightinformation.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeoutOrNull
import moozy.flightinformation.domain.repository.flights.FlightsRepository
import moozy.flightinformation.presentation.mapper.toUiModels
import moozy.flightinformation.presentation.state.flights.FlightArrivalsUiState
import javax.inject.Inject

/**
 * 抓取的不變式是「**有需求 ∧ 資料失效**」，兩者缺一不可：
 *  - **需求**：有人在收集 [state]。沒人看就別抓（省電、省流量、省後端）。
 *    由 [SharingStarted.WhileSubscribed] 表達，所以 app 進背景會自動停、回前景自動恢復。
 *  - **失效**：新鮮期過了（自然衰減），或使用者按下刷新（主動宣告不可信）。
 *
 * 寫成一個迴圈之後，「手動刷新要重新計時」不需要任何特殊處理：迴圈回到頂端重抓，
 * 新鮮期自然從那一刻重算。先前為了在事件模型下做到這件事，得靠一個可從外部重設、
 * 又不想用取消協程來重啟的計時器；換成狀態模型後那個需求就消失了。
 */
@HiltViewModel
class FlightsViewModel @Inject constructor(
    private val flightsRepository: FlightsRepository
) : ViewModel() {

    /** 使用者宣告手上的資料已不可信。CONFLATED：連按只算一次。 */
    private val invalidated = Channel<Unit>(Channel.CONFLATED)

    private val _refreshEvent = MutableSharedFlow<Unit>()
    val refreshEvent = _refreshEvent.asSharedFlow()

    val state: StateFlow<FlightArrivalsUiState> = flow {
        var current: FlightArrivalsUiState = FlightArrivalsUiState.Loading
        while (true) {
            current = load(current)
            emit(current)
            if (current is FlightArrivalsUiState.Content) _refreshEvent.emit(Unit)

            // 等使用者作廢資料，最多等一個新鮮期。兩條路都是「資料失效了」，
            // 所以回傳值用不上：醒來就回到迴圈頂端重抓。
            withTimeoutOrNull(FRESHNESS_MILLIS) { invalidated.receive() }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_GRACE_MILLIS),
        initialValue = FlightArrivalsUiState.Loading,
    )

    /** 下拉刷新：把資料標記為失效，等待中的迴圈會立刻重抓。 */
    fun refresh() {
        invalidated.trySend(Unit)
    }

    private suspend fun load(previous: FlightArrivalsUiState): FlightArrivalsUiState =
        flightsRepository.fetchArrivals()
            .mapCatching { it.toUiModels() }
            .fold(
                onSuccess = { items ->
                    FlightArrivalsUiState.Content(items = items, isRefreshing = false)
                },
                onFailure = { error ->
                    // 刷新失敗時寧可留著舊資料，也不要把畫面清空；
                    // 只有從頭就沒東西可顯示時才進錯誤畫面。
                    if (previous is FlightArrivalsUiState.Content) previous
                    else FlightArrivalsUiState.Error(error.message ?: "Unknown error")
                },
            )

    private companion object {
        const val FRESHNESS_MILLIS = 10_000L

        /** 短暫離開（轉螢幕、切頁又切回）不要重啟整條流程。 */
        const val SUBSCRIPTION_GRACE_MILLIS = 5_000L
    }
}
