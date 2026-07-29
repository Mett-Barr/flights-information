package moozy.flightinformation.data.repository.flights

import moozy.flightinformation.data.datasource.flights.FlightsDataSource
import moozy.flightinformation.data.datasource.flights.dto.toDomain
import moozy.flightinformation.domain.model.flights.FlightArrival
import moozy.flightinformation.domain.repository.flights.FlightsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlightsRepositoryImpl @Inject constructor(
    private val networkDataSource: FlightsDataSource
) : FlightsRepository {

    /** DTO 到此為止：對外只吐 domain model，來源格式不外洩。 */
    override suspend fun fetchArrivals(): Result<List<FlightArrival>> =
        networkDataSource.fetchArrivals().map { it.toDomain() }
}
