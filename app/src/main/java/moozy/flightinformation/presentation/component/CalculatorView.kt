package moozy.flightinformation.presentation.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moozy.flightinformation.R
import moozy.flightinformation.feature.calculator.Calculator
import moozy.flightinformation.feature.calculator.CurrentState

val NUMBER_FONT_SIZE = 48.sp
val BUTTON_FONT_SIZE = 36.sp


/**
* 這段是很久以前寫的拿過來用，應該長很醜哈哈哈
 */
@Preview
@Composable
fun CalculatorUI(calculator: Calculator = Calculator()) {
    Surface {
        Column {
            Column(modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .padding(16.dp)

            ) {

                val infix = remember {
                    derivedStateOf {
                        var text = ""
                        calculator.infix.map { text += it }
                        text
                    }
                }
//            BasicTextField(
//                value = infix.value,
//                onValueChange = {},
//                readOnly = true,
//                modifier = Modifier
//                    .weight(1F)
//                    .verticalScroll(rememberScrollState()),
//                textStyle = TextStyle(fontSize = 48.sp),
//            )
                Text(
                    text = infix.value,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .weight(1F),
                    fontSize = NUMBER_FONT_SIZE,
//                maxLines = 1
                )
                Row {
                    Text(text = "=", fontSize = NUMBER_FONT_SIZE)

                    var lastNumber by remember {
                        mutableStateOf(0.0)
                    }

                    var formatState by remember {
                        mutableStateOf(true)
                    }

                    val equal = remember {
                        derivedStateOf {
                            val ans = Calculator.cal(calculator.infix.toList())

                            if (ans == "no value") {
                                formatState = false

                                if (lastNumber % 1.0 == 0.0) {
                                    lastNumber.toInt().toString()
                                } else lastNumber.toString()
                            } else {
                                formatState = true

                                lastNumber = ans.toDouble()
                                ans
                            }
                        }
                    }
                    Text(text = equal.value,
                        modifier = Modifier
                            .weight(1F)
                            .enableAlpha { formatState }
                            .horizontalScroll(rememberScrollState()),
//                            .alpha(if (formatState) LocalContentAlpha.current else ContentAlpha.disabled),
                        fontSize = NUMBER_FONT_SIZE,
                        textAlign = TextAlign.End)
                }
            }


            Row(modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(horizontal = 4.dp)) {
                val modifier = Modifier
                    .padding(4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .weight(1f)
                    .align(Alignment.CenterVertically)

                TextButton(string = "(", modifier, {
                    when (calculator.currentState.value) {
                        CurrentState.NULL, CurrentState.OPERATOR, CurrentState.LEFT -> true
                        else -> false
                    }
                }) { calculator.operatorInput("(") }
                TextButton(string = ")", modifier, {
                    if (calculator.parenthesesState.value > 0) {
                        when (calculator.currentState.value) {
                            CurrentState.ZERO, CurrentState.INTEGER, CurrentState.DOUBLE, CurrentState.RIGHT -> true
                            else -> false
                        }
                    } else false
                }) { calculator.operatorInput(")") }

                IconButton(painter = painterResource(id = R.drawable.ic_backspace_fill0_wght400_grad0_opsz48),
                    modifier = modifier, { true }) { calculator.btBackSpace() }
//                TextButton(string = "BS", modifier, { true }) { calculator.btBackSpace() } // TODO

                TextButton(string = "AC", modifier, { true }) { calculator.btAC() }
            }

            Row(modifier = Modifier.padding(horizontal = 4.dp)) {
                val modifier = Modifier
                    .padding(4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .weight(1f)
                    .align(Alignment.CenterVertically)

                TextButton(string = "7", modifier, {
                    when (calculator.currentState.value) {
                        CurrentState.RIGHT -> false
                        else -> true
                    }
                }) { calculator.numInput("7") }
                TextButton(string = "8", modifier, {
                    when (calculator.currentState.value) {
                        CurrentState.RIGHT -> false
                        else -> true
                    }
                }) { calculator.numInput("8") }
                TextButton(string = "9", modifier, {
                    when (calculator.currentState.value) {
                        CurrentState.RIGHT -> false
                        else -> true
                    }
                }) { calculator.numInput("9") }
                TextButton(string = "+", modifier, {
                    when (calculator.currentState.value) {
                        CurrentState.ZERO, CurrentState.INTEGER, CurrentState.DOUBLE, CurrentState.RIGHT -> true
                        else -> false
                    }
                }) { calculator.operatorInput("+") }
            }

            Row(modifier = Modifier.padding(horizontal = 4.dp)) {
                val modifier = Modifier
                    .padding(4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .weight(1f)
                    .align(Alignment.CenterVertically)

                TextButton(string = "4", modifier, {
                    when (calculator.currentState.value) {
                        CurrentState.RIGHT -> false
                        else -> true
                    }
                }) { calculator.numInput("4") }
                TextButton(string = "5", modifier, {
                    when (calculator.currentState.value) {
                        CurrentState.RIGHT -> false
                        else -> true
                    }
                }) { calculator.numInput("5") }
                TextButton(string = "6", modifier, {
                    when (calculator.currentState.value) {
                        CurrentState.RIGHT -> false
                        else -> true
                    }
                }) { calculator.numInput("6") }
                TextButton(string = "-", modifier, {
//                when (viewModel.currentState.value) {
//                    CurrentState.ZERO, CurrentState.INTEGER, CurrentState.DOUBLE, CurrentState.RIGHT -> true
//                    else -> false
//                }

                    true
                }) {
                    when (calculator.currentState.value) {
                        CurrentState.OPERATOR, CurrentState.LEFT, CurrentState.NULL -> calculator.numInput(
                            "-")
                        else -> calculator.operatorInput("-")
                    }
                }
            }

            Row(modifier = Modifier.padding(horizontal = 4.dp)) {
                val modifier = Modifier
                    .padding(4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .weight(1f)
                    .align(Alignment.CenterVertically)

                TextButton(string = "1", modifier, {
                    when (calculator.currentState.value) {
                        CurrentState.RIGHT -> false
                        else -> true
                    }
                }) { calculator.numInput("1") }
                TextButton(string = "2", modifier, {
                    when (calculator.currentState.value) {
                        CurrentState.RIGHT -> false
                        else -> true
                    }
                }) { calculator.numInput("2") }
                TextButton(string = "3", modifier, {
                    when (calculator.currentState.value) {
                        CurrentState.RIGHT -> false
                        else -> true
                    }
                }) { calculator.numInput("3") }
                TextButton(string = "×", modifier, {
                    when (calculator.currentState.value) {
                        CurrentState.ZERO, CurrentState.INTEGER, CurrentState.DOUBLE, CurrentState.RIGHT -> true
                        else -> false
                    }
                }) { calculator.operatorInput("×") }
            }

            Row(modifier = Modifier.padding(horizontal = 4.dp)) {
                val modifier = Modifier
                    .padding(4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .weight(1f)
                    .align(Alignment.CenterVertically)

                TextButton(string = "00", modifier, {
                    when (calculator.currentState.value) {
                        CurrentState.ZERO, CurrentState.RIGHT -> false
                        else -> true
                    }
                }) { calculator.numInput("00") }
                TextButton(string = "0", modifier, {
                    when (calculator.currentState.value) {
                        CurrentState.ZERO, CurrentState.RIGHT -> false
                        else -> true
                    }
                }) { calculator.numInput("0") }
                TextButton(string = ".", modifier, {
                    when (calculator.currentState.value) {
                        CurrentState.NULL, CurrentState.ZERO, CurrentState.INTEGER, CurrentState.NEGATIVE -> true
                        else -> false
                    }
                }) { calculator.numInput(".") }
                TextButton(string = "÷", modifier, {
                    when (calculator.currentState.value) {
                        CurrentState.ZERO, CurrentState.INTEGER, CurrentState.DOUBLE, CurrentState.RIGHT -> true
                        else -> false
                    }
                }) { calculator.operatorInput("÷") }
            }

        }
    }
}

@Composable
fun TextButton(string: String, modifier: Modifier, clickable: () -> Boolean, click: () -> Unit) {

    val contentAlpha by animateFloatAsState(buttonAlpha(boolean = clickable()))

    Box(modifier = modifier
        .alpha(contentAlpha)
        .clickable(clickable()) { click() }
        .padding(8.dp)) {
        Text(text = string, modifier = Modifier.align(Alignment.Center), fontSize = BUTTON_FONT_SIZE)
    }
}

@Composable
fun IconButton(painter: Painter, modifier: Modifier, clickable: () -> Boolean, click: () -> Unit) {

    val contentAlpha by animateFloatAsState(buttonAlpha(boolean = clickable()))

    Box(modifier = modifier
        .graphicsLayer {
            alpha = contentAlpha
        }
        .clickable(clickable()) { click() }
        .padding(8.dp)) {
        Icon(painter = painter,
            contentDescription = null,
            modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun buttonAlpha(boolean: Boolean) =
    if (boolean) 1f else 0.38f

fun Modifier.enableAlpha(boolean: () -> Boolean) = composed {
    val contentAlpha by animateFloatAsState(buttonAlpha(boolean = boolean()))

    Modifier.graphicsLayer {
        alpha = contentAlpha
    }
}
