package moozy.flightinformation.presentation.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import moozy.flightinformation.domain.value.CurrencyCode
import moozy.flightinformation.presentation.state.currency.CurrencyModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CurrencyView(
    currency: CurrencyModel,
    modifier: Modifier = Modifier
) {
    SharedTransitionLayout {
        Card {
            AnimatedContent(targetState = currency, label = "currency") { targetCurrency ->
                when (targetCurrency) {
                    is CurrencyModel.Rate -> {
                        Row(
                            modifier = modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) {
                            Text(
                                text = targetCurrency.currencyCode.code,
                                modifier = Modifier
                                    .sharedElement(
                                        sharedContentState = rememberSharedContentState(key = "code/${targetCurrency.currencyCode.code}"),
                                        animatedVisibilityScope = this@AnimatedContent
                                    )
                            )
                            Text(
                                text = targetCurrency.rate,
                                modifier = Modifier
                                    .sharedElement(
                                        sharedContentState = rememberSharedContentState(key = "rate/${targetCurrency.currencyCode.code}"),
                                        animatedVisibilityScope = this@AnimatedContent
                                    )
                            )
                        }
                    }

                    is CurrencyModel.RateWithBase -> {
                        Row(
                            modifier = modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) {
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = targetCurrency.currencyCode.code,
                                modifier = Modifier
                                    .sharedElement(
                                        sharedContentState = rememberSharedContentState(key = "code/${targetCurrency.currencyCode.code}"),
                                        animatedVisibilityScope = this@AnimatedContent
                                    )
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = targetCurrency.rate,
                                modifier = Modifier
                                    .sharedElement(
                                        sharedContentState = rememberSharedContentState(key = "rate/${targetCurrency.currencyCode.code}"),
                                        animatedVisibilityScope = this@AnimatedContent
                                    )
                            )
                            Text(text = targetCurrency.conversion)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
fun CurrencyModelRateViewPreview() {
    var currency by remember {
        mutableStateOf(CurrencyModel.Rate(CurrencyCode.AUD, "1.5"))
    }
    CurrencyView(currency = currency)
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
fun CurrencyModelRateWithBaseViewPreview() {
    var currency by remember {
        mutableStateOf(CurrencyModel.RateWithBase(CurrencyCode.AUD, "1.5", "15000"))
    }
    CurrencyView(currency = currency)
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
fun CurrencyViewSwitchPreview() {
    var currency by remember {
        mutableStateOf<CurrencyModel>(CurrencyModel.Rate(CurrencyCode.AUD, "1.5"))
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
            currency = CurrencyModel.RateWithBase(CurrencyCode.AUD, "1.5", "15000")
            delay(1500)
            currency = CurrencyModel.Rate(CurrencyCode.AUD, "1.5")
        }
    }

    CurrencyView(currency = currency)
}
