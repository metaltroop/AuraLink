package com.auralink.services

import android.content.Intent
import android.net.Uri
import android.telecom.Call
import android.telecom.CallScreeningService

class AuraCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.handle?.schemeSpecificPart
        
        if (phoneNumber != null) {
            val sharedPrefs = getSharedPreferences("AuralinkPrefs", android.content.Context.MODE_PRIVATE)
            val isDrivingMode = sharedPrefs.getBoolean("DRIVING_MODE", false)
            val isAlwaysActive = sharedPrefs.getBoolean("ALWAYS_ACTIVE", false)
            
            android.util.Log.d("AuraCallScreening", "Incoming call: $phoneNumber. Driving: $isDrivingMode, Always: $isAlwaysActive")

            if (!isDrivingMode && !isAlwaysActive) {
                android.util.Log.d("AuraCallScreening", "Skipping: Features disabled.")
                // Feature is disabled, do not start service
                respondToCall(callDetails, CallResponse.Builder().build())
                return
            }

            // Forward directly to the Foreground Service
            val intent = Intent(this, AuralinkForegroundService::class.java).apply {
                action = AuralinkForegroundService.ACTION_INCOMING_CALL
                putExtra(AuralinkForegroundService.EXTRA_PHONE_NUMBER, phoneNumber)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }

        // Allow the call to proceed as normal
        respondToCall(callDetails, CallResponse.Builder().build())
    }
}
