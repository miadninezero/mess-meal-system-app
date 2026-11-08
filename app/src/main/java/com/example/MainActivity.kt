package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.AuthManager
import com.example.data.repository.MessRepository
import com.example.ui.MessViewModel
import com.example.ui.screens.BazaarLogScreen
import com.example.ui.screens.DailyMealsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DepositsScreen
import com.example.ui.screens.MessSelectScreen
import com.example.ui.screens.SettlementScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.theme.MessMealTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MessMealTheme {
                AppContent()
            }
        }
    }
}

enum class Screen {
    DASHBOARD,
    DAILY_MEALS,
    BAZAAR_LOG,
    DEPOSITS,
    SETTLEMENT,
    MESS_SELECT
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var isCheckingAuth by remember { mutableStateOf(currentUser == null) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        if (currentUser == null) {
            AuthManager.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                    isCheckingAuth = false
                },
                onUnauthenticated = {
                    isCheckingAuth = false
                },
                scope = coroutineScope
            )
        } else {
            isCheckingAuth = false
        }
    }

    if (isCheckingAuth) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (currentUser == null) {
        SignInScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        val uid = currentUser!!.uid
        key(uid) {
            MainFeatureScreen(
                currentUserId = uid,
                onSignOut = {
                    AuthManager.signOut(
                        credentialManager = credentialManager,
                        scope = coroutineScope,
                        onSignOutComplete = {
                            currentUser = null
                        }
                    )
                }
            )
        }
    }
}

@Composable
fun MainFeatureScreen(
    currentUserId: String,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { MessRepository(context) }
    val viewModel: MessViewModel = viewModel(
        key = currentUserId,
        factory = MessViewModel.Factory(currentUserId, repository, context)
    )

    val currentMess by viewModel.currentMess.collectAsState()
    val selectedMessId by viewModel.selectedMessId.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUiMessage()
        }
    }

    // If user has not chosen or joined any mess yet, show MessSelectScreen
    if (selectedMessId.isBlank()) {
        MessSelectScreen(
            viewModel = viewModel,
            onSignOut = onSignOut
        )
        return
    }

    // BackHandler for sub-screens
    if (currentScreen != Screen.DASHBOARD) {
        BackHandler {
            currentScreen = Screen.DASHBOARD
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentScreen != Screen.MESS_SELECT) {
                NavigationBar(
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    NavigationBarItem(
                        selected = currentScreen == Screen.DASHBOARD,
                        onClick = { currentScreen = Screen.DASHBOARD },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.DAILY_MEALS,
                        onClick = { currentScreen = Screen.DAILY_MEALS },
                        icon = { Icon(Icons.Default.Restaurant, contentDescription = "Daily Meals") },
                        label = { Text("Meals") }
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.BAZAAR_LOG,
                        onClick = { currentScreen = Screen.BAZAAR_LOG },
                        icon = { Icon(Icons.Default.ShoppingBag, contentDescription = "Bazaar Log") },
                        label = { Text("Bazaar") }
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.DEPOSITS,
                        onClick = { currentScreen = Screen.DEPOSITS },
                        icon = { Icon(Icons.Default.Payments, contentDescription = "Deposits") },
                        label = { Text("Deposits") }
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.SETTLEMENT,
                        onClick = { currentScreen = Screen.SETTLEMENT },
                        icon = { Icon(Icons.Default.Assessment, contentDescription = "Settlement") },
                        label = { Text("Balance") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.DASHBOARD -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToMeals = { currentScreen = Screen.DAILY_MEALS },
                        onNavigateToBazaar = { currentScreen = Screen.BAZAAR_LOG },
                        onNavigateToDeposits = { currentScreen = Screen.DEPOSITS },
                        onNavigateToSettlement = { currentScreen = Screen.SETTLEMENT },
                        onChangeMess = { currentScreen = Screen.MESS_SELECT },
                        onSignOut = onSignOut
                    )
                }
                Screen.DAILY_MEALS -> {
                    DailyMealsScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = Screen.DASHBOARD }
                    )
                }
                Screen.BAZAAR_LOG -> {
                    BazaarLogScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = Screen.DASHBOARD }
                    )
                }
                Screen.DEPOSITS -> {
                    DepositsScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = Screen.DASHBOARD }
                    )
                }
                Screen.SETTLEMENT -> {
                    SettlementScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = Screen.DASHBOARD }
                    )
                }
                Screen.MESS_SELECT -> {
                    MessSelectScreen(
                        viewModel = viewModel,
                        onSignOut = onSignOut,
                        onReturnToDashboard = { currentScreen = Screen.DASHBOARD }
                    )
                }
            }
        }
    }
}
