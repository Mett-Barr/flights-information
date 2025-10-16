package moozy.flightinformation.data.repository.flights

import moozy.flightinformation.data.datasource.flights.FlightsDataSource
import moozy.flightinformation.data.datasource.flights.dto.InstantScheduleDomesticArrivalDto
import moozy.flightinformation.domain.repository.flights.FlightsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlightsRepositoryImpl @Inject constructor(
    private val networkDataSource: FlightsDataSource
) : FlightsRepository {

    override suspend fun fetchArrivals(): Result<List<InstantScheduleDomesticArrivalDto.InstantScheduleDomesticArrivalDtoItem>> {
        return networkDataSource.fetchArrivals()
    }
}