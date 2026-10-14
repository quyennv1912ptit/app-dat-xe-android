package com.example.app_dat_xe.feature.auth.ui

import com.google.firebase.auth.PhoneAuthProvider

object OtpData {
    var verificationId: String? = null
    var facebookName: String? = null
    var facebookIdToken: String? = null
    var phoneIdToken: String? = null
    var phoneNumber: String? = null
    var googleIdToken: String? = null
    var providerIdToken: String? = null
    var providerPhotoUrl: String? = null
    var forceResendingToken: PhoneAuthProvider.ForceResendingToken? = null

    fun clear() {
        verificationId = null
        facebookName = null
        facebookIdToken = null
        phoneIdToken = null
        phoneNumber = null
        googleIdToken = null
        providerIdToken = null
        providerPhotoUrl = null
        forceResendingToken = null
    }
}
