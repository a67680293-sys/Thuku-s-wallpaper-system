package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.FirebaseManager
import com.example.data.UserProfile
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(onBack: () -> Unit) {
    var users by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var wallpaperCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        users = FirebaseManager.getAllUsers()
        wallpaperCount = FirebaseManager.getWallpapers().size
        isLoading = false
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Admin Panel") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Total Users", style = MaterialTheme.typography.titleMedium)
                        Text("${users.size}", style = MaterialTheme.typography.displaySmall)
                    }
                }
                Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Wallpapers", style = MaterialTheme.typography.titleMedium)
                        Text("$wallpaperCount", style = MaterialTheme.typography.displaySmall)
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            Text("User Management", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(users) { user ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(user.email, style = MaterialTheme.typography.bodyLarge)
                                Text("Role: ${user.role}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (user.role != "admin") {
                                val newRole = if (user.role == "staff") "user" else "staff"
                                Button(onClick = {
                                    scope.launch {
                                        FirebaseManager.setUserRole(user.uid, newRole)
                                        users = FirebaseManager.getAllUsers()
                                    }
                                }) {
                                    Text(if (newRole == "staff") "Make Staff" else "Revoke Staff")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}
