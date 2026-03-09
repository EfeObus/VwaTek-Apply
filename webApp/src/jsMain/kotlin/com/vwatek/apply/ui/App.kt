package com.vwatek.apply.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.vwatek.apply.presentation.auth.AuthIntent
import com.vwatek.apply.presentation.auth.AuthView
import com.vwatek.apply.presentation.auth.AuthViewModel
import com.vwatek.apply.ui.components.Sidebar
import com.vwatek.apply.ui.screens.DashboardScreen
import com.vwatek.apply.ui.screens.ResumeScreen
import com.vwatek.apply.ui.screens.ResumeOptimizerScreen
import com.vwatek.apply.ui.screens.CoverLetterScreen
import com.vwatek.apply.ui.screens.InterviewScreen
import com.vwatek.apply.ui.screens.NOCScreen
import com.vwatek.apply.ui.screens.JobBankScreen
import com.vwatek.apply.ui.screens.SettingsScreen
import com.vwatek.apply.ui.screens.AuthScreen
import com.vwatek.apply.ui.screens.TrackerScreen
import com.vwatek.apply.ui.screens.SubscriptionScreen
import com.vwatek.apply.ui.screens.PaywallScreen
import com.vwatek.apply.ui.screens.SalaryInsightsScreen
import com.vwatek.apply.ui.screens.LinkedInOptimizerScreen
import com.vwatek.apply.ui.screens.OrganizationScreen
import org.jetbrains.compose.web.dom.*
import org.koin.core.context.GlobalContext
import kotlinx.browser.window

/** Map URL hash fragments to Screen enum values. */
private val hashToScreen = mapOf(
    "dashboard" to Screen.DASHBOARD,
    "resumes" to Screen.RESUMES,
    "optimizer" to Screen.RESUME_OPTIMIZER,
    "coverletters" to Screen.COVER_LETTERS,
    "interview" to Screen.INTERVIEW,
    "tracker" to Screen.TRACKER,
    "noc" to Screen.NOC,
    "jobbank" to Screen.JOB_BANK,
    "salary" to Screen.SALARY_INSIGHTS,
    "linkedin" to Screen.LINKEDIN_OPTIMIZER,
    "organization" to Screen.ORGANIZATION,
    "subscription" to Screen.SUBSCRIPTION,
    "profile" to Screen.PROFILE,
    "settings" to Screen.SETTINGS
)

/** Reverse map: Screen → hash fragment for URL updates. */
private val screenToHash = hashToScreen.entries.associate { (k, v) -> v to k }

enum class Screen {
    DASHBOARD,
    RESUMES,
    RESUME_OPTIMIZER,
    COVER_LETTERS,
    INTERVIEW,
    TRACKER,
    NOC,
    JOB_BANK,
    SALARY_INSIGHTS,
    LINKEDIN_OPTIMIZER,
    ORGANIZATION,
    SUBSCRIPTION,
    PAYWALL,
    PROFILE,
    SETTINGS,
    AUTH
}

