package moozy.flightinformation.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavKey

/** deep link 的 scheme，必須與 AndroidManifest 裡 MainActivity 的 intent-filter 一致。 */
const val DEEP_LINK_SCHEME: String = "flightinformation"

private const val HOST_FLIGHTS = "flights"
private const val HOST_CURRENCY = "currency"

/**
 * 讀啟動 Activity 的 intent，決定 back stack 的初始內容。
 *
 * 只在第一次組合時取值。之後的還原交給 `rememberNavBackStack`：設定變更與行程死亡要還原的是
 * 「使用者當下停在哪」，而不是重新套用一次當初的 deep link。
 *
 * MainActivity 用預設的 standard launchMode，外部 intent 會建立新的 Activity 實例，
 * 因此不必攔 onNewIntent 也收得到 deep link。
 */
@Composable
fun rememberInitialBackStack(): Array<NavKey> {
    val context = LocalContext.current
    return remember(context) { initialBackStack(context.findActivity()?.intent?.data) }
}

/**
 * 把 deep link 展開成「合成 back stack」，而不是單獨一個畫面。
 *
 * `flightinformation://currency` 會變成 `[Flights, Currency]`，所以從外部直接進到匯率頁時，
 * 返回鍵會先回到航班頁而不是立刻離開 app——deep link 進來的畫面也該有上層路徑可回。
 * 認不得的 URI（包含一般點圖示啟動的 null）一律回到起始頁。
 */
internal fun initialBackStack(uri: Uri?): Array<NavKey> {
    val target = uri?.let(::deepLinkTarget) ?: NavRoute.start
    return if (target == NavRoute.start) {
        arrayOf<NavKey>(NavRoute.start)
    } else {
        arrayOf<NavKey>(NavRoute.start, target)
    }
}

private fun deepLinkTarget(uri: Uri): NavRoute? {
    if (!DEEP_LINK_SCHEME.equals(uri.scheme, ignoreCase = true)) return null
    return when (uri.host?.lowercase()) {
        HOST_FLIGHTS -> NavRoute.Flights
        HOST_CURRENCY -> NavRoute.Currency
        else -> null
    }
}

/** LocalContext 不保證就是 Activity，可能被 ContextThemeWrapper 之類的包了好幾層。 */
private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
