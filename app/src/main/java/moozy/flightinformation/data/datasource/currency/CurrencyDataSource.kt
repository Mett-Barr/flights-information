package moozy.flightinformation.data.datasource.currency

import moozy.flightinformation.data.datasource.currency.dto.CurrenciesDto

interface CurrencyDataSource {
    suspend fun fetchLatest(base: String?, currenciesCsv: String): Result<CurrenciesDto>
}