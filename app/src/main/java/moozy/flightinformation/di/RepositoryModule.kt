package moozy.flightinformation.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import moozy.flightinformation.domain.repository.currency.CurrencyRepository
import moozy.flightinformation.data.repository.currency.CurrencyRepositoryImpl
import moozy.flightinformation.domain.repository.flights.FlightsRepository
import moozy.flightinformation.data.repository.flights.FlightsRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindFlightsRepository(
        impl: FlightsRepositoryImpl
    ): FlightsRepository


    @Binds
    @Singleton
    abstract fun bindCurrencyRepository(
        impl: CurrencyRepositoryImpl
    ): CurrencyRepository
}
