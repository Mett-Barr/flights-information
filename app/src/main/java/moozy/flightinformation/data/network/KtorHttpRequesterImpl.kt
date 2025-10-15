package moozy.flightinformation.data.network

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KtorHttpRequesterImpl @Inject constructor(
    private val client: HttpClient,
    private val json: Json
) : KtorHttpRequester {

    override suspend fun <T> request(
        deserializer: DeserializationStrategy<T>,
        build: HttpRequestBuilder.() -> Unit
    ): Result<T> = runCatching {
        val rsp: HttpResponse = client.request(build)
        json.decodeFromString(deserializer, rsp.bodyAsText())
    }
}
