package moozy.flightinformation.feature.calculator


import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import java.util.Stack


/**
 * 這段是很久以前寫的拿過來用，應該長很醜哈哈哈
 */
class Calculator {
    val infix = mutableStateListOf<String>()

    // 當前狀態
    val currentState: MutableState<CurrentState> = mutableStateOf(CurrentState.NULL)

    // 括號狀態
    val parenthesesState = mutableIntStateOf(0)

    private fun inputReplace(string: String) {
        infix.removeAt(infix.lastIndex)
        infix.add(string)
    }

    private fun inputNew(string: String) {
        infix.add(string)
    }

    private fun inputAppend(string: String) {
        infix.add(infix.removeAt(infix.lastIndex) + string)
    }


    fun numInput(inputNum: String) {
        when (currentState.value) {
            // null：直接儲存輸入
            CurrentState.NULL -> {
                when (inputNum) {
                    "0", "00" -> {
                        infix.add("0")
                        currentState.value = CurrentState.ZERO
                    }
                    "." -> {
                        infix.add("0.")
                        currentState.value = CurrentState.DOUBLE
                    }
                    "-" -> {
                        infix.add(inputNum)
                        currentState.value = CurrentState.NEGATIVE
                    }
                    else -> {
                        infix.add(inputNum)
                        currentState.value = CurrentState.INTEGER
                    }
                }
            }

            // input:Num state:Zero
            CurrentState.ZERO -> {
                when (inputNum) {
                    "." -> {
                        inputAppend(".")
                        currentState.value = CurrentState.DOUBLE
                    }
                    "0", "00" -> {}
                    "-" -> {
                        inputReplace(inputNum)
                        currentState.value = CurrentState.NEGATIVE
                    }
                    else -> {
                        inputReplace(inputNum)
                        currentState.value = CurrentState.INTEGER
                    }
                }
            }

            // input:Num  state:Int
            CurrentState.INTEGER -> {
                if (inputNum == ".") {
                    inputAppend(inputNum)
                    currentState.value = CurrentState.DOUBLE
                } else {
                    inputAppend(inputNum)
                }
            }

            // input:Num  state:Double
            CurrentState.DOUBLE -> {
                inputAppend(inputNum)
            }

            // input:Num  state:Double
            CurrentState.NEGATIVE -> {
                when (inputNum) {
                    "0", "00" -> {
                        inputAppend("0")
                        currentState.value = CurrentState.ZERO
                    }
                    "." -> {
                        inputAppend("0.")
                        currentState.value = CurrentState.DOUBLE
                    }
                    else -> {
                        inputAppend(inputNum)
                        currentState.value = CurrentState.INTEGER
                    }
                }
            }

            // input:Num  state:Operator
            CurrentState.OPERATOR -> {
                when (inputNum) {
                    "0", "00" -> {
                        inputNew("0")
                        currentState.value = CurrentState.ZERO
                    }
                    "." -> {
                        inputNew("0.")
                        currentState.value = CurrentState.DOUBLE
                    }
                    "-" -> {
                        inputNew(inputNum)
                        currentState.value = CurrentState.NEGATIVE
                    }
                    else -> {
                        inputNew(inputNum)
                        currentState.value = CurrentState.INTEGER
                    }
                }
            }

            // input:Num  state:(
            CurrentState.LEFT -> {
                when (inputNum) {
                    "0", "00" -> {
                        infix.add("0")
                        currentState.value = CurrentState.ZERO
                    }
                    "." -> {
                        infix.add("0.")
                        currentState.value = CurrentState.DOUBLE
                    }
                    "-" -> {
                        infix.add(inputNum)
                        currentState.value = CurrentState.NEGATIVE
                    }
                    else -> {
                        infix.add(inputNum)
                        currentState.value = CurrentState.INTEGER
                    }
                }
            }

            // input:Num state:)
            CurrentState.RIGHT -> {
                when (inputNum) {
                    "0", "00" -> {

                    }
                }
            }
        }
    }

