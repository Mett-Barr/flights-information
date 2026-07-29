package moozy.flightinformation.presentation.mapper

import moozy.flightinformation.domain.model.currency.Currencies
import moozy.flightinformation.domain.model.currency.CurrencyRate
import moozy.flightinformation.domain.value.CurrencyCode
import moozy.flightinformation.domain.value.CurrencyInfo
import moozy.flightinformation.domain.value.MoneyCode
import moozy.flightinformation.presentation.model.currency.CurrencyRow
import moozy.flightinformation.presentation.model.currency.CurrencyRowPlain
import moozy.flightinformation.presentation.model.currency.CurrencyRowWithConversion
import moozy.flightinformation.presentation.state.currency.CurrencyUiState
import java.math.BigDecimal
import java.math.RoundingMode


/** 使用者的換算要求（輸入金額與其貨幣） */
data class Conversion(
    val amount: BigDecimal,
    val input: CurrencyCode
)

/**
 * 只輸出純資料 row：
 * - rate/amount 一律 BigDecimal
 * - 無格式化、無拼字串
 * - 公式：converted = baseAmount * rate[target] / rate[input]
 */
fun mapCurrenciesToRows(
    currencies: Currencies,
    conversion: Conversion? = null, // null → Plain；非 null → WithConversion
    infoIndex: Map<String, CurrencyInfo> = CurrencyCode.entries.associateBy { it.code },
    scale: Int = 18,
    rounding: RoundingMode = RoundingMode.HALF_UP
): List<CurrencyRow> {

    fun CurrencyRate.rawCode(): String = when (val c = code) {
        is MoneyCode.Known -> c.code.code
        is MoneyCode.Unknown -> c.raw
    }

    // code -> rate
    val rateIndex: Map<String, BigDecimal> =
        currencies.list.associate { it.rawCode() to it.rate }

    val baseCodeStr: String? = conversion?.input?.code
    val denom: BigDecimal? = conversion?.let { rateIndex[it.input.code] } // rate[input]

    return currencies.list.map { r ->
        val codeStr = r.rawCode()
        val info = infoIndex[codeStr]
        val name = info?.fullName ?: codeStr
        val symbol = info?.symbol ?: ""
        val rate = r.rate

        if (conversion == null || denom == null || denom.compareTo(BigDecimal.ZERO) == 0) {
            CurrencyRowPlain(
                code = codeStr,
                name = name,
                symbol = symbol,
                rate = rate
            )
        } else {
            val isBaseRow = (codeStr == baseCodeStr)
            val converted: BigDecimal? = if (isBaseRow) {
                conversion.amount
            } else {
                val num = rateIndex[codeStr]
                if (num == null) null
                else conversion.amount.multiply(num).divide(denom, scale, rounding)
            }

            CurrencyRowWithConversion(
                code = codeStr,
                name = name,
                symbol = symbol,
                rate = rate,
                baseCode = baseCodeStr!!,
                baseAmount = conversion.amount,
                convertedAmount = converted!!
            )
        }
    }.sortedBy { it.code }
}


// 複製貼上直接測試型最簡單實現（單函數、零外部狀態）
fun nextContent(
    content: CurrencyUiState.Content,   // ① 當前 Content（必填）
    chosenBase: CurrencyCode?,          // ② 當前選中的 Currency（可為 null；需存在於 rows）
    amountText: String?,                // ③ 當前輸入金額（String；可為 null/空/非法）
    scale: Int = 18,
    rounding: RoundingMode = RoundingMode.HALF_UP,
    maxDecimals: Int = 3
): CurrencyUiState.Content {
    val rows = content.rows
    val isRefreshing = content.isRefreshing
    val selectedSet = content.selected

    // 小工具：把小數位「限制為最多 N 位」且不補 0
    fun BigDecimal.atMostScale(max: Int, rm: RoundingMode): BigDecimal =
        if (this.scale() <= max) this.stripTrailingZeros()
        else this.setScale(max, rm).stripTrailingZeros()

    val rateIndex: Map<String, BigDecimal> = rows.associate { it.code to it.rate }

    val baseFromChosen: CurrencyCode? = chosenBase?.takeIf { rateIndex.containsKey(it.code) }
    val baseFromOld: CurrencyCode? = content.baseCode
        .takeIf { bc -> rateIndex.containsKey(bc.code) }
    val baseFromRows: CurrencyCode? = rows.asSequence()
        .mapNotNull { r -> CurrencyCode.entries.firstOrNull { it.code == r.code } }
        .firstOrNull()
    val baseCode: CurrencyCode? = baseFromChosen ?: baseFromOld ?: baseFromRows

    val baseAmount: BigDecimal? = amountText?.trim()?.takeIf { it.isNotEmpty() }
        ?.let { runCatching { BigDecimal(it) }.getOrNull() }

    if (baseAmount == null || baseAmount.compareTo(BigDecimal.ZERO) == 0) {
        val plainRows = rows.map { r ->
            CurrencyRowPlain(code = r.code, name = r.name, symbol = r.symbol, rate = r.rate)
        }
        return CurrencyUiState.Content.Plain(
            rows = plainRows,
            selected = selectedSet,
            isRefreshing = isRefreshing,
            baseCode = content.baseCode,
            selectedBaseCurrency = content.selectedBaseCurrency
        )
    }

    val denom: BigDecimal? = baseCode?.let { rateIndex[it.code] }

    // baseAmount 在上面的 early return 已排除 null，這裡只需檢查 base 幣別與其匯率。
    if (baseCode == null || denom == null || denom.signum() == 0) {
        val plainRows: List<CurrencyRowPlain> = rows.map { r ->
            CurrencyRowPlain(code = r.code, name = r.name, symbol = r.symbol, rate = r.rate)
        }
        return CurrencyUiState.Content.Plain(
            rows = plainRows,
            selected = selectedSet,
            isRefreshing = isRefreshing,
            baseCode = content.baseCode,
            selectedBaseCurrency = content.selectedBaseCurrency
        )
    }

    val withRows: List<CurrencyRowWithConversion> = rows.map { r ->
        val raw = if (r.code == baseCode.code) baseAmount
        else baseAmount.multiply(rateIndex[r.code]!!).divide(denom, scale, rounding)
        val converted = raw.atMostScale(maxDecimals, rounding)   // ★ 關鍵：最多三位小數
        CurrencyRowWithConversion(
            code = r.code,
            name = r.name,
            symbol = r.symbol,
            rate = r.rate,
            convertedAmount = converted,
            baseAmount = baseAmount,
            // 標示的基準幣別必須就是上面當分母的那一個，否則畫面會用 A 的匯率掛 B 的名字。
            baseCode = baseCode.code
        )
    }

    return CurrencyUiState.Content.WithConversion(
        rows = withRows,
        baseAmount = baseAmount,
        baseCode = baseCode,
        selected = selectedSet,
        isRefreshing = isRefreshing,
        selectedBaseCurrency = content.selectedBaseCurrency,
    )
}
