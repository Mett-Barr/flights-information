package moozy.flightinformation.presentation.viewmodel

import app.cash.turbine.test
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import moozy.flightinformation.domain.model.currency.Currencies
import moozy.flightinformation.domain.model.currency.CurrencyRate
import moozy.flightinformation.domain.error.LoadError
import moozy.flightinformation.domain.repository.currency.CurrencyRepository
import moozy.flightinformation.domain.value.CurrencyCode
import moozy.flightinformation.domain.value.MoneyCode
import moozy.flightinformation.presentation.model.currency.CurrencyRowWithConversion
import moozy.flightinformation.presentation.state.currency.CurrencyUiState
import moozy.flightinformation.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class CurrencyViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeCurrencyRepository(
        var result: Result<Currencies> = Result.success(currencies()),
    ) : CurrencyRepository {
        var callCount = 0
            private set
        var lastBase: CurrencyCode? = null
            private set
        var lastCodes: Set<CurrencyCode> = emptySet()
            private set

        override suspend fun getLatest(
            base: CurrencyCode?,
            codes: Set<CurrencyCode>,
        ): Result<Currencies> {
            callCount++
            lastBase = base
            lastCodes = codes
            return result
        }
    }

    @Test
    fun `starts in loading`() {
        val viewModel = CurrencyViewModel(FakeCurrencyRepository())

        assertTrue(viewModel.state.value is CurrencyUiState.Loading)
    }

    @Test
    fun `load publishes content after a successful request`() = runTest {
        val viewModel = CurrencyViewModel(FakeCurrencyRepository())

        viewModel.load()
        advanceUntilIdle()

        assertTrue(viewModel.state.value is CurrencyUiState.Content.Plain)
    }

    @Test
    fun `load publishes error after a failed request`() = runTest {
        val viewModel = CurrencyViewModel(
            FakeCurrencyRepository(Result.failure(IOException("offline"))),
        )

        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.state.value as CurrencyUiState.Error
        assertEquals(LoadError.Unknown("offline"), state.error)
    }

    @Test
    fun `load requests a fixed currency set that contains the base`() = runTest {
        val repository = FakeCurrencyRepository()
        val viewModel = CurrencyViewModel(repository)

        viewModel.load()
        advanceUntilIdle()

        // 顯示哪些幣別是產品決定，不是亂數決定：清單固定，冷啟動之間才可重現、可斷言。
        assertEquals(
            setOf(
                CurrencyCode.USD,
                CurrencyCode.EUR,
                CurrencyCode.JPY,
                CurrencyCode.GBP,
                CurrencyCode.CNY,
                CurrencyCode.HKD,
                CurrencyCode.KRW,
                CurrencyCode.SGD,
                CurrencyCode.THB,
                CurrencyCode.AUD,
            ),
            repository.lastCodes,
        )
        // 需求是「六種以上」。
        assertTrue(repository.lastCodes.size >= 6)
        // base 沒被一起請求的話，回應裡就沒有它那一列，換算會失去分母。
        assertEquals(CurrencyCode.USD, repository.lastBase)
        assertTrue(CurrencyCode.USD in repository.lastCodes)
    }

    @Test
    fun `a refresh always requests the selected base alongside the selection`() = runTest {
        val repository = FakeCurrencyRepository()
        val viewModel = CurrencyViewModel(repository)
        viewModel.load()
        advanceUntilIdle()

        // 使用者把幣別全部取消勾選：空清單會被 repository 直接判失敗。
        viewModel.onCurrencySelect(CurrencyCode.USD)
        viewModel.onCurrencySelect(CurrencyCode.EUR)
        viewModel.onBaseCurrencySelect(CurrencyCode.EUR)
        val content = viewModel.state.value as CurrencyUiState.Content
        assertTrue(content.selected.isEmpty())

        viewModel.getCurrencies(content)
        advanceUntilIdle()

        assertEquals(setOf(CurrencyCode.EUR), repository.lastCodes)
    }

    @Test
    fun `a refresh without a selected base still requests the default base`() = runTest {
        val repository = FakeCurrencyRepository()
        val viewModel = CurrencyViewModel(repository)
        viewModel.load()
        advanceUntilIdle()

        // 沒選 base 時 repository 會退回 USD，請求清單也必須有 USD。
        viewModel.onCurrencySelect(CurrencyCode.USD)
        val content = viewModel.state.value as CurrencyUiState.Content

        viewModel.getCurrencies(content)
        advanceUntilIdle()

        assertEquals(setOf(CurrencyCode.EUR, CurrencyCode.USD), repository.lastCodes)
    }

    @Test
    fun `a reload keeps the selected base currency`() = runTest {
        val viewModel = CurrencyViewModel(FakeCurrencyRepository())
        viewModel.load()
        advanceUntilIdle()

        viewModel.onBaseCurrencySelect(CurrencyCode.EUR)
        val content = viewModel.state.value as CurrencyUiState.Content

        viewModel.getCurrencies(content)
        advanceUntilIdle()

        // 重建狀態物件時漏掉的欄位會靜默退回預設值，使用者選的基準幣別就這樣被洗掉。
        val reloaded = viewModel.state.value as CurrencyUiState.Content
        assertEquals(CurrencyCode.EUR, reloaded.selectedBaseCurrency)
    }

    @Test
    fun `a failed refresh keeps the previously loaded rows`() = runTest {
        val repository = FakeCurrencyRepository()
        val viewModel = CurrencyViewModel(repository)
        viewModel.load()
        advanceUntilIdle()
        val loaded = viewModel.state.value as CurrencyUiState.Content

        viewModel.state.test {
            repository.result = Result.failure(IOException("offline"))
            viewModel.getCurrencies(loaded)
            advanceUntilIdle()

            // 刷新失敗只該收掉指示器；已經載到的匯率不該被錯誤畫面清空。
            val afterFailure = expectMostRecentItem() as CurrencyUiState.Content
            assertEquals(loaded.rows, afterFailure.rows)
            assertFalse(afterFailure.isRefreshing)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `load requests the repository only once`() = runTest {
        val repository = FakeCurrencyRepository()
        val viewModel = CurrencyViewModel(repository)

        viewModel.load()
        viewModel.load()
        advanceUntilIdle()

        assertEquals(1, repository.callCount)
    }

    @Test
    fun `refresh completes without consuming virtual time`() = runTest {
        val viewModel = CurrencyViewModel(FakeCurrencyRepository())
        viewModel.load()
        advanceUntilIdle()
        val content = viewModel.state.value as CurrencyUiState.Content

        viewModel.getCurrencies(content)
        advanceUntilIdle()

        assertEquals(0L, testScheduler.currentTime)
        val refreshed = viewModel.state.value as CurrencyUiState.Content
        assertFalse(refreshed.isRefreshing)
    }

    @Test
    fun `selecting a currency toggles it in the content selection`() = runTest {
        val viewModel = CurrencyViewModel(FakeCurrencyRepository())
        viewModel.load()
        advanceUntilIdle()

        viewModel.onCurrencySelect(CurrencyCode.EUR)

        val content = viewModel.state.value as CurrencyUiState.Content.Plain
        assertEquals(persistentSetOf(CurrencyCode.USD), content.selected)

        viewModel.onCurrencySelect(CurrencyCode.EUR)

        val selectedAgain = viewModel.state.value as CurrencyUiState.Content.Plain
        assertEquals(persistentSetOf(CurrencyCode.USD, CurrencyCode.EUR), selectedAgain.selected)
    }

    @Test
    fun `base selection and money input update content conversion`() = runTest {
        val viewModel = CurrencyViewModel(FakeCurrencyRepository())
        viewModel.load()
        advanceUntilIdle()

        viewModel.onBaseCurrencySelect(CurrencyCode.EUR)
        val selected = viewModel.state.value as CurrencyUiState.Content.Plain
        assertEquals(CurrencyCode.EUR, selected.selectedBaseCurrency)

        viewModel.inputMoney(selected, "10")
        advanceUntilIdle()

        val converted = viewModel.state.value as CurrencyUiState.Content.WithConversion
        val eur = converted.rows.single { it.code == "EUR" } as CurrencyRowWithConversion
        assertEquals(0, BigDecimal("10").compareTo(eur.convertedAmount))
    }

    @Test
    fun `money input uses the selected base currency from content`() = runTest {
        val viewModel = CurrencyViewModel(FakeCurrencyRepository())
        viewModel.load()
        advanceUntilIdle()

        viewModel.onBaseCurrencySelect(CurrencyCode.EUR)
        val content = viewModel.state.value as CurrencyUiState.Content

        viewModel.inputMoney(content, "10")
        advanceUntilIdle()

        val converted = viewModel.state.value as CurrencyUiState.Content.WithConversion
        val eur = converted.rows.single { it.code == "EUR" } as CurrencyRowWithConversion
        assertEquals(0, BigDecimal("10").compareTo(eur.convertedAmount))
    }

    @Test
    fun `money input keeps the selected base after reading state again`() = runTest {
        val viewModel = CurrencyViewModel(FakeCurrencyRepository())
        viewModel.load()
        advanceUntilIdle()

        viewModel.onBaseCurrencySelect(CurrencyCode.EUR)
        val contentAfterRecreation = viewModel.state.value as CurrencyUiState.Content

        viewModel.inputMoney(contentAfterRecreation, "10")
        advanceUntilIdle()

        val converted = viewModel.state.value as CurrencyUiState.Content.WithConversion
        val eur = converted.rows.single { it.code == "EUR" } as CurrencyRowWithConversion
        assertEquals(0, BigDecimal("10").compareTo(eur.convertedAmount))
    }

    private companion object {
        fun currencies() = Currencies(
            base = CurrencyCode.USD,
            list = listOf(
                CurrencyRate(MoneyCode.Known(CurrencyCode.USD), BigDecimal("1.0")),
                CurrencyRate(MoneyCode.Known(CurrencyCode.EUR), BigDecimal("0.8")),
            ),
        )
    }
}
