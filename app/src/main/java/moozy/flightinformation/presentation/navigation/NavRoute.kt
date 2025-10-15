package moozy.flightinformation.presentation.navigation

import androidx.annotation.DrawableRes
import moozy.flightinformation.R

enum class NavRoute(
    @param:DrawableRes val iconId: Int,
    val label: String,
    val contentDescription: String
) {
    Flights(
        iconId = R.drawable.ic_flight_land_24px,
        label = "Flights",
        contentDescription = "Flights"
    ),
    Currency(
        iconId = R.drawable.ic_paid_24px,
        label = "Currency",
        contentDescription = "Currency"
    )
}