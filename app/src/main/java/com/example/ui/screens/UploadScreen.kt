package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.FirebaseManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(onBack: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var type by remember { mutableStateOf("image") }
    var isUploading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var uploadStatus by remember { mutableStateOf<String?>(null) }

    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upload Wallpaper") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = type == "image", onClick = { type = "image" })
            Text("Image")
            Spacer(Modifier.width(16.dp))
            RadioButton(selected = type == "video", onClick = { type = "video" })
            Text("Video")
        }

        Button(onClick = {
            pickerLauncher.launch(if (type == "image") "image/*" else "video/*")
        }, modifier = Modifier.fillMaxWidth()) {
            Text(if (selectedUri != null) "File Selected" else "Select File")
        }

        if (isUploading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Button(
                onClick = {
                    if (selectedUri != null && title.isNotBlank()) {
                        scope.launch {
                            isUploading = true
                            uploadStatus = null
                            val success = FirebaseManager.uploadWallpaper(selectedUri!!, title, type)
                            isUploading = false
                            uploadStatus = if (success) "Upload Successful!" else "Upload Failed."
                            if (success) {
                                title = ""
                                selectedUri = null
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedUri != null && title.isNotBlank()
            ) {
                Text("Upload")
            }
        }

        if (uploadStatus != null) {
            Text(uploadStatus!!, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
}
