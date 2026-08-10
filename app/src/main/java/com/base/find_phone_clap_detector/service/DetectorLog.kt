package com.base.find_phone_clap_detector.service

import timber.log.Timber

object DetectorLog {
    private const val DEFAULT_TAG = "PhoneDetector"
    private const val TAG_PREFIX = "PhoneDetector/"

    private fun prefixedTag(tag: String): String {
        return if (tag.startsWith(TAG_PREFIX)) tag else "$TAG_PREFIX$tag"
    }

    fun d(tag: String = DEFAULT_TAG, message: String) {
        Timber.tag(prefixedTag(tag)).d(message)
    }

    fun i(tag: String = DEFAULT_TAG, message: String) {
        Timber.tag(prefixedTag(tag)).i(message)
    }

    fun w(tag: String = DEFAULT_TAG, message: String) {
        Timber.tag(prefixedTag(tag)).w(message)
    }

    fun w(tag: String = DEFAULT_TAG, message: String, throwable: Throwable) {
        Timber.tag(prefixedTag(tag)).w(throwable, message)
    }

    fun e(tag: String = DEFAULT_TAG, message: String) {
        Timber.tag(prefixedTag(tag)).e(message)
    }

    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable) {
        Timber.tag(prefixedTag(tag)).e(throwable, message)
    }
}
