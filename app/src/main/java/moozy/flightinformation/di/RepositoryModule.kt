package moozy.flightinformation.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import moozy.flightinformation.data.repository.flights.FlightsRepository
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
}