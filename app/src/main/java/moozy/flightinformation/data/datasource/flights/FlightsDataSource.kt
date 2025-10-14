package moozy.flightinformation.data.datasource.flights

import moozy.flightinformation.data.datasource.flights.dto.InstantScheduleDomesticArrivalDto

interface FlightsDataSource {
    suspend fun fetchArrivals(): Result<List<InstantScheduleDomesticArrivalDto.InstantScheduleDomesticArrivalDtoItem>>
}