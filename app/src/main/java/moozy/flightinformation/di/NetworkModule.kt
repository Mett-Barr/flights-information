package moozy.flightinformation.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import kotlinx.serialization.json.Json
import moozy.flightinformation.BuildConfig
import moozy.flightinformation.data.network.KtorHttpRequester
import moozy.flightinformation.data.network.KtorHttpRequesterImpl
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // 共用 JSON
    @Provides
    @Singleton
    @Named("ApiJson")
    fun provideApiJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }


    // --- Flight HttpClient ---
    @Provides
    @Singleton
    @Named("FlightsClient")
    fun provideFlightsClient(): HttpClient = HttpClient {
        installTimeout()
    }

    // --- Currency HttpClient ---
    @Provides
    @Singleton
    @Named("CurrencyClient")
    fun provideCurrencyClient(): HttpClient = HttpClient {
        installTimeout()
        defaultRequest {
            url("https://api.freecurrencyapi.com/v1/")
            header("apikey", BuildConfig.FREE_CURRENCY_API_KEY)
        }
    }

    // --- Flight Requester ---
    @Provides
    @Singleton
    @Named("FlightsRequester")
    fun provideFlightsRequester(
        @Named("FlightsClient") client: HttpClient,
        @Named("ApiJson") json: Json
    ): KtorHttpRequester = KtorHttpRequesterImpl(client, json)

    // --- Currency Requester ---
    @Provides
    @Singleton
    @Named("CurrencyRequester")
    fun provideCurrencyRequester(
        @Named("CurrencyClient") client: HttpClient,
        @Named("ApiJson") json: Json
    ): KtorHttpRequester = KtorHttpRequesterImpl(client, json)
}

/**
 * Ktor 預設不設逾時，一個沒回應的請求會無限等下去。
 * 上限取一個新鮮期：等超過那麼久，資料本來就該重抓了。
 */
private fun HttpClientConfig<*>.installTimeout() {
    install(HttpTimeout) {
        requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
        connectTimeoutMillis = REQUEST_TIMEOUT_MILLIS
        socketTimeoutMillis = REQUEST_TIMEOUT_MILLIS
    }
}

private const val REQUEST_TIMEOUT_MILLIS = 10_000L
