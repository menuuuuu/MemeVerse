package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String, // email or username
    val username: String,
    val email: String,
    val bio: String = "",
    val profilePicHexColor: String = "0xFF512DA8", // Beautiful placeholder colors
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val totalLikes: Int = 0,
    val isBanned: Boolean = false,
    val isAdmin: Boolean = false,
    val isGoogleUser: Boolean = false
) : Serializable

@Entity(tableName = "memes")
data class Meme(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val hashtags: String, // space-separated tags like "#satire #funny"
    val category: String, // e.g. "Satire", "Animals", "Gaming", "Fail", "Dank"
    val creatorId: String,
    val creatorName: String,
    val videoGradientStartColor: String = "0xFFFF5722", // UI representations
    val videoGradientEndColor: String = "0xFFFFC107",
    val animatedEmoji: String = "😂", // Fun visual overlay for simulated short video
    val likeCount: Int = 0,
    val viewCount: Int = 0,
    val commentCount: Int = 0,
    val uploadTimestamp: Long = System.currentTimeMillis(),
    val isReported: Boolean = false,
    val isDeleted: Boolean = false
) : Serializable

@Entity(tableName = "comments")
data class Comment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val memeId: Int,
    val userId: String,
    val username: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "likes", primaryKeys = ["userId", "memeId"])
data class Like(
    val userId: String,
    val memeId: Int
)

@Entity(tableName = "follows", primaryKeys = ["followerId", "followedId"])
data class Follow(
    val followerId: String,
    val followedId: String
)

@Entity(tableName = "reports")
data class Report(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val memeId: Int,
    val reportedUserId: String,
    val reporterId: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING" // PENDING, RESOLVED, DISMISSED
) : Serializable
