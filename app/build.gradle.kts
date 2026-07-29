import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
}

detekt {
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}
// 只有匯率 API 需要金鑰；航班那支是公開端點。
val freeCurrencyApiKey = localProps.getProperty("free_currency_api_key") ?: ""

android {
    namespace = "moozy.flightinformation"
    // API 37 以 minor 版本的形式發行（platforms;android-37.0）。省略 minor
    // 時工具會去解析不存在的 android-37 目標，所以明確寫出來。
    compileSdk = 37
    compileSdkMinor = 0

    defaultConfig {
        applicationId = "moozy.flightinformation"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "FREE_CURRENCY_API_KEY", "\"$freeCurrencyApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // java.time is only available from API 26; minSdk is 24.
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true // Enable BuildConfig
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        // 警告不該累積：一旦容忍，新的就淹沒在舊的裡面，直到沒人再讀它們。
        allWarningsAsErrors = true
    }
}

dependencies {

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)


    // 版本一律由上面的 Compose BOM 供應。
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation)


    implementation(libs.androidx.material3.adaptive)
    implementation(libs.androidx.material3.adaptive.navigation.suite)

    // Navigation 3。back stack 的還原走 kotlinx.serialization，所以 NavKey 都要 @Serializable。
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)



    implementation(libs.hilt.android)
    implementation(libs.androidx.compose.runtime)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)


    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.coroutines.core)

    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    implementation(project(":feature:calculator"))

    testImplementation(libs.junit)
    testImplementation(libs.konsist)
    // 計算機的測試是從來源專案原樣搬過來的，用 kotlin.test 的斷言；改寫成 JUnit4 斷言
    // 就不再是當初驗證過的那份測試了，所以補依賴而不是改測試。
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
