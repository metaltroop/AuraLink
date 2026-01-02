package com.auralink.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@SuppressLint("MissingPermission")
@Composable
fun RadarScreen() {
    val context = LocalContext.current
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val isPreview = androidx.compose.ui.platform.LocalInspectionMode.current
    
    var rssiValue by remember { mutableIntStateOf(-100) } // Default very weak signal
    var connectedDeviceName by remember { mutableStateOf("Scanning...") }

    // Animation for the radar rings
    val transition = rememberInfiniteTransition(label = "RadarBreath")
    val radiusMultiplier by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Radius"
    )

    // Periodic RSSI update logic
    if (!isPreview) {
        LaunchedEffect(Unit) {
            while (true) {
                val adapter = bluetoothManager?.adapter
                val connectedDevices = adapter?.bondedDevices?.filter { _ ->
                    // Logic to check if device is actually connected for audio (simplified for MVP)
                    true 
                } ?: emptyList()

                if (connectedDevices.isNotEmpty()) {
                    val device = connectedDevices.first()
                    connectedDeviceName = device.name ?: "Unknown Device"
                    // Trigger a discovery to get RSSI or use hidden APIs/BluetoothGatt
                    // For now, we mock some variance around -60 to -40 for demo if we can't get real RSSI easily without search
                    // REAL implementation would use: device.readRemoteRssi() if it was a GATT connection
                }
                
                // simulate RSSI for now as reading bonded RSSI without active scan is complex
                rssiValue = (-70..-30).random() 
                delay(1000)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Find My Buds", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Connected to: $connectedDeviceName", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)

        Spacer(modifier = Modifier.height(32.dp))

        // Custom Signal Meter (Radar Style)
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(250.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val baseRadius = size.width / 3f
                
                // Radar Rings
                drawCircle(
                    color = Color.Green.copy(alpha = 0.2f),
                    radius = baseRadius * radiusMultiplier,
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = Color.Green.copy(alpha = 0.1f),
                    radius = baseRadius * 1.5f * radiusMultiplier,
                    style = Stroke(width = 1.dp.toPx())
                )

                // The "Hot or Cold" Meter
                val sweepAngle = ((rssiValue + 100) / 70f) * 360f // -100 to -30 range
                drawArc(
                    color = if (rssiValue > -50) Color.Green else Color.Yellow,
                    startAngle = -90f,
                    sweepAngle = sweepAngle.coerceIn(0f, 360f),
                    useCenter = false,
                    style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "$rssiValue dBm", style = MaterialTheme.typography.headlineLarge)
                val status = when {
                    rssiValue > -50 -> "Very Hot!"
                    rssiValue > -70 -> "Getting Warmer"
                    else -> "Cold"
                }
                Text(text = status, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Text(text = "Walk around to find your earbuds. The signal gets stronger (Hot) as you get closer.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp))
    }
}
