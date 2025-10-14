package moozy.flightinformation.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import moozy.flightinformation.data.network.DefaultKtorHttpRequester
import moozy.flightinformation.data.network.KtorHttpRequester
import moozy.flightinformation.data.datasource.flights.api.FlightApi
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = HttpClient {

    }

    @Provides
    @Singleton
    fun provideKtorHttpRequester(
        client: HttpClient
    ): KtorHttpRequester = DefaultKtorHttpRequester(client)

    @Provides
    @Singleton
    fun provideFlightApi(http: KtorHttpRequester): FlightApi = FlightApi(http)
}