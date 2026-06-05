package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MemeVerseViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = MemeVerseRepository(
        userDao = database.userDao(),
        memeDao = database.memeDao(),
        commentDao = database.commentDao(),
        likeDao = database.likeDao(),
        followDao = database.followDao(),
        reportDao = database.reportDao()
    )

    // Authentication States
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val followedCreatorIds: StateFlow<List<String>> = _currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else repository.getFollowedIds(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Screen States
    val allMemes: StateFlow<List<Meme>> = repository.allMemes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trendingMemes: StateFlow<List<Meme>> = repository.trendingMemes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<User>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingReports: StateFlow<List<Report>> = repository.pendingReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReports: StateFlow<List<Report>> = repository.allReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Category (for feed filtering, if active)
    private val _activeCategory = MutableStateFlow<String>("All")
    val activeCategory: StateFlow<String> = _activeCategory.asStateFlow()

    // Searching Stream Flows
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchedMemes: StateFlow<List<Meme>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.trim().isEmpty()) {
                repository.allMemes
            } else {
                repository.searchMemes(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Upload SIMULATOR States
    private val _uploadProgress = MutableStateFlow<Float?>(null) // null means not uploading
    val uploadProgress: StateFlow<Float?> = _uploadProgress.asStateFlow()

    private val _uploadSuccess = MutableSharedFlow<Boolean>()
    val uploadSuccess: SharedFlow<Boolean> = _uploadSuccess.asSharedFlow()

    init {
        viewModelScope.launch {
            // Seeding default database
            repository.checkAndPrepopulate()
            // Sync with Firestore backend if it exists
            repository.syncFirestoreVideos()
            // Log in as developer default account or let them go to Auth
            val defaultUser = repository.getUser("gamerma209@gmail.com")
            if (defaultUser != null) {
                _currentUser.value = defaultUser
            }
        }
    }

    // Set Category Filter
    fun setCategory(category: String) {
        _activeCategory.value = category
    }

    // Search Query Change
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Authentication Flows
    fun loginWithEmail(email: String, usernameForSignup: String = "", isSignUp: Boolean = false) {
        viewModelScope.launch {
            _authError.value = null
            val trimmedEmail = email.trim()
            if (trimmedEmail.isEmpty()) {
                _authError.value = "Email is required!"
                return@launch
            }

            if (isSignUp) {
                val trimmedUsername = usernameForSignup.trim()
                if (trimmedUsername.isEmpty()) {
                    _authError.value = "Username is required for signup!"
                    return@launch
                }
                val existing = repository.getUser(trimmedEmail)
                if (existing != null) {
                    _authError.value = "User already exists with this email!"
                    return@launch
                }
                // Determine placeholder color
                val hexColors = listOf("0xFFE040FB", "0xFF00E5FF", "0xFF00E676", "0xFFFF9100", "0xFFFF2D55")
                val newUser = User(
                    id = trimmedEmail,
                    username = trimmedUsername,
                    email = trimmedEmail,
                    bio = "Newly joined MemeWarlord trainee.",
                    profilePicHexColor = hexColors.random()
                )
                repository.saveUser(newUser)
                _currentUser.value = newUser
            } else {
                val existing = repository.getUser(trimmedEmail)
                if (existing == null) {
                    // Automatically pre-populate and sign in for high convenience
                    val hexColors = listOf("0xFFFFE600", "0xFF00FFCC", "0xFFD500F9")
                    val newUser = User(
                        id = trimmedEmail,
                        username = trimmedEmail.substringBefore("@"),
                        email = trimmedEmail,
                        bio = "New user.",
                        profilePicHexColor = hexColors.random()
                    )
                    repository.saveUser(newUser)
                    _currentUser.value = newUser
                } else if (existing.isBanned) {
                    _authError.value = "This user has been banned for violating community rules."
                } else {
                    _currentUser.value = existing
                }
            }
        }
    }

    fun loginWithGoogle(email: String, name: String) {
        viewModelScope.launch {
            _authError.value = null
            val existing = repository.getUser(email)
            if (existing != null) {
                if (existing.isBanned) {
                    _authError.value = "This Google account is banned from MemeVerse."
                } else {
                    _currentUser.value = existing
                }
            } else {
                val newUser = User(
                    id = email,
                    username = name,
                    email = email,
                    bio = "Signed in with Google. Free speech meme lover.",
                    profilePicHexColor = "0xFF4285F4", // Google Blue
                    isGoogleUser = true
                )
                repository.saveUser(newUser)
                _currentUser.value = newUser
            }
        }
    }

    fun logout() {
        _currentUser.value = null
    }

    fun updateProfile(bio: String, username: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updated = user.copy(bio = bio, username = username)
            repository.updateUser(updated)
            _currentUser.value = updated
        }
    }

    // Video Upload Simulation
    fun uploadVideo(title: String, description: String, hashtags: String, category: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _uploadProgress.value = 0.0f
            // Progress simulations
            delay(400)
            _uploadProgress.value = 0.25f
            delay(400)
            _uploadProgress.value = 0.55f
            delay(400)
            _uploadProgress.value = 0.85f
            delay(300)
            _uploadProgress.value = 1.0f
            delay(200)

            // Save actual database entries
            repository.uploadMeme(
                title = title,
                description = description,
                hashtags = if (hashtags.startsWith("#")) hashtags else hashtags.split(" ").joinToString(" ") { if (it.startsWith("#") || it.isEmpty()) it else "#$it" },
                category = category,
                creatorId = user.id,
                creatorName = user.username
            )

            // Reset progress & trigger success
            _uploadProgress.value = null
            _uploadSuccess.emit(true)
        }
    }

    // Interactive Action APIs
    fun toggleLike(memeId: Int) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.toggleLike(user.id, memeId)
        }
    }

    fun toggleFollow(followedCreatorId: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.toggleFollow(user.id, followedCreatorId)
        }
    }

    fun postComment(memeId: Int, commentText: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.commentMeme(memeId, user.id, user.username, commentText)
        }
    }

    fun deleteComment(commentId: Int, memeId: Int) {
        viewModelScope.launch {
            repository.deleteComment(commentId, memeId)
        }
    }

    // Report Videos or Users
    fun reportVideo(memeId: Int, reason: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val memes = allMemes.value
            val target = memes.find { it.id == memeId } ?: return@launch
            repository.reportMeme(memeId, target.creatorId, user.id, reason)
        }
    }

    fun reportUser(reportedUserId: String, reason: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.reportUser(reportedUserId, user.id, reason)
        }
    }

    // Increment Meme View Duration (Trigger play state metrics)
    fun incrementView(memeId: Int) {
        viewModelScope.launch {
            repository.incrementViewCount(memeId)
        }
    }

    // Admin commands
    fun adminDeleteVideo(memeId: Int) {
        val user = _currentUser.value ?: return
        if (!user.isAdmin) return
        viewModelScope.launch {
            repository.deleteMeme(memeId)
        }
    }

    fun adminBanUser(userIdToBan: String) {
        val user = _currentUser.value ?: return
        if (!user.isAdmin) return
        viewModelScope.launch {
            repository.banUser(userIdToBan)
        }
    }

    fun adminUnbanUser(userIdToUnban: String) {
        val user = _currentUser.value ?: return
        if (!user.isAdmin) return
        viewModelScope.launch {
            repository.unbanUser(userIdToUnban)
        }
    }

    fun adminResolveReport(reportId: Int, approve: Boolean) {
        val user = _currentUser.value ?: return
        if (!user.isAdmin) return
        viewModelScope.launch {
            val status = if (approve) "RESOLVED" else "DISMISSED"
            repository.resolveReport(reportId, status)
        }
    }

    // Dynamic Flow checks
    fun queryIsFollowing(creatorId: String): Flow<Boolean> {
        val loggedIn = _currentUser.value ?: return flowOf(false)
        return repository.isFollowing(loggedIn.id, creatorId)
    }

    fun queryHasLiked(memeId: Int): Flow<Boolean> {
        val loggedIn = _currentUser.value ?: return flowOf(false)
        return repository.hasLiked(loggedIn.id, memeId)
    }

    fun queryCommentsForMeme(memeId: Int): Flow<List<Comment>> {
        return repository.getCommentsForMeme(memeId)
    }
}
