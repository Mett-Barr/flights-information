package moozy.flightinformation.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moozy.flightinformation.presentation.state.flights.FlightArrivalItemUiModel
import moozy.flightinformation.presentation.state.flights.fakeFlightArrivalItem

private object Dimens {
    val cornerRadius = 16.dp
    val elevation = 4.dp
    val logoWidth = 80.dp
    val badgeCornerRadius = 12.dp

    val spacingExtraSmall = 4.dp
    val spacingSmall = 8.dp
    val spacingMedium = 16.dp

    val paddingMedium = 12.dp
    val badgePaddingHorizontal = 12.dp
    val badgePaddingVertical = 6.dp
}

@Composable
fun FlightArrivalCard(
    item: FlightArrivalItemUiModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevation)
    ) {
        Column {
            // Top Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.Top
            ) {
                // Logo
                UrlImageWithShimmer(
                    url = item.airlineLogoUrl,
                    modifier = Modifier
                        .width(Dimens.logoWidth)
                        .fillMaxHeight()
                )

                // Info Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(Dimens.paddingMedium)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = item.expectedLabelText,
                            fontSize = 14.sp,
                        )
                        StatusBadge(text = item.badgeText, statusKey = item.statusKey)
                    }
                    Spacer(modifier = Modifier.height(Dimens.spacingExtraSmall))
                    Text(
                        text = item.headlineTimeText,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(Dimens.spacingSmall))
                    Text(
                        text = item.carrierLineText,
                        fontSize = 16.sp,
                    )
                }
            }

            // Divider
            HorizontalDivider(Modifier, DividerDefaults.Thickness)

            // Bottom Section
            Column(modifier = Modifier.padding(Dimens.paddingMedium)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    InfoColumn(title = "Departure", value = item.departureText, modifier = Modifier.weight(1f))
                    InfoColumn(title = "Boarding Gate", value = item.gateText, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(Dimens.spacingSmall))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    InfoColumn(title = "Aircraft", value = item.aircraftText, modifier = Modifier.weight(1f))
                    InfoColumn(title = "Flight Status", value = item.flightStatusText, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun InfoColumn(title: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = title,
            fontSize = 12.sp,
        )
        Spacer(modifier = Modifier.height(Dimens.spacingExtraSmall))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun StatusBadge(text: String, statusKey: String) {
    val isDarkTheme = isSystemInDarkTheme()
    val (backgroundColor, textColor) = when (statusKey.uppercase()) {
        "ON_TIME" -> if (isDarkTheme) Color(0xFF00363A) to Color(0xFFB1F0F7) else Color(0xFFE0F7FA) to Color(0xFF00838F)
        "DELAYED" -> if (isDarkTheme) Color(0xFF452700) to Color(0xFFFFDDB8) else Color(0xFFFFF3E0) to Color(0xFFE65100)
        "CANCELLED" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        "ARRIVED" -> if (isDarkTheme) Color(0xFF0A3818) to Color(0xFFB8F3B6) else Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "DEPARTED" -> if (isDarkTheme) Color(0xFF002B4D) to Color(0xFFD0E4FF) else Color(0xFFE3F2FD) to Color(0xFF0D47A1)
        "SCHEDULE_CHANGE" -> if (isDarkTheme) Color(0xFF3C006C) to Color(0xFFF3D8FF) else Color(0xFFF3E5F5) to Color(0xFF4A148C)
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(Dimens.badgeCornerRadius))
            .padding(horizontal = Dimens.badgePaddingHorizontal, vertical = Dimens.badgePaddingVertical),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(
    name = "467dp x 369dp",
    showBackground = true,
    backgroundColor = 0xFFF0F0F0,
    widthDp = 467,
    heightDp = 369
)
@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun FlightArrivalCardPreview() {
    Box(modifier = Modifier.padding(Dimens.paddingMedium)) {
        FlightArrivalCard(item = fakeFlightArrivalItem)
    }
}
