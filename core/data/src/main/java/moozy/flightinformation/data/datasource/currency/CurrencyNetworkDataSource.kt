package moozy.flightinformation.data.datasource.currency

import moozy.flightinformation.data.datasource.currency.api.CurrencyApi
import moozy.flightinformation.data.datasource.currency.dto.CurrenciesDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrencyNetworkDataSource @Inject constructor(
    private val api: CurrencyApi
) : CurrencyDataSource {
    override suspend fun fetchLatest(base: String?, currenciesCsv: String): Result<CurrenciesDto> =
        api.latest(base, currenciesCsv)
}
