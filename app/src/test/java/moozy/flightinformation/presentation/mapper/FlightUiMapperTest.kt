package moozy.flightinformation.presentation.mapper

import moozy.flightinformation.domain.model.flights.FlightArrival
import moozy.flightinformation.domain.model.flights.FlightStatus
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
    fun `expected label always uses scheduled time`() {
        val result = flightArrival(
            scheduled = LocalTime.of(9, 5),
            actual = LocalTime.of(10, 1),
        ).toUiModel()

        assertEquals("Expected 09:05", result.expectedLabelText)
    }

    @Test
    fun `expected label uses placeholder when scheduled time is absent`() {
        val result = flightArrival(scheduled = null, actual = LocalTime.of(10, 1)).toUiModel()

        assertEquals("Expected --:--", result.expectedLabelText)
    }

    @Test
    fun `arrived badge text is displayed`() = assertBadgeText(FlightStatus.Arrived, "Arrived")

    @Test
    fun `departed badge text is displayed`() = assertBadgeText(FlightStatus.Departed, "Departed")

    @Test
    fun `schedule changed badge text is displayed`() =
        assertBadgeText(FlightStatus.ScheduleChanged, "Schedule change")

    @Test
    fun `cancelled badge text is displayed`() = assertBadgeText(FlightStatus.Cancelled, "Cancelled")

    @Test
    fun `delayed badge text is displayed`() = assertBadgeText(FlightStatus.Delayed, "Delayed")

    @Test
    fun `on time badge text is displayed`() = assertBadgeText(FlightStatus.OnTime, "On time")

    @Test
    fun `unknown badge text is displayed`() = assertBadgeText(FlightStatus.Unknown("Diverted"), "Unknown")

    @Test
    fun `delayed status includes delay cause`() {
        val result = flightArrival(status = FlightStatus.Delayed, delayCause = "Weather").toUiModel()

        assertEquals("Delayed · Weather", result.flightStatusText)
    }

    @Test
    fun `delayed status without cause is displayed plainly`() {
        val result = flightArrival(status = FlightStatus.Delayed).toUiModel()

        assertEquals("Delayed", result.flightStatusText)
    }

    @Test
    fun `unknown status displays nonblank raw value`() {
        val result = flightArrival(status = FlightStatus.Unknown("Diverted")).toUiModel()

        assertEquals("Diverted", result.flightStatusText)
    }

    @Test
    fun `unknown status displays fallback for blank raw value`() {
        val result = flightArrival(status = FlightStatus.Unknown("   ")).toUiModel()

        assertEquals("Status unknown", result.flightStatusText)
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
    fun `arrived status key is displayed`() = assertStatusKey(FlightStatus.Arrived, "ARRIVED")

    @Test
    fun `departed status key is displayed`() = assertStatusKey(FlightStatus.Departed, "DEPARTED")

    @Test
    fun `schedule changed status key is displayed`() =
        assertStatusKey(FlightStatus.ScheduleChanged, "SCHEDULE_CHANGE")

    @Test
    fun `cancelled status key is displayed`() = assertStatusKey(FlightStatus.Cancelled, "CANCELLED")

    @Test
    fun `delayed status key is displayed`() = assertStatusKey(FlightStatus.Delayed, "DELAYED")

    @Test
    fun `on time status key is displayed`() = assertStatusKey(FlightStatus.OnTime, "ON_TIME")

    @Test
    fun `unknown status key is displayed`() = assertStatusKey(FlightStatus.Unknown("Diverted"), "UNKNOWN")

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
    fun `gate uses placeholder when absent`() {
        val result = flightArrival(gate = null).toUiModel()

        assertEquals("Gate --", result.gateText)
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

    private fun assertBadgeText(status: FlightStatus, expected: String) {
        assertEquals(expected, flightArrival(status = status).toUiModel().badgeText)
    }

    private fun assertStatusKey(status: FlightStatus, expected: String) {
        assertEquals(expected, flightArrival(status = status).toUiModel().statusKey)
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
