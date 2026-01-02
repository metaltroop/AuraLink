package com.auralink.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.content.IntentFilter
import androidx.core.app.NotificationCompat
import com.auralink.data.ContactRepository
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class AuralinkForegroundService : Service(), CoroutineScope {

    private lateinit var ttsManager: TTSManager
    private lateinit var audioRouter: AudioRouter
    private lateinit var contactRepository: ContactRepository
    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
    
    companion object {
        const val CHANNEL_ID = "AuralinkServiceChannel"
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_INCOMING_CALL = "ACTION_INCOMING_CALL"
        const val EXTRA_PHONE_NUMBER = "EXTRA_PHONE_NUMBER"
        const val ACTION_ANNOUNCE_MESSAGE = "ACTION_ANNOUNCE_MESSAGE"
        const val EXTRA_MESSAGE_TEXT = "EXTRA_MESSAGE_TEXT"
    }

    private val batteryReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED") {
                val level = intent.getIntExtra("android.bluetooth.device.extra.BATTERY_LEVEL", -1)
                
                if (level != -1 && level <= 20) {
                    val sharedPrefs = getSharedPreferences("AuralinkPrefs", Context.MODE_PRIVATE)
                    val isBatteryAlertEnabled = sharedPrefs.getBoolean("BATTERY_ALERTS", false)
                    val lastAlertTime = sharedPrefs.getLong("LAST_BATTERY_ALERT_TIME", 0)
                    val currentTime = System.currentTimeMillis()
                    
                    // Alert only once every 30 minutes to avoid spamming
                    if (isBatteryAlertEnabled && (currentTime - lastAlertTime > 30 * 60 * 1000)) {
                        announceBatteryLow(level)
                        sharedPrefs.edit().putLong("LAST_BATTERY_ALERT_TIME", currentTime).apply()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        ttsManager = TTSManager(this)
        audioRouter = AudioRouter(this)
        contactRepository = ContactRepository(this)
        createNotificationChannel()

        // Setup completion listener for ringtone restoration
        ttsManager.setCompletionListener {
            android.util.Log.d("AuralinkService", "Announcement done. Restoring audio.")
            audioRouter.restoreRingtone()
            audioRouter.disableBluetoothSCO()
        }

        val filter = IntentFilter().apply {
            addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED")
        }
        registerReceiver(batteryReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_STOP) {
            android.util.Log.d("AuralinkService", "Starting Foreground Service")
            startForegroundService()
        }

        when (intent?.action) {
            ACTION_STOP -> stopForegroundService()
            ACTION_INCOMING_CALL -> handleIncomingCall(intent.getStringExtra(EXTRA_PHONE_NUMBER))
            ACTION_ANNOUNCE_MESSAGE -> handleMessageAnnouncement(intent.getStringExtra(EXTRA_MESSAGE_TEXT))
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(1, notification)
        }
    }

    private fun stopForegroundService() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun handleIncomingCall(phoneNumber: String?) {
        if (phoneNumber == null) return

        val sharedPrefs = getSharedPreferences("AuralinkPrefs", Context.MODE_PRIVATE)
        val isDrivingMode = sharedPrefs.getBoolean("DRIVING_MODE", false)
        val isAlwaysActive = sharedPrefs.getBoolean("ALWAYS_ACTIVE", false)

        var effectiveDrivingMode = isDrivingMode
        // Check Expiry for Driving Mode
        if (isDrivingMode) {
            val expiryTime = sharedPrefs.getLong("DRIVING_MODE_EXPIRY", 0L)
            if (System.currentTimeMillis() > expiryTime) {
                android.util.Log.d("AuralinkService", "Driving Mode Expired. Disabling.")
                effectiveDrivingMode = false
                sharedPrefs.edit().putBoolean("DRIVING_MODE", false).apply()
            }
        }

        // Logic: active if (Always Active) OR (Effective Driving Mode).
        val shouldAnnounce = isAlwaysActive || effectiveDrivingMode

        android.util.Log.d("AuralinkService", "Handling Incoming Call. Driving: $effectiveDrivingMode, Always: $isAlwaysActive")

        if (!shouldAnnounce) {
            android.util.Log.d("AuralinkService", "Skipping: Neither Always Active nor Driving Mode is ON")
            stopSelf()
            return
        }

        // Bluetooth check: only announce if headset is connected
        val isHeadsetConnected = audioRouter.isBluetoothHeadsetConnected()
        android.util.Log.d("AuralinkService", "Bluetooth Headset Connected: $isHeadsetConnected")

        if (!isHeadsetConnected) {
            android.util.Log.d("AuralinkService", "Skipping: Bluetooth headset not connected")
            stopSelf()
            return
        }

        launch {
            val name = contactRepository.getContactName(phoneNumber)
            
            // Audio Prep: Mute ringtone and enable SCO
            audioRouter.muteRingtone()
            audioRouter.enableBluetoothSCO()
            
            // Provide a small delay for SCO to connect (hardware dependent)
            delay(1200) 
            announceCaller(name)
        }
    }

    private fun announceCaller(name: String) {
        val sharedPrefs = getSharedPreferences("AuralinkPrefs", Context.MODE_PRIVATE)
        val delayMs = sharedPrefs.getLong("ANNOUNCEMENT_DELAY", 1000L)
        
        launch {
            delay(delayMs)
            ttsManager.speak("Incoming call from $name")
        }
    }

    private fun announceBatteryLow(level: Int) {
        if (!audioRouter.isBluetoothHeadsetConnected()) return
        
        launch {
            audioRouter.muteRingtone()
            audioRouter.enableBluetoothSCO()
            delay(1200)
            ttsManager.speak("Warning: Your earbuds battery is at $level percent.")
        }
    }

    private fun handleMessageAnnouncement(message: String?) {
        if (message == null) return

        if (!audioRouter.isBluetoothHeadsetConnected()) return

        launch {
            audioRouter.muteRingtone() // Reusing ringtone mute for notification ducking
            audioRouter.enableBluetoothSCO()
            delay(1200)
            ttsManager.speak(message)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Auralink Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Auralink Active")
            .setContentText("Listening for incoming calls...")
            .setSmallIcon(android.R.drawable.ic_menu_call) // Placeholder icon
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterReceiver(batteryReceiver)
        job.cancel()
        ttsManager.shutdown()
        audioRouter.disableBluetoothSCO()
        super.onDestroy()
    }
}
