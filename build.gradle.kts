// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false

    id("org.jetbrains.kotlin.android") version "2.3.21" apply false
    id("com.android.library") version "9.1.1" apply false
    id("com.google.dagger.hilt.android") version "2.59.2" apply false
    id("com.google.devtools.ksp") version "2.3.4" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
    id("com.google.firebase.crashlytics") version "3.0.7" apply false
    id("com.google.firebase.firebase-perf") version "2.0.2" apply false
}