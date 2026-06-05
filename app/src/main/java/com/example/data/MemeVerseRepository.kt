package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class MemeVerseRepository(
    private val userDao: UserDao,
    private val memeDao: MemeDao,
    private val commentDao: CommentDao,
    private val likeDao: LikeDao,
    private val followDao: FollowDao,
    private val reportDao: ReportDao
) {
    // Standard Flows
    val allMemes: Flow<List<Meme>> = memeDao.getAllMemesFlow()
    val trendingMemes: Flow<List<Meme>> = memeDao.getTrendingMemesFlow()
    val allReports: Flow<List<Report>> = reportDao.getAllReportsFlow()
    val pendingReports: Flow<List<Report>> = reportDao.getPendingReportsFlow()
    val allUsers: Flow<List<User>> = userDao.getAllUsersFlow()

    // Query streams
    fun getMemesByCategory(category: String): Flow<List<Meme>> = memeDao.getMemesByCategoryFlow(category)
    fun getMemesByCreator(creatorId: String): Flow<List<Meme>> = memeDao.getMemesByCreatorFlow(creatorId)
    fun searchMemes(query: String): Flow<List<Meme>> = memeDao.searchMemesFlow(query)
    fun getCommentsForMeme(memeId: Int): Flow<List<Comment>> = commentDao.getCommentsForMemeFlow(memeId)
    fun hasLiked(userId: String, memeId: Int): Flow<Boolean> = likeDao.hasLikedFlow(userId, memeId)
    fun isFollowing(followerId: String, followedId: String): Flow<Boolean> = followDao.isFollowingFlow(followerId, followedId)
    fun getFollowedIds(followerId: String): Flow<List<String>> = followDao.getFollowedIdsFlow(followerId)
    fun getUserFlow(userId: String): Flow<User?> = userDao.getUserFlowById(userId)

    // User Operations
    suspend fun getUser(userId: String): User? = withContext(Dispatchers.IO) {
        userDao.getUserById(userId)
    }

    suspend fun saveUser(user: User) = withContext(Dispatchers.IO) {
        userDao.insertUser(user)
    }

    suspend fun updateUser(user: User) = withContext(Dispatchers.IO) {
        userDao.updateUser(user)
    }

    suspend fun banUser(userId: String) = withContext(Dispatchers.IO) {
        userDao.updateUserBanStatus(userId, isBanned = true)
    }

    suspend fun unbanUser(userId: String) = withContext(Dispatchers.IO) {
        userDao.updateUserBanStatus(userId, isBanned = false)
    }

    // Meme Operations
    suspend fun uploadMeme(
        title: String,
        description: String,
        hashtags: String,
        category: String,
        creatorId: String,
        creatorName: String
    ): Int = withContext(Dispatchers.IO) {
        // Generate beautiful gradient color values for simulated screen background
        val colors = listOf(
            "0xFFFF1744" to "0xFFFF8F00",
            "0xFF00E676" to "0xFF00B0FF",
            "0xFF651FFF" to "0xFFD500F9",
            "0xFFFF9100" to "0xFFFF1744",
            "0xFFF50057" to "0xFF40C4FF",
            "0xFF00E5FF" to "0xFF76FF03"
        )
        val selected = colors.random()
        val emojis = listOf("😂", "🫡", "💀", "🤣", "🤔", "🌶️", "🤡", "🔥", "🐱", "🎮", "🤨", "🗿")
        val emoji = emojis.random()

        val meme = Meme(
            title = title,
            description = description,
            hashtags = hashtags,
            category = category,
            creatorId = creatorId,
            creatorName = creatorName,
            videoGradientStartColor = selected.first,
            videoGradientEndColor = selected.second,
            animatedEmoji = emoji
        )
        memeDao.insertMeme(meme).toInt()
    }

    suspend fun deleteMeme(memeId: Int) = withContext(Dispatchers.IO) {
        memeDao.deleteMeme(memeId)
        // Also resolve associated reports
        reportDao.resolvePendingReportsForMeme(memeId, "RESOLVED")
    }

    suspend fun incrementViewCount(memeId: Int) = withContext(Dispatchers.IO) {
        memeDao.incrementViewCount(memeId)
    }

    suspend fun syncFirestoreVideos() = withContext(Dispatchers.IO) {
        val firestoreMemes = FirestoreSync.fetchUploadedVideos()
        firestoreMemes.forEach { memeDao.insertMeme(it) }
    }

    // Interactive Actions
    suspend fun toggleLike(userId: String, memeId: Int) = withContext(Dispatchers.IO) {
        val alreadyLiked = likeDao.hasLiked(userId, memeId)
        val meme = memeDao.getMemeById(memeId)
        if (alreadyLiked) {
            likeDao.deleteLike(Like(userId, memeId))
            memeDao.updateLikeCount(memeId, -1)
            if (meme != null) {
                userDao.updateCreatorTotalLikes(meme.creatorId, -1)
            }
        } else {
            likeDao.insertLike(Like(userId, memeId))
            memeDao.updateLikeCount(memeId, 1)
            if (meme != null) {
                userDao.updateCreatorTotalLikes(meme.creatorId, 1)
            }
        }
    }

    suspend fun toggleFollow(followerId: String, followedId: String) = withContext(Dispatchers.IO) {
        if (followerId == followedId) return@withContext
        val alreadyFollowing = followDao.isFollowing(followerId, followedId)
        if (alreadyFollowing) {
            followDao.deleteFollow(Follow(followerId, followedId))
            userDao.updateFollowerCount(followedId, -1)
            userDao.updateFollowingCount(followerId, -1)
        } else {
            followDao.insertFollow(Follow(followerId, followedId))
            userDao.updateFollowerCount(followedId, 1)
            userDao.updateFollowingCount(followerId, 1)
        }
    }

    suspend fun commentMeme(memeId: Int, userId: String, username: String, commentText: String) = withContext(Dispatchers.IO) {
        if (commentText.trim().isEmpty()) return@withContext
        val comment = Comment(
            memeId = memeId,
            userId = userId,
            username = username,
            text = commentText
        )
        commentDao.insertComment(comment)
        memeDao.updateCommentCount(memeId, 1)
    }

    suspend fun deleteComment(commentId: Int, memeId: Int) = withContext(Dispatchers.IO) {
        commentDao.deleteComment(commentId)
        memeDao.updateCommentCount(memeId, -1)
    }

    // Reports Operations
    suspend fun reportMeme(memeId: Int, reportedUserId: String, reporterId: String, reason: String) = withContext(Dispatchers.IO) {
        val report = Report(
            memeId = memeId,
            reportedUserId = reportedUserId,
            reporterId = reporterId,
            reason = reason
        )
        reportDao.insertReport(report)
        memeDao.updateReportedStatus(memeId, true)
    }

    suspend fun reportUser(reportedUserId: String, reporterId: String, reason: String) = withContext(Dispatchers.IO) {
        val report = Report(
            memeId = 0, // 0 means reported full user profile
            reportedUserId = reportedUserId,
            reporterId = reporterId,
            reason = reason
        )
        reportDao.insertReport(report)
    }

    suspend fun resolveReport(reportId: Int, newStatus: String) = withContext(Dispatchers.IO) {
        reportDao.updateReportStatus(reportId, newStatus)
    }

    // Prepopulate DB with beautiful sample datasets
    suspend fun checkAndPrepopulate() = withContext(Dispatchers.IO) {
        val existingUsers = allUsers.first()
        if (existingUsers.isEmpty()) {
            // Seed premium accounts
            val adminUser = User(
                id = "admin@memeverse.com",
                username = "MemeWarlord",
                email = "admin@memeverse.com",
                bio = "MemeVerse Lead Admin. Free speech, memes, and troll reviews.",
                profilePicHexColor = "0xFFFF3F3F", // Deep neon red
                followerCount = 1337,
                followingCount = 42,
                totalLikes = 9001,
                isBanned = false,
                isAdmin = true
            )
            val satireGuru = User(
                id = "satire_guru@memeverse.com",
                username = "SatireGuru",
                email = "satire_guru@memeverse.com",
                bio = "Criticizing society, politics and modern developers. Premium satire.",
                profilePicHexColor = "0xFFFF9100", // Bright orange
                followerCount = 450,
                followingCount = 120,
                totalLikes = 1500
            )
            val dankKing = User(
                id = "dank_king@memeverse.com",
                username = "DankKing",
                email = "dank_king@memeverse.com",
                bio = "4K quality template designer. Troll files uploaded daily! 🔥",
                profilePicHexColor = "0xFF00E5FF", // Cyan
                followerCount = 890,
                followingCount = 85,
                totalLikes = 4320
            )
            val developerUser = User(
                id = "gamerma209@gmail.com",
                username = "GamerDeveloper",
                email = "gamerma209@gmail.com",
                bio = "Dev of MemeVerse. Support free speech, criticism, and high-contrast dark visualizers.",
                profilePicHexColor = "0xFF00FFCC", // Mint
                followerCount = 2000,
                followingCount = 5,
                totalLikes = 9999,
                isAdmin = true
            )

            userDao.insertUser(adminUser)
            userDao.insertUser(satireGuru)
            userDao.insertUser(dankKing)
            userDao.insertUser(developerUser)

            // Seed sample humorous reels with custom color themes & elements
            val memes = listOf(
                Meme(
                    id = 1,
                    title = "When they ask a simple yes/no question",
                    description = "Politicians during debate preparing to talk for 5 minutes about agriculture. 🤐 #debate #satire #politics #funny",
                    hashtags = "#debate #satire #politics #funny",
                    category = "Satire",
                    creatorId = "satire_guru@memeverse.com",
                    creatorName = "SatireGuru",
                    videoGradientStartColor = "0xFFE65100",
                    videoGradientEndColor = "0xFFF57C00",
                    animatedEmoji = "🤐",
                    likeCount = 340,
                    viewCount = 1200,
                    commentCount = 3
                ),
                Meme(
                    id = 2,
                    title = "The solo queue ranking cycle",
                    description = "Teammates operating on potato juice batteries and 700ms screen lag. Help me. 🤬 #gaming #fail #soloqueue",
                    hashtags = "#gaming #fail #soloqueue",
                    category = "Gaming",
                    creatorId = "dank_king@memeverse.com",
                    creatorName = "DankKing",
                    videoGradientStartColor = "0xFF1A237E",
                    videoGradientEndColor = "0xFF3949AB",
                    animatedEmoji = "🤬",
                    likeCount = 820,
                    viewCount = 2900,
                    commentCount = 2
                ),
                Meme(
                    id = 3,
                    title = "Me trying to calculate 2 + 2 in high stakes exam",
                    description = "My brain has exited the room to compose dynamic UI. 🤯 #dank #math #fail #relatable",
                    hashtags = "#dank #math #fail #relatable",
                    category = "Dank",
                    creatorId = "dank_king@memeverse.com",
                    creatorName = "DankKing",
                    videoGradientStartColor = "0xFF311B92",
                    videoGradientEndColor = "0xFF651FFF",
                    animatedEmoji = "🤯",
                    likeCount = 512,
                    viewCount = 1800,
                    commentCount = 1
                ),
                Meme(
                    id = 4,
                    title = "Pure defense mechanism activated",
                    description = "When the owner brings out the loudest vacuum cleaner on earth. Dog vs machine! 🐕 #animals #dog #funny #vacuum",
                    hashtags = "#animals #dog #funny #vacuum",
                    category = "Animals",
                    creatorId = "admin@memeverse.com",
                    creatorName = "MemeWarlord",
                    videoGradientStartColor = "0xFF004D40",
                    videoGradientEndColor = "0xFF00897B",
                    animatedEmoji = "🐕",
                    likeCount = 110,
                    viewCount = 450,
                    commentCount = 0
                ),
                Meme(
                    id = 5,
                    title = "Gym bro vs 500 lbs deadlift",
                    description = "Gravity won another round. He is completely fine but his ego is now negative 10. 🏋️‍♂️ #fail #gym #bro #lift",
                    hashtags = "#fail #gym #bro #lift",
                    category = "Fail",
                    creatorId = "satire_guru@memeverse.com",
                    creatorName = "SatireGuru",
                    videoGradientStartColor = "0xFF3E2723",
                    videoGradientEndColor = "0xFF6D4C41",
                    animatedEmoji = "🏋️‍♂️",
                    likeCount = 4220,
                    viewCount = 12500,
                    commentCount = 3
                )
            )

            memes.forEach { memeDao.insertMeme(it) }

            // Seed initial comment threads representing free speech debate
            val comments = listOf(
                Comment(
                    memeId = 1,
                    userId = "dank_king@memeverse.com",
                    username = "DankKing",
                    text = "Classic! They can talk for hours without saying single word."
                ),
                Comment(
                    memeId = 1,
                    userId = "admin@memeverse.com",
                    username = "MemeWarlord",
                    text = "Please keep constructive criticisms flowing! Nice post."
                ),
                Comment(
                    memeId = 1,
                    userId = "gamerma209@gmail.com",
                    username = "GamerDeveloper",
                    text = "Lmao, hit too close to home. 😭"
                ),
                Comment(
                    memeId = 2,
                    userId = "gamerma209@gmail.com",
                    username = "GamerDeveloper",
                    text = "This is why I only play cozy singleplayer farming games now."
                ),
                Comment(
                    memeId = 2,
                    userId = "satire_guru@memeverse.com",
                    username = "SatireGuru",
                    text = "The real meme is thinking solo queue ranking represents your actual skill."
                ),
                Comment(
                    memeId = 3,
                    userId = "admin@memeverse.com",
                    username = "MemeWarlord",
                    text = "Me using the scientific calculator to verify 7 x 8 just in case."
                ),
                Comment(
                    memeId = 5,
                    userId = "dank_king@memeverse.com",
                    username = "DankKing",
                    text = "Bro is built like a question mark of determination!"
                ),
                Comment(
                    memeId = 5,
                    userId = "admin@memeverse.com",
                    username = "MemeWarlord",
                    text = "That form is a direct ticket to vertebral rearrangement."
                ),
                Comment(
                    memeId = 5,
                    userId = "satire_guru@memeverse.com",
                    username = "SatireGuru",
                    text = "He survived. The barbell did not."
                )
            )

            comments.forEach { commentDao.insertComment(it) }

            // Seed follower status
            followDao.insertFollow(Follow("dank_king@memeverse.com", "admin@memeverse.com"))
            userDao.updateFollowerCount("admin@memeverse.com", 1)
            followDao.insertFollow(Follow("satire_guru@memeverse.com", "admin@memeverse.com"))
            userDao.updateFollowerCount("admin@memeverse.com", 1)

            // Seed likes status
            likeDao.insertLike(Like("gamerma209@gmail.com", 1))
            likeDao.insertLike(Like("gamerma209@gmail.com", 2))
        }
    }
}
