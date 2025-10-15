package moozy.flightinformation.domain.model.currency

import java.math.BigDecimal

// 這邊選擇使用 String 而不是 CurrencyCode 是因為不確定是否有未枚舉新值
data class Currency(val code: String, val rate: BigDecimal)

data class Currencies(val list: List<Currency>)