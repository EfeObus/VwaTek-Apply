package com.vwatek.apply.android.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vwatek.apply.android.ui.screens.*
import com.vwatek.apply.android.ui.theme.VwaTekApplyTheme
import com.vwatek.apply.data.api.SubscriptionApiClient
import com.vwatek.apply.domain.model.BillingPeriod
import com.vwatek.apply.domain.model.SubscriptionTier
import com.vwatek.apply.i18n.LocaleManager
import com.vwatek.apply.i18n.Strings
import com.vwatek.apply.presentation.auth.AuthViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * All possible navigation destinations in the app.
 */
enum class NavigationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Home("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    Resume("resume", "Resumes", Icons.Filled.Description, Icons.Outlined.Description),
    Optimizer("optimizer", "Optimizer", Icons.Filled.AutoFixHigh, Icons.Outlined.AutoFixHigh),
    CoverLetter("coverletter", "Letters", Icons.Filled.Email, Icons.Outlined.Email),
    Interview("interview", "Interview", Icons.Filled.Mic, Icons.Outlined.Mic),
    NOC("noc", "NOC Codes", Icons.Filled.WorkOutline, Icons.Outlined.WorkOutline),
    JobBank("jobbank", "Job Bank", Icons.Filled.Search, Icons.Outlined.Search),
    Tracker("tracker", "Job Tracker", Icons.Filled.Checklist, Icons.Outlined.Checklist),
    @Suppress("DEPRECATION")
    SalaryInsights("salary", "Salary", Icons.Filled.TrendingUp, Icons.Outlined.TrendingUp),
    LinkedInOptimizer("linkedin", "LinkedIn", Icons.Filled.Link, Icons.Outlined.Link),
    Organization("organization", "Organization", Icons.Filled.Business, Icons.Outlined.Business),
    Subscription("subscription", "Premium", Icons.Filled.Star, Icons.Outlined.Star),
    Profile("profile", "Profile", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle),
    Settings("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

/**
 * The 5 primary items shown in the bottom navigation bar.
 */
private val PRIMARY_NAV_ITEMS = listOf(
    NavigationItem.Home,
    NavigationItem.Resume,
    NavigationItem.Optimizer,
    NavigationItem.CoverLetter,
    NavigationItem.Profile
)

/**
 * Drawer menu sections for grouped navigation.
 */
private data class DrawerSection(val title: String, val items: List<NavigationItem>)

private val DRAWER_SECTIONS = listOf(
    DrawerSection("Main", listOf(
        NavigationItem.Home,
        NavigationItem.Resume,
        NavigationItem.Optimizer,
        NavigationItem.CoverLetter,
    )),
    DrawerSection("Career Tools", listOf(
        NavigationItem.Interview,
        NavigationItem.Tracker,
        NavigationItem.JobBank,
        NavigationItem.NOC,
    )),
    DrawerSection("Insights", listOf(
        NavigationItem.SalaryInsights,
        NavigationItem.LinkedInOptimizer,
    )),
    DrawerSection("Account", listOf(
        NavigationItem.Organization,
        NavigationItem.Subscription,
        NavigationItem.Profile,
        NavigationItem.Settings,
    ))
)

/** Get localized label for a NavigationItem. */
private fun NavigationItem.localizedLabel(s: Strings): String = when (this) {
    NavigationItem.Home -> s.navDashboard
    NavigationItem.Resume -> s.navResume
    NavigationItem.Optimizer -> s.navOptimizer
    NavigationItem.CoverLetter -> s.navCoverLetter
    NavigationItem.Interview -> s.navInterview
    NavigationItem.NOC -> s.navNOC
    NavigationItem.JobBank -> s.navJobBank
    NavigationItem.Tracker -> s.navTracker
    NavigationItem.SalaryInsights -> s.navSalaryInsights
    NavigationItem.LinkedInOptimizer -> s.navLinkedInOptimizer
    NavigationItem.Organization -> s.navOrganization
    NavigationItem.Subscription -> s.navSubscription
    NavigationItem.Profile -> s.navProfile
    NavigationItem.Settings -> s.navSettings
}

/** Get localized title for a DrawerSection. */
private fun DrawerSection.localizedTitle(s: Strings): String = when (title) {
    "Career Tools" -> s.navCareerTools
    "Insights" -> s.navInsights
    "Account" -> s.navAccount
    else -> title
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VwaTekApp(
    windowSizeClass: WindowSizeClass,
    deepLinkUri: String? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val authViewModel: AuthViewModel = koinInject()
    val subscriptionApiClient: SubscriptionApiClient = koinInject()
    val authState by authViewModel.state.collectAsState()
    val locale by LocaleManager.currentLocale.collectAsState()
    val s = LocaleManager.strings
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedItem by remember { mutableStateOf(NavigationItem.Home) }
    var checkoutMessage by remember { mutableStateOf<String?>(null) }
    var isCheckingOut by remember { mutableStateOf(false) }

    // Handle deep links
    LaunchedEffect(deepLinkUri) {
        if (deepLinkUri != null) {
            when {
                // Handle checkout success callback
                deepLinkUri.startsWith("vwatekapply://checkout/success") -> {
                    checkoutMessage = "Payment successful! Your subscription is now active."
                    selectedItem = NavigationItem.Subscription
                }
                // Handle checkout cancel callback
                deepLinkUri.startsWith("vwatekapply://checkout/cancel") -> {
                    checkoutMessage = "Payment cancelled."
                    selectedItem = NavigationItem.Subscription
                }
                // Handle other deep links
                else -> {
                    val host = deepLinkUri.removePrefix("vwatekapply://").split("/").firstOrNull()
                    val target = NavigationItem.entries.firstOrNull { it.route == host }
                    if (target != null) selectedItem = target
                }
            }
            onDeepLinkConsumed()
        }
    }
    
    // Show checkout message
    LaunchedEffect(checkoutMessage) {
        checkoutMessage?.let {
            snackbarHostState.showSnackbar(it)
            checkoutMessage = null
        }
    }
    
    // Checkout handler function
    val handleCheckout: (SubscriptionTier, BillingPeriod) -> Unit = { tier, billingPeriod ->
        scope.launch {
            isCheckingOut = true
            val successUrl = "vwatekapply://checkout/success"
            val cancelUrl = "vwatekapply://checkout/cancel"
            
            subscriptionApiClient.createCheckoutSession(
                tier = tier,
                billingPeriod = billingPeriod,
                successUrl = successUrl,
                cancelUrl = cancelUrl
            ).onSuccess { response ->
                isCheckingOut = false
                // Open checkout URL in browser
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(response.checkoutUrl))
                context.startActivity(intent)
            }.onFailure { error ->
                isCheckingOut = false
                checkoutMessage = "Checkout failed: ${error.message}"
            }
        }
    }

    if (!authState.isAuthenticated) {
        AuthScreen(viewModel = authViewModel)
    } else {
        val useNavRail = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium
        if (useNavRail) {
            TabletLayout(
                selectedItem = selectedItem,
                onItemSelected = { selectedItem = it },
                authViewModel = authViewModel,
                authState = authState,
                onStartCheckout = handleCheckout,
                isCheckingOut = isCheckingOut,
                snackbarHostState = snackbarHostState
            )
        } else {
            PhoneLayout(
                selectedItem = selectedItem,
                onItemSelected = { selectedItem = it },
                authViewModel = authViewModel,
                authState = authState,
                onStartCheckout = handleCheckout,
                isCheckingOut = isCheckingOut,
                snackbarHostState = snackbarHostState
            )
        }
    }
}

// ── Phone Layout: Drawer + 5-item Bottom Nav ──────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneLayout(
    selectedItem: NavigationItem,
    onItemSelected: (NavigationItem) -> Unit,
    authViewModel: AuthViewModel,
    authState: com.vwatek.apply.presentation.auth.AuthViewState,
    onStartCheckout: (SubscriptionTier, BillingPeriod) -> Unit = { _, _ -> },
    isCheckingOut: Boolean = false,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                selectedItem = selectedItem,
                onItemSelected = { item ->
                    onItemSelected(item)
                    scope.launch { drawerState.close() }
                },
                authState = authState
            )
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            selectedItem.localizedLabel(LocaleManager.strings),
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    PRIMARY_NAV_ITEMS.forEach { item ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (selectedItem == item) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.localizedLabel(LocaleManager.strings)
                                )
                            },
                            label = {
                                Text(
                                    item.localizedLabel(LocaleManager.strings),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 11.sp
                                )
                            },
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
                    onNavigateToItem = onItemSelected,
                    onStartCheckout = onStartCheckout,
                    isCheckingOut = isCheckingOut
                )
            }
        }
    }
}

