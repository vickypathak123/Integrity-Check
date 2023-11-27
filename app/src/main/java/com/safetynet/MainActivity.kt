package com.safetynet

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.safetynet.integritycheck.Interface.CheckPlayIntegrityStatus
import com.safetynet.integritycheck.integrity.AppProtector
import kotlin.math.log


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.e("parth", "onCreate: " )
        AppProtector
            .with(this)
            .appName(getString(R.string.app_name))
            .packageName(packageName)
            .checkIntegrity(object : CheckPlayIntegrityStatus {
                override fun onSuccess() {

                }
            })

    }


}