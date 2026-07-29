package moozy.flightinformation.data.network

import io.ktor.http.URLBuilder
import io.ktor.http.encodedPath
import io.ktor.http.takeFrom

interface Endpoint {
    val origin: String
    val path: String
}

fun URLBuilder.applyEndpoint(
    endpoint: Endpoint
) {
    takeFrom(endpoint.origin)          // 帶 scheme/host/(port)
    encodedPath = endpoint.path     // 若 origin 含固定路徑，改成安全追加
}
