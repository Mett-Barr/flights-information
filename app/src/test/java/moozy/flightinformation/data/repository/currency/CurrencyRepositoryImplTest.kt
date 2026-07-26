package moozy.flightinformation.data.repository.currency

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import moozy.flightinformation.data.datasource.currency.CurrencyDataSource
import moozy.flightinformation.data.datasource.currency.dto.CurrenciesDto
import moozy.flightinformation.domain.value.CurrencyCode
import moozy.flightinformation.domain.value.MoneyCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal

class CurrencyRepositoryImplTest {

    private class FakeCurrencyDataSource(
        var result: Result<CurrenciesDto>,
    ) : CurrencyDataSource {
        var callCount = 0
            private set
        var receivedBase: String? = null
            private set
        var receivedCurrenciesCsv: String? = null
            private set

        override suspend fun fetchLatest(base: String?, currenciesCsv: String): Result<CurrenciesDto> {
            callCount++
            receivedBase = base
            receivedCurrenciesCsv = currenciesCsv
            return result
        }
    }

    private fun dataSourceReturning(data: Map<String, JsonPrimitive> = emptyMap()) =
        FakeCurrencyDataSource(Result.success(CurrenciesDto(data)))

    @Test
    fun `rejects empty codes without calling the data source`() = runTest {
        val dataSource = dataSourceReturning()

        val result = CurrencyRepositoryImpl(dataSource).getLatest(null, emptySet())

        assertTrue(result.isFailure)
        assertEquals(0, dataSource.callCount)
    }

    @Test
    fun `sends multiple codes as a comma separated list without spaces`() = runTest {
        val dataSource = dataSourceReturning()

        CurrencyRepositoryImpl(dataSource).getLatest(
            base = CurrencyCode.USD,
            codes = linkedSetOf(CurrencyCode.EUR, CurrencyCode.JPY),
        )

        assertEquals("EUR,JPY", dataSource.receivedCurrenciesCsv)
    }

    @Test
    fun `uses USD in the domain but preserves a null data source base`() = runTest {
        val dataSource = dataSourceReturning(mapOf("EUR" to JsonPrimitive("0.93")))

        val currencies = CurrencyRepositoryImpl(dataSource)
            .getLatest(base = null, codes = setOf(CurrencyCode.EUR))
            .getOrThrow()

        assertEquals(CurrencyCode.USD, currencies.base)
        assertEquals(null, dataSource.receivedBase)
    }

    @Test
    fun `preserves an explicit base for both the data source and domain`() = runTest {
        val dataSource = dataSourceReturning()

        val currencies = CurrencyRepositoryImpl(dataSource)
            .getLatest(base = CurrencyCode.JPY, codes = setOf(CurrencyCode.EUR))
            .getOrThrow()

        assertEquals(CurrencyCode.JPY.code, dataSource.receivedBase)
        assertEquals(CurrencyCode.JPY, currencies.base)
    }

    @Test
    fun `propagates a data source failure unchanged`() = runTest {
        val failure = IOException("offline")
        val dataSource = FakeCurrencyDataSource(Result.failure(failure))

        val result = CurrencyRepositoryImpl(dataSource).getLatest(null, setOf(CurrencyCode.EUR))

        assertTrue(result.isFailure)
        assertSame(failure, result.exceptionOrNull())
    }

    @Test
    fun `maps known and unknown codes while dropping invalid rates`() = runTest {
        val dataSource = dataSourceReturning(
            mapOf(
                "EUR" to JsonPrimitive("1.00"),
                "XTS" to JsonPrimitive("2.50"),
                "JPY" to JsonPrimitive("not-a-number"),
            ),
        )

        val rates = CurrencyRepositoryImpl(dataSource)
            .getLatest(null, setOf(CurrencyCode.EUR))
            .getOrThrow()
            .list

        assertEquals(2, rates.size)
        assertEquals(MoneyCode.Known(CurrencyCode.EUR), rates[0].code)
        assertEquals(0, rates[0].rate.compareTo(BigDecimal("1.0")))
        assertEquals(MoneyCode.Unknown("XTS"), rates[1].code)
        assertEquals(0, rates[1].rate.compareTo(BigDecimal("2.5")))
        assertFalse(rates.any { it.code == MoneyCode.Known(CurrencyCode.JPY) })
    }
}
