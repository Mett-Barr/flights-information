package moozy.flightinformation.feature.calculator

/**
 * 計算機按鍵的 test tag。
 *
 * 來源專案把它放在共用的 `core.testing.tags` 模組；這裡只有一個使用者，所以就近放在
 * 計算機自己的 package。用 enum 常數名（而非 [KeyLabel.label]）當 tag，才不會因為
 * 顯示文字（"×"、"⌫"）改動就讓測試失效。
 */
fun calculatorKeyTag(key: KeyLabel): String {
    val name =
        when (key) {
            is NumKey -> key.name
            is OpKey -> key.name
            is SysKey -> key.name
        }
    return "calculator_key_$name"
}
