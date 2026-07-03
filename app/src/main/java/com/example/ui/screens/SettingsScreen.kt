package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.example.data.SettingsKeys
import com.example.data.dataStore
import com.example.data.UserProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userProfile: UserProfile?,
    onNavigateToAdmin: () -> Unit,
    onNavigateToUpload: () -> Unit,
    onNavigateToPreview: () -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs by context.dataStore.data.collectAsState(initial = null)
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
    }

    if (prefs == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!hasOverlayPermission) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Overlay permission is required.", color = MaterialTheme.colorScheme.onErrorContainer)
                        Button(onClick = {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                            overlayPermissionLauncher.launch(intent)
                        }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                            Text("Grant Permission")
                        }
                    }
                }
            }

            if (userProfile?.role == "admin" || userProfile?.role == "staff") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Staff Options", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        if (userProfile.role == "admin") {
                            Button(onClick = onNavigateToAdmin, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Admin Panel")
                            }
                        }
                        Button(onClick = onNavigateToUpload, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.AddCircle, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Upload Wallpaper")
                        }
                    }
                }
            }
            
            Button(onClick = onNavigateToPreview, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                Icon(Icons.Default.Preview, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Test Preview Mode")
            }

            Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                
                Column {
                    Text("Icon Size: ${prefs!![SettingsKeys.SIZE] ?: 64}dp")
                    Slider(
                        value = (prefs!![SettingsKeys.SIZE] ?: 64).toFloat(),
                        onValueChange = { v -> scope.launch { context.dataStore.edit { it[SettingsKeys.SIZE] = v.toInt() } } },
                        valueRange = 40f..120f
                    )
                }

                Column {
                    Text("Opacity: ${((prefs!![SettingsKeys.OPACITY] ?: 1f) * 100).toInt()}%")
                    Slider(
                        value = prefs!![SettingsKeys.OPACITY] ?: 1f,
                        onValueChange = { v -> scope.launch { context.dataStore.edit { it[SettingsKeys.OPACITY] = v } } },
                        valueRange = 0.2f..1f
                    )
                }
                
                Column {
                    Text("Shape Mask")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val currentShape = prefs!![SettingsKeys.SHAPE] ?: "circle"
                        listOf("circle", "square", "rounded").forEach { shape ->
                            FilterChip(
                                selected = currentShape == shape,
                                onClick = { scope.launch { context.dataStore.edit { it[SettingsKeys.SHAPE] = shape } } },
                                label = { Text(shape.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Behavior & Effects", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Lock Position")
                    Switch(
                        checked = prefs!![SettingsKeys.IS_LOCKED] ?: false,
                        onCheckedChange = { v -> scope.launch { context.dataStore.edit { it[SettingsKeys.IS_LOCKED] = v } } }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Glow Effect")
                    Switch(
                        checked = prefs!![SettingsKeys.GLOW_EFFECT] ?: false,
                        onCheckedChange = { v -> scope.launch { context.dataStore.edit { it[SettingsKeys.GLOW_EFFECT] = v } } }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Border Effect")
                    Switch(
                        checked = prefs!![SettingsKeys.BORDER_EFFECT] ?: false,
                        onCheckedChange = { v -> scope.launch { context.dataStore.edit { it[SettingsKeys.BORDER_EFFECT] = v } } }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Loop Video")
                    Switch(
                        checked = prefs!![SettingsKeys.LOOP_VIDEO] ?: false,
                        onCheckedChange = { v -> scope.launch { context.dataStore.edit { it[SettingsKeys.LOOP_VIDEO] = v } } }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Enable Audio")
                    Switch(
                        checked = prefs!![SettingsKeys.AUDIO_ENABLED] ?: true,
                        onCheckedChange = { v -> scope.launch { context.dataStore.edit { it[SettingsKeys.AUDIO_ENABLED] = v } } }
                    )
                }
            }
        }
        
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
            Text("Logout")
        }
    }
}
}
