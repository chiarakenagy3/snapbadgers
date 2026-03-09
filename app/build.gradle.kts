plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.snapbadgers"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.snapbadgers"
        // TODO: AI Hub sample uses 31, we can safely move up to 29
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // TODO: from AI Hub sample app
        // resValue( "string", "tfLiteModelAsset", "classifier.tflite")
        // resValue( "string", "tfLiteLabelsAsset", "labels.txt")
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
    }
}

/**
 * Pre-build validation - from AI Hub sample
 */

tasks.preBuild {
    doFirst {
        val modelFile = file("src/main/assets/classifier.tflite")
        if (!modelFile.exists()) {
            throw RuntimeException("classifier.tflite is missing")
        }

        for (i in 1..3) {
            val filename = "src/main/assets/images/Sample$i.png"
            val fp = file(filename)

            if (!fp.exists() || fp.length() < 1000) {
                throw RuntimeException("Project assets are missing.")
            }
        }
    }
}
dependencies {
    // Existing Android + Compose dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    /**
     * AI Hub / TensorFlow Lite dependencies
      */

    // Tfl Lite runtime
    api("org.tensorflow:tensorflow-lite:2.17.0")

    // TensorFlow support library
    api("org.tensorflow:tensorflow-lite-support:0.5.0")

    // GPU acceleration
    implementation("org.tensorflow:tensorflow-lite-gpu:2.17.0")
    implementation("org.tensorflow:tensorflow-lite-gpu-api:2.17.0")
    implementation("org.tensorflow:tensorflow-lite-gpu-delegate-plugin:0.4.4")

    // Qualcomm QNN delegate (AI Hub acceleration)
    implementation("com.qualcomm.qti:qnn-runtime:2.40.0")
    implementation("com.qualcomm.qti:qnn-litert-delegate:2.40.0")
}