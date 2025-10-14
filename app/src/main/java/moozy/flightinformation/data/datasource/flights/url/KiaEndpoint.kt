package moozy.flightinformation.data.datasource.flights.url

private const val KIA_BASE = "https://www.kia.gov.tw"

enum class KiaEndpoint(private val path: String) {
    INSTANT_DOM_ARR("/Announce/NewsArea/InstantSchedule_DOMARR.json");

    init { require(path.startsWith('/')) }
    val url: String = KIA_BASE + path   // 這時就能用「非 companion」常數做 eager 初始化
    fun urlFrom(host: String) = host.removeSuffix("/") + path
}