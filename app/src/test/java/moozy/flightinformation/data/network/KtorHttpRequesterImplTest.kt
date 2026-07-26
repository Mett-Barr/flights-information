package moozy.flightinformation.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import moozy.flightinformation.domain.error.LoadError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * 這一層唯一的職責是「把 HTTP 的失敗方式收斂成 domain 認得的 [LoadError]」。
 * 只有 data 層認識 Ktor 的例外型別，所以分類必須在這裡完成——讓 ViewModel 去
 * 判斷 Throwable，等於要 presentation 認識 Ktor。
 */
class KtorHttpRequesterImplTest {

    @Serializable
    private data class Payload(val ok: Boolean)

    private val json = Json { ignoreUnknownKeys = true }

    private fun requesterThat(
        handler: MockRequestHandleScope.() -> HttpResponseData,
    ): KtorHttpRequesterImpl =
        KtorHttpRequesterImpl(HttpClient(MockEngine { handler() }), json)

    private suspend fun KtorHttpRequesterImpl.get(): Result<Payload> =
        request(Payload.serializer()) {
            method = HttpMethod.Get
            url("https://example.test/payload")
        }

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun `returns the parsed body on success`() = runTest {
        val requester = requesterThat { respond("""{"ok":true}""", HttpStatusCode.OK, jsonHeaders) }

        assertEquals(Payload(ok = true), requester.get().getOrThrow())
    }

    @Test
    fun `treats a 4xx as a failure even when the body parses`() = runTest {
        // 這是舊實作漏掉的情境：狀態碼沒被檢查，而 body 又剛好是合法 JSON，
        // 於是錯誤回應被當成資料，畫面顯示空清單而不是錯誤。
        val requester = requesterThat { respond("""{"ok":false}""", HttpStatusCode.Unauthorized, jsonHeaders) }

        val error = requester.get().exceptionOrNull()
        assertTrue(error is LoadError.Server)
        assertEquals(401, (error as LoadError.Server).code)
    }

    @Test
    fun `treats a 5xx as a server failure`() = runTest {
        val requester = requesterThat {
            respond("upstream unavailable", HttpStatusCode.ServiceUnavailable, jsonHeaders)
        }

        val error = requester.get().exceptionOrNull()
        assertTrue(error is LoadError.Server)
        assertEquals(503, (error as LoadError.Server).code)
    }

    @Test
    fun `reports connectivity problems as no network`() = runTest {
        val requester = requesterThat {
            throw IOException("Unable to resolve host")
        }

        assertTrue(requester.get().exceptionOrNull() is LoadError.NoNetwork)
    }

    @Test
    fun `reports an unparseable body as a malformed response`() = runTest {
        val requester = requesterThat { respond("not json at all", HttpStatusCode.OK, jsonHeaders) }

        assertTrue(requester.get().exceptionOrNull() is LoadError.Malformed)
    }

    @Test
    fun `never leaks a raw ktor or serialization exception`() = runTest {
        // domain 只認得 LoadError；任何漏出去的原始例外都會變成使用者看到的
        // 'Unable to resolve host ...' 這類訊息。
        listOf<MockRequestHandleScope.() -> HttpResponseData>(
            { respond("""{"ok":false}""", HttpStatusCode.BadRequest, jsonHeaders) },
            { respond("<html>oops</html>", HttpStatusCode.OK, jsonHeaders) },
            { throw IOException("offline") },
        ).forEach { handler ->
            val error = requesterThat(handler).get().exceptionOrNull()
            assertTrue("$error 不是 LoadError", error is LoadError)
        }
    }
}
