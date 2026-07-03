package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import com.example.data.FirebaseManager

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(Modifier.padding(32.dp)) {
            Column(
                Modifier.padding(16.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (FirebaseManager.isMockMode) {
                    Text("Mock Mode (No Firebase)", style = MaterialTheme.typography.titleLarge)
                    Text("Select a mock role to login as:")
                    Button(onClick = { FirebaseManager.currentMockUserId = "mock1"; onLoginSuccess() }) { Text("Admin") }
                    Button(onClick = { FirebaseManager.currentMockUserId = "mock2"; onLoginSuccess() }) { Text("Staff") }
                    Button(onClick = { FirebaseManager.currentMockUserId = "mock3"; onLoginSuccess() }) { Text("User") }
                } else {
                    Text("Login or Register", style = MaterialTheme.typography.titleLarge)
                    
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (errorMsg != null) {
                        Text(errorMsg!!, color = MaterialTheme.colorScheme.error)
                    }

                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Button(onClick = {
                                scope.launch {
                                    isLoading = true
                                    errorMsg = null
                                    try {
                                        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).await()
                                        onLoginSuccess()
                                    } catch (e: Exception) {
                                        errorMsg = e.localizedMessage
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }) {
                                Text("Login")
                            }
                            
                            OutlinedButton(onClick = {
                                scope.launch {
                                    isLoading = true
                                    errorMsg = null
                                    try {
                                        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).await()
                                        onLoginSuccess()
                                    } catch (e: Exception) {
                                        errorMsg = e.localizedMessage
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }) {
                                Text("Register")
                            }
                        }
                    }
                }
            }
        }
    }
}
