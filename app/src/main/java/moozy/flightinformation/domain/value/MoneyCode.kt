package moozy.flightinformation.domain.value

import moozy.flightinformation.domain.value.CurrencyCode

sealed interface MoneyCode {
    data class Known(val code: CurrencyCode) : MoneyCode {
        override val codeString: String get() = code.code
    }
    data class Unknown(val raw: String) : MoneyCode {
        override val codeString: String get() = raw
    }

    val codeString: String
}