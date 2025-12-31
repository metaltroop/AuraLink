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
    }

    override fun onCreate() {
        super.onCreate()
        ttsManager = TTSManager(this)
        audioRouter = AudioRouter(this)
        contactRepository = ContactRepository(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_STOP) {
            startForegroundService()
        }

        when (intent?.action) {
            ACTION_STOP -> stopForegroundService()
            ACTION_INCOMING_CALL -> handleIncomingCall(intent.getStringExtra(EXTRA_PHONE_NUMBER))
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
                // Ideally we also update the Tile, but TileService logic updates on click/listening.
            }
        }

        // Logic: active if (Always Active) OR (Effective Driving Mode).
        val shouldAnnounce = isAlwaysActive || effectiveDrivingMode

        if (!shouldAnnounce) {
            android.util.Log.d("AuralinkService", "Skipping: Neither Always Active nor Driving Mode is ON")
            return
        }

        // Bluetooth check: only announce if headset is connected
        if (!audioRouter.isBluetoothHeadsetConnected()) {
            android.util.Log.d("AuralinkService", "Skipping: Bluetooth headset not connected")
            return
        }

        launch {
            val name = contactRepository.getContactName(phoneNumber)
            announceCaller(name)
        }
    }

    private fun announceCaller(name: String) {
        audioRouter.enableBluetoothSCO()
        // Provide a small delay for SCO to connect
        launch(Dispatchers.IO) {
            Thread.sleep(1000)
            withContext(Dispatchers.Main) {
                ttsManager.speak("Incoming call from $name")
            }
            Thread.sleep(4000) 
            // Cleanup after announcement
            withContext(Dispatchers.Main) {
                // Optional: Just let call take over, or disable SCO if desired to revert to ringtone
                // audioRouter.disableBluetoothSCO() 
            }
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
        job.cancel()
        ttsManager.shutdown()
        audioRouter.disableBluetoothSCO()
        super.onDestroy()
    }
}
