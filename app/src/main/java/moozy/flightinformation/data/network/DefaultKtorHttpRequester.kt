package moozy.flightinformation.data.network

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultKtorHttpRequester @Inject constructor(
    private val client: HttpClient
) : KtorHttpRequester {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun <T> request(
        deserializer: DeserializationStrategy<T>,
        build: HttpRequestBuilder.() -> Unit
    ): Result<T> = runCatching {
        val rsp = client.request(build)
        // 若你要沿用 ContentNegotiation，可用 bodyAsText() 再用你注入的 Json 解碼：
        json.decodeFromString(deserializer, rsp.bodyAsText())
    }
}
