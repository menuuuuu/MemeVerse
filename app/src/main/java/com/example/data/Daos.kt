package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): User?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserFlowById(userId: String): Flow<User?>

    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Query("UPDATE users SET isBanned = :isBanned WHERE id = :userId")
    suspend fun updateUserBanStatus(userId: String, isBanned: Boolean)

    @Query("UPDATE users SET followerCount = followerCount + :delta WHERE id = :userId")
    suspend fun updateFollowerCount(userId: String, delta: Int)

    @Query("UPDATE users SET followingCount = followingCount + :delta WHERE id = :userId")
    suspend fun updateFollowingCount(userId: String, delta: Int)

    @Query("UPDATE users SET totalLikes = totalLikes + :delta WHERE id = :userId")
    suspend fun updateCreatorTotalLikes(userId: String, delta: Int)
}

@Dao
interface MemeDao {
    @Query("SELECT * FROM memes WHERE isDeleted = 0 ORDER BY uploadTimestamp DESC")
    fun getAllMemesFlow(): Flow<List<Meme>>

    @Query("SELECT * FROM memes WHERE isDeleted = 0 AND category = :category ORDER BY uploadTimestamp DESC")
    fun getMemesByCategoryFlow(category: String): Flow<List<Meme>>

    @Query("SELECT * FROM memes WHERE isDeleted = 0 AND creatorId = :creatorId ORDER BY uploadTimestamp DESC")
    fun getMemesByCreatorFlow(creatorId: String): Flow<List<Meme>>

    @Query("SELECT * FROM memes WHERE isDeleted = 0 AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR hashtags LIKE '%' || :query || '%') ORDER BY uploadTimestamp DESC")
    fun searchMemesFlow(query: String): Flow<List<Meme>>

    @Query("SELECT * FROM memes WHERE isDeleted = 0 ORDER BY (likeCount + viewCount * 0.1) DESC")
    fun getTrendingMemesFlow(): Flow<List<Meme>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeme(meme: Meme): Long

    @Query("UPDATE memes SET likeCount = likeCount + :delta WHERE id = :memeId")
    suspend fun updateLikeCount(memeId: Int, delta: Int)

    @Query("UPDATE memes SET viewCount = viewCount + 1 WHERE id = :memeId")
    suspend fun incrementViewCount(memeId: Int)

    @Query("UPDATE memes SET commentCount = commentCount + :delta WHERE id = :memeId")
    suspend fun updateCommentCount(memeId: Int, delta: Int)

    @Query("UPDATE memes SET isDeleted = 1 WHERE id = :memeId")
    suspend fun deleteMeme(memeId: Int)

    @Query("UPDATE memes SET isReported = :isReported WHERE id = :memeId")
    suspend fun updateReportedStatus(memeId: Int, isReported: Boolean)

    @Query("SELECT * FROM memes WHERE id = :memeId LIMIT 1")
    suspend fun getMemeById(memeId: Int): Meme?
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE memeId = :memeId ORDER BY timestamp ASC")
    fun getCommentsForMemeFlow(memeId: Int): Flow<List<Comment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: Comment): Long

    @Query("DELETE FROM comments WHERE id = :commentId")
    suspend fun deleteComment(commentId: Int)
}

@Dao
interface LikeDao {
    @Query("SELECT EXISTS(SELECT 1 FROM likes WHERE userId = :userId AND memeId = :memeId)")
    fun hasLikedFlow(userId: String, memeId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM likes WHERE userId = :userId AND memeId = :memeId)")
    suspend fun hasLiked(userId: String, memeId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLike(like: Like)

    @Delete
    suspend fun deleteLike(like: Like)
}

@Dao
interface FollowDao {
    @Query("SELECT EXISTS(SELECT 1 FROM follows WHERE followerId = :followerId AND followedId = :followedId)")
    fun isFollowingFlow(followerId: String, followedId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM follows WHERE followerId = :followerId AND followedId = :followedId)")
    suspend fun isFollowing(followerId: String, followedId: String): Boolean

    @Query("SELECT followedId FROM follows WHERE followerId = :followerId")
    fun getFollowedIdsFlow(followerId: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollow(follow: Follow)

    @Delete
    suspend fun deleteFollow(follow: Follow)
}

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY timestamp DESC")
    fun getAllReportsFlow(): Flow<List<Report>>

    @Query("SELECT * FROM reports WHERE status = 'PENDING' ORDER BY timestamp DESC")
    fun getPendingReportsFlow(): Flow<List<Report>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: Report): Long

    @Query("UPDATE reports SET status = :status WHERE id = :reportId")
    suspend fun updateReportStatus(reportId: Int, status: String)

    @Query("UPDATE reports SET status = :status WHERE memeId = :memeId AND status = 'PENDING'")
    suspend fun resolvePendingReportsForMeme(memeId: Int, status: String)
}
