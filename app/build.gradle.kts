import java.util.Properties
import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        load(file.inputStream())
    }
}

fun getQuotedProperty(key: String): String {
    val value = localProperties.getProperty(key)?.removeSurrounding("\"")?.removeSurrounding("'") ?: ""
    return "\"$value\""
}

if (projectDir.absolutePath.any { it.code > 127 }) {
    // Keep build outputs on an ASCII-only path so Gradle/JUnit workers can resolve classes on Windows.
    layout.buildDirectory.set(
        file("${System.getProperty("user.home")}\\.snapbadgers-build\\app")
    )
}

android {
    namespace = "com.example.snapbadgers"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.snapbadgers"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SPOTIFY_TOKEN", getQuotedProperty("spotify.token"))
        buildConfigField("String", "SPOTIFY_CLIENT_ID", getQuotedProperty("spotify.client.id"))
        buildConfigField("String", "SPOTIFY_CLIENT_SECRET", getQuotedProperty("spotify.client.secret"))
        buildConfigField("String", "SPOTIFY_REFRESH_TOKEN", getQuotedProperty("spotify.refresh.token"))
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        noCompress += "tflite"
    }

    testOptions {
        animationsDisabled = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support)

    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.mockito.core)
    androidTestImplementation(libs.mockito.kotlin)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.withType<Test>().configureEach {
    if (name == "testDebugUnitTest") {
        val kotlinUnitTestClasses = files(layout.buildDirectory.dir("tmp/kotlin-classes/debugUnitTest"))
        testClassesDirs = files(testClassesDirs, kotlinUnitTestClasses)
        classpath = classpath.plus(kotlinUnitTestClasses)
    }
}
