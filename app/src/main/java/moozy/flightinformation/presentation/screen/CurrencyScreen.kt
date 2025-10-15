package moozy.flightinformation.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import moozy.flightinformation.presentation.state.currency.CurrencyUiState
import moozy.flightinformation.presentation.viewmodel.CurrencyViewModel

@Composable
fun CurrencyScreen(
    modifier: Modifier = Modifier,
    viewModel: CurrencyViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column {
        when(state) {
            is CurrencyUiState.Error -> Text("Error")
            CurrencyUiState.Loading -> CircularProgressIndicator()
            is CurrencyUiState.Success -> {
                (state as CurrencyUiState.Success).currencies.list.forEach {
                    Row {
                        Text(it.code)
                        Text(it.rate.toString())
                    }
                }
            }
        }
    }
}