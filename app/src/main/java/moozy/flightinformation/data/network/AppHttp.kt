package moozy.flightinformation.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import moozy.flightinformation.BuildConfig
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder


object AppHttp {
    private const val REQUEST_TIMEOUT_MS: Long = 15_000
    private const val CONNECT_TIMEOUT_MS: Long = 10_000
    private const val SOCKET_TIMEOUT_MS: Long = 15_000
    private val logLevel: LogLevel = LogLevel.BODY

    // engine 也用純賦值（正式 OkHttp；測試時直接換 MockEngine）
    private val engine: HttpClientEngine = OkHttp.create()

    // 共用 JSON（可改設定）
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        isLenient = true
    }

    // —— 全域唯一 HttpClient（純賦值建立；要重建就直接重新賦值） ——
    val client: HttpClient = HttpClient(engine) {
        install(ContentNegotiation) { json(json) }

        install(Logging) {
            // DEBUG 全開，Release 降級
            level = if (BuildConfig.DEBUG) LogLevel.ALL else LogLevel.INFO

            // Android 直接輸出到 Logcat（Ktor 內建）
            logger = Logger.Companion.ANDROID

            // 避免外洩敏感資訊
            sanitizeHeader {
                it.equals(
                    HttpHeaders.Authorization,
                    ignoreCase = true
                ) || it.equals("X-Api-Key", true)
            }

            //（選）只記錄特定網域
            filter { request -> request.url.host.contains("api.example.com") }
        }

        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            socketTimeoutMillis = SOCKET_TIMEOUT_MS
        }

        expectSuccess = false
        HttpResponseValidator {
            validateResponse { rsp: HttpResponse ->
                if (!rsp.status.isSuccess()) throw ClientRequestException(rsp, "HTTP ${rsp.status}")
            }
        }
    }

    suspend inline fun <reified T> requestResult(
        noinline build: HttpRequestBuilder.() -> Unit
    ): Result<T> = runCatching {
        val rsp = client.request(build)
        rsp.body<T>()
    }
}