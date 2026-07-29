package moozy.flightinformation.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import moozy.flightinformation.R

/**
 * 兩個 top-level 目的地。
 *
 * 一個型別同時扮演兩個角色：Nav3 back stack 裡的 [NavKey]，以及導覽列項目的描述來源
 * （圖示與文字）。分頁與目的地在這個 app 裡是一對一，拆成兩份只會多一張得同步維護的對照表。
 *
 * 每個目的地是 `data object` 而不是 enum entry：Nav3 在 Android 上以反射式的 NavKeySerializer
 * 還原 back stack——它讀 `value::class.java.name`，再 `Class.forName(...).kotlin.serializer()`。
 * 無狀態的 object 是這條路徑上最穩的形狀，也是官方文件示範的寫法；enum 只有在「所有 entry
 * 都沒有 class body」時才碰巧可行，是個不該依賴的巧合。
 */
sealed interface NavRoute : NavKey {

    @get:DrawableRes
    val iconId: Int

    @get:StringRes
    val labelResId: Int

    @get:StringRes
    val contentDescriptionResId: Int

    @Serializable
    data object Flights : NavRoute {
        override val iconId: Int get() = R.drawable.ic_flight_land_24px
        override val labelResId: Int get() = R.string.navigation_flights
        override val contentDescriptionResId: Int get() = R.string.navigation_flights
    }

    @Serializable
    data object Currency : NavRoute {
        override val iconId: Int get() = R.drawable.ic_paid_24px
        override val labelResId: Int get() = R.string.navigation_currency
        override val contentDescriptionResId: Int get() = R.string.navigation_currency
    }

    companion object {
        /** 導覽列的顯示順序。 */
        val entries: List<NavRoute> = listOf(Flights, Currency)

        /**
         * back stack 的底層。
         *
         * 任何分頁按返回都先回到它，回到它之後再按才離開 app——這是 Android 對 top-level
         * 目的地的建議行為。deep link 也一律以它為合成起點，所以從外部進來的畫面同樣有上一頁。
         */
        val start: NavRoute = Flights
    }
}
