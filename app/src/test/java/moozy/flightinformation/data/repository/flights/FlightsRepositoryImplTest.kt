package moozy.flightinformation.data.repository.flights

import kotlinx.coroutines.test.runTest
import moozy.flightinformation.data.datasource.flights.FlightsDataSource
import moozy.flightinformation.data.datasource.flights.dto.InstantScheduleDomesticArrivalDto.InstantScheduleDomesticArrivalDtoItem
import moozy.flightinformation.domain.model.flights.FlightStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.LocalTime

/**
 * Repository 現在的職責是把來源格式收斂成 domain model，所以測的是那個轉換，
 * 而不是網路。資料來源用 fake 餵固定 DTO，不碰真的 API。
 */
class FlightsRepositoryImplTest {

    private class FakeFlightsDataSource(
        private val result: Result<List<InstantScheduleDomesticArrivalDtoItem>>,
    ) : FlightsDataSource {
        override suspend fun fetchArrivals() = result
    }

    private fun dto(
        expectTime: String? = "09:00",
        realTime: String? = "08:56",
        airFlyStatus: String? = "抵達",
        airLineNum: String? = "B78690",
        airLineCode: String? = "UIA",
        airLineName: String? = "立榮航空",
        upAirportName: String? = "澎湖",
        upAirportCode: String? = "MZG",
        airBoardingGate: String? = "17",
        airPlaneType: String? = "AT76",
        airFlyDelayCause: String? = "",
    ) = InstantScheduleDomesticArrivalDtoItem(
        expectTime = expectTime,
        realTime = realTime,
        airLineName = airLineName,
        airLineCode = airLineCode,
        airLineLogo = "https://example.test/logo.png",
        airLineUrl = "https://example.test/contact",
        airLineNum = airLineNum,
        upAirportCode = upAirportCode,
        upAirportName = upAirportName,
        airPlaneType = airPlaneType,
        airBoardingGate = airBoardingGate,
        airFlyStatus = airFlyStatus,
        airFlyDelayCause = airFlyDelayCause,
    )

    private fun repositoryReturning(vararg items: InstantScheduleDomesticArrivalDtoItem) =
        FlightsRepositoryImpl(FakeFlightsDataSource(Result.success(items.toList())))

    @Test
    fun `maps a dto onto the domain model`() = runTest {
        val arrival = repositoryReturning(dto()).fetchArrivals().getOrThrow().single()

        assertEquals("B78690", arrival.flightNumber)
        assertEquals("立榮航空", arrival.airlineName)
        assertEquals("澎湖", arrival.originName)
        assertEquals("MZG", arrival.originCode)
        assertEquals(LocalTime.of(9, 0), arrival.scheduled)
        assertEquals(LocalTime.of(8, 56), arrival.actual)
        assertEquals(FlightStatus.Arrived, arrival.status)
        assertEquals("17", arrival.gate)
        assertEquals("AT76", arrival.aircraftType)
    }

    @Test
    fun `keeps the IATA flight number without prefixing the ICAO code`() = runTest {
        // airLineNum is already a full IATA number (B7 + 8690) while airLineCode is
        // the unrelated ICAO code, so the two must never be concatenated.
        val arrival = repositoryReturning(dto(airLineNum = "AE332", airLineCode = "MDA"))
            .fetchArrivals().getOrThrow().single()

        assertEquals("AE332", arrival.flightNumber)
    }

    @Test
    fun `normalises status wording from both languages`() = runTest {
        suspend fun statusOf(raw: String?) =
            repositoryReturning(dto(airFlyStatus = raw)).fetchArrivals().getOrThrow().single().status

        assertEquals(FlightStatus.Arrived, statusOf("抵達"))
        assertEquals(FlightStatus.Arrived, statusOf("ARRIVED"))
        assertEquals(FlightStatus.Delayed, statusOf("延誤"))
        assertEquals(FlightStatus.Cancelled, statusOf("取消"))
        assertEquals(FlightStatus.ScheduleChanged, statusOf("時間更改"))
        assertEquals(FlightStatus.OnTime, statusOf("準時"))
    }

    @Test
    fun `keeps the original wording for statuses it does not recognise`() = runTest {
        val status = repositoryReturning(dto(airFlyStatus = "轉降松山"))
            .fetchArrivals().getOrThrow().single().status

        assertEquals(FlightStatus.Unknown("轉降松山"), status)
    }

    @Test
    fun `treats blank fields as absent rather than empty strings`() = runTest {
        val arrival = repositoryReturning(
            dto(realTime = "", airBoardingGate = "  ", airFlyDelayCause = ""),
        ).fetchArrivals().getOrThrow().single()

        assertNull(arrival.actual)
        assertNull(arrival.gate)
        assertNull(arrival.delayCause)
    }

    @Test
    fun `leaves unparseable times as null instead of failing`() = runTest {
        val arrival = repositoryReturning(dto(expectTime = "not a time"))
            .fetchArrivals().getOrThrow().single()

        assertNull(arrival.scheduled)
    }

    @Test
    fun `propagates a data source failure`() = runTest {
        val repository =
            FlightsRepositoryImpl(FakeFlightsDataSource(Result.failure(IOException("offline"))))

        assertTrue(repository.fetchArrivals().isFailure)
    }
}
