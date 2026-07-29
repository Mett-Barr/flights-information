package moozy.flightinformation.domain.repository.flights

import moozy.flightinformation.domain.model.flights.FlightArrival

interface FlightsRepository {
    suspend fun fetchArrivals(): Result<List<FlightArrival>>
}
