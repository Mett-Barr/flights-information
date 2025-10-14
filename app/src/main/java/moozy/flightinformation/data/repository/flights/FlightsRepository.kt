package moozy.flightinformation.data.repository.flights

import moozy.flightinformation.data.datasource.flights.dto.InstantScheduleDomesticArrivalDto

interface FlightsRepository {
    suspend fun fetchArrivals(): Result<List<InstantScheduleDomesticArrivalDto.InstantScheduleDomesticArrivalDtoItem>>
}