package moozy.flightinformation.feature.calculator

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.Flow

@Preview
@Composable
fun CalculatorUI(
    modifier: Modifier = Modifier,
    calculator: Calculator = Calculator(),
) {
    fun isEnabled(key: KeyLabel): Boolean = calculator.canInput(key)

    val shape =
        androidx.compose.foundation.shape
            .RoundedCornerShape(16.dp)

    val normalText = MaterialTheme.colorScheme.onSurface
    val operatorText =
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
    val disabledText = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)

    @Composable
    fun RowScope.KeyButton(
        key: KeyLabel,
        height: androidx.compose.ui.unit.Dp,
        fontSizeSp: Int,
        fontWeight: FontWeight,
        muted: Boolean,
    ) {
        val enabled = isEnabled(key)

        val interactionSource =
            rememberHapticInteractionSource(
                enabled = enabled,
                // pressType / releaseType 採用預設值，需要時再覆寫
            )

        val contentColor =
            if (!enabled) {
                disabledText
            } else if (muted) {
                operatorText
            } else {
                normalText
            }

        TextButton(
            onClick = { calculator.onKey(key) },
            interactionSource = interactionSource,
            enabled = enabled,
            shape = shape,
            colors =
                androidx.compose.material3.ButtonDefaults.textButtonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    contentColor = contentColor,
                    disabledContentColor = disabledText,
                ),
            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(0.dp),
            modifier =
                Modifier
                    .weight(1f)
                    .height(height)
                    .testTag(calculatorKeyTag(key)),
        ) {
            Text(
                // SysKey.Backspace.label 本身就是 "⌫"，不需要額外對照
                text = key.label,
                fontSize = fontSizeSp.sp,
                fontWeight = fontWeight,
                color = contentColor,
            )
        }
    }

    @Composable
    fun KeyRow(vararg keys: KeyLabel) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            keys.forEach { k ->
                val isTopRow =
                    (k == OpKey.LParen || k == OpKey.RParen || k == SysKey.Backspace || k == SysKey.AC)

                val height = if (isTopRow) 56.dp else 64.dp

                val muted =
                    when (k) {
                        is OpKey -> true
                        SysKey.Backspace -> true
                        SysKey.AC -> false
                        is NumKey -> false
                    }

                val fontSizeSp =
                    when (k) {
                        SysKey.AC -> 18
                        SysKey.Backspace -> 20
                        OpKey.LParen, OpKey.RParen -> 18
                        is NumKey -> 28
                        is OpKey -> 28
                    }

                val fontWeight =
                    when (k) {
                        SysKey.AC -> FontWeight.Medium
                        SysKey.Backspace -> FontWeight.Light
                        is NumKey -> FontWeight.Light
                        is OpKey -> FontWeight.Light
                    }

                KeyButton(
                    key = k,
                    height = height,
                    fontSizeSp = fontSizeSp,
                    fontWeight = fontWeight,
                    muted = muted,
                )
            }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        KeyRow(OpKey.LParen, OpKey.RParen, SysKey.Backspace, SysKey.AC)
        KeyRow(NumKey.Seven, NumKey.Eight, NumKey.Nine, OpKey.Plus)
        KeyRow(NumKey.Four, NumKey.Five, NumKey.Six, OpKey.Minus)
        KeyRow(NumKey.One, NumKey.Two, NumKey.Three, OpKey.Multiply)
        KeyRow(NumKey.DoubleZero, NumKey.Zero, NumKey.Dot, OpKey.Divide)
    }
}

@Composable
fun rememberHapticInteractionSource(
    enabled: Boolean,
    pressType: HapticFeedbackType = HapticFeedbackType.KeyboardTap,
    releaseType: HapticFeedbackType = HapticFeedbackType.SegmentFrequentTick,
    vibrateOnCancel: Boolean = false,
    cancelType: HapticFeedbackType = HapticFeedbackType.Reject,
): MutableInteractionSource {
    val haptic = LocalHapticFeedback.current

    val src =
        remember {
            HapticMutableInteractionSource(
                delegate = MutableInteractionSource(),
                haptic = haptic,
                enabled = enabled,
                pressType = pressType,
                releaseType = releaseType,
                vibrateOnCancel = vibrateOnCancel,
                cancelType = cancelType,
            )
        }

    // 把 enabled / type / haptic 在每次 recomposition 時同步進去，讓 InteractionSource 本身維持穩定
    SideEffect {
        src.haptic = haptic
        src.enabled = enabled
        src.pressType = pressType
        src.releaseType = releaseType
        src.vibrateOnCancel = vibrateOnCancel
        src.cancelType = cancelType
    }

    return src
}

private class HapticMutableInteractionSource(
    private val delegate: MutableInteractionSource,
    var haptic: HapticFeedback,
    var enabled: Boolean,
    var pressType: HapticFeedbackType,
    var releaseType: HapticFeedbackType,
    var vibrateOnCancel: Boolean,
    var cancelType: HapticFeedbackType,
) : MutableInteractionSource {
    override val interactions: Flow<Interaction>
        get() = delegate.interactions

    override suspend fun emit(interaction: Interaction) {
        maybeHaptic(interaction)
        delegate.emit(interaction)
    }

    override fun tryEmit(interaction: Interaction): Boolean {
        maybeHaptic(interaction)
        return delegate.tryEmit(interaction)
    }

    private fun maybeHaptic(interaction: Interaction) {
        if (!enabled) return
        when (interaction) {
            is PressInteraction.Press -> haptic.performHapticFeedback(pressType)
            is PressInteraction.Release -> haptic.performHapticFeedback(releaseType)
            is PressInteraction.Cancel ->
                if (vibrateOnCancel) {
                    haptic.performHapticFeedback(
                        cancelType,
                    )
                }
        }
    }
}
