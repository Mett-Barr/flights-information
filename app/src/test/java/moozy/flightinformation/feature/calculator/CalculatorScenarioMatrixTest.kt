package moozy.flightinformation.feature.calculator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CalculatorScenarioMatrixTest {
    @Test
    fun legal_expression_matrix_is_computed_correctly() {
        val precedence = Calculator().also { press(it, SysKey.AC, NumKey.Two, OpKey.Plus, NumKey.Three, OpKey.Multiply, NumKey.Four) }
        val grouped =
            Calculator().also {
                press(
                    it,
                    SysKey.AC,
                    OpKey.LParen,
                    NumKey.Two,
                    OpKey.Plus,
                    NumKey.Three,
                    OpKey.RParen,
                    OpKey.Multiply,
                    NumKey.Four,
                )
            }
        val mixed =
            Calculator().also {
                press(
                    it,
                    SysKey.AC,
                    OpKey.LParen,
                    NumKey.One,
                    OpKey.Plus,
                    NumKey.Three,
                    OpKey.RParen,
                    OpKey.Divide,
                    NumKey.Two,
                    OpKey.Plus,
                    NumKey.Six,
                )
            }
        val decimal =
            Calculator().also {
                press(it, SysKey.AC, NumKey.One, NumKey.Dot, NumKey.Five, OpKey.Plus, NumKey.Two, NumKey.Dot, NumKey.Two, NumKey.Five)
            }
        val negative = Calculator().also { press(it, SysKey.AC, OpKey.Minus, NumKey.Two, OpKey.Plus, NumKey.Three) }

        assertEquals(14.0, precedence.equal)
        assertEquals(20.0, grouped.equal)
        assertEquals(8.0, mixed.equal)
        assertEquals(3.75, decimal.equal)
        assertEquals(1.0, negative.equal)
    }

    @Test
    fun illegal_expression_matrix_is_blocked_by_typed_input_guard() {
        val trailingOperator = Calculator().also { press(it, SysKey.AC, NumKey.Two, OpKey.Plus) }
        val unmatchedLeftParen = Calculator().also { press(it, SysKey.AC, OpKey.LParen, NumKey.Two, OpKey.Plus, NumKey.Three) }
        val unmatchedRightParenBlocked = Calculator().also { press(it, SysKey.AC, NumKey.Two, OpKey.Plus, NumKey.Three, OpKey.RParen) }

        assertNull(trailingOperator.equal)
        assertEquals(1, unmatchedLeftParen.parenthesesState.intValue)
        assertNull(unmatchedLeftParen.equal)
        assertEquals(0, unmatchedRightParenBlocked.parenthesesState.intValue)
        assertEquals("2+3", unmatchedRightParenBlocked.infixString)
        assertEquals(5.0, unmatchedRightParenBlocked.equal)
    }

    @Test
    fun consecutive_operator_input_replaces_previous_operator() {
        val calculator = Calculator()
        press(calculator, SysKey.AC, NumKey.Eight, OpKey.Plus, OpKey.Minus, NumKey.Two)

        assertEquals(3, calculator.infix.size)
        assertEquals(OpKey.Minus.label, calculator.infix[1]) // + replaced by -
        assertEquals(6.0, calculator.equal) // 8-2=6
    }

    @Test
    fun unary_negative_flow_is_supported() {
        val calculator = Calculator()
        press(calculator, SysKey.AC, OpKey.Minus, NumKey.Five, OpKey.Plus, NumKey.Two)

        assertEquals(-3.0, calculator.equal)
        assertEquals(-3L, calculator.lastStableAmountText.value.toLong())
    }

    @Test
    fun unary_minus_at_expression_start_is_evaluated_by_the_parser() {
        val calculator = Calculator()
        press(calculator, SysKey.AC, OpKey.Minus, NumKey.Five)

        assertEquals("-5", calculator.lastStableAmountText.value)
    }

    @Test
    fun unary_minus_before_addition_is_evaluated_by_the_parser() {
        val calculator = Calculator()
        press(calculator, SysKey.AC, OpKey.Minus, NumKey.Two, OpKey.Plus, NumKey.Three)

        assertEquals(1.0, calculator.equal)
    }

    @Test
    fun unary_minus_after_multiply_preserves_the_previous_operator() {
        val calculator = Calculator()
        press(calculator, SysKey.AC, NumKey.Two, OpKey.Multiply, OpKey.Minus, NumKey.Three)

        assertEquals("-6", calculator.lastStableAmountText.value)
    }

    @Test
    fun unary_minus_after_divide_is_evaluated_by_the_parser() {
        val calculator = Calculator()
        press(calculator, SysKey.AC, NumKey.One, NumKey.Zero, OpKey.Divide, OpKey.Minus, NumKey.Two)

        assertEquals(-5.0, calculator.equal)
    }

    @Test
    fun unary_minus_before_parenthesized_expression_is_evaluated_by_the_parser() {
        val calculator = Calculator()
        press(
            calculator,
            SysKey.AC,
            OpKey.Minus,
            OpKey.LParen,
            NumKey.Two,
            OpKey.Plus,
            NumKey.Three,
            OpKey.RParen,
        )

        assertEquals("-5", calculator.lastStableAmountText.value)
    }

    @Test
    fun minus_replaces_a_trailing_plus_operator() {
        val calculator = Calculator()
        press(calculator, SysKey.AC, NumKey.Two, OpKey.Plus, OpKey.Minus)

        assertEquals("2-", calculator.infixString)
    }

    @Test
    fun minus_does_not_replace_a_trailing_multiply_operator() {
        val calculator = Calculator()
        press(calculator, SysKey.AC, NumKey.Two, OpKey.Multiply, OpKey.Minus)

        assertEquals("2×-", calculator.infixString)
    }

    @Test
    fun multiply_replaces_a_trailing_multiply_unary_minus_sequence() {
        val calculator = Calculator()
        press(calculator, SysKey.AC, NumKey.Two, OpKey.Multiply, OpKey.Minus, OpKey.Multiply)

        assertEquals("2×", calculator.infixString)
    }

    @Test
    fun divide_replaces_a_trailing_multiply_unary_minus_sequence() {
        val calculator = Calculator()
        press(calculator, SysKey.AC, NumKey.Two, OpKey.Multiply, OpKey.Minus, OpKey.Divide)

        assertEquals("2÷", calculator.infixString)
    }

    @Test
    fun plus_replaces_a_trailing_divide_unary_minus_sequence() {
        val calculator = Calculator()
        press(calculator, SysKey.AC, NumKey.Two, OpKey.Divide, OpKey.Minus, OpKey.Plus)

        assertEquals("2+", calculator.infixString)
    }

    @Test
    fun plus_is_blocked_after_a_leading_unary_minus() {
        val calculator = Calculator()
        press(calculator, SysKey.AC, OpKey.Minus)

        assertFalse(calculator.canInput(OpKey.Plus))
        calculator.onKey(OpKey.Plus)
        assertEquals("-", calculator.infixString)
    }

    @Test
    fun plus_is_blocked_after_a_unary_minus_inside_parentheses() {
        val calculator = Calculator()
        press(calculator, SysKey.AC, OpKey.LParen, OpKey.Minus)

        assertFalse(calculator.canInput(OpKey.Plus))
        calculator.onKey(OpKey.Plus)
        assertEquals("(-", calculator.infixString)
    }

    @Test
    fun unary_minus_after_multiply_still_evaluates() {
        val calculator = Calculator()
        press(calculator, SysKey.AC, NumKey.Two, OpKey.Multiply, OpKey.Minus, NumKey.Three)

        assertEquals(-6.0, calculator.equal)
    }

    @Test
    fun repeated_unary_minus_after_multiply_preserves_multiply() {
        val calculator = Calculator()
        press(calculator, SysKey.AC, NumKey.Two, OpKey.Multiply, OpKey.Minus, OpKey.Minus)

        assertEquals("2×-", calculator.infixString)
    }

    @Test
    fun unary_minus_inside_parentheses_is_evaluated_by_the_parser() {
        val calculator = Calculator()
        press(calculator, SysKey.AC, OpKey.LParen, OpKey.Minus, NumKey.Three, OpKey.RParen)

        assertEquals(-3.0, calculator.equal)
    }

    @Test
    fun key_guard_matrix_blocks_illegal_and_allows_legal_inputs() {
        val calculator = Calculator()
        assertFalse(calculator.canInput(OpKey.RParen))
        assertTrue(calculator.canInput(OpKey.Plus))
        assertFalse(calculator.canInput(OpKey.LParen))

        calculator.onKey(SysKey.AC)
        assertTrue(calculator.canInput(OpKey.LParen))
        assertFalse(calculator.canInput(OpKey.RParen))

        press(calculator, OpKey.LParen, NumKey.Seven, OpKey.RParen)

        assertFalse(calculator.canInput(NumKey.One))
        assertTrue(calculator.canInput(OpKey.Plus))
    }

    // --- Complex arithmetic ---

    @Test
    fun nested_parentheses() {
        // ((2+3)×4) = 20
        val calculator = Calculator()
        press(
            calculator, SysKey.AC,
            OpKey.LParen, OpKey.LParen,
            NumKey.Two, OpKey.Plus, NumKey.Three,
            OpKey.RParen, OpKey.Multiply, NumKey.Four,
            OpKey.RParen,
        )
        assertEquals(20.0, calculator.equal)
    }

    @Test
    fun two_parenthesized_groups_multiplied() {
        // (2+3)×(4+1) = 25
        val calculator = Calculator()
        press(
            calculator, SysKey.AC,
            OpKey.LParen, NumKey.Two, OpKey.Plus, NumKey.Three, OpKey.RParen,
            OpKey.Multiply,
            OpKey.LParen, NumKey.Four, OpKey.Plus, NumKey.One, OpKey.RParen,
        )
        assertEquals(25.0, calculator.equal)
    }

    @Test
    fun mixed_precedence_with_parentheses_and_subtraction() {
        // 1+2×(3+4)-5 = 1+14-5 = 10
        val calculator = Calculator()
        press(
            calculator, SysKey.AC,
            NumKey.One, OpKey.Plus,
            NumKey.Two, OpKey.Multiply,
            OpKey.LParen, NumKey.Three, OpKey.Plus, NumKey.Four, OpKey.RParen,
            OpKey.Minus, NumKey.Five,
        )
        assertEquals(10.0, calculator.equal)
    }

    @Test
    fun long_addition_chain() {
        // 1+2+3+4+5 = 15
        val calculator = Calculator()
        press(
            calculator, SysKey.AC,
            NumKey.One, OpKey.Plus, NumKey.Two, OpKey.Plus, NumKey.Three,
            OpKey.Plus, NumKey.Four, OpKey.Plus, NumKey.Five,
        )
        assertEquals(15.0, calculator.equal)
    }

    @Test
    fun long_subtraction_chain() {
        // 20-3-2-5 = 10
        val calculator = Calculator()
        press(
            calculator, SysKey.AC,
            NumKey.Two, NumKey.Zero,
            OpKey.Minus, NumKey.Three,
            OpKey.Minus, NumKey.Two,
            OpKey.Minus, NumKey.Five,
        )
        assertEquals(10.0, calculator.equal)
    }

    @Test
    fun mixed_all_four_operators() {
        // 10+6÷3-2×4 = 10+2-8 = 4
        val calculator = Calculator()
        press(
            calculator, SysKey.AC,
            NumKey.One, NumKey.Zero,
            OpKey.Plus, NumKey.Six, OpKey.Divide, NumKey.Three,
            OpKey.Minus, NumKey.Two, OpKey.Multiply, NumKey.Four,
        )
        assertEquals(4.0, calculator.equal)
    }

    @Test
    fun negative_times_negative_in_parentheses() {
        // (-3)×(-2) = 6
        val calculator = Calculator()
        press(
            calculator, SysKey.AC,
            OpKey.LParen, OpKey.Minus, NumKey.Three, OpKey.RParen,
            OpKey.Multiply,
            OpKey.LParen, OpKey.Minus, NumKey.Two, OpKey.RParen,
        )
        assertEquals(6.0, calculator.equal)
    }

    @Test
    fun division_yielding_decimal_respects_precision() {
        // 10÷3 = 3.33 (maxDecimals=2)
        val calculator = Calculator()
        press(calculator, SysKey.AC, NumKey.One, NumKey.Zero, OpKey.Divide, NumKey.Three)
        assertEquals("3.33", calculator.lastStableAmountText.value)
    }

    @Test
    fun division_by_zero_does_not_commit_a_saturated_infinity() {
        val calculator = Calculator()
        press(calculator, SysKey.AC, NumKey.Two, OpKey.Divide, NumKey.Zero)

        assertNull(calculator.equal)
        assertEquals("2", calculator.lastStableAmountText.value)
    }

    @Test
    fun zero_divided_by_zero_remains_unrepresentable() {
        val calculator = Calculator()
        press(calculator, SysKey.AC, NumKey.Zero, OpKey.Divide, NumKey.Zero)

        assertNull(calculator.equal)
    }

    @Test
    fun nineteen_digit_input_does_not_commit_a_saturated_long() {
        val calculator = Calculator()
        press(calculator, SysKey.AC, *Array(19) { NumKey.Nine })

        assertNotEquals("9223372036854775807", calculator.lastStableAmountText.value)
    }

    @Test
    fun terminating_decimal_result_remains_formatted() {
        val calculator = Calculator()
        press(calculator, SysKey.AC, NumKey.One, NumKey.Zero, OpKey.Divide, NumKey.Four)

        assertEquals("2.5", calculator.lastStableAmountText.value)
    }

    @Test
    fun repeating_decimal_result_remains_rounded() {
        val calculator = Calculator()
        press(calculator, SysKey.AC, NumKey.One, NumKey.Zero, OpKey.Divide, NumKey.Three)

        assertEquals("3.33", calculator.lastStableAmountText.value)
    }

    @Test
    fun backspace_recomputes_key_enablement_for_parentheses() {
        val calculator = Calculator()
        press(calculator, SysKey.AC, OpKey.LParen, NumKey.Seven, OpKey.Plus, NumKey.Eight, OpKey.RParen)

        assertEquals(0, calculator.parenthesesState.intValue)
        assertFalse(calculator.canInput(OpKey.RParen))

        calculator.onKey(SysKey.Backspace)
        assertEquals(1, calculator.parenthesesState.intValue)
        assertTrue(calculator.canInput(OpKey.RParen))
    }

    private fun press(
        calculator: Calculator,
        vararg keys: KeyLabel,
    ) {
        keys.forEach(calculator::onKey)
    }
}
