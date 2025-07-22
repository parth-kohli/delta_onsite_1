package com.example.bedtime

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings

class WifiPrompter: Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val panelIntent = Intent(Settings.Panel.ACTION_WIFI)
        startActivityForResult(panelIntent, 100)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 100) {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (wifiManager.isWifiEnabled) {
                val repeatIntent = Intent(this, WifiPrompter::class.java)
                repeatIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(repeatIntent)
            } else {
                finish()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}