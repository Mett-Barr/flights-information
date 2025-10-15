package moozy.flightinformation.data.repository.flights

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import moozy.flightinformation.data.datasource.flights.FlightsDataSource
import moozy.flightinformation.data.datasource.flights.FlightsNetworkDataSource
import moozy.flightinformation.data.datasource.flights.api.FlightsApi
import moozy.flightinformation.data.datasource.flights.dto.InstantScheduleDomesticArrivalDto
import moozy.flightinformation.data.network.KtorHttpRequesterImpl
import org.junit.Test

class FlightsRepositoryImplTest {

    @Test
    fun `fetchArrivals returns success from datasource`() = runTest {

        // 相當於 @Provides @Named("ApiJson")
        val json = Json { ignoreUnknownKeys = true; isLenient = true }

        // 相當於 @Provides @Named("FlightsClient")
        val client = HttpClient {
            install(ContentNegotiation) { json(json) }
        }

        // 相當於 @Provides @Named("FlightsRequester")
        val requester = KtorHttpRequesterImpl(client, json)

        // 相當於 Hilt @Inject
        val api = FlightsApi(requester)
        val ds = FlightsNetworkDataSource(api)
        val repo = FlightsRepositoryImpl(ds)

        // 直接測試整條真實線
        val result = repo.fetchArrivals()
        println("✅ 成功: ${result.isSuccess}")
        println("結果:")
        println(result.getOrElse { "❌ 錯誤: ${it.message}" }.let {
            it as? List<InstantScheduleDomesticArrivalDto.InstantScheduleDomesticArrivalDtoItem>
        }?.first())
    }
}