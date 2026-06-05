package com.example.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirestoreSync {
    
    suspend fun fetchUploadedVideos(): List<Meme> {
        val memes = mutableListOf<Meme>()
        try {
            val db = FirebaseFirestore.getInstance()
            // Fetch uploaded video documents from "videos" collection
            val result = db.collection("videos").get().await()
            for (document in result) {
                try {
                    val id = document.getLong("id")?.toInt() ?: continue
                    val creatorId = document.getString("creatorId") ?: "unknown"
                    val creatorName = document.getString("creatorName") ?: "Unknown"
                    val title = document.getString("title") ?: "Untitled"
                    val description = document.getString("description") ?: ""
                    val animatedEmoji = document.getString("animatedEmoji") ?: "🤪"
                    val category = document.getString("category") ?: "All"
                    val hashtags = document.getString("hashtags") ?: ""
                    
                    val meme = Meme(
                        id = id,
                        creatorId = creatorId,
                        creatorName = creatorName,
                        title = title,
                        description = description,
                        hashtags = hashtags,
                        category = category,
                        animatedEmoji = animatedEmoji,
                        videoGradientStartColor = "0xFF1A237E",
                        videoGradientEndColor = "0xFF3949AB"
                    )
                    memes.add(meme)
                } catch (e: Exception) {
                    Log.e("FirestoreSync", "Error parsing video doc: ${document.id}", e)
                }
            }
        } catch (e: Exception) {
            // This expects an IllegalStateException if Firebase is not configured with google-services.json
            Log.w("FirestoreSync", "Firestore not configured or query failed.")
        }
        return memes
    }
}
