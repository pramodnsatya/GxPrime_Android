package com.pramod.validator

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.pramod.validator.navigation.NavGraph
import com.pramod.validator.navigation.Screen
import com.pramod.validator.ui.theme.ValidatorTheme
import com.pramod.validator.viewmodel.AuthViewModel
import com.pramod.validator.viewmodel.AuthState
import com.pramod.validator.utils.NetworkMonitor
import com.pramod.validator.utils.AISummaryGenerator
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate() for Android 12+
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize Crashlytics
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        
        // Configure Firebase Auth with longer timeout
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        auth.useAppLanguage()
        
        // Enable Firestore offline persistence
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .build()
        db.firestoreSettings = settings
        
        // Monitor network and process pending AI summaries when online
        val networkMonitor = NetworkMonitor(this)
        applicationScope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                if (isOnline) {
                    android.util.Log.d("MainActivity", "🌐 Online: Checking for pending AI summaries...")
                    AISummaryGenerator.processPendingSummaries(this@MainActivity, applicationScope)
                }
            }
        }
        
        // Handle deep links
        handleDeepLink(intent)
        
        enableEdgeToEdge()
        setContent {
            ValidatorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel = viewModel()
                    
                    // Determine start destination based on auth status and user role
                    val authState by authViewModel.authState.collectAsState()
                    val currentUser by authViewModel.currentUser.collectAsState()
                    
                    // Show loading screen while loading auth state or user data
                    if (authState is AuthState.Loading || (authState is AuthState.Authenticated && currentUser == null)) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = "Loading...",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    } else {
                        // Calculate destination synchronously
                        val startDestination = when {
                            authState is AuthState.Authenticated -> {
                                currentUser?.let { user ->
                                    when (user.role) {
                                        "SUPER_ADMIN" -> Screen.SuperAdminHome.route
                                        "ENTERPRISE_ADMIN" -> {
                                            val enterpriseId = user.enterpriseId
                                            if (enterpriseId.isNullOrBlank()) {
                                                Screen.Login.route // Fallback to login if no enterprise ID
                                            } else {
                                                Screen.EnterpriseAdminHome.createRoute(enterpriseId)
                                            }
                                        }
                                        else -> Screen.Home.route
                                    }
                                } ?: Screen.Login.route
                            }
                            else -> Screen.Login.route
                        }
                        
                        // Show NavGraph with calculated destination
                        NavGraph(
                            navController = navController,
                            startDestination = startDestination
                        )
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleDeepLink(it) }
    }
    
    private fun handleDeepLink(intent: Intent) {
        val data: Uri? = intent.data
        if (data != null) {
            android.util.Log.d("MainActivity", "Deep link received: $data")
            
            when {
                data.scheme == "https" && data.host == "yourapp.com" && data.path?.startsWith("/invite") == true -> {
                    val token = data.getQueryParameter("token")
                    if (token != null) {
                        android.util.Log.d("MainActivity", "Invitation token: $token")
                        // Navigate to invitation acceptance screen
                        navigateToInvitationAcceptance(token)
                    }
                }
                data.scheme == "validator" && data.host == "invite" -> {
                    val token = data.getQueryParameter("token")
                    if (token != null) {
                        android.util.Log.d("MainActivity", "Invitation token: $token")
                        // Navigate to invitation acceptance screen
                        navigateToInvitationAcceptance(token)
                    }
                }
            }
        }
    }
    
    private fun navigateToInvitationAcceptance(token: String) {
        // This will be handled by the navigation system
        // For now, we'll store the token in a shared preference or pass it through the navigation
        val sharedPref = getSharedPreferences("invitation", MODE_PRIVATE)
        sharedPref.edit().putString("invitation_token", token).apply()
        android.util.Log.d("MainActivity", "Stored invitation token: $token")
    }
}