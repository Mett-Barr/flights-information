package moozy.flightinformation.data.repository.currency

import moozy.flightinformation.data.datasource.currency.CurrencyDataSource
import moozy.flightinformation.data.datasource.currency.CurrencyNetworkDataSource
import moozy.flightinformation.data.datasource.currency.dto.toCurrencies
import moozy.flightinformation.domain.model.currency.Currencies
import moozy.flightinformation.domain.model.currency.CurrencyCode
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
        if (codes.isEmpty()) {
            return Result.failure(IllegalArgumentException("At least one currency code is required"))
        }
        val currenciesCsv = codes.toCsv()
        return networkDataSource.fetchLatest(base?.code, currenciesCsv).mapCatching { it.toCurrencies() }
    }

    private fun Iterable<CurrencyCode>.toCsv(): String =
        joinToString(",") { it.code }
}