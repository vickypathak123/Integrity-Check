package com.safetynet.integritycheck.google_consent

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm.OnConsentFormDismissedListener
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform


class GoogleMobileAdsConsentManager private constructor(context: Context) {
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    private val TAG = GoogleMobileAdsConsentManager::class.java.simpleName

    /** Interface definition for a callback to be invoked when consent gathering is complete. */
    fun interface OnConsentGatheringCompleteListener {
        fun consentGatheringComplete(error: FormError?)
    }

    /** Helper variable to determine if the app can request ads. */
    val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()

    /** Helper variable to determine if the privacy options form is required. */
    val isPrivacyOptionsRequired: Boolean
        get() =
            consentInformation.privacyOptionsRequirementStatus ==
                    ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /**
     * Helper method to call the UMP SDK methods to request consent information and load/show a
     * consent form if necessary.
     */
    fun gatherConsent(
        activity: Activity,
        deviceId: String,
        isDebug: Boolean,
        onConsentGatheringCompleteListener: OnConsentGatheringCompleteListener
    ) {
        // For testing purposes, you can force a DebugGeography of EEA or NOT_EEA.

        val params = if (isDebug) {
            val debugSettings =
                ConsentDebugSettings.Builder(activity)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    // Check your logcat output for the hashed device ID e.g.
                    // "Use new ConsentDebugSettings.Builder().addTestDeviceHashedId("ABCDEF012345")" to use
                    // the debug functionality.
                    .addTestDeviceHashedId(deviceId)
                    .build()
            ConsentRequestParameters.Builder().setConsentDebugSettings(debugSettings).build()
        } else {
            ConsentRequestParameters.Builder().build()
        }
        // Requesting an update to consent information should be called on every app launch.
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    // Consent has been gathered.
                    onConsentGatheringCompleteListener.consentGatheringComplete(formError)
                    Log.e(TAG, "gatherConsent:  Consent grant")

                }
            },
            { requestConsentError ->
                onConsentGatheringCompleteListener.consentGatheringComplete(requestConsentError)
                Log.e(TAG, "gatherConsent:  Consent Fail")
            }
        )
    }

    /** Helper method to call the UMP SDK method to show the privacy options form. */
    fun showPrivacyOptionsForm(
        activity: Activity,
        onConsentFormDismissedListener: OnConsentFormDismissedListener
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity, onConsentFormDismissedListener)
    }

    companion object {
        @Volatile
        private var instance: GoogleMobileAdsConsentManager? = null

        fun getInstance(context: Context) =
            instance ?: synchronized(this) {
                instance ?: GoogleMobileAdsConsentManager(context).also { instance = it }
            }
    }
}