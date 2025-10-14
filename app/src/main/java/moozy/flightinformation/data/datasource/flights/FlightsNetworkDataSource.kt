package moozy.flightinformation.data.datasource.flights

import moozy.flightinformation.data.datasource.flights.api.FlightApi
import moozy.flightinformation.data.datasource.flights.dto.InstantScheduleDomesticArrivalDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlightsNetworkDataSource @Inject constructor(
    private val api: FlightApi
) : FlightsDataSource {
    override suspend fun fetchArrivals(): Result<List<InstantScheduleDomesticArrivalDto.InstantScheduleDomesticArrivalDtoItem>> {
        return api.instantDomesticArrivals()
    }
}