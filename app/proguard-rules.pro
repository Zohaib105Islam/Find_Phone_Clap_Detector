# --- Adjust SDK ---
-keep class com.adjust.sdk.** { *; }

# --- Google Play Services: Advertising ID ---
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient {
    com.google.android.gms.ads.identifier.AdvertisingIdClient$Info getAdvertisingIdInfo(android.content.Context);
}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient$Info {
    java.lang.String getId();
    boolean isLimitAdTrackingEnabled();
}

# --- Install Referrer API ---
-keep public class com.android.installreferrer.** { *; }

# --- Your App Data Classes ---
-keep public class com.iobits.findmyphoneviaclap.ui.dataClasses.** { *; }
-keep public class com.iobits.findmyphoneviaclap.module.** { *; }
-keep public class com.iobits.findmyphoneviaclap.managers.** { *; }
-keep public class com.iobits.findmyphoneviaclap.myApplication.** { *; }
-keep public class com.iobits.findmyphoneviaclap.utils.** { *; }
-keep public class com.iobits.findmyphoneviaclap.service.** { *; }

-keep public class com.iobits.findmyphoneviaclap.ui.viewModels.LanguageViewModel { *; }
-keep public class com.iobits.findmyphoneviaclap.ui.repository.LanguageRepository { *; }
-keep class com.iobits.findmyphoneviaclap.ui.dataClasses.LanguageSharedPreferences { *; }

-keep class com.iobits.findmyphoneviaclap.ui.dataClasses.ModelLanguageListType { *; }
-keep public class com.iobits.findmyphoneviaclap.ui.activities.LanguageActivity { *; }
-keep public class com.iobits.findmyphoneviaclap.ui.adapters.LanguagesAdapter { *; }
-keep public class com.iobits.findmyphoneviaclap.utils.Language { *; }
-keep public class com.iobits.findmyphoneviaclap.utils.LocaleHelper { *; }

-keep public class com.iobits.findmyphoneviaclap.** { *; }
# --- ViewModel (Jetpack) ---
-keep class * extends androidx.lifecycle.ViewModel { *; }

# --- Hilt Dependency Injection ---
-keep class dagger.hilt.** { *; }
-keep class androidx.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.lifecycle.HiltViewModelFactory { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# --- Kotlin Metadata (for reflection, DI, etc.) ---
-keepclassmembers class ** {
    @kotlin.Metadata *;
}

# --- Required attributes for annotations/reflection ---
-keepattributes Signature
-keepattributes *Annotation*


# WorkManager (used internally by many SDKs)
-keep class androidx.work.** { *; }

# Keep workers
-keep class * extends androidx.work.ListenableWorker { *; }

# AndroidX Startup
-keep class androidx.startup.** { *; }

# Prevent R8 removing generated classes
-keep class **_Impl { *; }

# Keep annotations
-keepattributes *Annotation*