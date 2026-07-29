import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt)
}


composeCompiler {
    // java.time 這類外部型別無法自行標註穩定性，集中在根目錄的設定檔宣告。
    stabilityConfigurationFiles.add(
        rootProject.layout.projectDirectory.file("compose_compiler_config.conf")
    )
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
}

android {
    lint {
        lintConfig = rootProject.file("lint.xml")
    }

    namespace = "moozy.flightinformation.feature.calculator"
    // API 37 以 minor 版本的形式發行（platforms;android-37.0）。省略 minor
    // 時工具會去解析不存在的 android-37 目標，所以明確寫出來。
    compileSdk = 37
    compileSdkMinor = 0

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        allWarningsAsErrors = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlin.test.junit)
}
