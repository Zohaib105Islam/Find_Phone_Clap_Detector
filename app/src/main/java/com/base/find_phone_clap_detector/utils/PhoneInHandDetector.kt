package com.base.find_phone_clap_detector.utils

interface PhoneInHandDetector {
    suspend fun isPhoneInHand(): Boolean
}