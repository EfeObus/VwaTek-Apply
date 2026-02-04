package com.vwatek.apply.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.vwatek.apply.android.ui.screens.*
import com.vwatek.apply.android.ui.theme.VwaTekApplyTheme
import com.vwatek.apply.presentation.auth.AuthViewModel
import org.koin.compose.koinInject

enum class NavigationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Home("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    Resume("resume", "Resumes", Icons.Filled.Menu, Icons.Outlined.Menu),
    Interview("interview", "Interview", Icons.Filled.Person, Icons.Outlined.Person),
    Profile("profile", "Profile", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)
}

// Internal navigation destinations
sealed class AppDestination {
    data object Main : AppDestination()
    data object Optimizer : AppDestination()
    data object CoverLetter : AppDestination()
    data object ResumeUpload : AppDestination()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VwaTekApp(windowSizeClass: WindowSizeClass) {
    val authViewModel: AuthViewModel = koinInject()
    val authState by authViewModel.state.collectAsState()
    
    var selectedItem by remember { mutableStateOf(NavigationItem.Home) }
    var currentDestination by remember { mutableStateOf<AppDestination>(AppDestination.Main) }
    
    // Check if user is authenticated
    if (!authState.isAuthenticated) {
        AuthScreen(viewModel = authViewModel)
    } else {
        // Handle internal navigation
        when (currentDestination) {
            is AppDestination.Optimizer -> {
                OptimizerScreen(
                    onNavigateBack = { currentDestination = AppDestination.Main }
                )
            }
            is AppDestination.CoverLetter -> {
                CoverLetterScreen(
                    onNavigateBack = { currentDestination = AppDestination.Main }
                )
            }
            is AppDestination.ResumeUpload -> {
                // Navigate back to main and switch to Resume tab
                currentDestination = AppDestination.Main
                selectedItem = NavigationItem.Resume
            }
            is AppDestination.Main -> {
                // Use NavigationRail for tablets and wider screens
                val useNavRail = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium
                
                if (useNavRail) {
                    TabletLayout(
                        selectedItem = selectedItem,
                        onItemSelected = { selectedItem = it },
                        authViewModel = authViewModel,
                        authState = authState,
                        onNavigateToOptimizer = { currentDestination = AppDestination.Optimizer },
                        onNavigateToCoverLetter = { currentDestination = AppDestination.CoverLetter },
                        onNavigateToInterview = { selectedItem = NavigationItem.Interview },
                        onNavigateToResume = { selectedItem = NavigationItem.Resume }
                    )
                } else {
                    PhoneLayout(
                        selectedItem = selectedItem,
                        onItemSelected = { selectedItem = it },
                        authViewModel = authViewModel,
                        authState = authState,
                        onNavigateToOptimizer = { currentDestination = AppDestination.Optimizer },
                        onNavigateToCoverLetter = { currentDestination = AppDestination.CoverLetter },
                        onNavigateToInterview = { selectedItem = NavigationItem.Interview },
                        onNavigateToResume = { selectedItem = NavigationItem.Resume }
                    )
                }
            }
        }
    }
}

@Composable
private fun PhoneLayout(
    selectedItem: NavigationItem,
    onItemSelected: (NavigationItem) -> Unit,
    authViewModel: AuthViewModel,
    authState: com.vwatek.apply.presentation.auth.AuthViewState,
    onNavigateToOptimizer: () -> Unit,
    onNavigateToCoverLetter: () -> Unit,
    onNavigateToInterview: () -> Unit,
    onNavigateToResume: () -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationItem.entries.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (selectedItem == item) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) },
                        selected = selectedItem == item,
                        onClick = { onItemSelected(item) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            ScreenContent(
                selectedItem = selectedItem,
                authViewModel = authViewModel,
                authState = authState,
                onNavigateToOptimizer = onNavigateToOptimizer,
                onNavigateToCoverLetter = onNavigateToCoverLetter,
                onNavigateToInterview = onNavigateToInterview,
                onNavigateToResume = onNavigateToResume
            )
        }
    }
}

@Composable
private fun TabletLayout(
    selectedItem: NavigationItem,
    onItemSelected: (NavigationItem) -> Unit,
    authViewModel: AuthViewModel,
    authState: com.vwatek.apply.presentation.auth.AuthViewState,
    onNavigateToOptimizer: () -> Unit,
    onNavigateToCoverLetter: () -> Unit,
    onNavigateToInterview: () -> Unit,
    onNavigateToResume: () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        NavigationRail {
            Spacer(modifier = Modifier.weight(1f))
            NavigationItem.entries.forEach { item ->
                NavigationRailItem(
                    icon = {
                        Icon(
                            if (selectedItem == item) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label
                        )
                    },
                    label = { Text(item.label) },
                    selected = selectedItem == item,
                    onClick = { onItemSelected(item) }
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }
        
        Box(modifier = Modifier.weight(1f)) {
            ScreenContent(
                selectedItem = selectedItem,
                authViewModel = authViewModel,
                authState = authState,
                onNavigateToOptimizer = onNavigateToOptimizer,
                onNavigateToCoverLetter = onNavigateToCoverLetter,
                onNavigateToInterview = onNavigateToInterview,
                onNavigateToResume = onNavigateToResume
            )
        }
    }
}

@Composable
private fun ScreenContent(
    selectedItem: NavigationItem,
    authViewModel: AuthViewModel,
    authState: com.vwatek.apply.presentation.auth.AuthViewState,
    onNavigateToOptimizer: () -> Unit,
    onNavigateToCoverLetter: () -> Unit,
    onNavigateToInterview: () -> Unit,
    onNavigateToResume: () -> Unit
) {
    when (selectedItem) {
        NavigationItem.Home -> HomeScreen(
            onNavigateToOptimizer = onNavigateToOptimizer,
            onNavigateToCoverLetter = onNavigateToCoverLetter,
            onNavigateToInterview = onNavigateToInterview,
            onNavigateToResume = onNavigateToResume
        )
        NavigationItem.Resume -> ResumeScreen()
        NavigationItem.Interview -> InterviewScreen()
        NavigationItem.Profile -> ProfileScreen(authViewModel, authState)
    }
}
