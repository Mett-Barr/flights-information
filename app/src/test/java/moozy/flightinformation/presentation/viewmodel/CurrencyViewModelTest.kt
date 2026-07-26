package moozy.flightinformation.presentation.viewmodel

import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import moozy.flightinformation.domain.model.currency.Currencies
import moozy.flightinformation.domain.model.currency.CurrencyRate
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

        override suspend fun getLatest(
            base: CurrencyCode?,
            codes: Set<CurrencyCode>,
        ): Result<Currencies> {
            callCount++
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

        assertTrue(viewModel.state.value is CurrencyUiState.Error)
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
