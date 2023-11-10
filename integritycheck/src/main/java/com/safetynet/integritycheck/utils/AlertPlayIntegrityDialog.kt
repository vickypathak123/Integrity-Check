package com.safetynet.integritycheck.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import com.safetynet.integritycheck.Interface.LinkTextClick
import com.safetynet.integritycheck.R
import com.safetynet.integritycheck.integrity.makeLink


object PlayIntegrityDialog {
    @SuppressLint("StaticFieldLeak")
    var alertPlayIntegrityDialog: AlertPlayIntegrityDialog? = null

    @JvmStatic
    fun show(
        activity: Activity,
        appName: String,
        packageName: String,
        callback: (isSetNow: Boolean) -> Unit
    ): AlertPlayIntegrityDialog {
        return if (alertPlayIntegrityDialog != null) {
            alertPlayIntegrityDialog!!
        } else {
            alertPlayIntegrityDialog =
                AlertPlayIntegrityDialog(activity, appName, packageName, callback)
            alertPlayIntegrityDialog!!
        }
    }
}

class AlertPlayIntegrityDialog(
    activity: Activity,
    appName: String,
    packageName: String,
    val callback: (isSetNow: Boolean) -> Unit
) {
    private var view: View

    init {
        view = activity.layoutInflater.inflate(R.layout.dialog_play_integrity, null)
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        dialog.setContentView(view)
        val txtDialogMessage = view.findViewById<TextView>(R.id.dialogMessage)
        val btnExit = view.findViewById<TextView>(R.id.btnExit)
        val btnInstallNow = view.findViewById<TextView>(R.id.btnInstallNow)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val alertMessage = activity.resources.getString(R.string.alert_message)
        activity.makeLink(
            String.format(alertMessage, appName),
            appName,
            txtDialogMessage,
            objects = object : LinkTextClick {
                override fun onClickLinkText(strType: String) {
                    try {
                        activity.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=$packageName")
                            )
                        )
                    } catch (e: ActivityNotFoundException) {
                        activity.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                            )
                        )
                    }
                }
            },
            "AppIntegrity"
        )
        btnExit.setOnClickListener {
            dialog.dismiss()
            callback(true)
        }
        btnInstallNow.setOnClickListener {
            callback(false)
        }
        dialog.show()
    }


}