    fun operatorInput(inputOperator: String) {
        when (currentState.value) {
            // input:Operator
            CurrentState.NULL -> {
                if (inputOperator == "(") inputNew(inputOperator)
                parenthesesState.value += 1
                currentState.value = CurrentState.LEFT
            }

            // input:Operator
            CurrentState.INTEGER, CurrentState.DOUBLE, CurrentState.ZERO -> {
                if (inputOperator == ")") {
                    inputNew(inputOperator)
                    parenthesesState.value -= 1
                    currentState.value = CurrentState.RIGHT
                } else {
                    inputNew(inputOperator)
                    currentState.value = CurrentState.OPERATOR
                }
            }

            // input:Operator
            CurrentState.RIGHT -> {
                inputNew(inputOperator)
                if (inputOperator == ")") {
                    currentState.value = CurrentState.RIGHT
                }
                currentState.value = CurrentState.OPERATOR
            }

            CurrentState.OPERATOR -> {
                if (inputOperator == "(") {
                    inputNew(inputOperator)
                    parenthesesState.value += 1
                    currentState.value = CurrentState.LEFT
                }
            }

            else -> {
                Log.d("!!! error", "operatorInput: $inputOperator")
            }
        }
    }

    private fun infixIsNotEmpty(): Boolean {
        return if (infix.isEmpty()) {
            currentState.value = CurrentState.NULL
            Log.d("!!! ", "infixIsNotEmpty: ${infix.isEmpty()}")
            false
        } else true
    }


    /** UI Function  */

    fun btBackSpace() {
        if (infix.last().isNotEmpty()) {

            when (currentState.value) {
                CurrentState.NULL -> {}

                CurrentState.ZERO -> {
                    infix.dropLast(1)
                    if (infix.isNotEmpty()) {
                        when (infix.last()) {
                            "(" -> currentState.value = CurrentState.LEFT
                            else -> {

                                currentState.value = CurrentState.OPERATOR
                            }
                        }
                    } else {
                        currentState.value = CurrentState.NULL
                    }
                }

                CurrentState.INTEGER -> {
                    infix.add(infix.removeAt(infix.lastIndex).dropLast(1))
                    if (infix.last() == "") {
                        infix.removeAt(infix.lastIndex)
                        if (infixIsNotEmpty()) {
                            when (infix.last()) {
                                "(" -> currentState.value = CurrentState.LEFT
                                else -> currentState.value = CurrentState.OPERATOR
                            }
                        }
                    }
                }

                CurrentState.DOUBLE -> {
                    val deletedStr = infix.last().takeLast(1)
                    infix.add(infix.removeAt(infix.lastIndex).dropLast(1))

                    if (deletedStr == ".") {
                        when (infix.last()) {
                            "0", "-0" -> currentState.value = CurrentState.ZERO
                            else -> currentState.value = CurrentState.INTEGER
                        }
                    }
                }

                CurrentState.NEGATIVE -> {
                    infix.dropLast(1)

                    if (infixIsNotEmpty()) {
                        if (infix.last() == "(") CurrentState.LEFT
                        else currentState.value = CurrentState.OPERATOR
                    }
                }

                CurrentState.OPERATOR -> {
                    infix.dropLast(1)

                    val previous = infix.last()
                    currentState.value =
                        if (previous == ")") CurrentState.RIGHT
                        else if (previous == "0") CurrentState.ZERO
                        else if (previous.toIntOrNull() is Int) CurrentState.INTEGER
                        else if (previous.toDoubleOrNull() is Double) CurrentState.DOUBLE
                        else {
                            Log.d("!!! error", "btBackSpace: $previous")
                            CurrentState.DOUBLE
                        }
                }

                CurrentState.LEFT -> {
                    infix.dropLast(1)

                    val previous = infix.last()
                    currentState.value =
                        if (previous == "(") CurrentState.LEFT
                        else CurrentState.OPERATOR

                }

                CurrentState.RIGHT -> {
                    infix.dropLast(1)

                    val previous = infix.last()
                    currentState.value =
                        if (previous == ")") CurrentState.RIGHT
                        else if (previous == "0") CurrentState.ZERO
                        else if (previous.toIntOrNull() is Int) CurrentState.INTEGER
                        else if (previous.toDoubleOrNull() is Double) CurrentState.DOUBLE
                        else {
                            Log.d("!!! error", "btBackSpace: $previous")
                            CurrentState.DOUBLE
                        }
                }
            }
        }
    }

