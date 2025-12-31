package com.auralink.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("AuralinkPrefs", Context.MODE_PRIVATE) }
    
    // State
    var volume by remember { mutableFloatStateOf(sharedPrefs.getFloat("SPEECH_VOLUME", 1.0f)) }
    var delay by remember { mutableFloatStateOf(sharedPrefs.getLong("ANNOUNCEMENT_DELAY", 1000L) / 1000f) }
    var drivingDuration by remember { mutableFloatStateOf(sharedPrefs.getLong("DRIVING_MODE_DURATION", 45L).toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Volume Slider
        Text(text = "Speech Volume: ${(volume * 100).toInt()}%")
        Slider(
            value = volume,
            onValueChange = { 
                volume = it
                sharedPrefs.edit().putFloat("SPEECH_VOLUME", it).apply()
            },
            valueRange = 0f..1f
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Delay Slider
        Text(text = "Announcement Delay: ${delay.toInt()}s")
        Slider(
            value = delay,
            onValueChange = { 
                delay = it
                sharedPrefs.edit().putLong("ANNOUNCEMENT_DELAY", (it * 1000).toLong()).apply()
            },
            valueRange = 0f..5f,
            steps = 4
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Driving Mode Duration Slider
        Text(text = "Driving Mode Duration: ${drivingDuration.toInt()} mins")
        Slider(
            value = drivingDuration,
            onValueChange = { 
                drivingDuration = it
                sharedPrefs.edit().putLong("DRIVING_MODE_DURATION", it.toLong()).apply()
            },
            valueRange = 15f..120f,
            steps = 6 // 15, 30, 45, 60, 75, 90, 105, 120 approx steps
        )
    }
}
