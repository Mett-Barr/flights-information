package moozy.flightinformation.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface NavRoute : NavKey

@Serializable
data object Home : NavRoute

@Serializable
data object Details : NavRoute
