package moozy.flightinformation.presentation.mapper

import androidx.annotation.StringRes
import moozy.flightinformation.R
import moozy.flightinformation.domain.error.LoadError

/**
 * 錯誤 → 文案是**呈現**決策，所以留在 presentation：domain 只說「發生了哪一類失敗」，
 * 由誰、用什麼語言告訴使用者，是這一層的事。
 */
@StringRes
fun LoadError.messageRes(): Int = when (this) {
    LoadError.NoNetwork -> R.string.error_no_network
    LoadError.Timeout -> R.string.error_timeout
    is LoadError.Server -> R.string.error_server
    is LoadError.Malformed, is LoadError.Unknown -> R.string.error_generic
}
