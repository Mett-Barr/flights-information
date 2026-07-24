import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}
val flightApiKey = localProps.getProperty("flight_api_key") ?: ""
val freeExchangeApiKet = localProps.getProperty("free_currency_api_key") ?: ""

android {
    namespace = "moozy.flightinformation"
    compileSdk = 36

    defaultConfig {
        applicationId = "moozy.flightinformation"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"


        buildConfigField("String", "FLIGHT_API_KEY", "\"$flightApiKey\"")
        buildConfigField("String", "FREE_CURRENCY_API_KEY", "\"$freeExchangeApiKet\"")
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
    }
}

dependencies {

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.material3.adaptive.navigation3)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)


    implementation("androidx.compose.animation:animation:1.10.0-alpha04")
    implementation("androidx.compose.foundation:foundation:1.10.0-alpha04")
    implementation("androidx.compose.material:material:1.10.0-alpha04")
    implementation("androidx.compose.runtime:runtime:1.10.0-alpha04")
    implementation("androidx.compose.ui:ui:1.10.0-alpha04")


    implementation("androidx.compose.material3:material3-window-size-class:1.4.0")
    implementation("androidx.compose.material3.adaptive:adaptive:1.3.0-alpha01")
    implementation("androidx.compose.material3.adaptive:adaptive-layout:1.3.0-alpha01")
    implementation("androidx.compose.material3.adaptive:adaptive-navigation:1.3.0-alpha01")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.5.0-alpha06")
    implementation("androidx.window:window:1.5.0")
    implementation("androidx.window:window-core:1.5.0")



    implementation(libs.hilt.android)
    implementation(libs.androidx.compose.runtime)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)


    implementation(libs.kotlinx.collections.immutable)

    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
