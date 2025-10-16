package moozy.flightinformation.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import moozy.flightinformation.domain.value.CurrencyCode
import moozy.flightinformation.presentation.component.CurrencyConversionItem
import moozy.flightinformation.presentation.component.CurrencyRateItem
import moozy.flightinformation.presentation.model.currency.CurrencyRowPlain
import moozy.flightinformation.presentation.model.currency.CurrencyRowWithConversion
import moozy.flightinformation.presentation.state.currency.CurrencyUiState

@Composable
fun CurrencyScreen(
    state: CurrencyUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues = PaddingValues(0.dp)
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (val currentState = state) {
            is CurrencyUiState.Error -> CurrencyError(Modifier.fillMaxSize())
            CurrencyUiState.Loading -> CurrencyLoading()
            is CurrencyUiState.Content -> CurrencySuccess(
                state = currentState,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun CurrencyError(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text("Error")
    }
}

@Composable
fun CurrencyLoading() {
    CircularProgressIndicator()
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CurrencySuccess(
    state: CurrencyUiState.Content,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {

//    PullToRefreshBox(
//        state.isRefreshing,
//        onRefresh = onRefresh
//    ) { }
    Box(modifier) {
        var showDialog by remember { mutableStateOf(false) }
        LazyColumn {
            items(state.rows) { currencyRow ->
                SharedTransitionLayout {
                    AnimatedContent(currencyRow) { row ->
                        when (val row = row) {
                            is CurrencyRowPlain -> CurrencyRateItem(
                                row,
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this,
                            )
                            is CurrencyRowWithConversion -> CurrencyConversionItem(
                                plain = row,
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this,
                            )
                        }
                    }
                }
            }
        }

//        Column {
//            state.rows.forEach {
//                Row(
//                    horizontalArrangement = Arrangement.spacedBy(8.dp),
//                ) {
//                    Text(it.code)
//                    Text(it.rateText)
//                }
//            }
//        }

        FloatingActionButton({}, modifier = Modifier.align(Alignment.BottomEnd)) {

        }

        if (showDialog) {
            Dialog(
                { showDialog = false }
            ) {
                LazyColumn {
                    items(CurrencyCode.entries) {
                        Text(it.code)
                        Text(it.name)
                    }
                }
            }
        }
    }
}