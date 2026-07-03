package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.example.data.UserProfile
import com.example.data.FirebaseManager

@Composable
fun MainApp() {
    val context = LocalContext.current
    var isFirebaseInitialized by remember { mutableStateOf(false) }
    var initializationChecked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                isFirebaseInitialized = true
                FirebaseManager.isMockMode = false
            } else {
                isFirebaseInitialized = false
                FirebaseManager.isMockMode = true
            }
        } catch (e: Exception) {
            isFirebaseInitialized = false
            FirebaseManager.isMockMode = true
        }
        initializationChecked = true
    }

    if (!initializationChecked) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    AppContent()
}

@Composable
fun FirebaseSetupScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Card(Modifier.padding(32.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Firebase Configuration Required", style = MaterialTheme.typography.titleLarge)
                Text("Please add your google-services.json file to the app directory (via AI Studio settings/files) to enable Authentication, Storage, and Firestore.")
            }
        }
    }
}

@Composable
fun AppContent() {
    val navController = rememberNavController()
    
    // Auth state wrapper
    var isAuthenticated by remember { mutableStateOf(FirebaseManager.isMockMode) }
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            if (FirebaseManager.isMockMode) {
                userProfile = FirebaseManager.getUserProfile(FirebaseManager.currentMockUserId)
            } else {
                val auth = try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
                if (auth?.currentUser != null) {
                    val uid = auth.currentUser!!.uid
                    val email = auth.currentUser!!.email ?: ""
                    userProfile = FirebaseManager.getUserProfile(uid)
                    if (userProfile == null) {
                        FirebaseManager.createUserProfile(uid, email)
                        userProfile = FirebaseManager.getUserProfile(uid)
                    }
                }
            }
        }
    }

    if (!isAuthenticated) {
        LoginScreen(onLoginSuccess = { isAuthenticated = true })
    } else {
        Scaffold(
            // Removed bottom bar
        ) { padding ->
            NavHost(navController, startDestination = "feed", modifier = Modifier.padding(padding)) {
                composable("feed") { FeedScreen(navController, userProfile) }
                composable("upload") { UploadScreen(onBack = { navController.popBackStack() }) }
                composable("preview") { PreviewScreen(onBack = { navController.popBackStack() }) }
                composable("admin") { AdminScreen(onBack = { navController.popBackStack() }) }
                composable("settings") { 
                    SettingsScreen(
                        userProfile = userProfile,
                        onNavigateToAdmin = { navController.navigate("admin") },
                        onNavigateToUpload = { navController.navigate("upload") },
                        onNavigateToPreview = { navController.navigate("preview") },
                        onBack = { navController.popBackStack() },
                        onLogout = {
                            if (!FirebaseManager.isMockMode) {
                                try { FirebaseAuth.getInstance().signOut() } catch (e: Exception) {}
                            }
                            isAuthenticated = false
                        }
                    )
                }
            }
        }
    }
}
