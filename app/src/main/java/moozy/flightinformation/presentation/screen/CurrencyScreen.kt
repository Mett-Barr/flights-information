package moozy.flightinformation.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dagger.Lazy
import moozy.flightinformation.domain.model.currency.CurrencyCode
import moozy.flightinformation.presentation.state.currency.CurrencyUiState

@Composable
fun CurrencyScreen(
    state: CurrencyUiState,
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues = PaddingValues(0.dp)
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (val currentState = state) {
            is CurrencyUiState.Error -> CurrencyError(Modifier.fillMaxSize())
            CurrencyUiState.Loading -> CurrencyLoading()
            is CurrencyUiState.Success -> CurrencySuccess(currentState, Modifier.fillMaxSize())
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

@Composable
fun CurrencySuccess(
    state: CurrencyUiState.Success,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        var showDialog by remember { mutableStateOf(false) }


        Button({ showDialog = !showDialog }) {

        }

        Button({ showDialog = !showDialog }) {

        }

        Column {
            state.currencies.list.forEach {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(it.code)
                    Text(it.rate.toString())
                }
            }
        }

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