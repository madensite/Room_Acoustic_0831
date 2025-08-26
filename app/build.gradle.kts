import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
}
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")

if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}
val openAiApiKey: String = localProperties.getProperty("OPENAI_API_KEY") ?: ""

android {
    namespace = "com.example.roomacoustic"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.roomacoustic"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "OPENAI_API_KEY", "\"$openAiApiKey\"")

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
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

    packaging {                                // ✅ 새 DSL
        /** .so 라이브러리 충돌 해소 */
        jniLibs {
            pickFirsts += setOf(
                "lib/**/libarcore_sdk_c.so",
                "lib/**/libfilament.so",
                "lib/**/libfilament-utils.so",
                "lib/**/libc++_shared.so"
            )
        }
        /** AAR 내부 리소스 중 첫 파일만 사용 (필요 시) */
        resources {
            pickFirsts += setOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.camera.viewfinder.core)
    implementation(libs.core)
    val kotlinxCoroutinesVersion = "1.10.1"

    /* Jetpack Navigation */
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("com.google.android.material:material:1.12.0")
    // coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$kotlinxCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinxCoroutinesVersion")

    /* Compose → View 연동 지원 */
    implementation("androidx.compose.ui:ui-viewbinding:1.5.4")

    /* Lifecycle / Coroutines */
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0")

    /* Fragment & Activity */
    implementation("androidx.fragment:fragment-ktx:1.7.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.activity:activity-ktx:1.10.1")

    /* ─── Compose UI ─── */
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)               // UI 툴링용

    /* Room (ksp) */
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp("androidx.room:room-compiler:${libs.versions.room.get()}")

    /* 테스트 */
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)

    /* ─── YOLOv8 TFLite & CameraX ─── */

    // TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-api:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-gpu-api:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-gpu-delegate-plugin:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4")

    // CameraX Core + PreviewView
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    implementation("androidx.camera:camera-extensions:1.4.2")   // 선택

    implementation("io.coil-kt:coil-compose:2.6.0")

    /* Chat Bot 기반 */
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("androidx.recyclerview:recyclerview:1.4.0")
    implementation ("androidx.lifecycle:lifecycle-livedata-ktx:$rootProject.liveDataVersion")
    implementation("androidx.compose.foundation:foundation:1.5.4")


    /* ARCore 기반 */
    implementation("com.google.ar:core:1.49.0")
    implementation("io.github.sceneview:sceneview:2.3.0")
    implementation("io.github.sceneview:arsceneview:2.3.0")

}
