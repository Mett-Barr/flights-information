package moozy.flightinformation.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldState
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScope
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

/**
 * App 的外殼：依視窗尺寸級距挑導覽元件，並把內容該自己補的 inset 算出來往下傳。
 *
 * 導覽元件的對照表就是 Material 的斷點表，唯一的差別是刻意不看長寬比，只看寬度——
 * 手機轉橫向時寬度跨過 600dp 自然變成 Rail，不需要另外一條「橫向」規則。
 */
@Composable
fun RotatableScaffold(
    navigationSuiteItems: NavigationSuiteScope.() -> Unit,
    state: NavigationSuiteScaffoldState = rememberNavigationSuiteScaffoldState(),
    content: @Composable (innerPadding: PaddingValues) -> Unit
) {
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val windowSizeClass = adaptiveInfo.windowSizeClass

    // Compact（<600dp）使用底部導覽列；600dp 以上使用窄側邊 Rail。
    // 桌上型姿態仍維持底部導覽列，避免側邊導覽元件壓在橫向鉸鏈上。
    // NavigationDrawer 在此應用只有兩個目的地時過寬，且會浪費橫向內容空間。
    val layoutType = if (
        !adaptiveInfo.windowPosture.isTabletop &&
            windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    ) {
        NavigationSuiteType.NavigationRail
    } else {
        NavigationSuiteType.NavigationBar
    }

    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current

    // NavigationSuiteScaffold 只替內容消費「導覽元件佔住的那一邊」：底部列消費 bottom，
    // Rail 與抽屜消費 start。它沒有 innerPadding 可拿，其餘幾邊得自己算出來往下傳，
    // 否則 edge-to-edge 之下內容會壓在狀態列或手勢區底下。
    val navigationAtBottom = layoutType == NavigationSuiteType.NavigationBar

    val innerPadding = PaddingValues(
        start = if (navigationAtBottom) insets.calculateStartPadding(layoutDirection) else 0.dp,
        top = insets.calculateTopPadding(),
        end = insets.calculateEndPadding(layoutDirection),
        bottom = if (navigationAtBottom) 0.dp else insets.calculateBottomPadding()
    )

    NavigationSuiteScaffold(
        layoutType = layoutType,
        navigationSuiteItems = navigationSuiteItems,
        state = state,
        content = { content(innerPadding) }
    )
}
