import dev.iurysouza.modulegraph.ModuleType.AndroidApp
import dev.iurysouza.modulegraph.ModuleType.AndroidLibrary
import dev.iurysouza.modulegraph.ModuleType.Kotlin
import dev.iurysouza.modulegraph.LinkText
import dev.iurysouza.modulegraph.Orientation
import dev.iurysouza.modulegraph.Theme

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    // Declared here (without applying) so it shares a class loader with the Hilt
    // plugin — see https://github.com/google/dagger/issues/3965
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.module.graph)
}

moduleGraphConfig {
    readmePath.set("${rootDir}/README.md")
    heading.set("### 模組相依圖")
    // 由上而下呈現，較容易讀出相依方向。
    orientation.set(Orientation.TOP_TO_BOTTOM)
    // 所有專案相依皆為 implementation，重複標示不會增加辨識資訊。
    linkText.set(LinkText.NONE)
    // 0.13.0 的巢狀分組會重複輸出同時作為來源與目標的節點；平面分組可穩定再生。
    nestingEnabled.set(false)
    // 以模組類型的 classDef 區分 app、Android library 與純 Kotlin 模組。
    setStyleByModuleType.set(true)
    theme.set(
        Theme.BASE(
            // theme 一旦指定為 base，Mermaid 就不再跟隨 GitHub 的深淺色，連線會
            // 落回預設的黑——在深色版面上等於看不見。這裡挑一個中間調的灰，
            // 淺底與深底都讀得到。
            themeVariables = mapOf("lineColor" to "#8C8C8C"),
            moduleTypes = listOf(
                AndroidApp("#2C4162"),
                AndroidLibrary("#3BD482"),
                Kotlin("#8150FF"),
            ),
        ),
    )
}
