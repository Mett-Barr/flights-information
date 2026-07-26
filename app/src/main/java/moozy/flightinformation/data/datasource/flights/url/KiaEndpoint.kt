package moozy.flightinformation.data.datasource.flights.url

import moozy.flightinformation.data.network.Endpoint

private const val KIA_BASE = "https://www.kia.gov.tw"

enum class KiaEndpoint(override val path: String) : Endpoint {
    INSTANT_DOM_ARR("/Announce/NewsArea/InstantSchedule_DOMARR.json");

    init { require(path.startsWith('/')) }

    override val origin: String get() = KIA_BASE
}
