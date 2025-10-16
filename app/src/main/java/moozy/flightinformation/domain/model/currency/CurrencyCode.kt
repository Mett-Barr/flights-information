package moozy.flightinformation.domain.model.currency

enum class CurrencyCode(
    override val code: String,
    override val fullName: String,
    override val symbol: String
) : CurrencyInfo {
    // 歐元區
    EUR("EUR", "Euro", "€"),

    // 美洲
    USD("USD", "US Dollar", "$"),
    CAD("CAD", "Canadian Dollar", "$"),
    BRL("BRL", "Brazilian Real", "R"),
    MXN("MXN", "Mexican Peso", "$"),

    // 亞洲/太平洋
    JPY("JPY", "Japanese Yen", "¥"),
    CNY("CNY", "Chinese Yuan", "¥"),
    HKD("HKD", "Hong Kong Dollar", "$"),
    INR("INR", "Indian Rupee", "₹"),
    IDR("IDR", "Indonesian Rupiah", "Rp"),
    KRW("KRW", "South Korean Won", "₩"),
    MYR("MYR", "Malaysian Ringgit", "RM"),
    PHP("PHP", "Philippine Peso", "₱"),
    SGD("SGD", "Singapore Dollar", "$"),
    THB("THB", "Thai Baht", "฿"),
    AUD("AUD", "Australian Dollar", "$"),
    NZD("NZD", "New Zealand Dollar", "$"),

    // 歐洲（非歐元區/其他）
    GBP("GBP", "British Pound Sterling", "£"),
    CHF("CHF", "Swiss Franc", "Fr"),
    SEK("SEK", "Swedish Krona", "kr"),
    NOK("NOK", "Norwegian Krone", "kr"),
    DKK("DKK", "Danish Krone", "kr"),
    CZK("CZK", "Czech Republic Koruna", "Kč"),
    HUF("HUF", "Hungarian Forint", "Ft"),
    PLN("PLN", "Polish Zloty", "zł"),
    RON("RON", "Romanian Leu", "L"),
    BGN("BGN", "Bulgarian Lev", "лв"),
    ISK("ISK", "Icelandic Króna", "kr"),
    HRK("HRK", "Croatian Kuna", "kn"),
    RUB("RUB", "Russian Ruble", "₽"),
    TRY("TRY", "Turkish Lira", "₺"),
    ILS("ILS", "Israeli New Sheqel", "₪"),

    // 非洲
    ZAR("ZAR", "South African Rand", "R");

    /**
     * 靜態方法 (Companion Object)
     * 用於通過貨幣代碼（例如 "USD"）快速查找對應的 Currency 列舉常數。
     */
    companion object {
        fun fromCode(code: String): CurrencyCode? {
            return entries.find { it.code.equals(code, ignoreCase = true) }
        }
    }
}