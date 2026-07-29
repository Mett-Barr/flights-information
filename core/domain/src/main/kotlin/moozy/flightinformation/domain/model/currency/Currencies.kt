package moozy.flightinformation.domain.model.currency

import moozy.flightinformation.domain.value.CurrencyCode

data class Currencies(
    val list: List<CurrencyRate>,
    val base: CurrencyCode
)