// ── Tablet Layout: Permanent sidebar + content ────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabletLayout(
    selectedItem: NavigationItem,
    onItemSelected: (NavigationItem) -> Unit,
    authViewModel: AuthViewModel,
    authState: com.vwatek.apply.presentation.auth.AuthViewState,
    onStartCheckout: (SubscriptionTier, BillingPeriod) -> Unit = { _, _ -> },
    isCheckingOut: Boolean = false,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    PermanentNavigationDrawer(
        drawerContent = {
            PermanentDrawerSheet(
                modifier = Modifier.width(280.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                DrawerHeader(authState)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp)
                ) {
                    DRAWER_SECTIONS.forEach { section ->
                        DrawerSectionContent(
                            section = section,
                            selectedItem = selectedItem,
                            onItemSelected = onItemSelected
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                ScreenContent(
                    selectedItem = selectedItem,
                    authViewModel = authViewModel,
                    authState = authState,
                    onNavigateToItem = onItemSelected,
                    onStartCheckout = onStartCheckout,
                    isCheckingOut = isCheckingOut
                )
            }
        }
    }
}

// ── Drawer Content ────────────────────────────────────────────────────────────

@Composable
private fun AppDrawerContent(
    selectedItem: NavigationItem,
    onItemSelected: (NavigationItem) -> Unit,
    authState: com.vwatek.apply.presentation.auth.AuthViewState
) {
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        DrawerHeader(authState)
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            DRAWER_SECTIONS.forEach { section ->
                DrawerSectionContent(
                    section = section,
                    selectedItem = selectedItem,
                    onItemSelected = onItemSelected
                )
            }
        }
    }
}

