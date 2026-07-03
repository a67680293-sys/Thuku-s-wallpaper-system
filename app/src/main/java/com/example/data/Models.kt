package com.example.data

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val role: String = "user" // "user", "staff", "admin"
)

data class Wallpaper(
    val id: String = "",
    val title: String = "",
    val type: String = "image", // "image" or "video"
    val url: String = "",
    val uploaderId: String = "",
    val uploaderEmail: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
