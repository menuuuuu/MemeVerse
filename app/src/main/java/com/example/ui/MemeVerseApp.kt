package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.viewmodel.MemeVerseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemeVerseApp(viewModel: MemeVerseViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Navigation sub-routing tabs
    var currentTab by remember { mutableStateOf("feed") }
    var showAdminDashboard by remember { mutableStateOf(false) }
    var showCommunityRules by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (currentUser == null) {
            // Authentication mode (Signup/Login)
            WelcomeScreen(
                viewModel = viewModel,
                authError = authError,
                onLoginSuccess = {
                    Toast.makeText(context, "Welcome back, custom meme lover!", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            val user = currentUser!!
            // Primary scaffolding layout
            Scaffold(
                bottomBar = {
                    if (!showAdminDashboard) {
                        MemeVerseBottomBar(
                            currentTab = currentTab,
                            onTabSelected = { currentTab = it },
                            isAdmin = user.isAdmin,
                            onOpenAdmin = { showAdminDashboard = true }
                        )
                    }
                },
                topBar = {
                    if (showAdminDashboard) {
                        CenterAlignedTopAppBar(
                            title = { Text("Admin Desk ⚔️", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                            navigationIcon = {
                                IconButton(onClick = { showAdminDashboard = false }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (showAdminDashboard && user.isAdmin) {
                        AdminDashboardScreen(viewModel = viewModel)
                    } else {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                            },
                            label = "tab_animation"
                        ) { tab ->
                            when (tab) {
                                "feed" -> MainFeedScreen(viewModel = viewModel, onShowRules = { showCommunityRules = true })
                                "search" -> SearchScreen(viewModel = viewModel)
                                "upload" -> UploadScreen(viewModel = viewModel, onUploadSuccess = { currentTab = "feed" })
                                "trending" -> TrendingScreen(viewModel = viewModel)
                                "profile" -> ProfileScreen(viewModel = viewModel, onShowRules = { showCommunityRules = true })
                            }
                        }
                    }

                    // Global overlays
                    if (showCommunityRules) {
                        CommunityRulesOverlay(onDismiss = { showCommunityRules = false })
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// BOTTOM BAR NAVIGATION
// ----------------------------------------------------
@Composable
fun MemeVerseBottomBar(
    currentTab: String,
    onTabSelected: (String) -> Unit,
    isAdmin: Boolean,
    onOpenAdmin: () -> Unit
) {
    Surface(
        color = Color.Black,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = Color(0xFF27272A),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            },
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                Triple("feed", "Feed", Icons.Filled.LiveTv),
                Triple("search", "Search", Icons.Filled.Search),
                Triple("upload", "Upload", Icons.Filled.Add),
                Triple("trending", "Trending", Icons.Filled.Whatshot),
                Triple("profile", "Profile", Icons.Filled.Person)
            )

            items.forEach { (route, label, icon) ->
                val isSelected = currentTab == route
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onTabSelected(route) }
                        .testTag("nav_tab_$route"),
                    contentAlignment = Alignment.Center
                ) {
                    if (route == "upload") {
                        // High-contrast stacked overlay Add button
                        Box(
                            modifier = Modifier.size(width = 44.dp, height = 28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Left Cyan Offset
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .offset(x = (-3).dp)
                                    .background(Color(0xFF22D3EE), RoundedCornerShape(8.dp))
                            )
                            // Right Rose Offset
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .offset(x = 3.dp)
                                    .background(Color(0xFFF43F5E), RoundedCornerShape(8.dp))
                            )
                            // White Center Accent
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Upload",
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.Black
                                )
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                modifier = Modifier.size(24.dp),
                                tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            if (isAdmin) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onOpenAdmin() }
                        .testTag("nav_tab_admin"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = "Admin",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Admin",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// AUTHENTICATION SCREEN (WELCOME & SIGNIN)
// ----------------------------------------------------
@Composable
fun WelcomeScreen(
    viewModel: MemeVerseViewModel,
    authError: String?,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }

    // Dynamic graphic loops
    val infiniteTransition = rememberInfiniteTransition(label = "welcoming")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF03030F), Color(0xFF100B25))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Neon circle branding representation with rotated brush overlay
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .drawBehind {
                        rotate(sweepAngle) {
                            drawCircle(
                                brush = Brush.sweepGradient(
                                    colors = listOf(Color(0xFFFFE600), Color(0xFF00FFCC), Color(0xFFFF1744), Color(0xFFFFE600))
                                ),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f)
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🤣",
                    fontSize = 52.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "MemeVerse",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp
            )

            Text(
                text = "Free Speech. Humorous Criticism.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Auth board
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF12122B))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isSignUp) "Create Account" else "Log In to Verse",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input")
                    )

                    if (isSignUp) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Enter Username") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_username_input")
                        )
                    }

                    if (authError != null) {
                        Text(
                            text = authError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.loginWithEmail(email, username, isSignUp)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("auth_submit_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = if (isSignUp) "Register" else "Enter Platform",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = { isSignUp = !isSignUp }) {
                        Text(
                            text = if (isSignUp) "Already have an account? Sign In" else "New to MemeVerse? Register Here",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 13.sp
                        )
                    }

                    Divider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    )

                    // Big Google Logon flow container
                    Button(
                        onClick = {
                            // Automatically logs in as virtual beautiful Google session
                            viewModel.loginWithGoogle("gamerma209@gmail.com", "MemeEnthusiast_Google")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("google_login_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA4335))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Group, contentDescription = "Google Icon", tint = Color.White)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Fast Sign In with Google", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                onClick = { /* Display rules page if needed */ },
                color = Color.Transparent
            ) {
                Text(
                    text = "By signing in, you agree to advocate free speech, avoid violence promotion, and abide by Community Guidelines.",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

// ----------------------------------------------------
// TIKTOK-STYLE VIDEO FEED SCREEN
// ----------------------------------------------------
@Composable
fun MainFeedScreen(viewModel: MemeVerseViewModel, onShowRules: () -> Unit) {
    val memes by viewModel.allMemes.collectAsStateWithLifecycle()
    val activeCategory by viewModel.activeCategory.collectAsStateWithLifecycle()
    val followedCreatorIds by viewModel.followedCreatorIds.collectAsStateWithLifecycle()

    var feedMode by remember { mutableStateOf("FOR YOU") } // "FOR YOU" vs "Following"

    val filteredMemes = remember(memes, activeCategory, feedMode, followedCreatorIds) {
        val baseList = if (activeCategory == "All") memes else memes.filter { it.category == activeCategory }
        if (feedMode == "Following") {
            baseList.filter { it.creatorId in followedCreatorIds }
        } else {
            baseList
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // High-contrast Bold Top Navigation (Following vs FOR YOU)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // FOLLOWING Tab
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { feedMode = "Following" }
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = "Following",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = if (feedMode == "Following") FontWeight.Black else FontWeight.Bold,
                    color = if (feedMode == "Following") Color.White else Color.White.copy(alpha = 0.5f),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 3.dp)
                        .background(
                            color = if (feedMode == "Following") Color.White else Color.Transparent,
                            shape = RoundedCornerShape(1.5.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // FOR YOU Tab
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { feedMode = "FOR YOU" }
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = "FOR YOU",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = if (feedMode == "FOR YOU") FontWeight.Black else FontWeight.Bold,
                    color = if (feedMode == "FOR YOU") Color.White else Color.White.copy(alpha = 0.5f),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(width = 24.dp, height = 3.dp)
                        .background(
                            color = if (feedMode == "FOR YOU") Color.White else Color.Transparent,
                            shape = RoundedCornerShape(1.5.dp)
                        )
                )
            }
        }

        // Horizontal bar categories top selectors
        val categories = listOf("All", "Satire", "Gaming", "Fail", "Dank", "Animals")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val isActive = activeCategory == cat
                Surface(
                    onClick = { viewModel.setCategory(cat) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isActive) MaterialTheme.colorScheme.primary else Color(0xFF18181B),
                    border = if (isActive) null else BorderStroke(1.dp, Color(0xFF27272A)),
                    modifier = Modifier.testTag("category_tab_$cat")
                ) {
                    Text(
                        text = cat,
                        color = if (isActive) Color.White else Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = if (isActive) FontWeight.Black else FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        if (filteredMemes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("👻", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "No short video memes in $activeCategory yet!",
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Be the first to upload an amazing sarcastic post in this category!",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                }
            }
        } else {
            // Full snap vertical list representing automated playback TikTok layout with VerticalPager
            val pagerState = rememberPagerState(pageCount = { filteredMemes.size })
            VerticalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) { page ->
                val meme = filteredMemes[page]
                val isVisible = pagerState.currentPage == page
                MemeVerticalItem(meme = meme, viewModel = viewModel, onShowRules = onShowRules, isPlaying = isVisible)
            }
        }
    }
}

@Composable
fun MemeVerticalItem(meme: Meme, viewModel: MemeVerseViewModel, onShowRules: () -> Unit, isPlaying: Boolean = true) {
    val commentsFlow = remember(meme.id) { viewModel.queryCommentsForMeme(meme.id) }
    val comments by commentsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val isLikedFlow = remember(meme.id) { viewModel.queryHasLiked(meme.id) }
    val isLiked by isLikedFlow.collectAsStateWithLifecycle(initialValue = false)

    val isFollowingFlow = remember(meme.creatorId) { viewModel.queryIsFollowing(meme.creatorId) }
    val isFollowing by isFollowingFlow.collectAsStateWithLifecycle(initialValue = false)

    var showCommentsSheet by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }
    var localIsSaved by remember { mutableStateOf(false) }

    // Fast-running play simulation bar
    var progress by remember { mutableStateOf(0.1f) }
    var isPaused by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(meme.id, isPaused, isPlaying) {
        if (isPlaying) {
            viewModel.incrementView(meme.id)
            while (!isPaused) {
                delay(100)
                progress += 0.015f
                if (progress >= 1.0f) progress = 0.0f
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(580.dp)
            .border(width = 1.dp, color = Color(0xFF27272A))
            .background(Color.Black)
            .testTag("meme_feed_item_${meme.id}")
    ) {
        // Background simulated loop using animated gradients
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(meme.videoGradientStartColor.removePrefix("0x").toLong(radix = 16)),
                            Color(meme.videoGradientEndColor.removePrefix("0x").toLong(radix = 16))
                        )
                    )
                )
        ) {
            // Animated visual loops drawn inside container
            val infiniteTransition = rememberInfiniteTransition(label = "playback_loops")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "beat"
            )

            // Centered huge humorous icon
            Box(
                modifier = Modifier
                    .size(160.dp * pulseScale)
                    .align(Alignment.Center)
                    .drawBehind {
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.15f),
                            radius = this.size.width / 2.0f
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = meme.animatedEmoji,
                    fontSize = 72.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Overlay transparent shadow to highlight captions
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                        startY = 350f
                    )
                )
        )

        // Bottom Left details (Creator, title, hashtags)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp)
                .fillMaxWidth(0.72f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22D3EE)), // Cyan styled circle border
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = meme.creatorName.take(1).uppercase(),
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "@${meme.creatorName}",
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-0.5).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Follow toggle button representation (capsule)
                Surface(
                    onClick = {
                        viewModel.toggleFollow(meme.creatorId)
                    },
                    shape = RoundedCornerShape(24.dp),
                    color = if (isFollowing) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("follow_toggle_btn")
                ) {
                    Text(
                        text = if (isFollowing) "Following" else "Follow",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = meme.title,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = meme.description,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif,
                lineHeight = 17.sp
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Icon(Icons.Filled.MusicNote, contentDescription = "Audio track", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Original Audio - MemeVerse Sound Sync",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Bottom horizontal progress bar representing playing reels (using modern progress lambda lambda)
        LinearProgressIndicator(
            progress = { progress },
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.White.copy(alpha = 0.2f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(4.dp)
        )

        // Absolute Right controls bar (Like, comments count, share, save, report)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Play vs Pause toggle overlay button
            IconButton(
                onClick = { isPaused = !isPaused },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .size(42.dp)
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = "Play/Pause",
                    tint = Color.White
                )
            }

            // Like Heart
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { viewModel.toggleLike(meme.id) },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .size(44.dp)
                        .testTag("like_btn_${meme.id}")
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) MaterialTheme.colorScheme.tertiary else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "${meme.likeCount}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Comments
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { showCommentsSheet = !showCommentsSheet },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .size(44.dp)
                        .testTag("comment_btn_${meme.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Comment,
                        contentDescription = "Comment",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "${meme.commentCount}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Bookmark/Save
            IconButton(
                onClick = {
                    localIsSaved = !localIsSaved
                    val status = if (localIsSaved) "saved locally" else "removed from bookmarks"
                    Toast.makeText(context, "Meme successfully $status!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .size(44.dp)
            ) {
                Icon(
                    imageVector = if (localIsSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = "Save video",
                    tint = if (localIsSaved) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Share
            IconButton(
                onClick = {
                    Toast.makeText(context, "Meme link copied to clipboard! Share critical humor everywhere.", Toast.LENGTH_LONG).show()
                },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Share",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Content Reporting Red Flag
            IconButton(
                onClick = { showReportDialog = true },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .size(42.dp)
                    .testTag("report_video_btn_${meme.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Report violations",
                    tint = Color.Red,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    // Floating comments sheet representation
    if (showCommentsSheet) {
        Dialog(onDismissRequest = { showCommentsSheet = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161626))
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Comments (${comments.size})",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        IconButton(onClick = { showCommentsSheet = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close comments", tint = Color.White)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (comments.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No comments yet! Express critical criticism freely.",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(comments) { comment ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "@${comment.username}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        // Simple deleted comment action representation for admins
                                        Text(
                                            text = "Just now",
                                            fontSize = 9.sp,
                                            color = Color.White.copy(alpha = 0.40f)
                                        )
                                    }
                                    Text(
                                        text = comment.text,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    var newCommentText by remember { mutableStateOf("") }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCommentText,
                            onValueChange = { newCommentText = it },
                            placeholder = { Text("Write humorous feedback...", fontSize = 12.sp, color = Color.White.copy(alpha = 0.4f)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("comment_input_box")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (newCommentText.isNotBlank()) {
                                    viewModel.postComment(meme.id, newCommentText)
                                    newCommentText = ""
                                }
                            },
                            modifier = Modifier
                                .height(46.dp)
                                .testTag("send_comment_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Post", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // CONTENT REPORT DIALOG
    if (showReportDialog) {
        Dialog(onDismissRequest = { showReportDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF261212)), // High-warning visual backdrop
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Report Sarcastic Content 🚨",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )

                    Text(
                        text = "MemeVerse advocates freedom of speech and political criticism. However, we have STRICT penalties for harmful items violating guidelines. Please check reasons:",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
                    )

                    val violations = listOf(
                        "Violence Promotion & Direct Threats",
                        "Harmful Personal Leaks / Doxxing",
                        "Illegal contraband, materials or exploits",
                        "Hate speech & severe harassment"
                    )

                    violations.forEach { opt ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reportReason = opt }
                                .padding(vertical = 5.dp)
                        ) {
                            RadioButton(
                                selected = reportReason == opt,
                                onClick = { reportReason = opt },
                                colors = RadioButtonDefaults.colors(selectedColor = Color.Red)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = opt, color = Color.White, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showReportDialog = false }) {
                            Text("Cancel", color = Color.White)
                        }

                        Button(
                            onClick = {
                                if (reportReason.isNotBlank()) {
                                    viewModel.reportVideo(meme.id, reportReason)
                                    Toast.makeText(context, "Thank you! Reported successfully to the MemeVerse Moderation queue.", Toast.LENGTH_LONG).show()
                                    showReportDialog = false
                                } else {
                                    Toast.makeText(context, "Please select warning reason", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.testTag("report_submit_btn")
                        ) {
                            Text("Submit Report", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// SEARCH USERS, VIDEOS, AND HASHTAGS SCREEN
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: MemeVerseViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchedMemes by viewModel.searchedMemes.collectAsStateWithLifecycle()
    val users by viewModel.allUsers.collectAsStateWithLifecycle()

    var activeSearchTab by remember { mutableStateOf("memes") } // memes or users

    val filteredUsers = remember(users, searchQuery) {
        if (searchQuery.isBlank()) users else users.filter { it.username.contains(searchQuery, ignoreCase = true) || it.email.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Explore MemeVerse",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Custom search bar input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("Search jokes, #hashtags, users, category...") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search magnifier", tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear input", tint = Color.White)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.25f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("user_search_input")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { activeSearchTab = "memes" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeSearchTab == "memes") MaterialTheme.colorScheme.primary else Color(0xFF1B1B2B)
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_tab_memes")
            ) {
                Text(
                    text = "Videos / Tags",
                    color = if (activeSearchTab == "memes") Color.Black else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = { activeSearchTab = "users" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeSearchTab == "users") MaterialTheme.colorScheme.primary else Color(0xFF1B1B2B)
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_tab_users")
            ) {
                Text(
                    text = "User Profiles",
                    color = if (activeSearchTab == "users") Color.Black else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (activeSearchTab == "memes") {
            if (searchedMemes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No memes or tags match '$searchQuery'",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(searchedMemes) { meme ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF121224)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(meme.videoGradientStartColor.removePrefix("0x").toLong(radix = 16)),
                                                Color(meme.videoGradientEndColor.removePrefix("0x").toLong(radix = 16))
                                            )
                                        )
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = meme.animatedEmoji,
                                    fontSize = 28.sp,
                                    modifier = Modifier.align(Alignment.TopEnd)
                                )

                                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                                    Text(
                                        text = meme.title,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "@${meme.creatorName}",
                                        color = Color.White.copy(alpha = 0.70f),
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = meme.hashtags,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 9.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (filteredUsers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No users found match '$searchQuery'",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredUsers) { usr ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF121224)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                                Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color(usr.profilePicHexColor.removePrefix("0x").toLong(radix = 16))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = usr.username.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = usr.username,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = usr.bio,
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (usr.isAdmin) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black) {
                                        Text("Admin", fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// DYNAMIC VIDEO UPLOAD SCREEN (SIMULATION WITH PROGRESS)
// ----------------------------------------------------
@Composable
fun UploadScreen(viewModel: MemeVerseViewModel, onUploadSuccess: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var hashtags by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Satire") }
    val categories = listOf("Satire", "Gaming", "Fail", "Dank", "Animals")

    val uploadProgress by viewModel.uploadProgress.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uploadSuccess.collect { success ->
            if (success) {
                Toast.makeText(context, "Meme short video shared successfully to MemeVerse!", Toast.LENGTH_LONG).show()
                onUploadSuccess()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Publish Sarcasm ➕",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Post political criticism, dank memes, fail compilation clips and funny animations. All constructive criticism and banter is supported under community guidelines.",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.65f),
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        if (uploadProgress != null) {
            // Upload simulation block
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131326)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        progress = uploadProgress ?: 0f,
                        color = MaterialTheme.colorScheme.secondary,
                        strokeWidth = 6.dp,
                        modifier = Modifier.size(72.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Uploading Video Assets...",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )

                    Text(
                        text = "${((uploadProgress ?: 0f) * 100).toInt()}% Done. Compiling video metadata into SQLite cluster repository...",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = uploadProgress ?: 0f,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        } else {
            // Simulated local video file select field
            var localFileSelected by remember { mutableStateOf(false) }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151421)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clickable {
                        localFileSelected = true
                        Toast.makeText(context, "Meme video template 'humor_rendering_reels.mp4' loaded from media selection", Toast.LENGTH_SHORT).show()
                    }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (localFileSelected) Icons.Filled.CheckCircle else Icons.Filled.VideoFile,
                            contentDescription = "Upload selection box",
                            tint = if (localFileSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(46.dp)
                        )
                        Text(
                            text = if (localFileSelected) "Selected: humor_rendering_reels.mp4" else "Tap to choose short video from phone",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            text = "Maximum size 100MB. Full support for mp4, webm, webp, gif.",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Video Title") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_title_box")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Short Description") },
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_desc_box")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = hashtags,
                onValueChange = { hashtags = it },
                label = { Text("Hashtags (space-separated, e.g. #satire #funny)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_tags_box")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Select Channel Category", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isChecked = category == cat
                    FilterChip(
                        selected = isChecked,
                        onClick = { category = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (!localFileSelected) {
                        Toast.makeText(context, "Please select local video file dummy!", Toast.LENGTH_SHORT).show()
                    } else if (title.isBlank() || description.isBlank()) {
                        Toast.makeText(context, "Title and description cannot be empty!", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.uploadVideo(title, description, hashtags, category)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("upload_meme_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Share to MemeVerse Feed 🚀", fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}

// ----------------------------------------------------
// TRENDING SECTION SCREEN
// ----------------------------------------------------
@Composable
fun TrendingScreen(viewModel: MemeVerseViewModel) {
    val trendingMemes by viewModel.trendingMemes.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Icon(Icons.Filled.Whatshot, contentDescription = "Trending heat indicator", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Hottest Humor ⚡",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = "Most viewed, liked, and shared sarcastic reels currently ruling the free speech algorithm.",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (trendingMemes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading heat charts...", color = Color.White.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(trendingMemes.take(5).zip(1..5)) { (meme, rank) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B2B)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Trending Rank Badge
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(48.dp)
                                    .background(
                                        when (rank) {
                                            1 -> Color(0xFFFFD700) // Gold
                                            2 -> Color(0xFFC0C0C0) // Silver
                                            3 -> Color(0xFFCD7F32) // Bronze
                                            else -> Color(0xFF28283D)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "#$rank",
                                    color = if (rank <= 3) Color.Black else Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            // Content details
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(12.dp)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = meme.title,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "@${meme.creatorName} in ${meme.category}",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 11.sp
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Favorite, contentDescription = "Likes", tint = Color.Red, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${meme.likeCount}", color = Color.White, fontSize = 10.sp)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Visibility, contentDescription = "Views", tint = Color.Cyan, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${meme.viewCount} views", color = Color.White, fontSize = 10.sp)
                                    }
                                }
                            }

                            // Emoji stamp representation
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(64.dp)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(meme.animatedEmoji, fontSize = 28.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// USER PROFILE SCREEN
// ----------------------------------------------------
@Composable
fun ProfileScreen(viewModel: MemeVerseViewModel, onShowRules: () -> Unit) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val memes by viewModel.allMemes.collectAsStateWithLifecycle()

    val context = LocalContext.current

    if (currentUser == null) return
    val user = currentUser!!

    val userUploadedMemes = remember(memes, user.id) {
        memes.filter { it.creatorId == user.id }
    }

    var showEditProfile by remember { mutableStateOf(false) }
    var editBio by remember { mutableStateOf(user.bio) }
    var editUsername by remember { mutableStateOf(user.username) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Profile 👤",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )

            Row {
                IconButton(onClick = onShowRules) {
                    Icon(Icons.Filled.Gavel, contentDescription = "Platform Rules", tint = MaterialTheme.colorScheme.primary)
                }

                IconButton(onClick = { viewModel.logout() }) {
                    Icon(Icons.Filled.ExitToApp, contentDescription = "Log out", tint = Color.Red)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large user Avatar
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(Color(user.profilePicHexColor.removePrefix("0x").toLong(radix = 16))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.username.take(2).uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "@${user.username}",
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 18.sp
        )

        Text(
            text = user.email,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp
        )

        Surface(
            modifier = Modifier.padding(vertical = 10.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF13132B)
        ) {
            Text(
                text = user.bio.ifEmpty { "This critical troll is silent. No bio written yet." },
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }

        // Stats counts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "${user.followingCount}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                Text(text = "Following", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "${user.followerCount}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                Text(text = "Followers", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "${user.totalLikes}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                Text(text = "Total Likes Received", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Button(
                onClick = { showEditProfile = true },
                modifier = Modifier
                    .weight(1f)
                    .testTag("edit_profile_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF252538))
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit Profile icon", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Modify Bio & Name", color = Color.White)
            }
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color.White.copy(alpha = 0.12f))

        Text(
            text = "My Uploaded Reels  (${userUploadedMemes.size})",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 15.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.0.dp),
            textAlign = TextAlign.Start
        )

        if (userUploadedMemes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("You haven't posted any funny short videos or satire yet!", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
            }
        } else {
            // Display user uploaded memes as an aesthetic responsive list
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                userUploadedMemes.forEach { meme ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131326)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Toast.makeText(context, "Meme reels is playing locally showing ${meme.likeCount} likes", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(
                                        Brush.sweepGradient(
                                            colors = listOf(Color(meme.videoGradientStartColor.removePrefix("0x").toLong(radix = 16)), Color(meme.videoGradientEndColor.removePrefix("0x").toLong(radix = 16)))
                                        ), shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(meme.animatedEmoji, fontSize = 18.sp)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = meme.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = "Category: ${meme.category} | ${meme.hashtags}", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Favorite, contentDescription = "Likes counter", tint = Color.Red, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "${meme.likeCount}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Floating Edit profile dialog popup
    if (showEditProfile) {
        Dialog(onDismissRequest = { showEditProfile = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131326)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Modify Profile info", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = editUsername,
                        onValueChange = { editUsername = it },
                        label = { Text("Display Username") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Short Bio") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showEditProfile = false }) {
                            Text("Discard", color = Color.White)
                        }

                        Button(
                            onClick = {
                                if (editUsername.isNotBlank()) {
                                    viewModel.updateProfile(editBio, editUsername)
                                    showEditProfile = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Save info", color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// COMMUNITY RULES OVERLAY SCREEN
// ----------------------------------------------------
@Composable
fun CommunityRulesOverlay(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0C1F)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MemeVerse Guidelines 📃",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close terms dialog", tint = Color.White)
                    }
                }

                Text(
                    text = "The home of political satire, dark comedy, internet memes, and witty banter. We support FREE SPEECH AND CRITICISM, but we enforce strict lines to keep the ecosystem safe and healthy:",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                val rules = listOf(
                    "No Direct Threats" to "We allow satire, mockery, criticism, but zero direct lines of physically threatening real life harms.",
                    "No Violence Promotion" to "Promoting, displaying, or calling for physical harm on groups/individuals is strictly forbidden.",
                    "No Severe Hate Speech" to "Slurs and systematic dehumanization of marginalized groups are banned.",
                    "No Personal Information/Doxxing" to "Absolutely zero doxxing (publishing home addresses, emails, numbers, private photos). Keep trolls strictly virtual!",
                    "No Illegal Content" to "Exploitation hacks, drug sales, links to illicit files, copyright malware. Everything illegal is immediately purged."
                )

                rules.forEachIndexed { idx, (ruleTitle, ruleDesc) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "${idx + 1}. $ruleTitle",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = ruleDesc,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("I Understand & Agree", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }
}

// ----------------------------------------------------
// ADMIN DASHBOARD SCREEN
// ----------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdminDashboardScreen(viewModel: MemeVerseViewModel) {
    val users by viewModel.allUsers.collectAsStateWithLifecycle()
    val memes by viewModel.allMemes.collectAsStateWithLifecycle()
    val pendingReports by viewModel.pendingReports.collectAsStateWithLifecycle()

    var activeAdminTab by remember { mutableStateOf("reports") } // reports, users, videos

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Site Statistics board
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1111)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "MemeVerse Core Audits 🛡️",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${users.size}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                        Text("Users", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${memes.size}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 16.sp)
                        Text("Active Videos", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${pendingReports.size}", fontWeight = FontWeight.Bold, color = Color.Red, fontSize = 16.sp)
                        Text("Active Reports", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    }
                }
            }
        }

        // Subtabs for Admin (reports queue, users list, videos list)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabs = listOf("reports" to "Queue", "users" to "All Users", "videos" to "All Videos")
            tabs.forEach { (key, label) ->
                val isSelected = activeAdminTab == key
                Button(
                    onClick = { activeAdminTab = key },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF281111)
                    ),
                    modifier = Modifier.weight(1f).testTag("admin_tab_$key")
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Display Admin subtab contents
        when (activeAdminTab) {
            "reports" -> {
                if (pendingReports.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🛡️", fontSize = 42.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No reports pending reviews!", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                            Text("All users are complying with guidelines.", color = Color.White.copy(alpha = 0.35f), fontSize = 11.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(pendingReports) { report ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF291A1A)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f)),
                                modifier = Modifier.testTag("report_item_${report.id}")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Report Id: #${report.id}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.Red
                                    )
                                    Text(
                                        text = "Reported Creator: @${report.reportedUserId}",
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Reason: ${report.reason}",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Take Action: Delete video
                                        if (report.memeId != 0) {
                                            Button(
                                                onClick = {
                                                    viewModel.adminDeleteVideo(report.memeId)
                                                    viewModel.adminResolveReport(report.id, approve = true)
                                                    Toast.makeText(context, "Offending sarcastic reel deleted, and report resolved!", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                                modifier = Modifier.weight(1f).height(36.dp)
                                            ) {
                                                Text("Delete Video", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // Take Action: Ban User
                                        Button(
                                            onClick = {
                                                viewModel.adminBanUser(report.reportedUserId)
                                                viewModel.adminResolveReport(report.id, approve = true)
                                                Toast.makeText(context, "User banned, and report resolved successfully!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                                            modifier = Modifier.weight(1f).height(36.dp)
                                        ) {
                                            Text("Ban User", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // Deny action (Dismiss Report)
                                        Button(
                                            onClick = {
                                                viewModel.adminResolveReport(report.id, approve = false)
                                                Toast.makeText(context, "Report dismissed successfully! Free speech preserved.", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E3E)),
                                            modifier = Modifier.weight(1f).height(36.dp)
                                        ) {
                                            Text("Dismiss", color = Color.White, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "users" -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(users) { usr ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131326)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "@${usr.username}",
                                        fontWeight = FontWeight.Bold,
                                        color = if (usr.isBanned) Color.Red else Color.White,
                                        fontSize = 13.sp
                                    )
                                    Text(text = usr.email, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                                    if (usr.isBanned) {
                                        Text("Status: Banned 🚫", color = Color.Red, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Row {
                                    if (usr.isBanned) {
                                        TextButton(onClick = { viewModel.adminUnbanUser(usr.id) }) {
                                            Text("Unban", color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        TextButton(onClick = { viewModel.adminBanUser(usr.id) }) {
                                            Text("Ban User", color = Color.Red, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "videos" -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(memes) { meme ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131326)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = meme.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(text = "Creator: @${meme.creatorName} | Category: ${meme.category}", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.adminDeleteVideo(meme.id)
                                        Toast.makeText(context, "Meme Video deleted by Admin", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete Video", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
