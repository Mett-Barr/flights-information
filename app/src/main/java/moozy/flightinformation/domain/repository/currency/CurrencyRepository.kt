package moozy.flightinformation.domain.repository.currency

import moozy.flightinformation.domain.model.currency.Currencies
import moozy.flightinformation.domain.value.CurrencyCode

interface CurrencyRepository {
    suspend fun getLatest(base: CurrencyCode?, codes: Set<CurrencyCode>): Result<Currencies>
}