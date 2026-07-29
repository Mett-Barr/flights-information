import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
}

detekt {
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
}

// kotlin-jvm 也套用 Java plugin，而 Java 端預設跟隨 Gradle 執行時的 JVM。
// 不釘住就會與下面的 jvmTarget 對不上，Gradle 會直接擋下建置。
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        allWarningsAsErrors = true
    }
}
