plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.dagger.hilt.android)
    alias(libs.plugins.ksp) // cần cho Room và Hilt
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20"
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    alias(libs.plugins.roborazzi) // screenshot testing — xem docs/UI_FIDELITY_WORKFLOW.md
}

android {
    namespace = "com.camera.studio.photoeditor"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.camera.studio.photoeditor"
        minSdk = 28
        targetSdk = 35
        versionCode = 17
        versionName = "1.0.12"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Keystore VGA52A chưa tồn tại — signingConfig thật sẽ thêm ở M8 (theo keystore/VGA52A/keystore.jks + pass.txt).
    // KHÔNG dùng lại keystore/mật khẩu của vga34b.

    buildTypes {
        debug {
            // Lib đọc meta-data firebase_crashlytics_collection_enabled=${crashlyticsCollectionEnabled}
            manifestPlaceholders["crashlyticsCollectionEnabled"] = "false"
        }
        release {
            // TẠM dùng debug keystore để cài máy test bản R8 (chưa có keystore VGA52A thật) — XOÁ
            // dòng này và gắn signingConfig thật (keystore/VGA52A/keystore.jks) trước khi phát hành.
            signingConfig = signingConfigs.getByName("debug")
            manifestPlaceholders["crashlyticsCollectionEnabled"] = "true"
            isMinifyEnabled = true          // BẮT BUỘC theo guide tích hợp
            isShrinkResources = true        // BẮT BUỘC (đi cặp với minifyEnabled)
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules-base.pro", // lõi dùng chung — xem docs/integration/16_ENCODE_R8_STANDARD.md
                "proguard-rules.pro"
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
    packaging {
        resources {
            excludes += "META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/atomicfu.kotlin_module"
        }
    }
    hilt {
        enableAggregatingTask = false
    }
    // Roborazzi screenshot testing (unit test JVM, không cần emulator) — xem docs/UI_FIDELITY_WORKFLOW.md
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperties["robolectric.pixelCopyRenderMode"] = "hardware"
            }
        }
    }
}

dependencies {
    // Base-application AAR (ads / IAP / splash / language / notification shell)
    api(project(":base-application"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.review.ktx)
    implementation(libs.material)

    testImplementation(libs.junit)
    // Roborazzi screenshot testing — render Composable ra PNG trên JVM, không cần emulator/device.
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Room (history quét)
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // Coil — load icon app (thay base64 của RN)
    implementation("io.coil-kt:coil-compose:2.0.0")
    // Coil video-frame decoder — dùng cho thumbnail video trong Gallery (thay MediaMetadataRetriever thủ
    // công không cache/không downsample từng gây giật khi vào màn/chuyển tab).
    implementation("io.coil-kt:coil-video:2.0.0")

    // Pager cho màn Intro dùng androidx.compose.foundation.pager (đã có trong Compose BoM).
    implementation("com.google.accompanist:accompanist-permissions:0.30.1")

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Coroutines & StateFlow
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Firebase / Billing / InstallReferrer — google-services.json hiện là MOCK (chưa có Firebase
    // project thật cho vga52a), khớp applicationId để plugin google-services không lỗi build.
    implementation(platform("com.google.firebase:firebase-bom:34.1.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-config")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.android.billingclient:billing-ktx:7.1.1")
    implementation("com.android.installreferrer:installreferrer:2.2")

    // Gson (dùng cho catalog asset JSON của Sticker/Frame/Template picker)
    implementation("com.google.code.gson:gson:2.10.1")

    // CameraX (preview + chụp ảnh + quay video)
    val cameraxVersion = "1.4.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-video:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // Media3 (burn overlay bitmap vào video sau khi quay + play lại kết quả)
    val media3Version = "1.5.1"
    implementation("androidx.media3:media3-transformer:$media3Version")
    implementation("androidx.media3:media3-effect:$media3Version")
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-common:$media3Version")

    // DataStore (cấu hình location/template/time — thay Redux UserSession của RN)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Đọc EXIF orientation ảnh chụp (CameraX takePicture ghi EXIF, cần xoay đúng chiều trước khi bake overlay)
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Photo Picker (chọn ảnh có sẵn cho "Edit photo") — activity result API có sẵn trong androidx.activity
    implementation("androidx.activity:activity-compose:1.9.3")
}
