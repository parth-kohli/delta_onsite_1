package com.example.bedtime

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.startActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneOffset
class BedTimeBackdrop : BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val screenOn = powerManager.isInteractive
        if (screenOn) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = DatabaseProvider.getDatabase(context)
                val bedtime = db.bedtimeDao().getbedtime()
                println(bedtime)
                if (bedtime != null) {
                    val now = LocalDateTime.now()
                    if (now.hour == bedtime.hour && now.minute == bedtime.minute && !bedtime.wifiBlocking) {

                        withContext(Dispatchers.Main) {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                                wifiManager.isWifiEnabled = false
                            } else {
                                val wifiPromptIntent =
                                    Intent(context, WifiPrompter::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                context.startActivity(wifiPromptIntent)
                            }
                            sendNotification(
                                context,
                                now.toEpochSecond(ZoneOffset.UTC).toInt(),
                                "Bedtime",
                                "Bedtime now, Wifi switched off",
                            )
                        }
                    }

                }
            }
        }
    }


    fun sendNotification(context: Context, notificationId: Int, title: String, body: String) {
        val channelId = "default_channel_id"
        val channelName = "Default Channel"

        val intent = Intent(context, com.example.bedtime.MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.plus)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply { enableLights(true); enableVibration(true); description = "Default channel for bedtime app" }
            manager.createNotificationChannel(channel)
        }
        manager.notify(notificationId, notificationBuilder.build())
    }
}
