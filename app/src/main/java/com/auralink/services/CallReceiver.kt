package com.auralink.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                Log.d("CallReceiver", "Incoming call detected: $incomingNumber")
                
                // Trigger service to announce
                val serviceIntent = Intent(context, AuralinkForegroundService::class.java).apply {
                    action = AuralinkForegroundService.ACTION_INCOMING_CALL
                    putExtra(AuralinkForegroundService.EXTRA_PHONE_NUMBER, incomingNumber)
                }
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    Log.e("CallReceiver", "Failed to start service", e)
                }
            }
        }
    }
}
