package moozy.flightinformation.domain.value

import moozy.flightinformation.domain.value.CurrencyCode

sealed interface MoneyCode {
    data class Known(val code: CurrencyCode) : MoneyCode
    data class Unknown(val raw: String) : MoneyCode
}