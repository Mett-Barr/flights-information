// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    // Declared here (without applying) so it shares a class loader with the Hilt
    // plugin — see https://github.com/google/dagger/issues/3965
    alias(libs.plugins.ksp) apply false
}