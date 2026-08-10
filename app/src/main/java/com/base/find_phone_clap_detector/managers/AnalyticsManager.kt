package com.base.find_phone_clap_detector.managers

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.Firebase

object AnalyticsManager {
    private var firebaseAnalytics: FirebaseAnalytics = Firebase.analytics
    fun logEvent(eventName: String) {
        firebaseAnalytics.logEvent(eventName, null)
    }
}