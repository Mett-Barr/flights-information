package moozy.flightinformation.data.repository.currency

import moozy.flightinformation.data.datasource.currency.CurrencyDataSource
import moozy.flightinformation.data.datasource.currency.CurrencyNetworkDataSource
import moozy.flightinformation.data.datasource.currency.dto.toCurrencies
import moozy.flightinformation.domain.model.currency.Currencies
import moozy.flightinformation.domain.model.currency.CurrencyRate
import moozy.flightinformation.domain.value.CurrencyCode
import moozy.flightinformation.domain.value.MoneyCode
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrencyRepositoryImpl @Inject constructor(
    private val networkDataSource: CurrencyDataSource
) : CurrencyRepository {

    override suspend fun getLatest(
        base: CurrencyCode?,
        codes: Set<CurrencyCode>
    ): Result<Currencies> {
//        return fakeResult
        if (codes.isEmpty()) {
            return Result.failure(IllegalArgumentException("At least one currency code is required"))
        }
        val currenciesCsv = codes.toCsv()
        return networkDataSource.fetchLatest(base?.code, currenciesCsv).mapCatching { it.toCurrencies(base ?: CurrencyCode.USD) }
    }

    private fun Iterable<CurrencyCode>.toCsv(): String =
        joinToString(",") { it.code }
}

val fakeResult: Result<Currencies> = Result.success(
    Currencies(
        base = CurrencyCode.USD,
        list = listOf(
            CurrencyRate(MoneyCode.Known(CurrencyCode.USD), BigDecimal("1.0")),
            CurrencyRate(MoneyCode.Known(CurrencyCode.EUR), BigDecimal("0.93")),
            CurrencyRate(MoneyCode.Known(CurrencyCode.JPY), BigDecimal("150")),
            CurrencyRate(MoneyCode.Known(CurrencyCode.GBP), BigDecimal("0.79")),
            CurrencyRate(MoneyCode.Known(CurrencyCode.CAD), BigDecimal("1.36")),
            CurrencyRate(MoneyCode.Known(CurrencyCode.AUD), BigDecimal("1.50")),
            CurrencyRate(MoneyCode.Known(CurrencyCode.CNY), BigDecimal("7.10")),
            CurrencyRate(MoneyCode.Known(CurrencyCode.KRW), BigDecimal("1360")),
            CurrencyRate(MoneyCode.Known(CurrencyCode.SGD), BigDecimal("1.35")),
            CurrencyRate(MoneyCode.Known(CurrencyCode.NZD), BigDecimal("1.67")),
            CurrencyRate(MoneyCode.Known(CurrencyCode.MYR), BigDecimal("4.73")),
            CurrencyRate(MoneyCode.Known(CurrencyCode.THB), BigDecimal("36.4")),
            CurrencyRate(MoneyCode.Known(CurrencyCode.INR), BigDecimal("83.1")),
            CurrencyRate(MoneyCode.Known(CurrencyCode.HKD), BigDecimal("7.82")),
            CurrencyRate(MoneyCode.Known(CurrencyCode.IDR), BigDecimal("15700")),
            CurrencyRate(MoneyCode.Known(CurrencyCode.PHP), BigDecimal("58.3")),
            CurrencyRate(MoneyCode.Known(CurrencyCode.CHF), BigDecimal("0.90")),
            CurrencyRate(MoneyCode.Known(CurrencyCode.SEK), BigDecimal("10.8")),
            CurrencyRate(MoneyCode.Known(CurrencyCode.NOK), BigDecimal("11.2")),
            CurrencyRate(MoneyCode.Known(CurrencyCode.DKK), BigDecimal("6.95")),
            CurrencyRate(MoneyCode.Known(CurrencyCode.PLN), BigDecimal("4.12")),
            CurrencyRate(MoneyCode.Known(CurrencyCode.HUF), BigDecimal("365")),
            CurrencyRate(MoneyCode.Known(CurrencyCode.CZK), BigDecimal("23.5")),
            CurrencyRate(MoneyCode.Known(CurrencyCode.RON), BigDecimal("4.65")),
            CurrencyRate(MoneyCode.Known(CurrencyCode.BGN), BigDecimal("1.82")),
            CurrencyRate(MoneyCode.Known(CurrencyCode.ISK), BigDecimal("138")),
            CurrencyRate(MoneyCode.Known(CurrencyCode.HRK), BigDecimal("7.52")),
            CurrencyRate(MoneyCode.Known(CurrencyCode.ZAR), BigDecimal("18.9")),
            CurrencyRate(MoneyCode.Known(CurrencyCode.BRL), BigDecimal("5.25")),
            CurrencyRate(MoneyCode.Known(CurrencyCode.TRY), BigDecimal("34.7"))
        )
    )
)
