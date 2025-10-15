package moozy.flightinformation.data.datasource.flights.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import moozy.flightinformation.data.network.KtorHttpRequesterImpl
import org.junit.Test


class FlightsApiTest {

    @Test
    fun test() = runTest {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        val client = HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
        }

        val requester = KtorHttpRequesterImpl(client, json)
        val api = FlightsApi(requester)

        val result = api.instantDomesticArrivals()
        println("✅ Request result: ${result.isSuccess}")
        println("Result detail:\n${result.getOrElse { e -> "❌ ${e.message}" }}")
    }
}