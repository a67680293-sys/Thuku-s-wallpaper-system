package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Intent
import android.provider.Settings
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.FloatingService
import com.example.R

@SuppressLint("MissingPermission")
@Composable
fun PreviewScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val hasOverlayPermission = Settings.canDrawOverlays(context)
    
    Box(Modifier.fillMaxSize().background(Color(0xFF1E1E1E))) { // Dark background to simulate a phone screen frame
        
        // Content Area inside a simulated "phone frame"
        Box(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 24.dp)
                .clip(RoundedCornerShape(32.dp)) // Simulate phone rounded corners
                .background(Color.DarkGray)
        ) {
            // Actual Device Wallpaper
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        try {
                            setImageDrawable(WallpaperManager.getInstance(ctx).drawable)
                        } catch (e: Exception) {
                            setBackgroundColor(android.graphics.Color.DKGRAY)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Fake App Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(top = 80.dp, start = 16.dp, end = 16.dp, bottom = 120.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(20) { index ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                        val iconColor = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Cyan, Color.Magenta)[index % 6].copy(alpha = 0.7f)
                        Box(Modifier.size(60.dp).clip(RoundedCornerShape(16.dp)).background(iconColor), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Apps, contentDescription = null, tint = Color.White)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("App ${index + 1}", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Fake Dock Area
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 140.dp).fillMaxWidth().padding(horizontal = 16.dp).height(90.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = 0.2f))) {
                Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    repeat(4) {
                        Box(Modifier.size(56.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color.White.copy(alpha = 0.8f)))
                    }
                }
            }
            
            // Back Button
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

        // Overlay Instructions at the bottom, overlapping the frame
        Card(
            Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Test Floating Player", style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Start the Floating Player to see how it looks overlaying your apps. You can drag it around to test the behavior.", style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (hasOverlayPermission) {
                            val intent = Intent(context, FloatingService::class.java)
                            context.startService(intent)
                        }
                    }, 
                    enabled = hasOverlayPermission,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (hasOverlayPermission) "Launch Player" else "Permission Required", style = MaterialTheme.typography.titleMedium)
                }
                
                if (!hasOverlayPermission) {
                    Spacer(Modifier.height(8.dp))
                    Text("Go to Settings tab to grant Overlay Permission.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
