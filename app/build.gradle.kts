plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.mediconnect"
    compileSdk = 36        // ← simple, no block

    defaultConfig {
        applicationId = "com.example.mediconnect"
        minSdk = 26        // ← must be 24, not 35
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation(libs.constraintlayout)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.auth)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.database)
    implementation(libs.legacy.support.v4)
    implementation(libs.cardview)
    implementation("im.zego:zego_uikit_prebuilt_call_android:+")
    implementation(libs.gridlayout)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}