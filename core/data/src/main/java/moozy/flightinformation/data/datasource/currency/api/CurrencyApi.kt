package moozy.flightinformation.data.datasource.currency.api

import io.ktor.http.HttpMethod
import moozy.flightinformation.data.datasource.currency.dto.CurrenciesDto
import moozy.flightinformation.data.datasource.currency.url.FreeCurrencyEndpoint
import moozy.flightinformation.data.network.KtorHttpRequester
import moozy.flightinformation.data.network.applyEndpoint
import moozy.flightinformation.data.network.request
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton


@Singleton
class CurrencyApi @Inject constructor(
    @param:Named("CurrencyRequester") private val http: KtorHttpRequester
) {
    suspend fun latest(
        base: String?,
        currenciesCsv: String
    ): Result<CurrenciesDto> = http.request {
        method = HttpMethod.Get
        url {
            applyEndpoint(FreeCurrencyEndpoint.LATEST)
            base?.let { value -> parameters.append("base_currency", value) }
            parameters.append("currencies", currenciesCsv)
        }
    }
}
