package moozy.flightinformation.util.number

fun String?.isNumber(): Boolean =
    this?.trim()?.takeIf { it.isNotEmpty() }?.let { runCatching { java.math.BigDecimal(it) }.isSuccess } ?: false