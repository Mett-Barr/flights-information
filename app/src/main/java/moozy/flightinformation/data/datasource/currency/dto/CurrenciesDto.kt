package moozy.flightinformation.data.datasource.currency.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import moozy.flightinformation.domain.model.currency.Currencies
import moozy.flightinformation.domain.model.currency.CurrencyRate
import moozy.flightinformation.domain.value.CurrencyCode
import moozy.flightinformation.domain.value.MoneyCode
import java.math.BigDecimal

@Serializable
data class CurrenciesDto(
    val data: Map<String, JsonPrimitive> // e.g. {"EUR": 0.93, "JPY": 150.2, ...}
)

/**
 * 將 API DTO 轉成 Domain：
 * - 直接 BigDecimal（避免 double 精度問題）
 * - 代碼：可辨識(Known) 或 未知(Unknown)
 * - 需要呼叫端明確給 base（多數匯率 API 都有 base，若無就由呼叫端決定）
 */
fun CurrenciesDto.toCurrencies(base: CurrencyCode): Currencies {
    fun JsonPrimitive.asBigDecimalOrNull(): BigDecimal? {
        // 盡量寬鬆：數字或字串數字都接受；失敗回 null
        val raw = if (isString) this.content else this.toString()
        return runCatching { BigDecimal(raw) }.getOrNull()
    }

    val list: List<CurrencyRate> = data.mapNotNull { (codeStr, jp) ->
        val bd = jp.asBigDecimalOrNull() ?: return@mapNotNull null
        val code = CurrencyCode.entries.find { it.code == codeStr }
            ?.let { MoneyCode.Known(it) }
            ?: MoneyCode.Unknown(codeStr)
        CurrencyRate(code = code, rate = bd)
    }

    return Currencies(list = list, base = base)
}

val currenciesJson = """{
    "data": {
        "EUR": 1,
        "JPY": 176.3319718999,
        "KRW": 1649.8980758438,
        "SGD": 1.5030074335,
        "THB": 37.7004041343
    }
}"""