package moozy.flightinformation.feature.calculator

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.roundToLong

private const val MULTIPLY_SYMBOL = "×"
private const val DIVIDE_SYMBOL = "÷"
private val BINARY_OPERATOR_TOKENS = setOf("+", "-", MULTIPLY_SYMBOL, DIVIDE_SYMBOL)

/** 沒有指定幣別時的 fallback 小數位數上限。 */
private const val DEFAULT_MAX_DECIMALS = 2
private const val DECIMAL_RADIX = 10L
private const val WHOLE_NUMBER_DIVISOR = 1.0

/**
 * 可變，但每個欄位都是 Compose 的 snapshot 狀態，變動會被正確通知——這正是
 * @Stable 的定義。沒有這個標註，Compose 只能保守假設它隨時可能改變，
 * 於是接收它的 composable 永遠無法被 skip。
 */
@Stable
class Calculator(
    initialValue: Double = 0.0,
    /** 幣別的小數位數（例如 USD=2, JPY=0, BHD=3），決定結果最多保留幾位。 */
    fractionDigits: Int = DEFAULT_MAX_DECIMALS,
) {
    /**
     * Mutable so that a currency change can update precision without
     * recreating (and thus resetting) the calculator instance.
     */
    var fractionDigits by mutableIntStateOf(fractionDigits)
        private set

    /**
     * Update the currency precision and re-format the current stable result.
     * Call this when the user switches currency mid-expression.
     */
    fun updateFractionDigits(newDigits: Int) {
        if (newDigits == fractionDigits) return
        fractionDigits = newDigits
        commitStableIfPossible() // re-format with new precision
    }
    val infix = mutableStateListOf<String>()

    val infixString by derivedStateOf {
        infix.joinToString("")
    }

    val equal: Double? by derivedStateOf {
        runCatching { cal(infix, fractionDigits).toDouble() }.getOrNull()
    }

    val currentState: MutableState<CurrentState> = mutableStateOf(CurrentState.NULL)
    val parenthesesState = mutableIntStateOf(0)
    val lastStableAmountText = mutableStateOf("0")

    // 每顆按鍵目前是否可按，供 UI 觀察決定 enabled 狀態（Compose 可觀察 map）
    val keyEnabled = mutableStateMapOf<KeyLabel, Boolean>()

    init {
        seedInitialValue(initialValue)
        refreshKeyEnabled()
    }

    /* ============================================================
     *  Public API：供 UI 呼叫
     * ============================================================ */

    fun onKey(key: KeyLabel) {
        if (!canInput(key)) return
        when (key) {
            is NumKey -> numInput(key)
            is OpKey -> operatorInput(key)
            is SysKey -> sysInput(key)
        }
    }

    fun canInput(key: KeyLabel): Boolean = keyEnabled[key] == true

    /* ============================================================
     *  狀態推導：token 與 state 的唯一對應來源
     * ============================================================ */
    private fun stateFromToken(token: String?): CurrentState =
        when {
            token == null -> CurrentState.NULL
            token == "(" -> CurrentState.LEFT
            token == ")" -> CurrentState.RIGHT
            token.endsWith(".") -> CurrentState.DOUBLE
            token == "0" || token == "-0" -> CurrentState.ZERO
            token.toIntOrNull() != null -> CurrentState.INTEGER
            token.toDoubleOrNull() != null -> CurrentState.DOUBLE
            else -> CurrentState.OPERATOR
        }

    private fun stateAfterRemovingTrailingBinaryOperators(): CurrentState {
        val lastNonOperatorIndex = infix.indexOfLast { it !in BINARY_OPERATOR_TOKENS }
        return stateFromToken(infix.getOrNull(lastNonOperatorIndex))
    }

    private fun reconcile() {
        currentState.value = stateFromToken(infix.lastOrNull())
        commitStableIfPossible()
        refreshKeyEnabled()
    }

    private fun seedInitialValue(value: Double) {
        infix.clear()
        val text =
            if (value == kotlin.math.floor(value) && !value.isNaN()) {
                value.toLong().toString()
            } else {
                value.toString()
            }
        infix.clear()
        infix.add(text)
        currentState.value = stateFromToken(text)
        lastStableAmountText.value = text
    }

    /**
     * Seeds the calculator with a human-readable display string (e.g. "500" or "500.50").
     * Unlike [seedInitialValue] which takes minor units as a Long, this method takes the
     * display-unit string directly, preserving decimal places as the user sees them.
     */
    fun seedFromDisplayString(value: String) {
        val str = value.trim().ifEmpty { "0" }
        infix.clear()
        infix.add(str)
        currentState.value = stateFromToken(str)
        lastStableAmountText.value = str
        parenthesesState.intValue = 0
        refreshKeyEnabled()
    }

    /* ============================================================
     *  Backspace（刪除最後一字元 + reconcile）
     * ============================================================ */
    fun btBackSpace() {
        if (infix.isEmpty()) return

        try {
            val lastToken = infix.last()

            // --- 維護括號計數 ---
            if (lastToken == "(") parenthesesState.intValue -= 1
            if (lastToken == ")") parenthesesState.intValue += 1

            when (currentState.value) {
                CurrentState.NULL -> Unit

                CurrentState.ZERO,
                CurrentState.OPERATOR,
                CurrentState.LEFT,
                CurrentState.RIGHT,
                -> {
                    infix.removeAt(infix.lastIndex)
                }

                CurrentState.INTEGER,
                CurrentState.DOUBLE,
                -> {
                    val token = infix.removeAt(infix.lastIndex)
                    val chopped = token.dropLast(1)
                    if (chopped.isNotEmpty()) {
                        infix.add(chopped)
                    }
                }
            }
        } finally {
            // 重新推導當前 state（唯一出口）
            currentState.value = stateFromToken(infix.lastOrNull())

            // 修正：刪到空時歸零，否則 lastStableAmountText 會殘留舊值
            if (infix.isEmpty()) {
                parenthesesState.intValue = 0
                lastStableAmountText.value = "0"
            } else {
                commitStableIfPossible()
            }
            refreshKeyEnabled()
        }
    }

    fun btAC() {
        infix.clear()
        currentState.value = CurrentState.NULL
        parenthesesState.intValue = 0
        lastStableAmountText.value = "0"
        refreshKeyEnabled()
    }

    /* ============================================================
     *  Key Input 處理
     * ============================================================ */

    private fun sysInput(key: SysKey) {
        when (key) {
            SysKey.Backspace -> btBackSpace()
            SysKey.AC -> btAC()
        }
        // btBackSpace / btAC 內部已自行呼叫 reconcile / refresh
    }

    fun numInput(key: NumKey) {
        try {
            when (key) {
                NumKey.Dot -> inputDot()
                NumKey.DoubleZero -> inputDigits("00")
                NumKey.Zero -> inputDigits("0")
                NumKey.One -> inputDigits("1")
                NumKey.Two -> inputDigits("2")
                NumKey.Three -> inputDigits("3")
                NumKey.Four -> inputDigits("4")
                NumKey.Five -> inputDigits("5")
                NumKey.Six -> inputDigits("6")
                NumKey.Seven -> inputDigits("7")
                NumKey.Eight -> inputDigits("8")
                NumKey.Nine -> inputDigits("9")
            }
        } finally {
            reconcile()
        }
    }

    fun operatorInput(key: OpKey) {
        try {
            when (key) {
                OpKey.LParen -> {
                    infix.add("(")
                    parenthesesState.intValue += 1
                }

                OpKey.RParen -> {
                    // 是否可加右括號已由 canInput 把關
                    infix.add(")")
                    parenthesesState.intValue -= 1
                }

                OpKey.Plus, OpKey.Minus, OpKey.Multiply, OpKey.Divide -> {
                    val op = key.label

                    // AOSP 規則：乘除後的負號是下一個因數的一元負號，不能取代。
                    if (currentState.value == CurrentState.OPERATOR) {
                        val previous = infix.last()
                        if (key == OpKey.Minus && previous in setOf(MULTIPLY_SYMBOL, DIVIDE_SYMBOL)) {
                            infix.add(op)
                        } else if (!(
                            key == OpKey.Minus &&
                            previous == OpKey.Minus.label &&
                            infix.getOrNull(infix.lastIndex - 1) in setOf(MULTIPLY_SYMBOL, DIVIDE_SYMBOL)
                        )) {
                            while (infix.lastOrNull() in BINARY_OPERATOR_TOKENS) {
                                infix.removeAt(infix.lastIndex)
                            }
                            infix.add(op)
                        }
                    } else {
                        infix.add(op)
                    }
                }
            }
        } finally {
            reconcile()
        }
    }

    /* ============================================================
     *  數字 / 字元輸入處理
     * ============================================================ */

    private fun inputDot() {
        when (currentState.value) {
            CurrentState.NULL,
            CurrentState.OPERATOR,
            CurrentState.LEFT,
            -> {
                // 從頭起一個 "0." token
                infix.add("0.")
            }

            CurrentState.ZERO,
            CurrentState.INTEGER,
            -> {
                // 在目前數字 token 後面補上 "."
                val t = infix.lastOrNull() ?: return
                if (!t.contains(".")) {
                    infix[infix.lastIndex] = "$t."
                }
            }

            CurrentState.DOUBLE -> {
                // 已經有小數點了，再按 "." 不做事
            }

            // 右括號後不允許直接接小數點（已由 canInput 擋下）
            CurrentState.RIGHT -> Unit
        }
    }

    private fun inputDigits(digits: String) {
        when (currentState.value) {
            CurrentState.NULL,
            CurrentState.OPERATOR,
            CurrentState.LEFT,
            -> {
                // 起一個新的 number token
                infix.add(if (digits == "00") "0" else digits) // 開頭不允許 "00"
            }

            CurrentState.ZERO -> {
                val t = infix.lastOrNull() ?: return

                // 處理前導零：0 + 0 => 0；0 + 5 => 5；-0 + 5 => -5
                if (t == "0") {
                    if (digits == "0" || digits == "00") return
                    infix[infix.lastIndex] = digits
                } else if (t == "-0") {
                    if (digits == "0" || digits == "00") return
                    infix[infix.lastIndex] = "-$digits"
                } else {
                    // 其他情況直接 append
                    infix[infix.lastIndex] = t + digits
                }
            }

            CurrentState.INTEGER,
            CurrentState.DOUBLE,
            -> {
                val t = infix.lastOrNull() ?: return
                infix[infix.lastIndex] = t + digits
            }

            CurrentState.RIGHT -> {
                // 右括號後不允許直接接數字（已由 canInput 擋下，這裡留空）
            }
        }
    }

    /* ============================================================
     *  Key Availability（決定每顆鍵在 UI 的 enabled 狀態）
     * ============================================================ */

    private fun refreshKeyEnabled() {
        // 預先算好每顆 key 是否可按並快取在外層 UI，UI 直接讀 canInput(key) 即可
        val allKeys: List<KeyLabel> =
            NumKey.entries + OpKey.entries + SysKey.entries

        allKeys.forEach { key ->
            val enabled = canInputInternal(key)
            if (keyEnabled[key] != enabled) keyEnabled[key] = enabled
        }
    }

    private fun canInputInternal(key: KeyLabel): Boolean =
        when (key) {
            is NumKey -> canInputNum(key)
            is OpKey -> canInputOp(key)
            is SysKey -> canInputSys(key)
        }

    private fun canInputSys(key: SysKey): Boolean =
        when (key) {
            // AC 永遠可按；backspace 依 infix 是否非空決定
            SysKey.AC -> true
            SysKey.Backspace -> infix.isNotEmpty()
        }

    private fun canInputNum(key: NumKey): Boolean {
        return when (key) {
            NumKey.Dot ->
                when (currentState.value) {
                    CurrentState.NULL, CurrentState.OPERATOR, CurrentState.LEFT -> true
                    CurrentState.ZERO, CurrentState.INTEGER -> {
                        val t = infix.lastOrNull() ?: return false
                        !t.contains(".")
                    }
                    CurrentState.DOUBLE -> {
                        val t = infix.lastOrNull() ?: return false
                        !t.contains(".") // DOUBLE 通常已含小數點，故多半回傳 false
                    }

                    // 右括號後不允許直接接小數點
                    CurrentState.RIGHT -> false
                }

            NumKey.DoubleZero ->
                when (currentState.value) {
                    CurrentState.INTEGER, CurrentState.DOUBLE -> true
                    CurrentState.ZERO -> {
                        val t = infix.lastOrNull() ?: return false
                        t != "0" && t != "-0"
                    }

                    // 開頭 / 運算子後 / 左括號後都不該起一個 "00"；右括號後不接數字
                    CurrentState.NULL,
                    CurrentState.OPERATOR,
                    CurrentState.LEFT,
                    CurrentState.RIGHT,
                    -> false
                }

            // 0 與 1–9
            NumKey.Zero,
            NumKey.One,
            NumKey.Two,
            NumKey.Three,
            NumKey.Four,
            NumKey.Five,
            NumKey.Six,
            NumKey.Seven,
            NumKey.Eight,
            NumKey.Nine,
            ->
                when (currentState.value) {
                    CurrentState.NULL, CurrentState.OPERATOR, CurrentState.LEFT,
                    CurrentState.ZERO, CurrentState.INTEGER, CurrentState.DOUBLE,
                    -> true

                    // 右括號後不允許直接接數字
                    CurrentState.RIGHT -> false
                }
        }
    }

    private fun canInputOp(key: OpKey): Boolean =
        when (key) {
            OpKey.LParen ->
                currentState.value in
                    setOf(
                        CurrentState.NULL,
                        CurrentState.OPERATOR,
                        CurrentState.LEFT,
                    )

            OpKey.RParen ->
                parenthesesState.intValue > 0 && currentState.value in
                    setOf(
                        CurrentState.ZERO,
                        CurrentState.INTEGER,
                        CurrentState.DOUBLE,
                        CurrentState.RIGHT,
                    )

            OpKey.Minus ->
                currentState.value in
                    setOf(
                        CurrentState.NULL,
                        CurrentState.LEFT,
                        CurrentState.ZERO,
                        CurrentState.INTEGER,
                        CurrentState.DOUBLE,
                        CurrentState.RIGHT,
                        CurrentState.OPERATOR, // allow replacing previous operator
                    )

            OpKey.Plus, OpKey.Multiply, OpKey.Divide ->
                stateAfterRemovingTrailingBinaryOperators() in
                    setOf(
                        CurrentState.ZERO,
                        CurrentState.INTEGER,
                        CurrentState.DOUBLE,
                        CurrentState.RIGHT,
                    )
        }

    /* ============================================================
     *  stable commit / expression closed 判斷
     * ============================================================ */

    private fun isExpressionClosed(): Boolean {
        if (infix.isEmpty()) return false
        if (parenthesesState.intValue != 0) return false
        return currentState.value in
            setOf(
                CurrentState.INTEGER,
                CurrentState.DOUBLE,
                CurrentState.ZERO,
                CurrentState.RIGHT,
            )
    }

    private fun commitStableIfPossible() {
        if (!isExpressionClosed()) return
        val computed = runCatching { cal(infix, fractionDigits) }.getOrNull() ?: return
        if (computed.isBlank() || computed == "no value") return
        lastStableAmountText.value = computed
    }

    /* ============================================================
     *  cal / postfix 計算引擎
     * ============================================================ */

    companion object {
        private const val UNARY_MINUS = "u-"

        fun cal(
            infix: List<String>,
            maxDecimals: Int = DEFAULT_MAX_DECIMALS,
        ): String {
            if (infix.isEmpty()) return "0"
            if (infix[0] == "") return "0"

            return try {
                val postfix = infixToPostfix(infix)
                val stack = ArrayDeque<Double>()
                calculate(postfix, stack)

                val result = stack.lastOrNull() ?: return "0"
                formatResult(result, maxDecimals) ?: "no value"
            } catch (_: Exception) {
                "no value"
            }
        }

        /**
         * 格式化計算結果：整數去小數點，非整數最多保留 [maxDecimals] 位並去尾部零。
         *
         * 例（maxDecimals=2）：`3.3333…` → `"3.33"`, `2.5` → `"2.5"`, `10.0` → `"10"`
         */
        private fun formatResult(value: Double, maxDecimals: Int): String? {
            if (!value.isFinite()) return null
            if (value % WHOLE_NUMBER_DIVISOR == 0.0) {
                if (value < Long.MIN_VALUE.toDouble() || value >= Long.MAX_VALUE.toDouble()) return null
                return value.toLong().toString()
            }
            // 用乘→四捨五入 截斷到 maxDecimals 位，再重建字串（避免 BigDecimal/科學記號）
            var factor = 1L
            repeat(maxDecimals) { factor *= DECIMAL_RADIX }
            val scaledValue = value * factor
            if (scaledValue < Long.MIN_VALUE.toDouble() || scaledValue >= Long.MAX_VALUE.toDouble()) return null
            val roundedLong = scaledValue.roundToLong()
            val sign = if (roundedLong < 0 && roundedLong / factor == 0L) "-" else ""
            val intPart = roundedLong / factor
            val fracPart = kotlin.math.abs(roundedLong % factor)
            val text = "$sign$intPart.${fracPart.toString().padStart(maxDecimals, '0')}"
            return text.trimEnd('0').trimEnd('.')
        }

        private fun calculate(
            postfix: List<String>,
            stack: ArrayDeque<Double>,
        ) {
            for (token in postfix) {
                assort(token, stack)
            }
        }

        private fun assort(
            token: String,
            stack: ArrayDeque<Double>,
        ) {
            val number = token.toDoubleOrNull()
            if (number != null) {
                stack.addLast(number)
                return
            }

            if (token == UNARY_MINUS) {
                if (stack.isEmpty()) errorToken(token)
                stack.addLast(-stack.removeLast())
                return
            }

            if (stack.size < 2) errorToken(token)

            val right = stack.removeLast()
            val left = stack.removeLast()

            val out =
                when (token) {
                    "+" -> left + right
                    "-" -> left - right
                    MULTIPLY_SYMBOL -> left * right
                    DIVIDE_SYMBOL -> left / right
                    else -> errorToken(token)
                }

            stack.addLast(out)
        }

        // 調車場演算法：分支數來自運算子優先序、結合性與括號三者的組合，
        // 是演算法本身的形狀，拆開只會讓讀者需要在兩個函式之間對照。
        @Suppress("CyclomaticComplexMethod")
        private fun infixToPostfix(infix: List<String>): List<String> {
            val stack = ArrayDeque<String>()
            val postfix = mutableListOf<String>()

            fun precedence(op: String): Int =
                when (op) {
                    "+", "-" -> 1
                    MULTIPLY_SYMBOL, DIVIDE_SYMBOL -> DEFAULT_MAX_DECIMALS
                    UNARY_MINUS -> DEFAULT_MAX_DECIMALS + 1
                    else -> 0
                }

            fun pushOperator(op: String) {
                val opPrec = precedence(op)
                val rightAssociative = op == UNARY_MINUS
                while (
                    stack.isNotEmpty() &&
                    stack.last() != "(" &&
                    if (rightAssociative) precedence(stack.last()) > opPrec else precedence(stack.last()) >= opPrec
                ) {
                    postfix.add(stack.removeLast())
                }
                stack.addLast(op)
            }

            fun outputUntilLeft() {
                while (stack.isNotEmpty() && stack.last() != "(") {
                    postfix.add(stack.removeLast())
                }
                if (stack.lastOrNull() == "(") {
                    stack.removeLast()
                } else {
                    errorToken(")")
                }
            }

            var expectOperand = true
            for (token in infix) {
                if (token.toDoubleOrNull() != null) {
                    postfix.add(token)
                    expectOperand = false
                    continue
                }
                when (token) {
                    "-" if (expectOperand) -> pushOperator(UNARY_MINUS)
                    "+", "-", MULTIPLY_SYMBOL, DIVIDE_SYMBOL -> pushOperator(token)
                    "(" -> stack.addLast(token)
                    ")" -> outputUntilLeft()
                    else -> errorToken(token)
                }
                expectOperand = token == "(" || token != ")"
            }

            while (stack.isNotEmpty()) {
                postfix.add(stack.removeLast())
            }
            return postfix
        }

        private fun errorToken(token: String): Nothing = throw IllegalArgumentException("Unknown/invalid token: $token")
    }
}

// 計算機輸入狀態機的當前狀態
enum class CurrentState {
    NULL,
    ZERO,
    INTEGER,
    DOUBLE,
    OPERATOR,
    LEFT,
    RIGHT,
}

sealed interface KeyLabel {
    val label: String
}

enum class NumKey(
    override val label: String,
) : KeyLabel {
    Zero("0"),
    DoubleZero("00"),
    Dot("."),
    One("1"),
    Two("2"),
    Three("3"),
    Four("4"),
    Five("5"),
    Six("6"),
    Seven("7"),
    Eight("8"),
    Nine("9"),
}

enum class OpKey(
    override val label: String,
) : KeyLabel {
    Plus("+"),
    Minus("-"),
    Multiply(MULTIPLY_SYMBOL),
    Divide(DIVIDE_SYMBOL),
    LParen("("),
    RParen(")"),
}

enum class SysKey(
    override val label: String,
) : KeyLabel {
    Backspace("⌫"),
    AC("AC"),
}
