package com.auralink.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService.RankingMap
import android.util.Log

class AuraNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val sharedPrefs = getSharedPreferences("AuralinkPrefs", Context.MODE_PRIVATE)
        val isWhispererEnabled = sharedPrefs.getBoolean("NOTIFICATION_WHISPERER", false)
        
        if (!isWhispererEnabled) return

        val packageName = sbn.packageName
        val whitelist = sharedPrefs.getStringSet("WHISPERER_WHITELIST", setOf("com.whatsapp", "com.google.android.apps.messaging")) ?: emptySet()

        if (packageName in whitelist) {
            val extras = sbn.notification.extras
            val title = extras.getString("android.title") ?: "Someone"
            val text = extras.getCharSequence("android.text")?.toString() ?: ""

            if (text.isNotEmpty()) {
                val appLabel = try {
                    packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
                } catch (e: Exception) {
                    "Message"
                }

                // Forward to Foreground Service for TTS announcement
                val announcement = "New $appLabel message from $title: $text"
                val intent = Intent(this, AuralinkForegroundService::class.java).apply {
                    action = AuralinkForegroundService.ACTION_ANNOUNCE_MESSAGE
                    putExtra(AuralinkForegroundService.EXTRA_MESSAGE_TEXT, announcement)
                }
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
        }
    }
}
