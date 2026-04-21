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
        // SECURITY NOTE: Spotify credentials are compiled into BuildConfig as plaintext strings.
        // In production, use Android Keystore or a backend token exchange. The client secret
        // should never ship in a release APK. Acceptable for capstone demo scope.
        buildConfigField("String", "SPOTIFY_TOKEN", getQuotedProperty("spotify.token"))
        buildConfigField("String", "SPOTIFY_CLIENT_ID", getQuotedProperty("spotify.client.id"))
        buildConfigField("String", "SPOTIFY_CLIENT_SECRET", getQuotedProperty("spotify.client.secret"))
        buildConfigField("String", "SPOTIFY_REFRESH_TOKEN", getQuotedProperty("spotify.refresh.token"))
    }

    buildTypes {
        release {
            // R8 minification is disabled to simplify debugging during development.
            // Enabling requires ProGuard keep rules for TFLite reflection,
            // Retrofit/Gson serialization, and Compose stability metadata.
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
        unitTests.isReturnDefaultValues = true
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

    // LiteRT (formerly TensorFlow Lite) — provides Interpreter, NnApiDelegate, DataType.
    // The tensorflow.lite and tensorflow.lite.support catalog entries were removed as
    // litert already bundles these classes under the org.tensorflow.lite package namespace.
    implementation(libs.litert)

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

// Sidesteps AGP's bundled ddmlib SplitApkInstaller, which consistently fails with
// "Failed to install-write all apks" when the device is forwarded via usbipd-win from
// WSL2. Manual `adb install -r -t` + `am instrument` works on the same device.
// Usage:
//   ./gradlew :app:onDeviceTest                         (runs the whole androidTest suite)
//   ./gradlew :app:onDeviceTest -PtestClass=FQN         (runs a specific @Test class)
//   ./gradlew :app:onDeviceTest -PnotClass=FQN          (excludes a class)
tasks.register<Exec>("onDeviceTest") {
    group = "verification"
    description = "Installs debug + androidTest APKs via plain `adb install` and runs `am instrument`. Use when gradle's ddmlib install path is broken (e.g., WSL+usbipd)."

    dependsOn("assembleDebug", "assembleDebugAndroidTest")

    val appApk = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk")
    val testApk = layout.buildDirectory.file("outputs/apk/androidTest/debug/app-debug-androidTest.apk")

    val testClass = project.findProperty("testClass") as String?
    val notClass = project.findProperty("notClass") as String?
    val filter = when {
        testClass != null -> "-e class $testClass"
        notClass != null -> "-e notClass $notClass"
        else -> ""
    }

    commandLine("sh", "-c", buildString {
        append("set -e; ")
        append("adb install -r -t '${appApk.get().asFile.absolutePath}' && ")
        append("adb install -r -t '${testApk.get().asFile.absolutePath}' && ")
        append("adb shell am instrument -w -r $filter ")
        append("com.example.snapbadgers.test/androidx.test.runner.AndroidJUnitRunner")
    })
}
