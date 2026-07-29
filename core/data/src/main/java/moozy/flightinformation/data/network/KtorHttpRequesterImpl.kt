package moozy.flightinformation.data.network

import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import moozy.flightinformation.domain.error.LoadError
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 這一層的職責是把 HTTP 的各種失敗方式，收斂成 domain 認得的 [LoadError]。
 *
 * 分類必須在此完成：只有 data 層認識 Ktor 的例外型別。若讓 ViewModel 去判斷
 * `Throwable`，presentation 就得認識 Ktor——與 DTO 外洩是同一種層級洩漏。
 */
@Singleton
class KtorHttpRequesterImpl @Inject constructor(
    private val client: HttpClient,
    private val json: Json
) : KtorHttpRequester {

    @Suppress("TooGenericExceptionCaught") // 將所有非取消失敗轉為 LoadError；取消例外在前一個 catch 立即重拋。
    override suspend fun <T> request(
        deserializer: DeserializationStrategy<T>,
        build: HttpRequestBuilder.() -> Unit
    ): Result<T> = try {
        val response: HttpResponse = client.request(build)
        // 狀態碼必須顯式檢查：錯誤回應也可能帶著合法 JSON（過期的 API key、
        // rate limit），若只靠「解析失敗」間接判斷，那些回應會被當成資料。
        if (!response.status.isSuccess()) {
            Result.failure(LoadError.Server(response.status.value))
        } else {
            Result.success(json.decodeFromString(deserializer, response.bodyAsText()))
        }
    } catch (cancellation: CancellationException) {
        throw cancellation // 取消不是失敗，必須讓它繼續往上傳
    } catch (error: Throwable) {
        Result.failure(error.toLoadError())
    }
}

private fun Throwable.toLoadError(): LoadError = when (this) {
    is LoadError -> this
    is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException ->
        LoadError.Timeout
    is SerializationException -> LoadError.Malformed(message)
    is IOException -> LoadError.NoNetwork
    else -> LoadError.Unknown(message)
}
