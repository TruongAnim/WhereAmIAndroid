import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// The ingest URL carries a shared secret, so it lives in local.properties
// (git-ignored) rather than in source control. Without it the app falls back
// to the public Traccar demo server.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

val defaultServerUrl: String =
    localProperties.getProperty("whereami.serverUrl")?.trim().takeUnless { it.isNullOrEmpty() }
        ?: "https://demo.traccar.org"

// The web map viewer. Not a secret, so it has a working default.
val viewerUrl: String =
    localProperties.getProperty("whereami.viewerUrl")?.trim().takeUnless { it.isNullOrEmpty() }
        ?: "https://whereami-1c55e.web.app"

android {
    namespace = "com.anim.where.am.i"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.anim.where.am.i"
        minSdk = 35
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "DEFAULT_SERVER_URL",
            "\"" + defaultServerUrl.replace("\\", "\\\\").replace("\"", "\\\"") + "\"",
        )
        buildConfigField(
            "String",
            "VIEWER_URL",
            "\"" + viewerUrl.replace("\\", "\\\\").replace("\"", "\\\"") + "\"",
        )
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.traccar.client.sdk)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.mlkit.barcode)
    implementation(libs.zxing.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}