package moozy.flightinformation.navigation

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import moozy.flightinformation.component.RotatableScaffold
import moozy.flightinformation.feature.flights.FlightsScreen
import moozy.flightinformation.feature.currency.CurrencyScreen
import moozy.flightinformation.feature.currency.CurrencyViewModel
import moozy.flightinformation.feature.flights.FlightsViewModel

@Composable
fun AppNavDisplay(modifier: Modifier = Modifier) {
    // ViewModel 仍然由這一層取得，作用域是宿主 Activity 的 ViewModelStore。刻意「不」把它們
    // 搬進 NavEntry：兩個目的地共用同一份實例的語意要維持，換頁也不該重建。
    //
    // 決定輪詢生死的不是 ViewModel 活多久，而是「誰在收集 state」——所以收集寫在各自的
    // NavEntry 內容裡（見下方），離開目的地時收集者跟著內容一起消失。
    val flightsViewModel = hiltViewModel<FlightsViewModel>()
    val currencyViewModel = hiltViewModel<CurrencyViewModel>()

    // 真正的 back stack：能被返回鍵彈出、能參與預測返回、能跨設定變更與行程死亡還原。
    // 起始內容由啟動 intent 決定，所以 deep link 進來的是一條有上層路徑的堆疊。
    val initialKeys = rememberInitialBackStack()
    val backStack = rememberNavBackStack(*initialKeys)
    val currentRoute = backStack.currentTopLevel()

    val scaffoldState = rememberNavigationSuiteScaffoldState()

    // 安全網。計算機會把導覽套件藏起來，而返回鍵不看導覽套件在不在——使用者可以在藏著的
    // 狀態下按返回換頁，那會停在一個沒有導覽列的畫面上（正是先前那個 soft-lock 的形狀）。
    // 目的地一改變就把它叫回來，讓「藏起來」永遠只在原地生效。
    LaunchedEffect(currentRoute) { scaffoldState.show() }

    RotatableScaffold(
        navigationSuiteItems = {
            navRouteItem(
                currentRoute = currentRoute,
                onClick = { backStack.switchTopLevel(it) }
            )
        },
        state = scaffoldState,
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            modifier = modifier,
            // 等同於舊版的 rememberSaveableStateHolder().SaveableStateProvider(root)：
            // 每個目的地的 rememberSaveable 狀態以 contentKey 分槽保存，切走再切回還在。
            entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator<NavKey>()),
            // 分頁之間是橫向關係，沿用原本的淡入淡出；
            // predictivePopTransitionSpec 刻意不覆寫，讓系統手勢維持平台預設的預測返回動畫。
            transitionSpec = { fadeIn().togetherWith(fadeOut()) },
            popTransitionSpec = { fadeIn().togetherWith(fadeOut()) },
            entryProvider = entryProvider<NavKey> {
                entry<NavRoute.Flights> {
                    // 收集寫在 entry 裡，不是外層：離開這個目的地後 NavDisplay 不再組合這段內容，
                    // 收集者隨之消失，WhileSubscribed 才會如設計般停掉十秒一次的輪詢。
                    // 把這行往外搬到 AppNavDisplay 頂端，輪詢就再也不會停。
                    val state by flightsViewModel.state.collectAsStateWithLifecycle()
                    FlightsScreen(
                        flightArrivalsUiState = state,
                        onRefresh = flightsViewModel::refresh,
                        refreshEvent = flightsViewModel.refreshEvent,
                        modifier = modifier,
                        innerPadding = innerPadding
                    )
                }

                entry<NavRoute.Currency> {
                    LaunchedEffect(Unit) {
                        currencyViewModel.load()
                    }
                    val state by currencyViewModel.state.collectAsStateWithLifecycle()
                    CurrencyScreen(
                        state = state,
                        onMoneyInput = currencyViewModel::inputMoney,
                        onCurrencySelect = currencyViewModel::onCurrencySelect,
                        onSearch = currencyViewModel::getCurrencies,
                        onBaseCurrencySelect = currencyViewModel::onBaseCurrencySelect,
                        onRetry = currencyViewModel::load,
                        modifier = modifier,
                        innerPadding = innerPadding
                    )
                }
            }
        )
    }
}

private fun NavigationSuiteScope.navRouteItem(
    currentRoute: NavRoute,
    onClick: (NavRoute) -> Unit
) {
    NavRoute.entries.forEach {
        item(
            selected = currentRoute == it,
            onClick = { onClick(it) },
            icon = {
                Icon(
                    painterResource(it.iconId),
                    contentDescription = stringResource(it.contentDescriptionResId)
                )
            },
            label = {
                Text(stringResource(it.labelResId))
            }
        )
    }
}
