package moozy.flightinformation.data.repository.currency

import kotlinx.coroutines.flow.StateFlow
import moozy.flightinformation.domain.model.currency.Currencies
import moozy.flightinformation.domain.model.currency.CurrencyCode

interface CurrencyRepository {
    suspend fun getLatest(base: String?, codes: Set<CurrencyCode>): Result<Currencies>
}