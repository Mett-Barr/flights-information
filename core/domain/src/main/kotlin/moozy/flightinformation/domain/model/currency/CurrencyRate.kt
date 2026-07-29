package moozy.flightinformation.domain.model.currency

import moozy.flightinformation.domain.value.MoneyCode
import java.math.BigDecimal


data class CurrencyRate(
    val code: MoneyCode,
    val rate: BigDecimal
)
