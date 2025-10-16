package moozy.flightinformation.presentation.screen

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import moozy.flightinformation.domain.value.CurrencyCode
import moozy.flightinformation.feature.calculator.Calculator
import moozy.flightinformation.presentation.component.BUTTON_FONT_SIZE
import moozy.flightinformation.presentation.component.CalculatorKeyboard
import moozy.flightinformation.presentation.component.CurrencyConversionItem
import moozy.flightinformation.presentation.component.CurrencyRateItem
import moozy.flightinformation.presentation.model.currency.CurrencyRowPlain
import moozy.flightinformation.presentation.model.currency.CurrencyRowWithConversion
import moozy.flightinformation.presentation.state.currency.CurrencyUiState

@Composable
fun CurrencyScreen(
    state: CurrencyUiState,
    onRefresh: () -> Unit,
    onCalculatorShow: () -> Unit,
    onCalculatorDismiss: () -> Unit,
    onMoneyInput: (
        content: CurrencyUiState.Content,                 // ① 當前 Content（必填）
        chosenBase: CurrencyCode?,                        // ② 當前選中的 Currency（可為 null；需存在於 rows）
        amountText: String?,
    ) -> Unit,
    onCurrencyClick: (String) -> Unit,
    onCurrencySelect: (CurrencyCode) -> Unit,
    onSearch: () -> Unit,
    onBaseCurrencySelect: (CurrencyCode) -> Unit,
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues = PaddingValues(0.dp)
) {
    when (val currentState = state) {
        is CurrencyUiState.Error -> CurrencyError(modifier)
        CurrencyUiState.Loading -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CurrencyLoading()
            }
        }

        is CurrencyUiState.Content -> CurrencySuccess(
            state = currentState,
            onRefresh = onRefresh,
            onCalculatorShow = onCalculatorShow,
            onCalculatorDismiss = onCalculatorDismiss,
            onMoneyInput = onMoneyInput,
            onCurrencyClick = onCurrencyClick,
            onCurrencySelect = onCurrencySelect,
            onSearch = onSearch,
            onBaseCurrencySelect = onBaseCurrencySelect,
            modifier = Modifier.fillMaxSize(),
            innerPadding = innerPadding
        )
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

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CurrencySuccess(
    state: CurrencyUiState.Content,
    onRefresh: () -> Unit,
    onCalculatorShow: () -> Unit,
    onCalculatorDismiss: () -> Unit,
    onMoneyInput: (
        content: CurrencyUiState.Content,                 // ① 當前 Content（必填）
        chosenBase: CurrencyCode?,                        // ② 當前選中的 Currency（可為 null；需存在於 rows）
        amountText: String?,
    ) -> Unit,
    onCurrencyClick: (String) -> Unit,
    onCurrencySelect: (CurrencyCode) -> Unit,
    onBaseCurrencySelect: (CurrencyCode) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues
) {
    val state by rememberUpdatedState(state)
    val calculator = remember { Calculator() }
    var showCalculator by remember { mutableStateOf(false) }
    var chosenBase by remember { mutableStateOf<CurrencyCode?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        snapshotFlow { calculator.equal }.collect {
            if (it != null) {
                Log.d("!!!", "calculator.equal $it")
                onMoneyInput(state, chosenBase, it.toString())
            }
        }
    }

    Box(modifier) {
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = innerPadding,
                state = lazyListState
            ) {
                items(state.rows, key = { it.code }) { currencyRow ->
                    SharedTransitionLayout {
                        AnimatedContent(
                            currencyRow is CurrencyRowPlain
                        ) { it ->
                            it
                            when (val row = currencyRow) {
                                is CurrencyRowPlain -> CurrencyRateItem(
                                    plain = row,
                                    onClick = {
                                        chosenBase =
                                            CurrencyCode.entries.find { it.code == row.code }
                                        onCalculatorShow()
                                        showCalculator = true
                                        onCurrencyClick(row.code)
                                        coroutineScope.launch {
                                            lazyListState.animateScrollToItem(0)
                                        }
                                    },
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this,
                                    modifier = Modifier.animateItem()
                                )

                                is CurrencyRowWithConversion -> CurrencyConversionItem(
                                    plain = row,
                                    onClick = {
                                        chosenBase =
                                            CurrencyCode.entries.find { it.code == row.code }
                                        onCalculatorShow()
                                        showCalculator = true
                                        onCurrencyClick(row.code)
                                        coroutineScope.launch {
                                            lazyListState.animateScrollToItem(0)
                                        }
                                    },
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this,
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(showCalculator) {
                Column {
                    val infixString by remember {
                        derivedStateOf {
                            calculator.infixString.takeIf { it.isNotBlank() } ?: "0"
                        }
                    }
                    AnimatedVisibility(!infixString.isBlank()) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = infixString,
                                fontSize = BUTTON_FONT_SIZE
                            )
                        }
                    }
                    CalculatorKeyboard(
                        calculator,
                        Modifier.padding(bottom = 24.dp) // 直接 hardcode 了，太累了
                    )
                }
            }

            // 太累了，直接寫命令式了
            BackHandler(showCalculator) {
                showCalculator = false
                onCalculatorDismiss()
            }
        }

        AnimatedVisibility(
            !showCalculator,
            enter = scaleIn(),
            exit = scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            ExtendedFloatingActionButton(
                { showDialog = true }
            ) {
                Text("Search")
            }
        }

        if (showDialog) {
            Dialog(
                { showDialog = false }
            ) {
                Card {
                    var isStage1 by remember {
                        mutableStateOf(true)
                    }
                    Column(modifier = Modifier.padding(16.dp)) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(CurrencyCode.entries) { currencyCode ->
                                ToggleButton(
                                    checked = if (isStage1) state.selected.any { it.code == currencyCode.code } else state.selectedBaseCurrency == currencyCode,
                                    onCheckedChange = {
                                        if (isStage1) {
                                            onCurrencySelect(currencyCode)
                                        } else {
                                            onBaseCurrencySelect(currencyCode)
                                        }
                                    }
                                ) {
                                    Text(currencyCode.code)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (isStage1) {
                                    isStage1 = false
                                } else {
                                    onSearch()
                                    showDialog = false
                                }
                            },
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .fillMaxWidth()
                        ) {
                            AnimatedContent(isStage1) {
                                Text(if (it) "select targets" else "search")
                            }
                        }
                    }
                }
            }
        }
    }
}