package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.FirebaseManager
import com.example.data.Wallpaper
import com.example.data.UserProfile
import com.example.data.SettingsKeys
import com.example.data.dataStore
import androidx.datastore.preferences.core.edit
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.media3.ui.PlayerView
import android.net.Uri
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.navigation.NavController
import kotlin.math.abs
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(navController: NavController, userProfile: UserProfile?) {
    val context = LocalContext.current
    var wallpapers by remember { mutableStateOf<List<Wallpaper>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    var selectedWallpaper by remember { mutableStateOf<Wallpaper?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

    val tabs = listOf("All", "Images", "Videos")

    LaunchedEffect(selectedWallpaper, selectedTab) {
        if (selectedWallpaper == null) {
            val allWallpapers = FirebaseManager.getWallpapers()
            wallpapers = when (selectedTab) {
                1 -> allWallpapers.filter { it.type == "image" }
                2 -> allWallpapers.filter { it.type == "video" }
                else -> allWallpapers
            }
            isLoading = false
        }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        if (selectedWallpaper != null) {
            WallpaperDetailsScreen(
                wallpaper = selectedWallpaper!!,
                onBack = { selectedWallpaper = null },
                onSetAsFloating = {
                    scope.launch {
                        context.dataStore.edit { prefs ->
                            if (selectedWallpaper!!.type == "video") {
                                prefs[SettingsKeys.VIDEO_URI] = selectedWallpaper!!.url
                            } else {
                                prefs[SettingsKeys.IMAGE_URI] = selectedWallpaper!!.url
                            }
                        }
                    }
                }
            )
        } else {
            Scaffold(
                topBar = {
                    Column {
                        CenterAlignedTopAppBar(
                            title = { Text("Discover", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                            actions = {
                                IconButton(onClick = { navController.navigate("settings") }) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                                }
                            }
                        )
                        ScrollableTabRow(
                            selectedTabIndex = selectedTab,
                            edgePadding = 16.dp,
                            containerColor = MaterialTheme.colorScheme.surface,
                            divider = {}
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = { Text(title, fontWeight = if (selectedTab == index) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal) },
                                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 80.dp),
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) {
                    items(wallpapers) { wallpaper ->
                        WallpaperCard(wallpaper) {
                            selectedWallpaper = wallpaper
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WallpaperCard(wallpaper: Wallpaper, onClick: () -> Unit) {
    val height = remember(wallpaper.id) { 180 + (abs(wallpaper.id.hashCode()) % 120) }
    Card(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .height(height.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            if (wallpaper.type == "image") {
                AsyncImage(
                    model = wallpaper.url,
                    contentDescription = wallpaper.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Video", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
            )
            
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = wallpaper.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(16.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = wallpaper.uploaderEmail.substringBefore("@").take(10),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun WallpaperDetailsScreen(wallpaper: Wallpaper, onBack: () -> Unit, onSetAsFloating: () -> Unit) {
    val context = LocalContext.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    
    DisposableEffect(wallpaper) {
        if (wallpaper.type == "video") {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(Uri.parse(wallpaper.url)))
                prepare()
                playWhenReady = true
            }
        }
        onDispose {
            exoPlayer?.release()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().height(450.dp)) {
                if (wallpaper.type == "image") {
                    AsyncImage(
                        model = wallpaper.url,
                        contentDescription = wallpaper.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    if (exoPlayer != null) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = exoPlayer
                                    useController = false
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(Modifier.fillMaxSize().background(Color.Black))
                    }
                }
                
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopStart)
                        .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            }

            Column(Modifier.padding(24.dp)) {
                Text(wallpaper.title, style = MaterialTheme.typography.headlineMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(wallpaper.uploaderEmail.substringBefore("@"), style = MaterialTheme.typography.titleMedium)
                        Text("Uploaded by", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                Spacer(Modifier.height(32.dp))
                
                Button(
                    onClick = { onSetAsFloating(); onBack() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(if (wallpaper.type == "video") Icons.Default.PlayArrow else Icons.Default.Image, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Set as Floating ${if (wallpaper.type == "video") "Video" else "Icon"}")
                }
                
                var canDelete by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                LaunchedEffect(Unit) {
                    val uid = if (FirebaseManager.isMockMode) FirebaseManager.currentMockUserId else com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    if (uid != null) {
                        val profile = FirebaseManager.getUserProfile(uid)
                        if (profile?.role == "admin" || (profile?.role == "staff" && wallpaper.uploaderId == uid)) {
                            canDelete = true
                        }
                    }
                }
                
                if (canDelete) {
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                if (FirebaseManager.deleteWallpaper(wallpaper.id)) {
                                    onBack() // Refresh happens automatically when FeedScreen recomposes or we can just go back
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                        Spacer(Modifier.width(8.dp))
                        Text("Delete Wallpaper")
                    }
                }
                
                Spacer(Modifier.height(100.dp)) // padding for bottom nav if any
            }
        }
    }
}
