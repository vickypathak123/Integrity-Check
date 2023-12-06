package com.safetynet.integritycheck.integrity

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.model.IntegrityDialogResponseCode.DIALOG_CANCELLED
import com.google.android.play.core.integrity.model.IntegrityDialogResponseCode.DIALOG_FAILED
import com.google.android.play.core.integrity.model.IntegrityDialogResponseCode.DIALOG_SUCCESSFUL
import com.google.android.play.core.integrity.model.IntegrityDialogResponseCode.DIALOG_UNAVAILABLE
import com.google.android.play.core.integrity.model.IntegrityDialogTypeCode.GET_LICENSED
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.playintegrity.v1.PlayIntegrity
import com.google.api.services.playintegrity.v1.PlayIntegrityRequestInitializer
import com.google.api.services.playintegrity.v1.model.DecodeIntegrityTokenRequest
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import com.safetynet.integritycheck.Interface.CheckPlayIntegrityStatus
import com.safetynet.integritycheck.google_consent.GoogleMobileAdsConsentManager
import com.safetynet.integritycheck.utils.Config
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

/**
 * @author Parthyu Umaraniya
 * @since 14 Sept 2023
 */

object AppProtector {
    @JvmStatic
    fun with(context: Context): CheckIntegrity {
        return CheckIntegrity(context)
    }
}

class CheckIntegrity(private val context: Context) {

    private var packageName: String = ""
    private var appName: String = ""
    private var deviceId: String = ""
    private var playIntegrityRemote: String = ""
    private var cloudProjectNumber: Long = 0
    private var isEnableDebugMode: Boolean = false
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private lateinit var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager

    @JvmName("appName")
    fun appName(appName: String) = this@CheckIntegrity.apply {
        this.appName = appName
    }

    @JvmName("packageName")
    fun packageName(packageName: String) = this@CheckIntegrity.apply {
        this.packageName = packageName
    }

    /** Helper method to set Cloud project number which you can find from google-services.json file */
    @JvmName("cloudProjectNumber")
    fun cloudProjectNumber(cloudProjectNumber: Long) = this@CheckIntegrity.apply {
        this.cloudProjectNumber = cloudProjectNumber
    }

    /** Helper method to set Json which you can get from Remote config */
    @JvmName("playIntegrityRemoteConfigJson")
    fun playIntegrityRemoteConfigJson(playIntegrityRemote: String) = this@CheckIntegrity.apply {
        this.playIntegrityRemote = playIntegrityRemote
    }

    /** Helper method to set deviceId which you can get from logs
     * it's required field to set debug testing for google-consent */
    @JvmName("deviceId")
    fun deviceId(deviceId: String) = this@CheckIntegrity.apply {
        this.deviceId = deviceId
    }

    /** Helper method to set isEnableDebugMode for set debug testing for google-consent
     * Params: enabled – True if want to enabled debug mode, false otherwise. */
    @JvmName("isEnableDebugMode")
    fun isEnableDebugMode(isEnableDebugMode: Boolean) = this@CheckIntegrity.apply {
        this.isEnableDebugMode = isEnableDebugMode
    }


    @JvmName("checkIntegrity")
    fun checkIntegrity(checkPlayIntegrityStatus: CheckPlayIntegrityStatus) =
        this@CheckIntegrity.apply {
            try {
                if (context.isOnline) {
                    logShow("Internet On")
                    requestIntegrityToken(
                        context,
                        packageName,
                        cloudProjectNumber,
                        playIntegrityRemote
                    ) { licensingVerdict ->
                        when (licensingVerdict) {
                            LICENSE.NOT_SAFE -> {

                                logShow("LICENSE NOT_SAFE")
                            }

                            LICENSE.SAFE, LICENSE.ERROR, LICENSE.OLD_PLAY_STORE -> {
                                logShow("LICENSE SAFE or OLD_PLAY_STORE or ERROR")
                                googleMobileAdsConsentManager =
                                    GoogleMobileAdsConsentManager.getInstance(context)
                                googleMobileAdsConsentManager.gatherConsent(
                                    context as Activity,
                                    deviceId,
                                    isEnableDebugMode
                                ) { consentError ->
                                    if (consentError != null) {
                                        // Consent not obtained in current session.
                                        logShow("ERROR ${consentError.errorCode}. ${consentError.message}")
                                    }
                                    logShow("RESULT ${consentError?.errorCode}. ${consentError?.message}")

                                    if (googleMobileAdsConsentManager.canRequestAds) {
                                        if (isMobileAdsInitializeCalled.getAndSet(true)) {
                                            logShow("ERROR ${consentError?.errorCode}. ${consentError?.message}")
                                        } else {
                                            checkPlayIntegrityStatus.onSuccess()
                                        }
                                    } else {
                                        checkPlayIntegrityStatus.onSuccess()
                                    }
                                }
                            }
                        }
                    }
                } else {
                    logShow("Internet Off")
                    checkPlayIntegrityStatus.onSuccess()
                }
            } catch (E: Exception) {
                logShow("Catch : " + E.message)
                checkPlayIntegrityStatus.onSuccess()
            }
        }
}

