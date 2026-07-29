package moozy.flightinformation.data.datasource.currency.url

import moozy.flightinformation.data.network.Endpoint

private const val FREE_CURRENCY_ORIGIN = "https://api.freecurrencyapi.com"

enum class FreeCurrencyEndpoint(override val path: String): Endpoint {
    LATEST("/v1/latest");

    init { require(path.startsWith('/')) }

    override val origin: String get() = FREE_CURRENCY_ORIGIN
}
