package moozy.flightinformation.domain.model.currency

import java.math.BigDecimal


data class Currency(val code: String, val rate: BigDecimal)

data class Currencies(val list: List<Currency>)

data class CurrencyMeta(
    val code: String,
    val name: String?,
    val symbol: String?,
    val decimalDigits: Int?
)