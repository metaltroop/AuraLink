package com.auralink.services

import android.content.Context
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class AlwaysActiveTileService : TileService() {

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
        val isActive = newState == Tile.STATE_ACTIVE
        sharedPrefs.edit().putBoolean("ALWAYS_ACTIVE", isActive).apply()

        if (isActive) {
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
        val isAlwaysActive = sharedPrefs.getBoolean("ALWAYS_ACTIVE", false)
        
        tile.state = if (isAlwaysActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
