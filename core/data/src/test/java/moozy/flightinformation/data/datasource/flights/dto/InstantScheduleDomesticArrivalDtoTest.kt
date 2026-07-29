package moozy.flightinformation.data.datasource.flights.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class InstantScheduleDomesticArrivalDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val singleJson = """
        {
            "expectTime": "09:00",
            "realTime": "08:55",
            "airLineName": "立榮航空",
            "airLineCode": "UIA",
            "airLineLogo": "https://www.kia.gov.tw/images/ALL-square/B7.png",
            "airLineUrl": "https://www.kia.gov.tw/contact.html#立榮航空",
            "airLineNum": "B78690",
            "upAirportCode": "MZG",
            "upAirportName": "澎湖",
            "airPlaneType": "AT76",
            "airBoardingGate": "17",
            "airFlyStatus": "抵達",
            "airFlyDelayCause": ""
        }
    """.trimIndent()

    @Test
    fun `反序列化-單一物件`() {
        val item = json.decodeFromString<InstantScheduleDomesticArrivalDto>(singleJson)

        assertEquals("09:00", item.expectTime)
        assertEquals("08:55", item.realTime)
        assertEquals("立榮航空", item.airLineName)
        assertEquals("UIA", item.airLineCode)
        assertEquals("https://www.kia.gov.tw/images/ALL-square/B7.png", item.airLineLogo)
        assertEquals("https://www.kia.gov.tw/contact.html#立榮航空", item.airLineUrl)
        assertEquals("B78690", item.airLineNum)
        assertEquals("MZG", item.upAirportCode)
        assertEquals("澎湖", item.upAirportName)
        assertEquals("AT76", item.airPlaneType)
        assertEquals("17", item.airBoardingGate)
        assertEquals("抵達", item.airFlyStatus)
        assertEquals("", item.airFlyDelayCause)
    }
}