@Composable
fun App() {
    val authViewModel = remember { GlobalContext.get().get<AuthViewModel>() }
    val authState by authViewModel.state.collectAsState()
    
    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
    var isSidebarOpen by remember { mutableStateOf(false) }
    
    // Hash-based routing: read initial hash and listen for changes
    LaunchedEffect(Unit) {
        // Parse initial hash on load
        val initialHash = window.location.hash.removePrefix("#/").removePrefix("#").lowercase()
        hashToScreen[initialHash]?.let { currentScreen = it }
        
        // Listen for browser back/forward navigation
        window.onhashchange = { _ ->
            val hash = window.location.hash.removePrefix("#/").removePrefix("#").lowercase()
            hashToScreen[hash]?.let { screen ->
                currentScreen = screen
            }
            Unit
        }
    }
    
    // Update URL hash when screen changes
    LaunchedEffect(currentScreen) {
        val hash = screenToHash[currentScreen]
        if (hash != null) {
            val currentHash = window.location.hash.removePrefix("#/").removePrefix("#").lowercase()
            if (currentHash != hash) {
                window.location.hash = "#/$hash"
            }
        }
    }
    
    // Show auth screen if not authenticated and trying to access protected features
    val showAuthScreen = currentScreen == Screen.AUTH || !authState.isAuthenticated
    
    // Redirect unauthenticated users to auth screen
    LaunchedEffect(authState.isAuthenticated) {
        if (!authState.isAuthenticated && currentScreen != Screen.AUTH) {
            currentScreen = Screen.AUTH
        }
    }
    
    Div(attrs = { classes("app-layout") }) {
        if (!showAuthScreen) {
            Sidebar(
                currentScreen = currentScreen,
                onNavigate = { screen ->
                    currentScreen = screen
                    isSidebarOpen = false
                },
                isOpen = isSidebarOpen,
                onClose = { isSidebarOpen = false },
                isAuthenticated = authState.isAuthenticated,
                userName = authState.user?.let { "${it.firstName} ${it.lastName}" },
                onAuthClick = { 
                    // If authenticated, switch to profile view; otherwise show login
                    if (authState.isAuthenticated) {
                        authViewModel.onIntent(AuthIntent.SwitchView(AuthView.PROFILE))
                    }
                    currentScreen = Screen.AUTH 
                }
            )
        }
        
        Div(attrs = { 
            if (showAuthScreen) classes("main-content", "full-width") 
            else classes("main-content") 
        }) {
            // Mobile header
            if (!showAuthScreen) {
                Div(attrs = { classes("mobile-header") }) {
                    Button(attrs = {
                        classes("menu-toggle")
                        onClick { isSidebarOpen = !isSidebarOpen }
                    }) {
                        Text("Menu")
                    }
                    Span(attrs = { classes("logo-text") }) {
                        Text("VwaTek Apply")
                    }
                }
            }
            
            when (currentScreen) {
                Screen.DASHBOARD -> DashboardScreen(
                    onNavigateToResumes = { currentScreen = Screen.RESUMES },
                    onNavigateToCoverLetters = { currentScreen = Screen.COVER_LETTERS },
                    onNavigateToInterview = { currentScreen = Screen.INTERVIEW }
                )
                Screen.RESUMES -> ResumeScreen()
                Screen.RESUME_OPTIMIZER -> ResumeOptimizerScreen()
                Screen.COVER_LETTERS -> CoverLetterScreen()
                Screen.INTERVIEW -> InterviewScreen()
                Screen.TRACKER -> TrackerScreen()
                Screen.NOC -> NOCScreen()
                Screen.JOB_BANK -> JobBankScreen()
                Screen.SALARY_INSIGHTS -> SalaryInsightsScreen()
                Screen.LINKEDIN_OPTIMIZER -> LinkedInOptimizerScreen()
                Screen.ORGANIZATION -> OrganizationScreen()
                Screen.SUBSCRIPTION -> SubscriptionScreen()
                Screen.PAYWALL -> PaywallScreen(
                    onNavigateBack = { currentScreen = Screen.DASHBOARD },
                    onSubscriptionComplete = { currentScreen = Screen.DASHBOARD }
                )
                Screen.SETTINGS -> SettingsScreen()
                Screen.PROFILE -> {
                    // Navigate to Auth screen with Profile view
                    if (authState.isAuthenticated) {
                        authViewModel.onIntent(AuthIntent.SwitchView(AuthView.PROFILE))
                    }
                    AuthScreen(
                        onNavigateBack = { currentScreen = Screen.DASHBOARD },
                        onLoginSuccess = { currentScreen = Screen.DASHBOARD },
                        onLogoutSuccess = { currentScreen = Screen.DASHBOARD }
                    )
                }
                Screen.AUTH -> AuthScreen(
                    onNavigateBack = { currentScreen = Screen.DASHBOARD },
                    onLoginSuccess = { currentScreen = Screen.DASHBOARD },
                    onLogoutSuccess = { currentScreen = Screen.DASHBOARD }
                )
            }
        }
    }
}
