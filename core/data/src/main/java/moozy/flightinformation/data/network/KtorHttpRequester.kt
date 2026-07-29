package moozy.flightinformation.data.network

import io.ktor.client.request.HttpRequestBuilder
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.serializer

interface KtorHttpRequester {
    suspend fun <T> request(
        deserializer: DeserializationStrategy<T>,
        build: HttpRequestBuilder.() -> Unit
    ): Result<T>
}

suspend inline fun <reified T> KtorHttpRequester.request(
    noinline build: HttpRequestBuilder.() -> Unit
): Result<T> = request(serializer<T>(), build)
