package moozy.flightinformation.domain.model.currency

interface CurrencyInfo {
    val code: String
    val fullName: String
    val symbol: String
}

data class CurrencyInfoData(
    override val code: String,
    override val fullName: String,
    override val symbol: String
): CurrencyInfo