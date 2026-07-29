package moozy.flightinformation.feature.currency

import kotlinx.collections.immutable.PersistentSet

fun <T> PersistentSet<T>.toggle(elem: T): PersistentSet<T> =
    if (elem in this) this.removing(elem) else this.adding(elem)

