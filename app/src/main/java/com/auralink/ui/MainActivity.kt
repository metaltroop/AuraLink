package com.auralink.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.auralink.ui.theme.AuralinkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissionManager = PermissionManager(this)

        setContent {
            AuralinkTheme {
                RequestPermissions(permissionManager = permissionManager) {
                    // Logic to execute when permissions are granted
                    // For now, we just show the content, app logic handles the rest
                }
                
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Dashboard", "Features", "Settings")
    var isRadarOpen by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            if (!isRadarOpen) {
                NavigationBar {
                    tabs.forEachIndexed { index, title ->
                        NavigationBarItem(
                            icon = { 
                                when (index) {
                                    0 -> Icon(Icons.Filled.Home, contentDescription = null)
                                    1 -> Icon(Icons.Filled.List, contentDescription = null)
                                    2 -> Icon(Icons.Filled.Settings, contentDescription = null)
                                }
                            },
                            label = { Text(title) },
                            selected = selectedTab == index,
                            onClick = { selectedTab = index }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier.padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            if (isRadarOpen) {
                Box {
                   RadarScreen()
                   IconButton(
                       onClick = { isRadarOpen = false },
                       modifier = Modifier.padding(16.dp).align(Alignment.TopStart)
                   ) {
                       Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                   }
                }
            } else {
                when (selectedTab) {
                    0 -> DashboardScreen()
                    1 -> FeaturesScreen(onOpenRadar = { isRadarOpen = true })
                    2 -> SettingsScreen()
                }
            }
        }
    }
}
