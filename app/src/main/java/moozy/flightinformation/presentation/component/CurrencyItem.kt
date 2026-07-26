@file:OptIn(ExperimentalSharedTransitionApi::class)

package moozy.flightinformation.presentation.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import moozy.flightinformation.presentation.model.currency.CurrencyRow
import moozy.flightinformation.presentation.model.currency.CurrencyRowPlain
import moozy.flightinformation.presentation.model.currency.CurrencyRowWithConversion
import java.math.BigDecimal

@Composable
fun CurrencyRateItem(
    plain: CurrencyRowPlain,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val symbol: String = plain.symbol
    val code: String = plain.code
    val rate: String = plain.rate.toString()
    with(sharedTransitionScope) {
        Row(
            modifier = modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = symbol,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = code,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.sharedElement(
                    sharedContentState = rememberSharedContentState(key = "code"),
                    animatedVisibilityScope = animatedVisibilityScope
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = rate,
                modifier = Modifier
                    .sharedElement(
                        sharedContentState = rememberSharedContentState(key = "rate"),
                        animatedVisibilityScope = animatedVisibilityScope
                    ),
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
fun CurrencyConversionItem(
    plain: CurrencyRowWithConversion,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val symbol: String = plain.symbol
    val code: String = plain.code
    val baseAmount: String = plain.baseAmount.toString()
    val baseCode: String = plain.baseCode
    val rate: String = plain.rate.toString()
    val conversion = plain.convertedAmount.toPlainString()
    with(sharedTransitionScope) {
        Row(
            modifier = modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = symbol,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.sharedElement(
                        sharedContentState = rememberSharedContentState(key = "code"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                )
                Row {
                    Text(
                        text = "1 $baseCode = ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = rate,
                        modifier = Modifier
                            .sharedElement(
                                sharedContentState = rememberSharedContentState(key = "rate"),
                                animatedVisibilityScope = animatedVisibilityScope
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " $code",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = conversion,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun CurrencyItemSwitchPreview() {
    MaterialTheme {
        var row by remember {
            mutableStateOf<CurrencyRow>(
                CurrencyRowPlain(
                    code = "AUD",
                    name = "Australian Dollar",
                    symbol = "$",
                    rate = BigDecimal("1.50")  // 例：對某基準的匯率
                )
            )
        }

        Column(Modifier.padding(24.dp)) {
            LazyColumn {
                items(3) {
                    SharedTransitionLayout {
                        val isPlain = row is CurrencyRowPlain
                        AnimatedContent(isPlain) {
                            it
                            when (row) {
                                is CurrencyRowPlain -> CurrencyRateItem(
                                    plain = row as CurrencyRowPlain,
                                    onClick = {
                                    },
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this,
                                )

                                is CurrencyRowWithConversion -> CurrencyConversionItem(
                                    plain = row as CurrencyRowWithConversion,
                                    onClick = {
                                    },
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row {
                Button(
                    onClick = {
                        row = when (val r = row) {
                            is CurrencyRowWithConversion -> r.copy(
                                // 簡單疊加以觀察動畫與數值變化
                                convertedAmount = r.convertedAmount + BigDecimal("0.5"),
                                rate = r.rate + BigDecimal("0.1"),
                                baseAmount = r.baseAmount  // 不變
                            )

                            is CurrencyRowPlain -> r.copy(
                                rate = r.rate + BigDecimal("0.1")
                            )
                        }
                    }
                ) { Text("++") }

                Spacer(Modifier.width(12.dp))

                Button(
                    onClick = {
                        row = when (val r = row) {
                            is CurrencyRowWithConversion -> CurrencyRowPlain(
                                code = r.code,
                                name = r.name,
                                symbol = r.symbol,
                                rate = r.rate
                            )

                            is CurrencyRowPlain -> {
                                // 切到有轉換版；為了示範，使用「自身為基底」：converted = baseAmount
                                val baseAmt = BigDecimal("12")
                                CurrencyRowWithConversion(
                                    code = r.code,
                                    name = r.name,
                                    symbol = r.symbol,
                                    rate = r.rate,
                                    convertedAmount = baseAmt,   // base=target 時等於 baseAmount
                                    baseAmount = baseAmt,
                                    baseCode = r.code
                                )
                            }
                        }
                    }
                ) { Text("Switch") }
            }
        }
    }
}
