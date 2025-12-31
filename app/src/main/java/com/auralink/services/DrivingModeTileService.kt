package com.auralink.services

import android.content.Context
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class DrivingModeTileService : TileService() {

    override fun onClick() {
        super.onClick()
        val tile = qsTile
        val newState = if (tile.state == Tile.STATE_ACTIVE) {
            Tile.STATE_INACTIVE
        } else {
            Tile.STATE_ACTIVE
        }

        tile.state = newState
        tile.updateTile()

        // Persist state
        val sharedPrefs = getSharedPreferences("AuralinkPrefs", Context.MODE_PRIVATE)
        val isDriving = newState == Tile.STATE_ACTIVE
        
        sharedPrefs.edit().putBoolean("DRIVING_MODE", isDriving).apply()

        if (isDriving) {
            // Set Expiry
            val durationMins = sharedPrefs.getLong("DRIVING_MODE_DURATION", 45L)
            val expiryTime = System.currentTimeMillis() + (durationMins * 60 * 1000)
            sharedPrefs.edit().putLong("DRIVING_MODE_EXPIRY", expiryTime).apply()

            // Auto-start Service
            val intent = android.content.Intent(this, AuralinkForegroundService::class.java).apply {
                action = AuralinkForegroundService.ACTION_START
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }

    }

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile
        val sharedPrefs = getSharedPreferences("AuralinkPrefs", Context.MODE_PRIVATE)
        val isDrivingMode = sharedPrefs.getBoolean("DRIVING_MODE", false)
        
        tile.state = if (isDrivingMode) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
