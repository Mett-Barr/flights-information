package moozy.flightinformation.presentation.mapper

import kotlinx.collections.immutable.PersistentSet
import moozy.flightinformation.data.datasource.currency.dto.CurrenciesDto
import moozy.flightinformation.domain.model.currency.Currencies
import moozy.flightinformation.domain.model.currency.CurrencyRate
import moozy.flightinformation.domain.value.CurrencyCode
import moozy.flightinformation.domain.value.CurrencyInfo
import moozy.flightinformation.domain.value.MoneyCode
import moozy.flightinformation.presentation.model.currency.CurrencyRow
import moozy.flightinformation.presentation.model.currency.CurrencyRowPlain
import moozy.flightinformation.presentation.model.currency.CurrencyRowWithConversion
import moozy.flightinformation.presentation.state.currency.CurrencyUiState
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale



/** 使用者的換算要求（輸入金額與其貨幣） */
data class Conversion(
    val amount: BigDecimal,
    val input: CurrencyCode
)


/**
 * 只輸出純資料 row：
 * - rate/amount 一律 BigDecimal
 * - 無格式化、無拼字串
 * - 公式：converted = baseAmount * rate[target] / rate[input]
 */
fun mapCurrenciesToRows(
    currencies: Currencies,
    conversion: Conversion? = null, // null → Plain；非 null → WithConversion
    infoIndex: Map<String, CurrencyInfo> = CurrencyCode.entries.associateBy { it.code },
    scale: Int = 18,
    rounding: RoundingMode = RoundingMode.HALF_UP
): List<CurrencyRow> {

    fun CurrencyRate.rawCode(): String = when (val c = code) {
        is MoneyCode.Known   -> c.code.code
        is MoneyCode.Unknown -> c.raw
    }

    // code -> rate
    val rateIndex: Map<String, BigDecimal> =
        currencies.list.associate { it.rawCode() to it.rate }

    val baseCodeStr: String? = conversion?.input?.code
    val denom: BigDecimal? = conversion?.let { rateIndex[it.input.code] } // rate[input]

    return currencies.list.map { r ->
        val codeStr = r.rawCode()
        val info = infoIndex[codeStr]
        val name = info?.fullName ?: codeStr
        val symbol = info?.symbol ?: ""
        val rate = r.rate

        if (conversion == null || denom == null || denom.compareTo(BigDecimal.ZERO) == 0) {
            CurrencyRowPlain(
                code = codeStr,
                name = name,
                symbol = symbol,
                rate = rate
            )
        } else {
            val isBaseRow = (codeStr == baseCodeStr)
            val converted: BigDecimal? = if (isBaseRow) {
                conversion.amount
            } else {
                val num = rateIndex[codeStr]
                if (num == null) null
                else conversion.amount.multiply(num).divide(denom, scale, rounding)
            }

            CurrencyRowWithConversion(
                code = codeStr,
                name = name,
                symbol = symbol,
                rate = rate,
                baseCode = baseCodeStr!!,
                baseAmount = conversion.amount,
                convertedAmount = converted!!
            )
        }
    }.sortedBy { it.code }
}

//fun buildContentState(
//    currencies: Currencies,
//    selected: PersistentSet<CurrencyCode>,
//    conversion: Conversion?,                // null → Plain；非 null → WithConversion
//    isRefreshing: Boolean = false,
//    locale: Locale = Locale.US,
//    infoIndex: Map<String, CurrencyInfo> = CurrencyCode.entries.associateBy { it.code }
//): CurrencyUiState.Content {
//    val rows = mapCurrenciesToRows(
//        currencies = currencies,
//        conversion = conversion,
//        locale = locale,
//        infoIndex = infoIndex
//    )
//    return if (conversion == null) {
//        CurrencyUiState.Content.Plain(
//            rows = rows,
//            selected = selected,
//            isRefreshing = isRefreshing
//        )
//    } else {
//        CurrencyUiState.Content.WithConversion(
//            rows = rows,
//            baseAmount = conversion.amount,
//            baseCode = conversion.input,
//            selected = selected,
//            isRefreshing = isRefreshing
//        )
//    }
//}