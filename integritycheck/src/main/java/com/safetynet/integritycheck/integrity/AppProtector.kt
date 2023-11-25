package com.safetynet.integritycheck.integrity

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.text.LineBreaker
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.preference.PreferenceManager
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.MetricAffectingSpan
import android.util.Base64
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat

import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityServiceException
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.android.play.core.integrity.model.IntegrityErrorCode
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.playintegrity.v1.PlayIntegrity
import com.google.api.services.playintegrity.v1.PlayIntegrityRequestInitializer
import com.google.api.services.playintegrity.v1.model.DecodeIntegrityTokenRequest
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import com.safetynet.integritycheck.Interface.CheckPlayIntegrityStatus
import com.safetynet.integritycheck.Interface.LinkTextClick
import com.safetynet.integritycheck.R
import com.safetynet.integritycheck.google_consent.GoogleMobileAdsConsentManager
import com.safetynet.integritycheck.utils.Config
import com.safetynet.integritycheck.utils.PlayIntegrityDialog

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

    @JvmName("deviceId")
    fun deviceId(deviceId: String) = this@CheckIntegrity.apply {
        this.deviceId = deviceId
    }

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
                    requestIntegrityToken(context, packageName) { licensingVerdict ->
                        when (licensingVerdict) {
                            LICENSE.NOT_SAFE -> {
                                (context as Activity).runOnUiThread {
                                    PlayIntegrityDialog.show(context, appName, packageName)
                                    {
                                        if (!it) {
                                            val appPackageName = packageName
                                            try {
                                                context.startActivity(
                                                    Intent(
                                                        Intent.ACTION_VIEW,
                                                        Uri.parse("market://details?id=$appPackageName")
                                                    )
                                                )
                                            } catch (e: ActivityNotFoundException) {
                                                context.startActivity(
                                                    Intent(
                                                        Intent.ACTION_VIEW,
                                                        Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                                                    )
                                                )
                                            }
                                        } else
                                            context.finishAffinity()
                                        exitProcess(0)
                                    }
                                }
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


private fun requestIntegrityToken(
    context: Context,
    packageName: String,
    callback: (LICENSE) -> Unit
) {
    try {
        logShow("${context.getDate()}")
        logShow("${context.config.isToday}")
        logShow("requestIntegrityToken")

        if (context.config.isToday == context.getDate()) {

            if (context.config.isPlayIntegrityCheck) {
                callback(LICENSE.SAFE)
            } else {
                callback(LICENSE.NOT_SAFE)
            }
            (context as Activity).runOnUiThread {
                if (com.safetynet.integritycheck.BuildConfig.DEBUG) {
                    Toast.makeText(context, "old request", Toast.LENGTH_SHORT).show()
                }
            }
            return
        } else {
            context.config.isToday = context.getDate()
        }
        (context as Activity).runOnUiThread {
            if (com.safetynet.integritycheck.BuildConfig.DEBUG) {
                Toast.makeText(context, "new request", Toast.LENGTH_SHORT).show()
            }
        }
        val myIntegrityManager = IntegrityManagerFactory.create(context)
        val myIntegrityTokenResponse = myIntegrityManager.requestIntegrityToken(
            IntegrityTokenRequest.builder()
                .setNonce(generateNonce())
                .build()

        )

        myIntegrityTokenResponse.addOnFailureListener { exception ->
            try {
                val errorMessage = getErrorText(exception as IntegrityServiceException)
                if (com.safetynet.integritycheck.BuildConfig.DEBUG) {
                    Toast.makeText(context, "error failure $errorMessage", Toast.LENGTH_SHORT)
                        .show()
                }
                logShow("addOnFailureListener $errorMessage")
                when (errorMessage) {
                    "-100", "-12", "-1", "-9", "-3", "-8" -> {
                        callback(LICENSE.SAFE)
                        context.config.isPlayIntegrityCheck = true
                    }

                    else -> {
                        callback(LICENSE.NOT_SAFE)
                        context.config.isPlayIntegrityCheck = false
                    }
                }
            } catch (e: Exception) {
                logShow("catch addOnFailureListener $e")
                logShow("addOnFailureListener $exception")
                callback(LICENSE.SAFE)
                context.config.isPlayIntegrityCheck = true
            }
        }



        myIntegrityTokenResponse.addOnCanceledListener {
            logShow("addOnCanceledListener")
        }

        myIntegrityTokenResponse.addOnSuccessListener { response ->
            try {
                logShow("addOnSuccessListener")
                val token = response.token()
                val requestObj = DecodeIntegrityTokenRequest()
                requestObj.integrityToken = token

                logShow("token : ${token}")
                logShow("requestObj.integrityToken : ${requestObj.integrityToken}")


                logShow("path ${context.javaClass.classLoader?.getResourceAsStream("credentials.json")}")
                val credentials = GoogleCredentials.fromStream(
                    requireNotNull(context.javaClass.classLoader?.getResourceAsStream("credentials.json"))
                )
                val requestInitializer: HttpRequestInitializer = HttpCredentialsAdapter(credentials)

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
                        val apiResponse =
                            play.v1().decodeIntegrityToken(packageName, requestObj).execute()
                        logShow("apiResponse: ${apiResponse.tokenPayloadExternal}")
                        logShow("apiResponse: ${apiResponse.tokenPayloadExternal}")
                        val accountDetails =
                            apiResponse.tokenPayloadExternal.accountDetails.appLicensingVerdict
                        logShow("accountDetails: $accountDetails")
                        val appIntegrity =
                            apiResponse.tokenPayloadExternal.appIntegrity.appRecognitionVerdict
                        logShow("appIntegrity: $appIntegrity")
                        if (accountDetails == "LICENSED") {
                            callback(LICENSE.SAFE)
                            context.config.isPlayIntegrityCheck = true
                        } else {
                            callback(LICENSE.NOT_SAFE)
                            context.config.isPlayIntegrityCheck = false
                        }
                    } catch (e: Exception) {
                        callback(LICENSE.SAFE)
                        context.config.isPlayIntegrityCheck = true
                        logShow("LICENSE.ERROR 1 : ${e.message}")
                        e.printStackTrace()
                    }
                }
                thread.start()
            } catch (e: Exception) {
                e.printStackTrace()
                callback(LICENSE.SAFE)
                context.config.isPlayIntegrityCheck = true
                logShow("LICENSE.ERROR 2 : ${e.message}")
            }
        }
    } catch (e: Exception) {
        callback(LICENSE.SAFE)
        context.config.isPlayIntegrityCheck = true
        logShow("requestIntegrityToken $e")
    }
}


private fun getErrorText(it: IntegrityServiceException): String {
    when (it.errorCode) {
        IntegrityErrorCode.API_NOT_AVAILABLE -> return "-1"
        IntegrityErrorCode.APP_NOT_INSTALLED -> return "-5"
        IntegrityErrorCode.APP_UID_MISMATCH -> return "-7"
        IntegrityErrorCode.CANNOT_BIND_TO_SERVICE -> return "-9"
        IntegrityErrorCode.CLIENT_TRANSIENT_ERROR -> return "-17"
        IntegrityErrorCode.CLOUD_PROJECT_NUMBER_IS_INVALID -> return "-16"
        IntegrityErrorCode.GOOGLE_SERVER_UNAVAILABLE -> return "-12"
        IntegrityErrorCode.INTERNAL_ERROR -> return "-100"
        IntegrityErrorCode.NETWORK_ERROR -> return "-3"
        IntegrityErrorCode.NONCE_IS_NOT_BASE64 -> return "-13"
        IntegrityErrorCode.NONCE_TOO_LONG -> return "-11"
        IntegrityErrorCode.NONCE_TOO_SHORT -> return "-10"
        IntegrityErrorCode.NO_ERROR -> return "0"
        IntegrityErrorCode.PLAY_SERVICES_NOT_FOUND -> return "-6"
        IntegrityErrorCode.PLAY_SERVICES_VERSION_OUTDATED -> return "-15"
        IntegrityErrorCode.PLAY_STORE_ACCOUNT_NOT_FOUND -> return "-4"
        IntegrityErrorCode.PLAY_STORE_NOT_FOUND -> return "-2"
        IntegrityErrorCode.PLAY_STORE_VERSION_OUTDATED -> return "-14"
        IntegrityErrorCode.TOO_MANY_REQUESTS -> return "-8"
    }
    return "0"
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

fun Context.getSharedPrefs() =
    PreferenceManager.getDefaultSharedPreferences(this)


val Context.config: Config get() = Config.newInstance(applicationContext)
fun Context.getDate(): String {
    val sdf = SimpleDateFormat("dd/M/yyyy")
    val currentDate = sdf.format(Date())
    System.out.println(" C DATE is  " + currentDate)
    logShow("getDate $currentDate")
    return currentDate
}

fun Context.makeLink(
    strSentence: String,
    strLinkWord: String,
    txtView: TextView,
    objects: LinkTextClick,
    strType: String
) {

    val spannableText = SpannableString(strSentence)
    val startIndex = strSentence.indexOf(strLinkWord)
    val endIndex = startIndex + strLinkWord.length
    val clickableSpan = object : ClickableSpan() {
        override fun onClick(widget: View) {
            objects.onClickLinkText(strType)
        }

        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.isUnderlineText = false
            val value = TypedValue()
            theme.resolveAttribute(R.attr.dialog_link_text_color, value, true)
            ds.color = value.data
        }
    }
    val customFont = ResourcesCompat.getFont(this, R.font.roboto)
    val customTypefaceSpan = CustomTypefaceSpan(customFont!!)
    if (startIndex != -1) {
        spannableText.setSpan(
            clickableSpan,
            startIndex,
            endIndex,
            Spannable.SPAN_INCLUSIVE_INCLUSIVE
        )
    }
    spannableText.setSpan(
        customTypefaceSpan,
        0,
        strSentence.length,
        Spannable.SPAN_INCLUSIVE_INCLUSIVE
    )
// Make sure to set the movement method on the TextView to handle the clicks
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        txtView.justificationMode = LineBreaker.JUSTIFICATION_MODE_INTER_WORD

    }
    txtView.textAlignment = TextView.TEXT_ALIGNMENT_TEXT_START
    txtView.movementMethod = LinkMovementMethod.getInstance()
    txtView.text = spannableText

}

class CustomTypefaceSpan(private val typeface: Typeface) : MetricAffectingSpan() {

    override fun updateDrawState(ds: TextPaint) {
        applyCustomTypeface(ds)
    }

    override fun updateMeasureState(paint: TextPaint) {
        applyCustomTypeface(paint)
    }

    private fun applyCustomTypeface(paint: Paint) {
        val oldTypeface = paint.typeface
        val oldStyle = oldTypeface?.style ?: 0

        val newTypeface = Typeface.create(typeface, oldStyle)
        paint.typeface = newTypeface
    }
}


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

