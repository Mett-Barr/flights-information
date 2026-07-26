package moozy.flightinformation.data.datasource.flights.api

import io.ktor.http.HttpMethod
import moozy.flightinformation.data.datasource.flights.dto.InstantScheduleDomesticArrivalDto
import moozy.flightinformation.data.datasource.flights.url.KiaEndpoint
import moozy.flightinformation.data.network.KtorHttpRequester
import moozy.flightinformation.data.network.applyEndpoint
import moozy.flightinformation.data.network.request
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class FlightsApi @Inject constructor(
    @param:Named("FlightsRequester") private val http: KtorHttpRequester
) {
    suspend fun instantDomesticArrivals(): Result<List<InstantScheduleDomesticArrivalDto.InstantScheduleDomesticArrivalDtoItem>> =
        http.request {
            method = HttpMethod.Get                 // ① 這支 API 是 GET
            url {
                applyEndpoint(KiaEndpoint.INSTANT_DOM_ARR)
            }
        }
}
