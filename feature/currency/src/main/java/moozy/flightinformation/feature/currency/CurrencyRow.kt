package moozy.flightinformation.feature.currency

import java.math.BigDecimal
import androidx.compose.runtime.Immutable

// ui/model/CurrencyRow.kt
@Immutable
sealed interface CurrencyRow {
    val code: String           // "EUR" or raw when unknown
    val name: String           // "Euro" or fallback raw
    val symbol: String         // "€" or ""
    val rate: BigDecimal       // formatted (e.g., "0.930000")
}

/** 無轉換（預設僅顯示匯率） */
@Immutable
data class CurrencyRowPlain(
    override val code: String,
    override val name: String,
    override val symbol: String,
    override val rate: BigDecimal
) : CurrencyRow

/** 有轉換（顯示「1 {base} = X {code}」） */
@Immutable
data class CurrencyRowWithConversion(
    override val code: String,
    override val name: String,
    override val symbol: String,
    override val rate: BigDecimal,
    val convertedAmount: BigDecimal, // 例："1 USD = 0.93 EUR",
    val baseAmount: BigDecimal,
    val baseCode: String,
) : CurrencyRow
