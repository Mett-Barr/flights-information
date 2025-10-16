import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import moozy.flightinformation.presentation.component.RotatableScaffold
import moozy.flightinformation.presentation.navigation.NavRoute
import moozy.flightinformation.presentation.screen.CurrencyScreen
import moozy.flightinformation.presentation.screen.FlightsScreen
import moozy.flightinformation.presentation.viewmodel.CurrencyViewModel
import moozy.flightinformation.presentation.viewmodel.FlightsViewModel


@Composable
fun AppNavDisplay(modifier: Modifier = Modifier) {
    var root by rememberSaveable {
        mutableStateOf(NavRoute.Currency)
    }
    // 先放這邊管，原本想用 Nav3 但是太不適合只好自己控導航
    val flightsViewModel = hiltViewModel<FlightsViewModel>()
    val currencyViewModel = hiltViewModel<CurrencyViewModel>()

    RotatableScaffold(
        navigationSuiteItems = {
            navRouteItem(
                currentRoot = root,
                onClick = {
                    root = it
                }
            )
        },

    ) { innerPadding ->
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
                            onScreenVisible = flightsViewModel::enableAutoRefresh,
                            onScreenHidden = flightsViewModel::disableAutoRefresh,
                            refreshEvent = flightsViewModel.refreshEvent,
                            innerPadding = innerPadding
                        )
                    }

                    NavRoute.Currency -> {
                        val state by currencyViewModel.state.collectAsStateWithLifecycle()
                        CurrencyScreen(
                            state = state,
                            onRefresh = currencyViewModel::getCurrencies,
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