package moozy.flightinformation.data.repository.currency

import kotlinx.coroutines.flow.StateFlow
import moozy.flightinformation.domain.model.currency.Currencies
import moozy.flightinformation.domain.value.CurrencyCode

interface CurrencyRepository {
    suspend fun getLatest(base: CurrencyCode?, codes: Set<CurrencyCode>): Result<Currencies>
}