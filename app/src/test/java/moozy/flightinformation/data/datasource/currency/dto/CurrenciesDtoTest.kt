package moozy.flightinformation.data.datasource.currency.dto

import kotlinx.serialization.json.Json
import org.junit.Test

class CurrenciesDtoTest {

    @Test
    fun testCurrenciesDtoSerialization() {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        val r = json.decodeFromString<CurrenciesDto>(currenciesJson)
        println(r)
    }
}