import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Stage v0.4: the key now comes from local.properties (gitignored, never
// committed) for local builds, or a Gradle project property (-P, fed by CI
// from an encrypted Actions secret) otherwise. Neither path touches git.
val localProperties =
    Properties().apply {
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            load(FileInputStream(localPropertiesFile))
        }
    }

// Stage v0.9: fail loudly at configuration time rather than silently building
// an app with an empty key (and a weather lookup that will always 401) --
// a missing secret should break the build, not ship quietly.
fun resolveOpenWeatherApiKey(): String =
    (project.findProperty("OPEN_WEATHER_API_KEY") as String?)
        ?: localProperties.getProperty("OPEN_WEATHER_API_KEY")
        ?: throw GradleException(
            "OPEN_WEATHER_API_KEY is not set. Provide it via " +
                "-POPEN_WEATHER_API_KEY=<key> or app/local.properties " +
                "(see CLAUDE.md for local setup / CI secret wiring).",
        )

android {
    namespace = "com.securitytraining.weatherapp"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.securitytraining.weatherapp"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1"

        buildConfigField("String", "OPEN_WEATHER_API_KEY", "\"${resolveOpenWeatherApiKey()}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.security.crypto)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