@Composable
private fun DrawerHeader(
    authState: com.vwatek.apply.presentation.auth.AuthViewState
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                )
            )
            .padding(24.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            val initials = buildString {
                authState.user?.firstName?.firstOrNull()?.let { append(it.uppercase()) }
                authState.user?.lastName?.firstOrNull()?.let { append(it.uppercase()) }
            }.ifEmpty { "?" }
            Text(
                text = initials,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = buildString {
                authState.user?.firstName?.let { append(it) }
                authState.user?.lastName?.let { append(" $it") }
            }.ifEmpty { "VwaTek User" },
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        authState.user?.email?.let { email ->
            Text(
                text = email,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun DrawerSectionContent(
    section: DrawerSection,
    selectedItem: NavigationItem,
    onItemSelected: (NavigationItem) -> Unit
) {
    Text(
        text = section.localizedTitle(LocaleManager.strings),
        modifier = Modifier.padding(start = 28.dp, top = 16.dp, bottom = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold
    )
    section.items.forEach { item ->
        NavigationDrawerItem(
            icon = {
                Icon(
                    if (selectedItem == item) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.localizedLabel(LocaleManager.strings)
                )
            },
            label = { Text(item.localizedLabel(LocaleManager.strings)) },
            selected = selectedItem == item,
            onClick = { onItemSelected(item) },
            modifier = Modifier.padding(horizontal = 12.dp),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

// ── Screen Dispatcher ─────────────────────────────────────────────────────────

@Composable
private fun ScreenContent(
    selectedItem: NavigationItem,
    authViewModel: AuthViewModel,
    authState: com.vwatek.apply.presentation.auth.AuthViewState,
    onNavigateToItem: (NavigationItem) -> Unit,
    onStartCheckout: (SubscriptionTier, BillingPeriod) -> Unit = { _, _ -> },
    isCheckingOut: Boolean = false
) {
    when (selectedItem) {
        NavigationItem.Home -> HomeScreen(
            onNavigateToOptimizer = { onNavigateToItem(NavigationItem.Optimizer) },
            onNavigateToCoverLetter = { onNavigateToItem(NavigationItem.CoverLetter) },
            onNavigateToInterview = { onNavigateToItem(NavigationItem.Interview) },
            onNavigateToResume = { onNavigateToItem(NavigationItem.Resume) }
        )
        NavigationItem.Resume -> ResumeScreen()
        NavigationItem.Optimizer -> OptimizerScreen()
        NavigationItem.CoverLetter -> CoverLetterScreen()
        NavigationItem.Interview -> InterviewScreen()
        NavigationItem.NOC -> NOCScreen()
        NavigationItem.JobBank -> JobBankScreen(onNavigateBack = { onNavigateToItem(NavigationItem.Home) })
        NavigationItem.Tracker -> TrackerScreen()
        NavigationItem.SalaryInsights -> SalaryInsightsScreen(
            onNavigateBack = { onNavigateToItem(NavigationItem.Home) },
            onShowPaywall = { onNavigateToItem(NavigationItem.Subscription) }
        )
        NavigationItem.LinkedInOptimizer -> LinkedInOptimizerScreen()
        NavigationItem.Organization -> OrganizationScreen()
        NavigationItem.Subscription -> SubscriptionScreen(
            onNavigateBack = { onNavigateToItem(NavigationItem.Home) },
            onStartCheckout = onStartCheckout
        )
        NavigationItem.Profile -> ProfileScreen(authViewModel, authState)
        NavigationItem.Settings -> SettingsScreen(onNavigateBack = { onNavigateToItem(NavigationItem.Home) })
    }
}
