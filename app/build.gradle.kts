plugins {
    alias(libs.plugins.android.application)
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.firebase.firebase-perf")
}

android {
    namespace = "com.base.find_phone_clap_detector"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.phonefinder.findmyphone.clapflash"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            //admob app id

            resValue("string", "admob_app_id", "")
            resValue("string", "ADMOB_BANNER_V2", "")
            resValue("string", "ADMOB_MREC_BANNER", "")
            resValue("string", "ADMOB_OPEN_AD", "")
            resValue("string", "ADMOB_SPLASH_OPEN_AD", "")
            resValue("string", "ADMOB_INTERSTITIAL_V2", "")
            resValue("string", "ADMOB_NATIVE_WITHOUT_MEDIA_V2", "")

            resValue("string", "ADMOB_NATIVE_WITH_MEDIA_V2", "")
            resValue("string", "ADMOB_NATIVE_WITH_MEDIA_SPLASH", "")
            resValue("string", "ADMOB_NATIVE_WITH_MEDIA_BOARDING", "")
            resValue("string", "ADMOB_NATIVE_FULL_BOARDING", "")

            resValue("string", "ADMOB_REWARD_VIDEO", "")
            resValue("string", "ADMOB_REWARD_INTER", "")
            resValue("string", "ADMOB_BANNER_COLLAPSIBLE", "")
            resValue("string", "ADMOB_SPLASH_INTER", "")

        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            //admob app id

            resValue("string", "admob_app_id", "")

            resValue("string", "ADMOB_BANNER_V2", "")
            resValue("string", "ADMOB_MREC_BANNER", "")

            resValue("string", "ADMOB_OPEN_AD", "")
            resValue("string", "ADMOB_SPLASH_OPEN_AD", "")

            resValue("string", "ADMOB_INTERSTITIAL_V2", "")
            resValue("string", "ADMOB_NATIVE_WITHOUT_MEDIA_V2", "")
            resValue("string", "ADMOB_NATIVE_WITH_MEDIA_V2", "")

            resValue("string", "ADMOB_NATIVE_WITH_MEDIA_SPLASH", "")
            resValue("string", "ADMOB_NATIVE_WITH_MEDIA_BOARDING", "")
            resValue("string", "ADMOB_NATIVE_FULL_BOARDING", "")

            resValue("string", "ADMOB_REWARD_VIDEO", "")
            resValue("string", "ADMOB_BANNER_COLLAPSIBLE", "")

            resValue("string", "ADMOB_SPLASH_INTER", "")

            resValue("string", "ADMOB_REWARD_INTER", "")
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
        resValues = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.sdp.android) // sdp

    //  Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.play.services.auth)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.config)
    implementation(libs.firebase.perf)

    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

//  for minify issue
    implementation(libs.infer.annotation)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    //shimmer effect
    implementation(libs.shimmer)

    //Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.hilt.android.compiler)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    // ViewModel utilities for Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Lifecycles only (without ViewModel or LiveData)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.navigation.fragment)

    // alternately - if using Java8, use the following instead of lifecycle-compiler
    implementation(libs.androidx.lifecycle.common.java8)
    // Saved state module for ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)

    // Kotlin
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.fragment.ktx)
    // Testing Navigation
    androidTestImplementation(libs.androidx.navigation.testing)

    //Glide
    implementation(libs.glide)

    implementation(libs.androidx.recyclerview)

    // BILLING LIBRARY
    implementation(libs.billing)

    implementation(libs.play.services.ads.identifier)
    implementation(libs.play.services.base)
    implementation(libs.picasso)

    // admob ads
    implementation(libs.play.services.ads)
    // ad consent
    implementation(libs.user.messaging.platform)

    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.lottie)

    //GSON of Retrofit
    implementation(libs.converter.gson)

    implementation(libs.timber)

}