plugins {
    alias(libs.plugins.android.application)
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.firebase.firebase-perf")
}

android {
    namespace = "com.base.find_phone_clap_detector"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.phonefinder.findmyphone.clapflash"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            adValues("ca-app-pub-3940256099942544~3347511713", "ca-app-pub-3940256099942544/9257395921", "ca-app-pub-3940256099942544/6300978111", "ca-app-pub-3940256099942544/1033173712", "ca-app-pub-3940256099942544/2247696110", "ca-app-pub-3940256099942544/5224354917")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            adValues("ca-app-pub-9262122906300240~9364070308", "ca-app-pub-9262122906300240/2415518570", "ca-app-pub-9262122906300240/6753384681", "ca-app-pub-9262122906300240/4654886642", "ca-app-pub-9262122906300240/5041681918", "ca-app-pub-9262122906300240/8785431906")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
        resValues = true
        compose = true
    }
}

fun com.android.build.api.dsl.ApplicationBuildType.adValues(app: String, open: String, banner: String, interstitial: String, native: String, reward: String) {
    resValue("string", "admob_app_id", app)
    resValue("string", "ADMOD_OPEN_AD", open)
    resValue("string", "ADMOD_OPEN_SPLASH_AD", open)
    resValue("string", "ADMOD_OPEN_SPLASH_AD_AFTER_FIRST", open)
    resValue("string", "ADMOB_BANNER_V2", banner)
    resValue("string", "ADMOB_BANNER_MEDIUM_RECTANGLE_V2", banner)
    resValue("string", "ADMOB_BANNER_COLLAPSIBLE", banner)
    resValue("string", "ADMOB_INTERSTITIAL_V2_SERVICE", interstitial)
    resValue("string", "ADMOB_INTERSTITIAL_V2", interstitial)
    resValue("string", "ADMOB_INTERSTITIAL_PREMIUM_V2", interstitial)
    resValue("string", "ADMOB_NATIVE_WITHOUT_MEDIA_V2", native)
    resValue("string", "ADMOB_NATIVE_WITHOUT_MEDIA_HOME_V2", native)
    resValue("string", "ADMOB_NATIVE_WITH_MEDIA_V2", native)
    resValue("string", "ADMOB_NATIVE_WITH_MEDIA_SPLASH_V2", native)
    resValue("string", "ADMOB_REWARD_VIDEO", reward)
    resValue("string", "ADMOB_REWARD_INTER", interstitial)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.sdp.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.shimmer)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.common.java8)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.navigation.fragment)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.navigation.testing)
    implementation(libs.glide)
    implementation(libs.androidx.recyclerview)
    implementation(libs.billing)
    implementation(libs.play.services.ads.identifier)
    implementation(libs.play.services.base)
    implementation(libs.picasso)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.lottie)
    implementation(libs.converter.gson)
    implementation(libs.timber)
    implementation(platform(libs.firebase.bom))
    implementation(libs.play.services.auth)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.config)
    implementation(libs.firebase.perf)
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.android.play:feature-delivery:2.1.0")
    implementation("com.google.android.play:feature-delivery-ktx:2.1.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0")
    implementation("me.relex:circleindicator:2.1.6")
    implementation("com.github.ismaeldivita:chip-navigation-bar:1.4.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("com.ncorti:slidetoact:0.11.0")
    implementation("com.makeramen:roundedimageview:2.3.0")
    implementation("com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava")
    implementation("com.google.guava:guava:33.6.0-jre")
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
