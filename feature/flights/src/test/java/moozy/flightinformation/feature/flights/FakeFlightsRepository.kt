package moozy.flightinformation.feature.flights

import kotlinx.coroutines.delay
import moozy.flightinformation.domain.model.flights.FlightArrival
import moozy.flightinformation.domain.model.flights.FlightStatus
import moozy.flightinformation.domain.repository.flights.FlightsRepository
import java.time.LocalTime

/**
 * Fake 而非 mock：官方建議測 repository/data source 時用 fake，因為它能建模
 * 「隨呼叫變化的狀態」（這裡是 [callCount]），而且不綁實作細節。
 */
class FakeFlightsRepository(
    var result: Result<List<FlightArrival>> = Result.success(listOf(anArrival())),
    var responseDelayMillis: Long = 0,
) : FlightsRepository {

    var callCount = 0
        private set

    override suspend fun fetchArrivals(): Result<List<FlightArrival>> {
        callCount++
        delay(responseDelayMillis)
        return result
    }

    companion object {
        fun anArrival(
            flightNumber: String = "B78690",
            status: FlightStatus = FlightStatus.Arrived,
        ) = FlightArrival(
            flightNumber = flightNumber,
            airlineName = "立榮航空",
            airlineLogoUrl = null,
            originName = "澎湖",
            originCode = "MZG",
            scheduled = LocalTime.of(9, 0),
            actual = LocalTime.of(8, 56),
            status = status,
            gate = "17",
            aircraftType = "AT76",
            delayCause = null,
        )
    }
}
