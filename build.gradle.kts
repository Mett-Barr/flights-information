import dev.iurysouza.modulegraph.ModuleType.AndroidApp
import dev.iurysouza.modulegraph.ModuleType.AndroidLibrary
import dev.iurysouza.modulegraph.ModuleType.Kotlin
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
    alias(libs.plugins.module.graph)
}

moduleGraphConfig {
    readmePath.set("${rootDir}/README.md")
    heading.set("### 模組相依圖")
    orientation.set(Orientation.TOP_TO_BOTTOM)
    nestingEnabled.set(true)
    setStyleByModuleType.set(true)
    theme.set(
        Theme.BASE(
            themeVariables = mapOf(
                "lineColor" to "#676767",
            ),
            moduleTypes = listOf(
                AndroidApp("#2C4162"),
                AndroidLibrary("#3BD482"),
                Kotlin("#8150FF"),
            ),
        ),
    )
}