    fun btAC() {
        infix.clear()
        currentState.value = CurrentState.NULL
    }


    companion object {
        fun cal(infix: List<String>): String {
//        infix = list
            val postfix = infixToPostfix(infix)
            val stack: Stack<Double> = Stack()
            if (infix.isEmpty()) {
                return "0"
            } else if (infix[0] == "") {
                return "0"
            }else {
                try {
                    val output = infixToPostfix(infix)

                    Log.d("!!!", output.toString())

                    calculate(postfix, stack)

                    return if (stack.peek()%1.0 == 0.0) {
                        stack.peek().toInt().toString()
                    } else stack.peek().toString()
                } catch (e: Exception) {
                    Log.d("!!! catch", "cal: $e")

                    Log.d("!!! stack", stack.toString())

                    Log.d("!!! infix", infix.toString())

                    Log.d("!!! postfix", postfix.toString())

                    return "no value"
                }
            }
        }

//    private var stack: Stack<Double> = Stack()

        private fun calculate(postfix: List<String>, stack: Stack<Double>) {
            for (it in postfix) {
                assort(it, stack)
            }

//        Log.d("!!!", "calculate: ${stack.peek()}")
            Log.d("!!!", "calculate: ${stack.isEmpty()}")
        }

        private fun assort(string: String, stack: Stack<Double>) {

            Log.d("!!! stack before", "assort: $stack")

            var numLeft = 0.0
            var numRight = 0.0

            fun num() {
                numRight = stack.pop()
                numLeft = stack.pop()
            }

            if (string.toDoubleOrNull() != null) {
                stack.push(string.toDouble())
            } else {
                num()
                Log.d("!!!", "right: $numRight left: $numLeft ")
                when (string) {
                    "+" -> {
                        stack.push(
                            numLeft + numRight
                        )
                    }
                    "-" -> {
                        stack.push(
                            numLeft - numRight
                        )
                    }
                    "×" -> {
                        stack.push(
                            numLeft * numRight
                        )
                    }
                    "÷" -> {
                        stack.push(
                            numLeft / numRight
                        )
                    }
                    else -> error(string)
                }

            }

            Log.d("!!! stack after", "assort: $stack")
        }

        private fun infixToPostfix(infix: List<String>): List<String> {
            val stack: Stack<String> = Stack()
            val postfix: MutableList<String> = mutableListOf()

            fun output(string: String) {
                postfix.add(string)
            }

            fun operatorCheck(string: String) {
                if (stack.size != 0) {
                    if (stack.peek() == "×" || stack.peek() == "÷") {
                        postfix.add(stack.pop())
                        stack.push(string)
                    } else {
                        stack.push(string)
                    }
                } else {
                    stack.push(string)
                }
            }

            fun outputUntilLeft() {
                while (stack.peek() != "(") {
                    postfix.add(stack.pop())
                }
                stack.pop()
            }

            fun assort(string: String) {
                if (string.toDoubleOrNull() != null) {
                    output(string)
                } else {
                    when (string) {
                        "+" -> {
                            operatorCheck(string)
                        }
                        "-" -> {
                            operatorCheck(string)
                        }
                        "×" -> {
                            stack.push(string)
                        }
                        "÷" -> {
                            stack.push(string)
                        }
                        "(" -> {
                            stack.push(string)
                        }
                        ")" -> {
                            outputUntilLeft()
                        }
                        else -> error(string)
                    }
                }
            }

            for (it in infix) {
                assort(it)
            }

            while (stack.isNotEmpty()) {
                postfix.add(stack.pop())
            }

            return postfix
        }

        private fun error(string: String) {
            Log.d("!!! error", "error string: $string")
        }
    }
}

// 決定可輸入的運算源和運算子
enum class CurrentState {
    NULL, ZERO, INTEGER, DOUBLE, NEGATIVE, OPERATOR, LEFT, RIGHT
}
