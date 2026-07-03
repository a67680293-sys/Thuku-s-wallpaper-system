package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.datastore.preferences.core.edit
import coil.compose.AsyncImage
import com.example.data.SettingsKeys
import com.example.data.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FloatingService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var composeView: ComposeView
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var lifecycleOwner: FloatingLifecycleOwner
    private var exoPlayer: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "floating_channel")
            .setContentTitle("Floating Player Active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
            
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(1, notification)
        }

        setupExoPlayer()
        setupOverlay()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "floating_channel",
                "Floating Player",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun setupExoPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()
        scope.launch {
            dataStore.data.collect { prefs ->
                val videoUriStr = prefs[SettingsKeys.VIDEO_URI]
                val loop = prefs[SettingsKeys.LOOP_VIDEO] ?: false
                val audioEnabled = prefs[SettingsKeys.AUDIO_ENABLED] ?: true

                if (exoPlayer?.repeatMode != if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF) {
                    exoPlayer?.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                }
                
                val targetVolume = if (audioEnabled) 1f else 0f
                if (exoPlayer?.volume != targetVolume) {
                    exoPlayer?.volume = targetVolume
                }
                
                // If media item changes, we need to handle that, but for now we just handle it here simply
                if (videoUriStr != null && exoPlayer?.currentMediaItem?.localConfiguration?.uri?.toString() != videoUriStr) {
                    exoPlayer?.setMediaItem(MediaItem.fromUri(Uri.parse(videoUriStr)))
                    exoPlayer?.prepare()
                }
            }
        }
    }

    private fun updateWindowLocation(x: Int, y: Int) {
        params.x = x
        params.y = y
        windowManager.updateViewLayout(composeView, params)
    }

    private fun savePosition(x: Int, y: Int) {
        scope.launch {
            dataStore.edit { prefs ->
                prefs[SettingsKeys.POS_X] = x
                prefs[SettingsKeys.POS_Y] = y
            }
        }
    }

    private fun setupOverlay() {
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        
        scope.launch {
            val prefs = dataStore.data.first()
            params.x = prefs[SettingsKeys.POS_X] ?: 100
            params.y = prefs[SettingsKeys.POS_Y] ?: 100
        }

        composeView = ComposeView(this).apply {
            setContent {
                val prefs by dataStore.data.collectAsState(initial = null)
                var isVideoMode by remember { mutableStateOf(false) }
                
                if (prefs == null) return@setContent

                val opacity = prefs!![SettingsKeys.OPACITY] ?: 1f
                val size = prefs!![SettingsKeys.SIZE] ?: 64
                val isLocked = prefs!![SettingsKeys.IS_LOCKED] ?: false
                val shapeStr = prefs!![SettingsKeys.SHAPE] ?: "circle"
                val imageUri = prefs!![SettingsKeys.IMAGE_URI]
                val glowEffect = prefs!![SettingsKeys.GLOW_EFFECT] ?: false
                val borderEffect = prefs!![SettingsKeys.BORDER_EFFECT] ?: false

                val shape: Shape = when (shapeStr) {
                    "square" -> RectangleShape
                    "rounded" -> RoundedCornerShape(16.dp)
                    else -> CircleShape
                }
                
                var videoWidth by remember { mutableStateOf(300.dp) }
                var videoHeight by remember { mutableStateOf(200.dp) }
                val density = androidx.compose.ui.platform.LocalDensity.current.density

                Box(
                    modifier = Modifier
                        .alpha(opacity)
                        .then(if (glowEffect) Modifier.shadow(16.dp, shape, ambientColor = Color.Cyan, spotColor = Color.Cyan) else Modifier)
                        .then(if (borderEffect) Modifier.border(2.dp, Color.White, shape) else Modifier)
                        .clip(shape)
                        .pointerInput(isLocked) {
                            if (!isLocked) {
                                detectDragGestures(
                                    onDragEnd = { savePosition(params.x, params.y) },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        params.x += dragAmount.x.toInt()
                                        params.y += dragAmount.y.toInt()
                                        windowManager.updateViewLayout(composeView, params)
                                    }
                                )
                            }
                        }
                ) {
                    if (isVideoMode) {
                        Box(modifier = Modifier.size(videoWidth, videoHeight)) {
                            AndroidView(
                                factory = { ctx ->
                                    PlayerView(ctx).apply {
                                        player = exoPlayer
                                        useController = true
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            
                            // Minimize button
                            IconButton(
                                onClick = { isVideoMode = false },
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(4.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    .size(32.dp)
                            ) {
                                Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = "Minimize Video", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            
                            // Dedicated Close button (terminates service)
                            IconButton(
                                onClick = { stopSelf() },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .background(Color.Red.copy(alpha = 0.8f), CircleShape)
                                    .size(32.dp)
                            ) {
                                Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = "Close Player Completely", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            
                            // Resize Handle
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(topStart = 12.dp))
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            videoWidth += (dragAmount.x / density).dp
                                            videoHeight += (dragAmount.y / density).dp
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(Modifier.size(8.dp).background(Color.White, CircleShape))
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(size.dp)
                                .background(Color.DarkGray)
                                .clickable { isVideoMode = true }
                        ) {
                            if (imageUri != null) {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = "Floating Icon",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Open Player",
                                    tint = Color.White,
                                    modifier = Modifier.align(Alignment.Center).size((size/2).dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        lifecycleOwner = FloatingLifecycleOwner()
        lifecycleOwner.onCreate()
        composeView.setViewTreeLifecycleOwner(lifecycleOwner)
        composeView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        composeView.setViewTreeViewModelStoreOwner(lifecycleOwner)
        lifecycleOwner.onResume()

        windowManager.addView(composeView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        lifecycleOwner.onDestroy()
        windowManager.removeView(composeView)
    }
}
