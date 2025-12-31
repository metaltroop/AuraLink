package com.auralink.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.auralink.services.AuralinkForegroundService

@SuppressLint("MissingPermission") // Permissions handled by PermissionManager
@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("AuralinkPrefs", Context.MODE_PRIVATE) }
    

    
    // Mutable States
    var isDrivingMode by remember { mutableStateOf(sharedPrefs.getBoolean("DRIVING_MODE", false)) }
    var isAlwaysActive by remember { mutableStateOf(sharedPrefs.getBoolean("ALWAYS_ACTIVE", false)) }
    var isServiceActive by remember { mutableStateOf(isDrivingMode || isAlwaysActive) } 
    var showPairedDevices by remember { mutableStateOf(false) }

    // Listen for changes in SharedPreferences
    DisposableEffect(key1 = sharedPrefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == "DRIVING_MODE") {
                isDrivingMode = prefs.getBoolean("DRIVING_MODE", false)
                isServiceActive = isDrivingMode || isAlwaysActive
            } else if (key == "ALWAYS_ACTIVE") {
                isAlwaysActive = prefs.getBoolean("ALWAYS_ACTIVE", false)
                isServiceActive = isDrivingMode || isAlwaysActive
            }
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    } 

    
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val adapter = bluetoothManager.adapter
    val pairedDevices = remember { adapter?.bondedDevices?.toList() ?: emptyList() }
    
    // logic to find connected device would ideally come from AudioRouter or BluetoothProfile
    // forcing a simple check for MVP visual
    val connectedDeviceName = "Bluetooth Headset (Active)" // Real impl would use AudioRouter.getConnectedDeviceName()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Service Status Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isServiceActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Service Status", style = MaterialTheme.typography.titleMedium)
                    Text(text = if (isServiceActive) "Active" else "Inactive", style = MaterialTheme.typography.bodyMedium)
                }
                Switch(
                    checked = isServiceActive,
                    onCheckedChange = { active ->
                        isServiceActive = active
                        // Sync with Always Active pref for consistency
                        sharedPrefs.edit().putBoolean("ALWAYS_ACTIVE", active).apply()
                        if (!active) {
                             // If turning OFF, also disable Driving Mode
                             sharedPrefs.edit().putBoolean("DRIVING_MODE", false).apply()
                        }
                        
                        val intent = Intent(context, AuralinkForegroundService::class.java).apply {
                            action = if (active) AuralinkForegroundService.ACTION_START else AuralinkForegroundService.ACTION_STOP
                        }
                        if (active) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                        } else {
                            context.startService(intent)
                        }
                    }
                )
            }
        }

        // Connected Device Section
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Connected Device", style = MaterialTheme.typography.titleMedium)
                Text(text = connectedDeviceName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            }
        }

        // Paired Devices Expander
        Button(
            onClick = { showPairedDevices = !showPairedDevices },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (showPairedDevices) "Hide Paired Devices" else "Show Paired Devices")
        }

        if (showPairedDevices) {
            LazyColumn {
                items(pairedDevices) { device ->
                    val deviceKey = "DEVICE_${device.address}"
                    // Default to FALSE now as per request
                    var isEnabled by remember { mutableStateOf(sharedPrefs.getBoolean(deviceKey, false)) }
                    ListItem(
                        headlineContent = { Text(device.name ?: "Unknown Device") },
                        supportingContent = { Text(device.address) },
                        trailingContent = {
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { 
                                    isEnabled = it 
                                    sharedPrefs.edit().putBoolean(deviceKey, it).apply()
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}
