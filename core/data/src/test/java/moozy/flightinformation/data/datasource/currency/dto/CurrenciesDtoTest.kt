package moozy.flightinformation.data.datasource.currency.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class CurrenciesDtoTest {

    @Test
    fun 反序列化會保留所有幣別與精確匯率() {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        val currenciesDto = json.decodeFromString<CurrenciesDto>(CURRENCIES_JSON)

        assertEquals(5, currenciesDto.data.size)
        assertEquals(0, BigDecimal("1").compareTo(BigDecimal(currenciesDto.data.getValue("EUR").content)))
        assertEquals(0, BigDecimal("176.3319718999").compareTo(BigDecimal(currenciesDto.data.getValue("JPY").content)))
    }

    private companion object {
        const val CURRENCIES_JSON = """{
            "data": {
                "EUR": 1,
                "JPY": 176.3319718999,
                "KRW": 1649.8980758438,
                "SGD": 1.5030074335,
                "THB": 37.7004041343
            }
        }"""
    }
}
