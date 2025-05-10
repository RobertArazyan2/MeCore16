plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.mecore"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.mecore"
        minSdk = 29
        targetSdk = 35
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
    implementation(libs.constraintlayout)
    implementation(libs.material.v190)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage)

    implementation(libs.glide)
    implementation(libs.squareup.okhttp)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Kept your nested dependencies block since you want to keep everything
    dependencies {
        // Agora Voice SDK
        implementation(libs.voice.sdk.v432)

        // Firebase dependencies (already in your project)
        implementation(libs.firebase.auth.v2300)
        implementation(libs.google.firebase.firestore)
        implementation(libs.firebase.messaging.v2400)

        // OkHttp for FCM notifications (already in your project)
        implementation(libs.squareup.okhttp)

            implementation(libs.voice.sdk.v440)

    }

    // Added androidx.core:core for ContextCompat in ChatActivity
    implementation(libs.core)
}