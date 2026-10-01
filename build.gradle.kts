// Root build file. Plugins are declared here with `apply false` and applied
// in module build files, so versions are resolved once from the version catalog.
//
// AGP 9+ has built-in Kotlin support (org.jetbrains.kotlin.android is no
// longer applied), but it depends on KGP 2.2.10 by default. The override
// below pins the Kotlin Gradle Plugin to the version this project targets,
// matching the Compose compiler plugin version in gradle/libs.versions.toml.
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
