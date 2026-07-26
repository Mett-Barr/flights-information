import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScope
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import moozy.flightinformation.domain.value.CurrencyCode
import moozy.flightinformation.presentation.component.RotatableScaffold
import moozy.flightinformation.presentation.navigation.NavRoute
import moozy.flightinformation.presentation.screen.CurrencyScreen
import moozy.flightinformation.presentation.screen.FlightsScreen
import moozy.flightinformation.presentation.viewmodel.CurrencyViewModel
import moozy.flightinformation.presentation.viewmodel.FlightsViewModel


@Composable
fun AppNavDisplay(modifier: Modifier = Modifier) {
    var root by rememberSaveable {
        mutableStateOf(NavRoute.Flights)
    }
    // 先放這邊管，原本想用 Nav3 但是太不適合只好自己控導航
    val flightsViewModel = hiltViewModel<FlightsViewModel>()
    val currencyViewModel = hiltViewModel<CurrencyViewModel>()
    val scaffoldState = rememberNavigationSuiteScaffoldState()

    RotatableScaffold(
        navigationSuiteItems = {
            navRouteItem(
                currentRoot = root,
                onClick = {
                    root = it
                }
            )
        },
        state = scaffoldState,
    ) { innerPadding ->
        val coroutineScope = rememberCoroutineScope()

        // 確保切換頁面狀態可保持
        rememberSaveableStateHolder().SaveableStateProvider(root) {
            AnimatedContent(
                root,
                modifier,
                transitionSpec = { fadeIn().togetherWith(fadeOut()) }
            ) {
                when (it) {
                    NavRoute.Flights -> {
                        val state by flightsViewModel.state.collectAsStateWithLifecycle()
                        FlightsScreen(
                            flightArrivalsUiState = state,
                            modifier = modifier,
                            onRefresh = flightsViewModel::refresh,
                            refreshEvent = flightsViewModel.refreshEvent,
                            innerPadding = innerPadding
                        )
                    }

                    NavRoute.Currency -> {
                        LaunchedEffect(Unit) {
                            currencyViewModel.load()
                        }
                        val state by currencyViewModel.state.collectAsStateWithLifecycle()
                        CurrencyScreen(
                            state = state,
                            onCalculatorShow = {
                                coroutineScope.launch {
                                    scaffoldState.hide()
                                }
                            },
                            onCalculatorDismiss = {
                                coroutineScope.launch {
                                    scaffoldState.show()
                                }
                            },
                            onMoneyInput = currencyViewModel::inputMoney,
                            onCurrencyClick = currencyViewModel::onCurrencyClick,
                            onCurrencySelect = currencyViewModel::onCurrencySelect,
                            onSearch = currencyViewModel::getCurrencies,
                            onBaseCurrencySelect = currencyViewModel::onBaseCurrencySelect,
                            onRetry = currencyViewModel::load,
                            modifier = modifier,
                            innerPadding = innerPadding
                        )
                    }
                }
            }
        }
    }
}

private fun NavigationSuiteScope.navRouteItem(
    currentRoot: NavRoute,
    onClick: (NavRoute) -> Unit
) {
    NavRoute.entries.forEach {
        item(
            selected = currentRoot == it,
            onClick = { onClick(it) },
            icon = {
                Icon(
                    painterResource(it.iconId),
                    contentDescription = it.contentDescription
                )
            },
            label = {
                Text(it.label)
            }
        )
    }
}
