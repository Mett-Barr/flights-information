package moozy.flightinformation.data.datasource.flights.api

import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import moozy.flightinformation.data.network.KtorHttpRequester
import moozy.flightinformation.data.network.request
import moozy.flightinformation.data.datasource.flights.dto.InstantScheduleDomesticArrivalDto
import moozy.flightinformation.data.datasource.flights.url.KiaEndpoint
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlightApi @Inject constructor(
    private val http: KtorHttpRequester
) {
    suspend fun instantDomesticArrivals(): Result<List<InstantScheduleDomesticArrivalDto.InstantScheduleDomesticArrivalDtoItem>> =
        http.request {
            method = HttpMethod.Get                 // ① 這支 API 是 GET
            url(KiaEndpoint.INSTANT_DOM_ARR.url)
        }
}