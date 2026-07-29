package moozy.flightinformation.data.datasource.flights.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import moozy.flightinformation.data.network.KtorHttpRequesterImpl
import org.junit.Assert.assertEquals
import org.junit.Test

class FlightsApiTest {

    @Test
    fun `instant domestic arrivals requests the expected endpoint`() = runTest {
        var requestUrl = ""
        val client = HttpClient(MockEngine { request ->
            requestUrl = request.url.toString()
            respond(
                content = "[]",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        })
        val api = FlightsApi(KtorHttpRequesterImpl(client, Json { ignoreUnknownKeys = true }))

        api.instantDomesticArrivals()

        assertEquals(
            "https://www.kia.gov.tw/Announce/NewsArea/InstantSchedule_DOMARR.json",
            requestUrl,
        )
    }
}
