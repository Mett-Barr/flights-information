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
import moozy.flightinformation.domain.error.LoadError
import moozy.flightinformation.domain.repository.flights.FlightsRepository
import moozy.flightinformation.presentation.mapper.toUiModels
import moozy.flightinformation.presentation.state.flights.FlightArrivalsUiState
import java.time.LocalDateTime
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
class FlightsViewModel(
    private val flightsRepository: FlightsRepository,
    private val clock: () -> LocalDateTime = LocalDateTime::now,
) : ViewModel() {

    @Inject
    constructor(flightsRepository: FlightsRepository) : this(flightsRepository, LocalDateTime::now)

    /** 使用者宣告手上的資料已不可信。CONFLATED：連按只算一次。 */
    private val invalidated = Channel<Unit>(Channel.CONFLATED)

    private val _refreshEvent = MutableSharedFlow<Unit>()
    val refreshEvent = _refreshEvent.asSharedFlow()

    val state: StateFlow<FlightArrivalsUiState> = flow {
        var current: FlightArrivalsUiState = FlightArrivalsUiState.Loading
        var refreshWasUserInitiated = false
        while (true) {
            if (refreshWasUserInitiated && current is FlightArrivalsUiState.Content) {
                current = current.copy(isRefreshing = true)
                emit(current)
            }
            current = load(current)
            emit(current)
            if (current is FlightArrivalsUiState.Content) _refreshEvent.emit(Unit)

            // 等使用者作廢資料，最多等一個新鮮期。兩條路都是「資料失效了」，
            // 但回傳值區分刷新來源，只有使用者主動刷新才顯示指示器。
            refreshWasUserInitiated =
                withTimeoutOrNull(FRESHNESS_MILLIS) { invalidated.receive() } != null
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
                    FlightArrivalsUiState.Content(
                        items = items,
                        updatedAt = clock(),
                        isRefreshing = false,
                    )
                },
                onFailure = { error ->
                    // 刷新失敗時寧可留著舊資料，也不要把畫面清空；
                    // 只有從頭就沒東西可顯示時才進錯誤畫面。
                    if (previous is FlightArrivalsUiState.Content) previous.copy(isRefreshing = false)
                    else FlightArrivalsUiState.Error(error.asLoadError())
                },
            )

    private companion object {
        const val FRESHNESS_MILLIS = 10_000L

        /** 短暫離開（轉螢幕、切頁又切回）不要重啟整條流程。 */
        const val SUBSCRIPTION_GRACE_MILLIS = 5_000L
    }
}

/** repository 之下的每一層都已把失敗收斂成 [LoadError]；這裡只是兜底。 */
private fun Throwable.asLoadError(): LoadError =
    this as? LoadError ?: LoadError.Unknown(message)
