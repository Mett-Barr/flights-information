package moozy.flightinformation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import moozy.flightinformation.presentation.screen.FlightScreen
import moozy.flightinformation.presentation.theme.FlightInformationTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlightInformationTheme {
                val viewModel: MainViewModel = hiltViewModel()
                val uiState by viewModel.ui.collectAsStateWithLifecycle()

                FlightScreen(flightArrivalsUiState = uiState)
            }
        }
    }
}
