package com.safetynet.integritycheck.utils

import android.content.Context
import com.safetynet.integritycheck.integrity.getSharedPrefs

import java.util.*

class Config(context: Context) {
    val prefs = context.getSharedPrefs()

    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    var isCheckConsent: Boolean
        get() = prefs.getBoolean("IS_CHECK_CONSENT", true)
        set(isCheck) = prefs.edit().putBoolean("IS_CHECK_CONSENT", isCheck).apply()
    var isPlayIntegrityCheck: Boolean
        get() = prefs.getBoolean("IS_PLAY_INTEGRITY_CHECK", false)
        set(isCheck) = prefs.edit().putBoolean("IS_PLAY_INTEGRITY_CHECK", isCheck).apply()

    var isToday: String?
        get() = prefs.getString("IS_TODAY","")
        set(todayDate) = prefs.edit().putString("IS_TODAY", todayDate).apply()

    var playData: String?
        get() = prefs.getString("play_Integrity","")
        set(playData) = prefs.edit().putString("play_Integrity", playData).apply()

}