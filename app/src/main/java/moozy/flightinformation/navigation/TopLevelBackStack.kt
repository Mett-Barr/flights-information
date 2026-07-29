package moozy.flightinformation.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * 切換 top-level 目的地。
 *
 * 分頁彼此平行，不該互相堆疊，所以切換後的 back stack 只有兩種形狀：`[start]` 或
 * `[start, target]`。堆疊有界，連按分頁不會長出一串歷史；返回鍵的語意也由這個形狀決定——
 * 在任一分頁按返回都回到起始頁，回到起始頁再按才離開 app。
 *
 * 過程中刻意不讓堆疊空掉：`NavDisplay` 對空 back stack 會直接拋例外，
 * 而 `clear()` 後再 `add()` 只是「剛好」沒被觀察到，不值得依賴。
 */
fun NavBackStack<NavKey>.switchTopLevel(target: NavRoute) {
    if (lastOrNull() == target) return

    val start = NavRoute.start
    if (firstOrNull() != start) add(0, start)
    while (size > 1) removeAt(lastIndex)
    if (target != start) add(target)
}

/**
 * 當前顯示中的目的地。
 *
 * back stack 理論上只裝得下 [NavRoute]，這裡仍然退回 [NavRoute.start] 而不是強制轉型：
 * 導覽列的選取狀態不值得為了型別純度冒 crash 的風險。
 */
fun NavBackStack<NavKey>.currentTopLevel(): NavRoute = lastOrNull() as? NavRoute ?: NavRoute.start
