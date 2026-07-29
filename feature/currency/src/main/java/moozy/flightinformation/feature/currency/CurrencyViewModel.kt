package moozy.flightinformation.feature.currency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import moozy.flightinformation.domain.error.LoadError
import moozy.flightinformation.domain.repository.currency.CurrencyRepository
import moozy.flightinformation.domain.value.CurrencyCode
import moozy.flightinformation.domain.value.MoneyCode
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
        if (hasLoaded && _state.value !is CurrencyUiState.Error) return
        hasLoaded = true

        viewModelScope.launch {
            getLatestCurrencies(
                base = DEFAULT_BASE_CURRENCY,
                codes = DEFAULT_CURRENCY_CODES
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

    // 每次選取都會重新請求。連續點選時舊的請求必須取消，否則多筆回應會競速，
    // 畫面可能停在先送出、後回來的那一份，也就是使用者已經取消掉的選擇。
    private var loadJob: Job? = null

    fun getCurrencies(state: CurrencyUiState.Content) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = state.copyRefreshing(true)

            getLatestCurrencies(state.selectedBaseCurrency, state.selected)
        }
    }

    private suspend fun getLatestCurrencies(
        base: CurrencyCode?,
        codes: Set<CurrencyCode>
    ) {
        // 失敗時要能把畫面上既有的資料留下來，所以先記住當前 Content。
        val previous: CurrencyUiState.Content? = _state.value as? CurrencyUiState.Content

        // base 沒被一起請求，回應裡就沒有它那一列，換算會失去分母（也會拿別的幣別當基準）。
        // base 為 null 時 repository 會退回 USD，這裡補的就是同一個預設值。
        val requestedCodes: Set<CurrencyCode> = codes + (base ?: DEFAULT_BASE_CURRENCY)

        _state.value = repository.getLatest(
            base = base,
            codes = requestedCodes
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
                    baseCode = currencies.base,
                    // 使用者挑的基準幣別是 UI 偏好，不該被每次載入洗掉。
                    selectedBaseCurrency = previous?.selectedBaseCurrency
                )
            },
            onFailure = { error ->
                // 刷新失敗時寧可留著舊資料，也不要把畫面清空；
                // 只有從頭就沒東西可顯示時才進錯誤畫面。
                previous?.copyRefreshing(false) ?: CurrencyUiState.Error(error.asLoadError())
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

    private companion object {
        /** 預設基準幣別：也是匯率 API 未指定 base 時的預設值，兩邊一致才不會標錯。 */
        val DEFAULT_BASE_CURRENCY = CurrencyCode.USD

        /**
         * 首次載入顯示的幣別：主要貨幣加上高雄機場鄰近航線常用的幣別。
         *
         * 固定清單而非隨機取樣——顯示哪些幣別是產品決定，隨機的話每次冷啟動都不一樣，
         * 使用者回報的問題無法重現，測試也無從斷言。
         */
        val DEFAULT_CURRENCY_CODES: Set<CurrencyCode> = setOf(
            CurrencyCode.USD,
            CurrencyCode.EUR,
            CurrencyCode.JPY,
            CurrencyCode.GBP,
            CurrencyCode.CNY,
            CurrencyCode.HKD,
            CurrencyCode.KRW,
            CurrencyCode.SGD,
            CurrencyCode.THB,
            CurrencyCode.AUD
        )
    }
}

/** repository 之下的每一層都已把失敗收斂成 [LoadError]；這裡只是兜底。 */
private fun Throwable.asLoadError(): LoadError =
    this as? LoadError ?: LoadError.Unknown(message)

/** [CurrencyUiState.Content] 是 sealed class，沒有共用的 `copy`，只能逐子型別複製。 */
private fun CurrencyUiState.Content.copyRefreshing(isRefreshing: Boolean): CurrencyUiState.Content =
    when (this) {
        is CurrencyUiState.Content.Plain -> copy(isRefreshing = isRefreshing)
        is CurrencyUiState.Content.WithConversion -> copy(isRefreshing = isRefreshing)
    }
