package moozy.flightinformation.presentation.screen

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import moozy.flightinformation.domain.value.CurrencyCode
import moozy.flightinformation.domain.error.LoadError
import moozy.flightinformation.R
import moozy.flightinformation.feature.calculator.Calculator
import moozy.flightinformation.presentation.component.BUTTON_FONT_SIZE
import moozy.flightinformation.presentation.component.CalculatorKeyboard
import moozy.flightinformation.presentation.component.CurrencyConversionItem
import moozy.flightinformation.presentation.component.CurrencyRateItem
import moozy.flightinformation.presentation.model.currency.CurrencyRowPlain
import moozy.flightinformation.presentation.model.currency.CurrencyRowWithConversion
import moozy.flightinformation.presentation.mapper.messageRes
import moozy.flightinformation.presentation.state.currency.CurrencyUiState

@Composable
fun CurrencyScreen(
    state: CurrencyUiState,
    onCalculatorShow: () -> Unit,
    onCalculatorDismiss: () -> Unit,
    onMoneyInput: (
        content: CurrencyUiState.Content,
        amountText: String?,
    ) -> Unit,
    onCurrencyClick: (String) -> Unit,
    onCurrencySelect: (CurrencyCode) -> Unit,
    onSearch: (CurrencyUiState.Content) -> Unit,
    onBaseCurrencySelect: (CurrencyCode) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues = PaddingValues(0.dp)
) {
    when (val currentState = state) {
        is CurrencyUiState.Error -> CurrencyError(
            error = currentState.error,
            onRetry = onRetry,
            modifier = modifier,
            innerPadding = innerPadding,
        )
        CurrencyUiState.Loading -> CurrencyLoading(modifier, innerPadding)

        is CurrencyUiState.Content -> CurrencyContent(
            state = currentState,
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
fun CurrencyError(
    error: LoadError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues = PaddingValues(0.dp),
) {
    Box(
        modifier = modifier.fillMaxSize().padding(innerPadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Text(stringResource(error.messageRes()))
            Button(onClick = onRetry, modifier = Modifier.testTag("currency_retry")) {
                Text(stringResource(R.string.action_retry))
            }
        }
    }
}

@Composable
fun CurrencyLoading(
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues = PaddingValues(0.dp),
) {
    Box(
        modifier = modifier.fillMaxSize().padding(innerPadding),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CurrencyContent(
    state: CurrencyUiState.Content,
    onCalculatorShow: () -> Unit,
    onCalculatorDismiss: () -> Unit,
    onMoneyInput: (
        content: CurrencyUiState.Content,
        amountText: String?,
    ) -> Unit,
    onCurrencyClick: (String) -> Unit,
    onCurrencySelect: (CurrencyCode) -> Unit,
    onBaseCurrencySelect: (CurrencyCode) -> Unit,
    onSearch: (CurrencyUiState.Content) -> Unit,
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues
) {
    val state by rememberUpdatedState(state)
    val calculator = remember { Calculator() }
    var showCalculator by rememberSaveable { mutableStateOf(false) }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        snapshotFlow { calculator.equal }.collect {
            if (it != null) {
                Log.d("!!!", "calculator.equal $it")
                onMoneyInput(state, it.toString())
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
                    var isStage1 by rememberSaveable {
                        mutableStateOf(true)
                    }
                    Column(modifier = Modifier.padding(16.dp)) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(CurrencyCode.entries) { currencyCode ->
                                FilterChip(
                                    selected = if (isStage1) {
                                        state.selected.any { it.code == currencyCode.code }
                                    } else {
                                        state.selectedBaseCurrency == currencyCode
                                    },
                                    onClick = {
                                        if (isStage1) {
                                            onCurrencySelect(currencyCode)
                                        } else {
                                            onBaseCurrencySelect(currencyCode)
                                        }
                                    },
                                    label = { Text(currencyCode.code) }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (isStage1) {
                                    isStage1 = false
                                } else {
                                    onSearch(state)
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

    if (state.isRefreshing) {
        Dialog({}) {
            CircularProgressIndicator()
        }
    }
}
