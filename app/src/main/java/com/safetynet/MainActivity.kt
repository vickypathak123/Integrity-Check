package com.safetynet

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.safetynet.integritycheck.Interface.CheckPlayIntegrityStatus
import com.safetynet.integritycheck.integrity.AppProtector


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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