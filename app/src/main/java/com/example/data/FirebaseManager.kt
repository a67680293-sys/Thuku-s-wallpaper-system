package com.example.data

import android.net.Uri
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

object FirebaseManager {
    var isMockMode = false

    // Mock data for previewing when Firebase is not configured
    private val mockWallpapers = mutableListOf(
        Wallpaper("1", "Neon Cyberpunk City", "image", "https://images.unsplash.com/photo-1518770660439-4636190af475?w=800", "mock", "mock@admin.com"),
        Wallpaper("2", "Abstract Geometric", "image", "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=800", "mock", "mock@admin.com"),
        Wallpaper("3", "Calm Nature", "image", "https://images.unsplash.com/photo-1472214103451-9374bd1c798e?w=800", "mock", "mock@admin.com"),
        Wallpaper("4", "Space Nebula", "image", "https://images.unsplash.com/photo-1462331940025-496dfbfc7564?w=800", "mock", "mock@admin.com"),
        Wallpaper("5", "Sample Video", "video", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", "mock", "mock@admin.com")
    )
    
    private val mockUsers = mutableListOf(
        UserProfile("mock1", "mishramadhav072@gmail.com", "admin"),
        UserProfile("mock2", "staff@app.com", "staff"),
        UserProfile("mock3", "user@app.com", "user")
    )
    
    var currentMockUserId = "mock1"
    
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }
    
    suspend fun getUserProfile(uid: String): UserProfile? {
        if (isMockMode) return mockUsers.find { it.uid == uid }
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            doc.toObject(UserProfile::class.java)
        } catch (e: Exception) { null }
    }

    suspend fun createUserProfile(uid: String, email: String) {
        if (isMockMode) {
            val role = if (email == "mishramadhav072@gmail.com") "admin" else "user"
            mockUsers.add(UserProfile(uid, email, role))
            return
        }
        val role = if (email == "mishramadhav072@gmail.com") "admin" else "user"
        val profile = UserProfile(uid, email, role)
        firestore.collection("users").document(uid).set(profile).await()
    }

    suspend fun getAllUsers(): List<UserProfile> {
        if (isMockMode) return mockUsers
        return try {
            val snapshot = firestore.collection("users").get().await()
            snapshot.toObjects(UserProfile::class.java)
        } catch(e: Exception) { emptyList() }
    }

    suspend fun setUserRole(uid: String, role: String) {
        if (isMockMode) {
            val index = mockUsers.indexOfFirst { it.uid == uid }
            if (index != -1) mockUsers[index] = mockUsers[index].copy(role = role)
            return
        }
        firestore.collection("users").document(uid).update("role", role).await()
    }

    suspend fun uploadWallpaper(uri: Uri, title: String, type: String): Boolean {
        if (isMockMode) {
            val uploaderId = currentMockUserId
            val uploaderEmail = mockUsers.find { it.uid == uploaderId }?.email ?: ""
            mockWallpapers.add(0, Wallpaper(UUID.randomUUID().toString(), title, type, uri.toString(), uploaderId, uploaderEmail))
            return true
        }
        val currentUser = auth.currentUser ?: return false
        val profile = getUserProfile(currentUser.uid) ?: return false
        if (profile.role != "staff" && profile.role != "admin") return false

        val wallpaperId = UUID.randomUUID().toString()
        val extension = if (type == "video") "mp4" else "jpg"
        val ref = storage.reference.child("wallpapers/$wallpaperId.$extension")

        return try {
            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            val wallpaper = Wallpaper(id = wallpaperId, title = title, type = type, url = downloadUrl, uploaderId = currentUser.uid, uploaderEmail = currentUser.email ?: "")
            firestore.collection("wallpapers").document(wallpaperId).set(wallpaper).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getWallpapers(): List<Wallpaper> {
        if (isMockMode) return mockWallpapers
        return try {
            val snapshot = firestore.collection("wallpapers").orderBy("timestamp", Query.Direction.DESCENDING).get().await()
            snapshot.toObjects(Wallpaper::class.java)
        } catch (e: Exception) { mockWallpapers } // Fallback to mock if it fails
    }

    suspend fun deleteWallpaper(id: String): Boolean {
        if (isMockMode) {
            mockWallpapers.removeIf { it.id == id }
            return true
        }
        val currentUser = auth.currentUser ?: return false
        val profile = getUserProfile(currentUser.uid) ?: return false
        
        return try {
            val doc = firestore.collection("wallpapers").document(id).get().await()
            val wallpaper = doc.toObject(Wallpaper::class.java) ?: return false
            
            if (profile.role == "admin" || (profile.role == "staff" && wallpaper.uploaderId == currentUser.uid)) {
                firestore.collection("wallpapers").document(id).delete().await()
                true
            } else false
        } catch (e: Exception) { false }
    }
}
