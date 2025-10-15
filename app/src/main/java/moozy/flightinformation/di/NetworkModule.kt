package moozy.flightinformation.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
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


    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = HttpClient {
        // 通用設定，例如：
        // install(ContentNegotiation) { json() }
    }

    // --- Flight HttpClient ---
    @Provides
    @Singleton
    @Named("FlightsClient")
    fun provideFlightsClient(): HttpClient = HttpClient {
        // 這裡不放 header，可能只要基本設定
    }

    // --- Currency HttpClient ---
    @Provides
    @Singleton
    @Named("CurrencyClient")
    fun provideCurrencyClient(): HttpClient = HttpClient {
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