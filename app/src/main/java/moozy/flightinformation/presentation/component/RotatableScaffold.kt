package moozy.flightinformation.presentation.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScope
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp

@Composable
fun RotatableScaffold(
    navigationSuiteItems: NavigationSuiteScope.() -> Unit,
    content: @Composable (innerPadding: PaddingValues) -> Unit
) {
    val wsc = currentWindowAdaptiveInfo().windowSizeClass
    val isLandscapeAspect = wsc.minWidthDp > wsc.minHeightDp

    // 作為保護：如果寬太窄／高太矮，不強制用 Rail，即使比率是橫向
    val safeForRail = wsc.isWidthAtLeastBreakpoint(600)  // 給個 600dp 為基準

    val layoutType = if (isLandscapeAspect && safeForRail) {
        NavigationSuiteType.NavigationRail
    } else {
        NavigationSuiteType.NavigationBar
    }

    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current

    // NavigationSuiteScaffold 沒 innerPadding 只能自己刻
    val innerPadding = PaddingValues(
        start = if (layoutType == NavigationSuiteType.NavigationBar) insets.calculateStartPadding(
            layoutDirection
        ) else 0.dp,
        top = insets.calculateTopPadding(),
        end = insets.calculateEndPadding(layoutDirection),
        bottom = if (layoutType == NavigationSuiteType.NavigationBar) 0.dp else insets.calculateBottomPadding()
    )

    NavigationSuiteScaffold(
        layoutType = layoutType, // 拿掉這行就交給官方預設自動切換
        navigationSuiteItems = navigationSuiteItems,
        content = { content(innerPadding) }
    )
}
