package moozy.flightinformation.data.remote.api

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import moozy.flightinformation.data.network.AppHttp
import moozy.flightinformation.data.remote.dto.InstantScheduleDomesticArrivalDto
import moozy.flightinformation.data.remote.url.KiaEndpoint

object FlightApi {
    suspend fun instantDomesticArrivals(): Result<List<InstantScheduleDomesticArrivalDto.InstantScheduleDomesticArrivalDtoItem>> =
        AppHttp.requestResult {
            method = HttpMethod.Get                 // ① 這支 API 是 GET
            url(KiaEndpoint.INSTANT_DOM_ARR.url)
        }
}