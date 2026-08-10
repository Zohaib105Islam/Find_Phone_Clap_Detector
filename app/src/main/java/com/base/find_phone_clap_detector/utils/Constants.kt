package com.base.find_phone_clap_detector.utils

import java.util.ArrayList

object Constants {
    //in app item sku
    const val ITEM_SKU_REMOVE_ADS_ONLY = "remove_ads" // this will only remove ads .
    const val ITEM_SKU_GET_PREMIUM = "get_premium" //this will remove ads and also enable premium
    const val ITEM_SKU_PRO_USER_SUB = "pro_version_offer" // pro mode .
    var ITEM_SKU_PRO_USER_SUB_TRIAL = "pro_version_trail" // pro mode .
    var afterDiscount = ""
    var oneTimeProductPremiumPrice = ""
    const val NUMBER_OF_ON_BOARDING_SLIDER = 3

    var ONE_TIME_PURCHASE_PRICE_DISCOUNTED = "1.50$"
    var ONE_TIME_PURCHASE_PRICE_FULL = "2.99$"

    var SERVICE_TYPE = "service_type"
    var ACTIVE_SERVICE_TYPE = "active_service_type"
    const val BY_CLAP = "by_clap"
    const val BY_PASSCODE = "by_passcode"
    const val DONT_TOUCH = "dont_touch"
    const val POCKET_MODE = "pocket_mode"
    const val BY_WHISTLE = "by_whistle"

    const val FROM_CHANGE_PASSCODE = "FROM_CHANGE_PASSCODE"
}