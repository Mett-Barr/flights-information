package moozy.flightinformation.presentation.mapper

import moozy.flightinformation.R
import moozy.flightinformation.domain.model.flights.FlightArrival
import moozy.flightinformation.domain.model.flights.FlightStatus
import moozy.flightinformation.presentation.state.flights.FlightStatusText
import moozy.flightinformation.presentation.state.flights.FlightStatusLevel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class FlightUiMapperTest {

    @Test
    fun `headline time uses actual time when available`() {
        val result = flightArrival(
            scheduled = LocalTime.of(9, 5),
            actual = LocalTime.of(10, 1),
        ).toUiModel()

        assertEquals("10:01", result.headlineTimeText)
    }

    @Test
    fun `headline time falls back to scheduled time`() {
        val result = flightArrival(scheduled = LocalTime.of(9, 5)).toUiModel()

        assertEquals("09:05", result.headlineTimeText)
    }

    @Test
    fun `headline time uses placeholder when both times are absent`() {
        val result = flightArrival(scheduled = null, actual = null).toUiModel()

        assertEquals("--:--", result.headlineTimeText)
    }

    @Test
    fun `scheduled time is preserved when actual time differs`() {
        val result = flightArrival(
            scheduled = LocalTime.of(9, 5),
            actual = LocalTime.of(10, 1),
        ).toUiModel()

        assertEquals("09:05", result.scheduledTimeText)
    }

    @Test
    fun `scheduled time uses placeholder when absent`() {
        val result = flightArrival(scheduled = null, actual = LocalTime.of(10, 1)).toUiModel()

        assertEquals("--:--", result.scheduledTimeText)
    }

    @Test
    fun `arrived badge maps to its resource`() =
        assertBadgeText(FlightStatus.Arrived, FlightStatusText.Resource(R.string.flight_status_arrived))

    @Test
    fun `departed badge maps to its resource`() =
        assertBadgeText(FlightStatus.Departed, FlightStatusText.Resource(R.string.flight_status_departed))

    @Test
    fun `schedule changed badge maps to its resource`() =
        assertBadgeText(FlightStatus.ScheduleChanged, FlightStatusText.Resource(R.string.flight_status_schedule_change))

    @Test
    fun `cancelled badge maps to its resource`() =
        assertBadgeText(FlightStatus.Cancelled, FlightStatusText.Resource(R.string.flight_status_cancelled))

    @Test
    fun `delayed badge maps to its resource`() =
        assertBadgeText(FlightStatus.Delayed, FlightStatusText.Resource(R.string.flight_status_delayed))

    @Test
    fun `on time badge maps to its resource`() =
        assertBadgeText(FlightStatus.OnTime, FlightStatusText.Resource(R.string.flight_status_on_time))

    @Test
    fun `unknown badge maps to its resource`() =
        assertBadgeText(FlightStatus.Unknown("Diverted"), FlightStatusText.Resource(R.string.flight_status_unknown))

    @Test
    fun `delayed status includes delay cause`() {
        val result = flightArrival(status = FlightStatus.Delayed, delayCause = "Weather").toUiModel()

        assertEquals(
            FlightStatusText.Resource(R.string.flight_status_delayed_with_cause, "Weather"),
            result.flightStatusText,
        )
    }

    @Test
    fun `delayed status without cause is displayed plainly`() {
        val result = flightArrival(status = FlightStatus.Delayed).toUiModel()

        assertEquals(FlightStatusText.Resource(R.string.flight_status_delayed), result.flightStatusText)
    }

    @Test
    fun `unknown status displays nonblank raw value`() {
        val result = flightArrival(status = FlightStatus.Unknown("Diverted")).toUiModel()

        assertEquals(FlightStatusText.Raw("Diverted"), result.flightStatusText)
    }

    @Test
    fun `unknown status displays fallback for blank raw value`() {
        val result = flightArrival(status = FlightStatus.Unknown("   ")).toUiModel()

        assertEquals(FlightStatusText.Resource(R.string.flight_status_unknown), result.flightStatusText)
    }

    @Test
    fun `other status lines match their badges`() {
        val statuses = listOf(
            FlightStatus.Arrived,
            FlightStatus.Departed,
            FlightStatus.ScheduleChanged,
            FlightStatus.Cancelled,
            FlightStatus.OnTime,
        )

        statuses.forEach { status ->
            val result = flightArrival(status = status).toUiModel()

            assertEquals(result.badgeText, result.flightStatusText)
        }
    }

    @Test
    fun `arrived status level is completed`() = assertStatusLevel(FlightStatus.Arrived, FlightStatusLevel.Completed)

    @Test
    fun `departed status level is completed`() = assertStatusLevel(FlightStatus.Departed, FlightStatusLevel.Completed)

    @Test
    fun `schedule changed status level needs attention`() =
        assertStatusLevel(FlightStatus.ScheduleChanged, FlightStatusLevel.Attention)

    @Test
    fun `cancelled status level is cancelled`() = assertStatusLevel(FlightStatus.Cancelled, FlightStatusLevel.Cancelled)

    @Test
    fun `cancelled status is marked disabled for presentation`() {
        assertEquals(true, flightArrival(status = FlightStatus.Cancelled).toUiModel().isCancelled)
        assertEquals(false, flightArrival(status = FlightStatus.OnTime).toUiModel().isCancelled)
    }

    @Test
    fun `delayed status level needs attention`() = assertStatusLevel(FlightStatus.Delayed, FlightStatusLevel.Attention)

    @Test
    fun `on time status level is on time`() = assertStatusLevel(FlightStatus.OnTime, FlightStatusLevel.OnTime)

    @Test
    fun `unknown status level is neutral`() = assertStatusLevel(FlightStatus.Unknown("Diverted"), FlightStatusLevel.Neutral)

    @Test
    fun `carrier line joins name and flight number`() {
        val result = flightArrival(airlineName = "Airline", flightNumber = "AB123").toUiModel()

        assertEquals("Airline · AB123", result.carrierLineText)
    }

    @Test
    fun `carrier line omits separator when only name is available`() {
        val result = flightArrival(airlineName = "Airline", flightNumber = " ").toUiModel()

        assertEquals("Airline", result.carrierLineText)
    }

    @Test
    fun `carrier line omits separator when only flight number is available`() {
        val result = flightArrival(airlineName = " ", flightNumber = "AB123").toUiModel()

        assertEquals("AB123", result.carrierLineText)
    }

    @Test
    fun `carrier line uses placeholder when both values are blank`() {
        val result = flightArrival(airlineName = " ", flightNumber = " ").toUiModel()

        assertEquals("--", result.carrierLineText)
    }

    @Test
    fun `departure joins origin name and code`() {
        val result = flightArrival(originName = "Penghu", originCode = "MZG").toUiModel()

        assertEquals("Penghu (MZG)", result.departureText)
    }

    @Test
    fun `departure displays only origin name when code is blank`() {
        val result = flightArrival(originName = "Penghu", originCode = " ").toUiModel()

        assertEquals("Penghu", result.departureText)
    }

    @Test
    fun `departure displays only origin code when name is blank`() {
        val result = flightArrival(originName = " ", originCode = "MZG").toUiModel()

        assertEquals("MZG", result.departureText)
    }

    @Test
    fun `departure uses placeholder when both values are blank`() {
        val result = flightArrival(originName = " ", originCode = " ").toUiModel()

        assertEquals("--", result.departureText)
    }

    @Test
    fun `gate is absent when source value is absent`() {
        val result = flightArrival(gate = null).toUiModel()

        assertEquals(null, result.gate)
    }

    @Test
    fun `aircraft uses placeholder when absent`() {
        val result = flightArrival(aircraftType = null).toUiModel()

        assertEquals("--", result.aircraftText)
    }

    @Test
    fun `list mapping preserves item order and count`() {
        val arrivals = listOf(
            flightArrival(airlineName = "First airline", flightNumber = "AB123"),
            flightArrival(airlineName = "Second airline", flightNumber = "CD456"),
        )

        val result = arrivals.toUiModels()

        assertEquals(2, result.size)
        assertEquals(
            listOf("First airline · AB123", "Second airline · CD456"),
            result.map { it.carrierLineText },
        )
    }

    private fun assertBadgeText(status: FlightStatus, expected: FlightStatusText) {
        assertEquals(expected, flightArrival(status = status).toUiModel().badgeText)
    }

    private fun assertStatusLevel(status: FlightStatus, expected: FlightStatusLevel) {
        assertEquals(expected, flightArrival(status = status).toUiModel().statusLevel)
    }

    private fun flightArrival(
        flightNumber: String = "AB123",
        airlineName: String = "Airline",
        originName: String = "Penghu",
        originCode: String = "MZG",
        scheduled: LocalTime? = LocalTime.of(9, 0),
        actual: LocalTime? = null,
        status: FlightStatus = FlightStatus.OnTime,
        gate: String? = "12",
        aircraftType: String? = "A320",
        delayCause: String? = null,
    ) = FlightArrival(
        flightNumber = flightNumber,
        airlineName = airlineName,
        airlineLogoUrl = "https://example.com/logo.png",
        originName = originName,
        originCode = originCode,
        scheduled = scheduled,
        actual = actual,
        status = status,
        gate = gate,
        aircraftType = aircraftType,
        delayCause = delayCause,
    )
}