private var showError = true
private var verdictCodeList = arrayListOf<String>()

private fun getCodeList(data: JSONArray): ArrayList<String> {
    val list = arrayListOf<String>()
    for (i in 0 until data.length()) {
        list.add(data.getString(i))
    }
    return list
}

private fun requestIntegrityToken(
    context: Context,
    packageName: String,
    cloudProjectNumber: Long,
    playIntegrityRemote: String,
    callback: (LICENSE) -> Unit
) {

    try {
        logShow("requestIntegrityToken")

        if (playIntegrityRemote.isNotEmpty()) {
            val obj = JSONObject(playIntegrityRemote)
            showError = obj.getBoolean("errorHide")
            if (obj.has("verdictsResponseCodes"))
                verdictCodeList = getCodeList(obj.getJSONArray("verdictsResponseCodes"))
        }
        logShow("showError = $showError  verdictCodeList = $verdictCodeList")
        // Create an instance of a manager.
        val standardIntegrityManager = IntegrityManagerFactory.createStandard(context)

        var integrityTokenProvider: StandardIntegrityManager.StandardIntegrityTokenProvider

        logShow("Start Integrity")
        // Prepare integrity token. Can be called once in a while to keep internal
        // state fresh.
        standardIntegrityManager.prepareIntegrityToken(
            StandardIntegrityManager.PrepareIntegrityTokenRequest.builder()
                .setCloudProjectNumber(cloudProjectNumber)
                .build()
        )
            .addOnSuccessListener { tokenProvider ->
                logShow("addOnSuccessListener: integrityTokenProvider=== $tokenProvider")

                integrityTokenProvider = tokenProvider

// Request integrity token by providing a user action request hash. Can be called
// several times for different user actions.
                val requestHash = generateNonce()  /*Settings.Secure.getString(context.contentResolver,
                    Settings.Secure.ANDROID_ID)*/
                val integrityTokenResponse = integrityTokenProvider.request(
                    StandardIntegrityManager.StandardIntegrityTokenRequest.builder()
                        .setRequestHash(
                            requestHash
                        )
                        .build()
                )
                val credentials = GoogleCredentials.fromStream(
                    requireNotNull(context.javaClass.classLoader?.getResourceAsStream("credentials.json"))
                )
                integrityTokenResponse
                    .addOnSuccessListener { response: StandardIntegrityManager.StandardIntegrityToken ->

                        logShow("StandardIntegrityToken:  Success === ${response.token()}")
                        val requestInitializer: HttpRequestInitializer =
                            HttpCredentialsAdapter(credentials)
                        val httpTransport = NetHttpTransport()
                        val jsonFactory = JacksonFactory()
                        val initializer = PlayIntegrityRequestInitializer()

                        val playIntegrity =
                            PlayIntegrity.Builder(httpTransport, jsonFactory, requestInitializer)
                                .setApplicationName(packageName)
                                .setGoogleClientRequestInitializer(initializer)

                        val play = playIntegrity.build()

                        val thread = Thread {
                            try {
                                val content =
                                    DecodeIntegrityTokenRequest().setIntegrityToken(response.token())
                                val apiResponse =
                                    play.v1().decodeIntegrityToken(packageName, content).execute()
                                logShow("apiResponse: $apiResponse")

                                showToast(context, "apiResponse: $apiResponse")

                                val appLicensingVerdict =
                                    apiResponse.tokenPayloadExternal.accountDetails.appLicensingVerdict

                                val appRecognitionVerdict =
                                    apiResponse.tokenPayloadExternal.appIntegrity.appRecognitionVerdict

                                val deviceRecognitionVerdict =
                                    apiResponse.tokenPayloadExternal.deviceIntegrity.deviceRecognitionVerdict

                                logShow("appLicensingVerdict: $appLicensingVerdict")
                                logShow("appRecognitionVerdict: $appRecognitionVerdict")
                                logShow("deviceRecognitionVerdict: $deviceRecognitionVerdict")

                                if (appLicensingVerdict == "LICENSED"
                                    && appRecognitionVerdict == "PLAY_RECOGNIZED" && deviceRecognitionVerdict[0] == "MEETS_DEVICE_INTEGRITY"
                                ) {
                                    callback(LICENSE.SAFE)
                                    context.config.isPlayIntegrityCheck = true
                                } else {


                                    val isSafeLast = verdictCodeList.isNotEmpty() &&
                                            (appLicensingVerdict == "LICENSED" || verdictCodeList.contains(
                                                appLicensingVerdict
                                            )) &&
                                            (appRecognitionVerdict == "PLAY_RECOGNIZED" || verdictCodeList.contains(
                                                appRecognitionVerdict
                                            )) &&
                                            (deviceRecognitionVerdict[0] == "MEETS_DEVICE_INTEGRITY" || verdictCodeList.contains(
                                                deviceRecognitionVerdict[0]
                                            ))


                                    if (isSafeLast) {
                                        callback(LICENSE.SAFE)
                                        context.config.isPlayIntegrityCheck = true
                                        return@Thread
                                    }

                                    context.config.isPlayIntegrityCheck = false
                                    response.showDialog(context as Activity?, GET_LICENSED)
                                        .addOnCanceledListener {
                                            logShow("Dialog closed")
                                        }.addOnCompleteListener {
                                            if (it.result == DIALOG_CANCELLED) {
                                                logShow("DIALOG_CANCELLED")
                                                context.finishAffinity()
                                                exitProcess(0)
                                            }
                                            if (it.result == DIALOG_FAILED) {
                                                logShow("DIALOG_FAILED")
                                            }
                                            if (it.result == DIALOG_SUCCESSFUL) {
                                                logShow("DIALOG_SUCCESSFUL")

                                                requestIntegrityToken(
                                                    context,
                                                    packageName,
                                                    cloudProjectNumber,
                                                    playIntegrityRemote,
                                                    callback
                                                )
                                            }
                                            if (it.result == DIALOG_UNAVAILABLE) {
                                                logShow("DIALOG_UNAVAILABLE")
                                            }

                                        }.addOnFailureListener {
                                            logShow("dialog Exception = ${it.message}")
                                            handleError(it)
                                        }
                                }
                            } catch (e: Exception) {
                                callback(LICENSE.SAFE)
                                context.config.isPlayIntegrityCheck = true
                                logShow("LICENSE.ERROR 1 : ${e.message}")
                                e.printStackTrace()
                            }
                        }
                        thread.start()

                    }
                    .addOnFailureListener { exception: java.lang.Exception? ->
                        callback(LICENSE.SAFE)
                        handleError(exception!!)

                    }


            }
            .addOnFailureListener { exception ->
                callback(LICENSE.SAFE)
                handleError(exception)
            }


    } catch (e: Exception) {
        callback(LICENSE.SAFE)
        context.config.isPlayIntegrityCheck = true
        logShow("requestIntegrityToken $e")
    }


}

fun handleError(exception: java.lang.Exception) {

    logShow("Madhuri handleError: $exception")
}


private fun generateNonce(): String {
    val allowed = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    val token = (1..50).map { allowed.random() }.joinToString("")
        .toByteArray(Charsets.UTF_8)
        .let { Base64.encodeToString(it, Base64.URL_SAFE) }
    logShow("generateNonce $token")
    return token
}

private fun logShow(msg: String) {
    Log.e("AppProtector", msg)
}

private fun showToast(context: Context, msg: String) {

    Handler(Looper.getMainLooper()).post {
        if (showError) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

}

fun Context.getSharedPrefs() =
    PreferenceManager.getDefaultSharedPreferences(this)


val Context.config: Config get() = Config.newInstance(applicationContext)

internal inline val Context.isOnline: Boolean
    get() {
        (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).let { connectivityManager ->
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)?.let {
                return it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
        }
        return false
    }

enum class LICENSE {
    SAFE, NOT_SAFE, ERROR, OLD_PLAY_STORE
}