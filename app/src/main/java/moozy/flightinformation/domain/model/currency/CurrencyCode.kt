package moozy.flightinformation.domain.model.currency

enum class CurrencyCode(val code: String, val fullName: String) {
    // 歐元區
    EUR("EUR", "Euro"),
    // 美洲
    USD("USD", "US Dollar"),
    CAD("CAD", "Canadian Dollar"),
    BRL("BRL", "Brazilian Real"),
    MXN("MXN", "Mexican Peso"),
    // 亞洲/太平洋
    JPY("JPY", "Japanese Yen"),
    CNY("CNY", "Chinese Yuan"),
    HKD("HKD", "Hong Kong Dollar"),
    INR("INR", "Indian Rupee"),
    IDR("IDR", "Indonesian Rupiah"),
    KRW("KRW", "South Korean Won"),
    MYR("MYR", "Malaysian Ringgit"),
    PHP("PHP", "Philippine Peso"),
    SGD("SGD", "Singapore Dollar"),
    THB("THB", "Thai Baht"),
    AUD("AUD", "Australian Dollar"),
    NZD("NZD", "New Zealand Dollar"),
    // 歐洲（非歐元區/其他）
    GBP("GBP", "British Pound Sterling"),
    CHF("CHF", "Swiss Franc"),
    SEK("SEK", "Swedish Krona"),
    NOK("NOK", "Norwegian Krone"),
    DKK("DKK", "Danish Krone"),
    CZK("CZK", "Czech Republic Koruna"),
    HUF("HUF", "Hungarian Forint"),
    PLN("PLN", "Polish Zloty"),
    RON("RON", "Romanian Leu"),
    BGN("BGN", "Bulgarian Lev"),
    ISK("ISK", "Icelandic Króna"),
    HRK("HRK", "Croatian Kuna"), // 備註：克羅埃西亞已加入歐元區，但您的原始列表仍有此項
    RUB("RUB", "Russian Ruble"),
    TRY("TRY", "Turkish Lira"),
    ILS("ILS", "Israeli New Sheqel"), // 備註：以色列
    // 非洲
    ZAR("ZAR", "South African Rand");

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