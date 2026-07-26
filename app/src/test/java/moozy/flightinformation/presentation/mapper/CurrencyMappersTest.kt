package moozy.flightinformation.presentation.mapper

import kotlinx.collections.immutable.persistentSetOf
import moozy.flightinformation.domain.value.CurrencyCode
import moozy.flightinformation.presentation.model.currency.CurrencyRow
import moozy.flightinformation.presentation.model.currency.CurrencyRowPlain
import moozy.flightinformation.presentation.model.currency.CurrencyRowWithConversion
import moozy.flightinformation.presentation.state.currency.CurrencyUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class CurrencyMappersTest {

    private fun row(code: String, rate: String) = CurrencyRowPlain(
        code = code,
        name = code,
        symbol = "",
        rate = BigDecimal(rate),
    )

    private fun content(
        rows: List<CurrencyRow> = listOf(row("EUR", "0.8"), row("USD", "1.0")),
        baseCode: CurrencyCode = CurrencyCode.USD,
    ) = CurrencyUiState.Content.Plain(
        rows = rows,
        selected = persistentSetOf(CurrencyCode.EUR),
        baseCode = baseCode,
    )

    @Test
    fun `returns plain rows for blank or invalid amounts`() {
        listOf(null, "", "   ", "not-a-number").forEach { amount ->
            val result = nextContent(content(), CurrencyCode.USD, amount)

            assertTrue(result is CurrencyUiState.Content.Plain)
        }
    }

    @Test
    fun `returns plain rows for a zero amount`() {
        val result = nextContent(content(), CurrencyCode.USD, "0.00")

        assertTrue(result is CurrencyUiState.Content.Plain)
    }

    @Test
    fun `converts with target rate divided by base rate`() {
        val result = nextContent(
            content(listOf(row("EUR", "0.8"), row("USD", "1.0"))),
            chosenBase = CurrencyCode.USD,
            amountText = "12.5",
        ) as CurrencyUiState.Content.WithConversion

        assertAmount("10.0", result.row("EUR").convertedAmount)
    }

    @Test
    fun `keeps the base row equal to the entered amount`() {
        val result = nextContent(content(), CurrencyCode.EUR, "12.5")
            as CurrencyUiState.Content.WithConversion

        assertAmount("12.5", result.row("EUR").convertedAmount)
    }

    @Test
    fun `returns plain rows when the chosen base rate is zero`() {
        val result = nextContent(
            content(listOf(row("USD", "0"), row("EUR", "0.8"))),
            CurrencyCode.USD,
            "10",
        )

        assertTrue(result is CurrencyUiState.Content.Plain)
    }

    @Test
    fun `returns plain rows when no usable base exists in the rows`() {
        val result = nextContent(
            content(listOf(row("XTS", "1.0")), CurrencyCode.USD),
            CurrencyCode.USD,
            "10",
        )

        assertTrue(result is CurrencyUiState.Content.Plain)
    }

    @Test
    fun `prefers a chosen base that exists in the rows`() {
        val result = nextContent(content(), CurrencyCode.EUR, "10")
            as CurrencyUiState.Content.WithConversion

        assertAmount("12.5", result.row("USD").convertedAmount)
    }

    @Test
    fun `falls back to the existing content base when chosen base is unavailable`() {
        val result = nextContent(
            content(listOf(row("EUR", "0.8"), row("USD", "1.0"))),
            CurrencyCode.JPY,
            "10",
        ) as CurrencyUiState.Content.WithConversion

        assertAmount("8.0", result.row("EUR").convertedAmount)
    }

    @Test
    fun `falls back to the first recognizable row when content base is unavailable`() {
        val result = nextContent(
            content(listOf(row("XTS", "2.0"), row("EUR", "0.8")), CurrencyCode.USD),
            chosenBase = null,
            amountText = "10",
        ) as CurrencyUiState.Content.WithConversion

        assertAmount("25.0", result.row("XTS").convertedAmount)
    }

    @Test
    fun `rounds converted values to max decimals using half up`() {
        val result = nextContent(
            content(listOf(row("USD", "1"), row("EUR", "200"))),
            CurrencyCode.EUR,
            "1",
            maxDecimals = 2,
            rounding = RoundingMode.HALF_UP,
        ) as CurrencyUiState.Content.WithConversion

        assertAmount("0.01", result.row("USD").convertedAmount)
    }

    private fun CurrencyUiState.Content.WithConversion.row(code: String): CurrencyRowWithConversion =
        rows.single { it.code == code } as CurrencyRowWithConversion

    private fun assertAmount(expected: String, actual: BigDecimal) {
        assertEquals(0, BigDecimal(expected).compareTo(actual))
    }
}
