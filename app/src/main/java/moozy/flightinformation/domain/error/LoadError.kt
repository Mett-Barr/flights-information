package moozy.flightinformation.domain.error

/**
 * 載入資料可能的失敗方式，用業務語彙表達。
 *
 * 定義在 domain，分類工作在 data 層完成：只有 data 層認識 Ktor 的例外型別，
 * 若讓 ViewModel 去判斷 `Throwable`，presentation 就得認識 Ktor——與 DTO 外洩
 * 是同一種層級洩漏。
 *
 * 繼承 [Exception] 只是為了能放進 `Result.failure`，不代表它是實作細節；
 * 對外它就是一個封閉的、可窮舉的錯誤集合。
 */
sealed class LoadError(message: String) : Exception(message) {

    /** 連線不到對方：沒網路、DNS 解析失敗、連線被拒。 */
    data object NoNetwork : LoadError("No network connection") {
        private fun readResolve(): Any = NoNetwork
    }

    /** 對方沒有在時限內回應。 */
    data object Timeout : LoadError("The request timed out") {
        private fun readResolve(): Any = Timeout
    }

    /** 對方回了非成功狀態碼。保留 [code] 以便記錄與除錯。 */
    data class Server(val code: Int) : LoadError("Server responded with $code")

    /** 有回應但格式不符預期，通常代表 API 換了形狀。 */
    data class Malformed(val reason: String?) : LoadError("Malformed response: $reason")

    /** 其餘未分類的失敗。 */
    data class Unknown(val reason: String?) : LoadError("Unknown error: $reason")
}
