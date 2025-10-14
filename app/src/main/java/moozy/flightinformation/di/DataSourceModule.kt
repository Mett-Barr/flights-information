package moozy.flightinformation.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import moozy.flightinformation.data.datasource.flights.FlightsDataSource
import moozy.flightinformation.data.datasource.flights.FlightsNetworkDataSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {
    @Binds
    @Singleton
    abstract fun bindFlightsNetworkDataSource(
        impl: FlightsNetworkDataSource
    ): FlightsDataSource
}