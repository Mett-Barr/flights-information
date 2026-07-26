package moozy.flightinformation.presentation.screen

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.SharedFlow
import moozy.flightinformation.presentation.component.FlightArrivalCard
import moozy.flightinformation.presentation.state.flights.FlightArrivalItemUiModel
import moozy.flightinformation.domain.error.LoadError
import moozy.flightinformation.presentation.mapper.messageRes
import moozy.flightinformation.presentation.state.flights.FlightArrivalsUiState
import moozy.flightinformation.presentation.state.flights.fakeFlightArrivalItem
import moozy.flightinformation.presentation.theme.FlightInformationTheme

@Composable
fun FlightsScreen(
    flightArrivalsUiState: FlightArrivalsUiState,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
    refreshEvent: SharedFlow<Unit>? = null,
    innerPadding: PaddingValues = PaddingValues(0.dp)
) {
    // 不需要通知 ViewModel「畫面出現/離開」：收集 state 本身就是需求訊號，
    // WhileSubscribed 會據此決定要不要繼續抓資料。
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = flightArrivalsUiState,
            transitionSpec = { fadeIn().togetherWith(fadeOut()) },
            modifier = Modifier.fillMaxSize()
        ) {
            when (it) {
                is FlightArrivalsUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.wrapContentSize())
                    }
                }

                is FlightArrivalsUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(it.error.messageRes()),
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                is FlightArrivalsUiState.Content -> {
                    FlightsContent(it, refreshEvent, onRefresh, innerPadding)
                }
            }
        }
    }
}

@Composable
private fun FlightsContent(
    flightArrivalsUiState: FlightArrivalsUiState.Content,
    refreshEvent: SharedFlow<Unit>?,
    onRefresh: () -> Unit,
    innerPadding: PaddingValues
) {
    if (flightArrivalsUiState.items.isEmpty()) {
        Text(
            text = "No flight information available.",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
    } else {
        val isRefreshing = flightArrivalsUiState.isRefreshing
        val lazyListState = rememberLazyListState()
        val pullRefreshState = rememberPullToRefreshState()

        LaunchedEffect(Unit) {
            refreshEvent?.collect {
                lazyListState.animateScrollToItem(0)
            }
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = pullRefreshState,
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(flightArrivalsUiState.items) { item ->
                    FlightArrivalItem(item = item)
                }

                if (innerPadding.calculateBottomPadding() == 0.dp) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }

    }
}

@Composable
private fun FlightArrivalItem(
    item: FlightArrivalItemUiModel,
    modifier: Modifier = Modifier
) {
    FlightArrivalCard(item, modifier)
}

@Preview
@Composable
fun FlightArrivalItemPreview() {
    FlightArrivalItem(fakeFlightArrivalItem)
}


@Preview
@Composable
fun CoilSimpleTest(
    imageUrl: String = "https://example.com/china_airlines_logo.png"
//    imageUrl: String = "https://www.kia.gov.tw/images/ALL-square/B7.png"
) {
    val context = LocalContext.current
    // 方案 A：最簡 AsyncImage
    AsyncImage(
        model = imageUrl,
        contentDescription = "Test Image",
        modifier = Modifier.fillMaxSize(),
        onError = { errorState ->
            // 顯示錯誤時可以放 fallback UI
            Log.d("!!!", "errorState: $errorState")
        },
        onSuccess = { successState ->
            // 成功後可以拿到 image 檔案資訊
            Log.d("!!!", "successState: $successState")
        }
    )
}

@Preview(showBackground = true, name = "Empty List Preview")
@Composable
fun FlightScreenEmptyPreview() {
    FlightInformationTheme {
        val emptyUiState = FlightArrivalsUiState.Content(items = emptyList())
        FlightsScreen(flightArrivalsUiState = emptyUiState)
    }
}

@Preview(showBackground = true, name = "Error State Preview")
@Composable
fun FlightScreenErrorPreview() {
    FlightInformationTheme {
        val errorUiState = FlightArrivalsUiState.Error(LoadError.NoNetwork)
        FlightsScreen(flightArrivalsUiState = errorUiState)
    }
}

@Preview(showBackground = true, name = "Loading State Preview")
@Composable
fun FlightScreenLoadingPreview() {
    FlightInformationTheme {
        val loadingUiState = FlightArrivalsUiState.Loading
        FlightsScreen(flightArrivalsUiState = loadingUiState)
    }
}
