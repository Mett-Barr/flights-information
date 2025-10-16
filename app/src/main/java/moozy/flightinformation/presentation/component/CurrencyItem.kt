@file:OptIn(ExperimentalSharedTransitionApi::class)

package moozy.flightinformation.presentation.component

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moozy.flightinformation.presentation.model.currency.CurrencyRowPlain
import moozy.flightinformation.presentation.model.currency.CurrencyRowWithConversion

@Composable
fun CurrencyRateItem(
    plain: CurrencyRowPlain,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val symbol: String = plain.symbol
    val code: String = plain.code
    val rate: String = plain.rate.toString()
    with(sharedTransitionScope) {
        Row(
            modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
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
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = rate,

                modifier = Modifier
                    .sharedElement(
                        sharedContentState = rememberSharedContentState(key = "rate"),
                        animatedVisibilityScope = animatedVisibilityScope
                    ),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun CurrencyRateItemPreview() {
//    CurrencyRateItem(
//        symbol = "€",
//        code = "EUR",
//        rate = "0.93"
//    )
//}

@Composable
fun CurrencyConversionItem(
    plain: CurrencyRowWithConversion,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val symbol: String = plain.symbol
    val code: String = plain.code
    val baseAmount: String = plain.baseAmount.toString()
    val baseCode: String = plain.baseCode
    val rate: String = plain.rate.toString()
    val conversion: String = plain.convertedAmount.toString()
    with(sharedTransitionScope) {
        Row(
            modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
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
                    fontWeight = FontWeight.Bold
                )
                Row {
                    Text(
                        text = "$baseAmount $baseCode = ",
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
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun CurrencyConversionItemPreview() {
//    CurrencyConversionItem(
//        symbol = "€",
//        code = "EUR",
//        baseAmount = "1",
//        baseCode = "USD",
//        rate = "0.93",
//        conversion = "93.00"
//    )
//}
