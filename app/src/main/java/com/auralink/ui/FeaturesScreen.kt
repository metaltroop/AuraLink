package com.auralink.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun FeaturesScreen(onOpenRadar: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("AuralinkPrefs", Context.MODE_PRIVATE) }
    
    var isWhispererEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("NOTIFICATION_WHISPERER", false)) }
    var isBatteryAlertEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("BATTERY_ALERTS", false)) }
    var whitelist by remember { 
        mutableStateOf(sharedPrefs.getStringSet("WHISPERER_WHITELIST", setOf("com.whatsapp", "com.google.android.apps.messaging"))?.toList() ?: emptyList()) 
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Pro Features", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 16.dp))

        // Notification Whisperer Card
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Notification Whisperer", style = MaterialTheme.typography.titleLarge)
                        Text(text = "Read messages aloud via Bluetooth", style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(
                        checked = isWhispererEnabled,
                        onCheckedChange = { 
                            isWhispererEnabled = it
                            sharedPrefs.edit().putBoolean("NOTIFICATION_WHISPERER", it).apply()
                        }
                    )
                }

                if (isWhispererEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Whitelisted Apps", style = MaterialTheme.typography.titleSmall)
                    
                    whitelist.forEach { pkg ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(text = pkg, style = MaterialTheme.typography.bodySmall)
                            IconButton(onClick = {
                                val newSet = whitelist.toMutableSet()
                                newSet.remove(pkg)
                                whitelist = newSet.toList()
                                sharedPrefs.edit().putStringSet("WHISPERER_WHITELIST", newSet).apply()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove")
                            }
                        }
                    }
                    
                    Button(
                        onClick = {
                            // Simple way to add apps for MVP, real would use an AppPicker
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Permission & App Stats")
                    }
                }
            }
        }

        // Battery Alert Card
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Battery Alerts", style = MaterialTheme.typography.titleLarge)
                        Text(text = "Voice warning when buds drop below 20%", style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(
                        checked = isBatteryAlertEnabled,
                        onCheckedChange = { 
                            isBatteryAlertEnabled = it
                            sharedPrefs.edit().putBoolean("BATTERY_ALERTS", it).apply()
                        }
                    )
                }
            }
        }
        
        // Find My Buds (Radar) Card
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Find My Buds", style = MaterialTheme.typography.titleLarge)
                        Text(text = "Locate lost earbuds using signal strength", style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(onClick = { 
                        // We'll handle this via navigation state in MainScreen
                        onOpenRadar()
                    }) {
                        Text("Open Radar")
                    }
                }
            }
        }
        
        Text(text = "More features coming soon (Voice Commands)...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
    }
}
