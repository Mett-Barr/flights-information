package moozy.flightinformation.feature.calculator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CalculatorArithmeticTest {
    @Test
    fun addition_updates_equal_and_stable_amount() {
        val calculator = Calculator()
        press(calculator, NumKey.Eight, OpKey.Plus, NumKey.Two)
        assertAmount(calculator, expectedEqual = 10.0, expectedStable = 10L)
    }

    @Test
    fun subtraction_updates_equal_and_stable_amount() {
        val calculator = Calculator()
        press(calculator, NumKey.Eight, OpKey.Minus, NumKey.Two)
        assertAmount(calculator, expectedEqual = 6.0, expectedStable = 6L)
    }

    @Test
    fun multiplication_updates_equal_and_stable_amount() {
        val calculator = Calculator()
        press(calculator, NumKey.Eight, OpKey.Multiply, NumKey.Two)
        assertAmount(calculator, expectedEqual = 16.0, expectedStable = 16L)
    }

    @Test
    fun division_updates_equal_and_stable_amount() {
        val calculator = Calculator()
        press(calculator, NumKey.Eight, OpKey.Divide, NumKey.Two)

        assertNotNull(calculator.equal)
        assertAmount(calculator, expectedEqual = 4.0, expectedStable = 4L)
    }

    @Test
    fun operations_from_typed_keys_match_expected_results() {
        val plus = Calculator().also { press(it, NumKey.Eight, OpKey.Plus, NumKey.Two) }
        val minus = Calculator().also { press(it, NumKey.Eight, OpKey.Minus, NumKey.Two) }
        val multiply = Calculator().also { press(it, NumKey.Eight, OpKey.Multiply, NumKey.Two) }
        val divide = Calculator().also { press(it, NumKey.Eight, OpKey.Divide, NumKey.Two) }

        assertEquals(10.0, plus.equal)
        assertEquals(6.0, minus.equal)
        assertEquals(16.0, multiply.equal)
        assertEquals(4.0, divide.equal)
    }

    // --- Multi-operator associativity (shunting-yard regression) ---

    @Test
    fun three_term_subtraction_then_addition_is_left_associative() {
        // 8 - 2 + 10 = 16  (not -4)
        val calculator = Calculator()
        press(calculator, NumKey.Eight, OpKey.Minus, NumKey.Two, OpKey.Plus, NumKey.One, NumKey.Zero)
        assertAmount(calculator, expectedEqual = 16.0, expectedStable = 16L)
    }

    @Test
    fun three_term_addition_then_subtraction_is_left_associative() {
        // 10 + 3 - 5 = 8
        val calculator = Calculator()
        press(calculator, NumKey.One, NumKey.Zero, OpKey.Plus, NumKey.Three, OpKey.Minus, NumKey.Five)
        assertAmount(calculator, expectedEqual = 8.0, expectedStable = 8L)
    }

    @Test
    fun mixed_precedence_multiplication_before_subtraction() {
        // 2 + 3 × 4 = 14  (not 20)
        val calculator = Calculator()
        press(calculator, NumKey.Two, OpKey.Plus, NumKey.Three, OpKey.Multiply, NumKey.Four)
        assertAmount(calculator, expectedEqual = 14.0, expectedStable = 14L)
    }

    @Test
    fun chained_division_is_left_associative() {
        // 8 ÷ 2 ÷ 2 = 2  (not 8)
        val calculator = Calculator()
        press(calculator, NumKey.Eight, OpKey.Divide, NumKey.Two, OpKey.Divide, NumKey.Two)
        assertAmount(calculator, expectedEqual = 2.0, expectedStable = 2L)
    }

    // --- Backspace on single-digit negative leaves the unary-minus token ---

    @Test
    fun backspace_single_digit_negative_keeps_unary_minus_operator() {
        val calculator = Calculator()
        press(calculator, SysKey.AC, OpKey.Minus, NumKey.Five)
        assertEquals(listOf("-", "5"), calculator.infix)

        calculator.onKey(SysKey.Backspace)
        assertEquals(CurrentState.OPERATOR, calculator.currentState.value)
        assertEquals("-", calculator.infixString)
    }

    @Test
    fun backspace_single_digit_negative_then_new_digit_works() {
        val calculator = Calculator()
        press(calculator, SysKey.AC, OpKey.Minus, NumKey.Five, SysKey.Backspace, NumKey.Three)
        assertEquals(-3.0, calculator.equal)
    }

    @Test
    fun backspace_multi_digit_negative_keeps_integer_state() {
        val calculator = Calculator()
        press(calculator, SysKey.AC, OpKey.Minus, NumKey.Five, NumKey.Two)
        assertEquals(listOf("-", "52"), calculator.infix)

        calculator.onKey(SysKey.Backspace)
        assertEquals(listOf("-", "5"), calculator.infix)
        assertEquals(CurrentState.INTEGER, calculator.currentState.value)
    }

    // --- Parenthesized unary minus ---

    @Test
    fun negative_parenthesized_expression_evaluates_correctly() {
        // -(2+3) = -5
        val calculator = Calculator()
        calculator.onKey(SysKey.AC)
        press(calculator, OpKey.Minus, OpKey.LParen, NumKey.Two, OpKey.Plus, NumKey.Three, OpKey.RParen)
        assertEquals(-5.0, calculator.equal)
    }

    @Test
    fun negative_parenthesized_multiply_evaluates_correctly() {
        // -(4) × 3 = -12
        val calculator = Calculator()
        calculator.onKey(SysKey.AC)
        press(
            calculator,
            OpKey.Minus, OpKey.LParen, NumKey.Four, OpKey.RParen,
            OpKey.Multiply, NumKey.Three,
        )
        assertEquals(-12.0, calculator.equal)
    }

    // --- Bug 3: operator replacement is dead code ---

    @Test
    fun operator_can_be_replaced_by_pressing_another_operator() {
        // 8 + × 2 = 16 (+ replaced by ×)
        val calculator = Calculator()
        press(calculator, NumKey.Eight, OpKey.Plus, OpKey.Multiply, NumKey.Two)
        assertEquals("×", calculator.infix[1])
        assertEquals(16.0, calculator.equal)
    }

    @Test
    fun minus_replaces_plus_operator() {
        // 8 + - 2 = 6 (+ replaced by -)
        val calculator = Calculator()
        press(calculator, NumKey.Eight, OpKey.Plus, OpKey.Minus, NumKey.Two)
        assertEquals("-", calculator.infix[1])
        assertEquals(6.0, calculator.equal)
    }

    @Test
    fun backspace_after_right_paren_allows_input_right_paren_again() {
        val calculator = Calculator()
        calculator.onKey(SysKey.AC)

        press(calculator, OpKey.LParen, NumKey.Seven, OpKey.Plus, NumKey.Eight, OpKey.RParen)

        calculator.onKey(SysKey.Backspace)
        assertEquals(1, calculator.parenthesesState.intValue)
        assertEquals(true, calculator.canInput(OpKey.RParen))

        calculator.onKey(OpKey.RParen)
        assertEquals(0, calculator.parenthesesState.intValue)
    }

    private fun press(
        calculator: Calculator,
        vararg keys: KeyLabel,
    ) {
        keys.forEach(calculator::onKey)
    }

    private fun assertAmount(
        calculator: Calculator,
        expectedEqual: Double,
        expectedStable: Long,
    ) {
        assertEquals(expectedEqual, calculator.equal)
        assertEquals(expectedStable, calculator.lastStableAmountText.value.toLong())
    }
}
