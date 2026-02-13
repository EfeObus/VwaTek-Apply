package com.vwatek.apply.ui.components

import androidx.compose.runtime.Composable
import com.vwatek.apply.ui.Screen
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.attributes.*

@Composable
fun Sidebar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    isOpen: Boolean,
    onClose: () -> Unit,
    isAuthenticated: Boolean = false,
    userName: String? = null,
    onAuthClick: () -> Unit = {}
) {
    Nav(attrs = {
        classes("sidebar")
        if (isOpen) classes("open")
    }) {
        // Logo
        Div(attrs = { classes("logo") }) {
            Img(
                src = "logo.png",
                alt = "VwaTek Apply Logo",
                attrs = {
                    style {
                        property("height", "40px")
                        property("width", "auto")
                    }
                }
            )
            Span(attrs = { classes("logo-text") }) {
                Text("VwaTek Apply")
            }
        }
        
        // User Section
        Div(attrs = { 
            classes("user-section")
            style {
                property("padding", "var(--spacing-md)")
                property("margin", "var(--spacing-md)")
                property("border-radius", "var(--border-radius-md)")
                property("background", "linear-gradient(135deg, var(--bg-secondary) 0%, rgba(99, 102, 241, 0.1) 100%)")
                property("border", "1px solid rgba(99, 102, 241, 0.2)")
            }
        }) {
            if (isAuthenticated && userName != null) {
                // Profile header row
                Div(attrs = { 
                    classes("flex", "items-center", "gap-sm")
                    style {
                        property("margin-bottom", "12px")
                    }
                }) {
                    // Avatar with gradient
                    Div(attrs = {
                        style {
                            property("width", "48px")
                            property("height", "48px")
                            property("border-radius", "50%")
                            property("background", "linear-gradient(135deg, var(--primary-color) 0%, #8b5cf6 100%)")
                            property("color", "white")
                            property("display", "flex")
                            property("align-items", "center")
                            property("justify-content", "center")
                            property("font-weight", "700")
                            property("font-size", "1.2rem")
                            property("box-shadow", "0 4px 12px rgba(99, 102, 241, 0.3)")
                        }
                    }) {
                        Text(userName.first().toString().uppercase())
                    }
                    Div(attrs = {
                        style {
                            property("flex", "1")
                        }
                    }) {
                        P(attrs = { 
                            style { 
                                property("font-weight", "600")
                                property("font-size", "1rem")
                                property("margin", "0")
                                property("color", "var(--text-primary)")
                            } 
                        }) { 
                            Text(userName) 
                        }
                        P(attrs = { 
                            style { 
                                property("font-size", "0.75rem")
                                property("margin", "2px 0 0 0")
                                property("color", "var(--text-secondary)")
                            }
                        }) { 
                            Text("Premium Member")
                        }
                    }
                }
                // View Profile Button
                Button(attrs = {
                    classes("view-profile-btn")
                    onClick { onAuthClick() }
                    style {
                        property("width", "100%")
                        property("padding", "10px 16px")
                        property("border", "none")
                        property("border-radius", "8px")
                        property("background", "var(--primary-color)")
                        property("color", "white")
                        property("font-weight", "500")
                        property("font-size", "0.875rem")
                        property("cursor", "pointer")
                        property("display", "flex")
                        property("align-items", "center")
                        property("justify-content", "center")
                        property("gap", "8px")
                        property("transition", "all 0.2s ease")
                    }
                }) {
                    RawHtml("""<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>""")
                    Text("View Profile")
                }
            } else {
                Div(attrs = { 
                    classes("flex", "items-center", "gap-sm")
                    onClick { onAuthClick() }
                    style {
                        property("cursor", "pointer")
                        property("padding", "8px 0")
                    }
                }) {
                    Div(attrs = {
                        style {
                            property("width", "48px")
                            property("height", "48px")
                            property("border-radius", "50%")
                            property("background", "var(--bg-tertiary)")
                            property("display", "flex")
                            property("align-items", "center")
                            property("justify-content", "center")
                        }
                    }) {
                        RawHtml(Icons.USER)
                    }
                    Div(attrs = {
                        style { property("flex", "1") }
                    }) {
                        P(attrs = { 
                            style { 
                                property("font-weight", "600")
                                property("font-size", "1rem")
                                property("margin", "0")
                            } 
                        }) { 
                            Text("Welcome!") 
                        }
                        P(attrs = { 
                            style { 
                                property("font-size", "0.75rem")
                                property("margin", "2px 0 0 0")
                                property("color", "var(--text-secondary)")
                            }
                        }) { 
                            Text("Sign in to continue") 
                        }
                    }
                }
                Button(attrs = {
                    onClick { onAuthClick() }
                    style {
                        property("width", "100%")
                        property("margin-top", "12px")
                        property("padding", "10px 16px")
                        property("border", "2px solid var(--primary-color)")
                        property("border-radius", "8px")
                        property("background", "transparent")
                        property("color", "var(--primary-color)")
                        property("font-weight", "500")
                        property("font-size", "0.875rem")
                        property("cursor", "pointer")
                        property("transition", "all 0.2s ease")
                    }
                }) {
                    Text("Sign In / Register")
                }
            }
        }
        
        // Navigation Menu
        Ul(attrs = { classes("nav-menu") }) {
            NavItem(
                label = "Dashboard",
                icon = Icons.DASHBOARD,
                isActive = currentScreen == Screen.DASHBOARD,
                onClick = { onNavigate(Screen.DASHBOARD) }
            )
            NavItem(
                label = "Resumes",
                icon = Icons.DOCUMENT,
                isActive = currentScreen == Screen.RESUMES,
                onClick = { onNavigate(Screen.RESUMES) }
            )
            NavItem(
                label = "Optimizer",
                icon = Icons.OPTIMIZE,
                isActive = currentScreen == Screen.RESUME_OPTIMIZER,
                onClick = { onNavigate(Screen.RESUME_OPTIMIZER) }
            )
            NavItem(
                label = "Cover Letters",
                icon = Icons.LETTER,
                isActive = currentScreen == Screen.COVER_LETTERS,
                onClick = { onNavigate(Screen.COVER_LETTERS) }
            )
            NavItem(
                label = "Interview Prep",
                icon = Icons.MICROPHONE,
                isActive = currentScreen == Screen.INTERVIEW,
                onClick = { onNavigate(Screen.INTERVIEW) }
            )
            NavItem(
                label = "Job Tracker",
                icon = Icons.BRIEFCASE,
                isActive = currentScreen == Screen.TRACKER,
                onClick = { onNavigate(Screen.TRACKER) }
            )
            NavItem(
                label = "NOC Codes",
                icon = Icons.CHART,
                isActive = currentScreen == Screen.NOC,
                onClick = { onNavigate(Screen.NOC) }
            )
            NavItem(
                label = "Job Bank",
                icon = Icons.SEARCH,
                isActive = currentScreen == Screen.JOB_BANK,
                onClick = { onNavigate(Screen.JOB_BANK) }
            )
            NavItem(
                label = "Salary Insights",
                icon = Icons.DOLLAR,
                isActive = currentScreen == Screen.SALARY_INSIGHTS,
                onClick = { onNavigate(Screen.SALARY_INSIGHTS) }
            )
            NavItem(
                label = "Subscription",
                icon = Icons.STAR,
                isActive = currentScreen == Screen.SUBSCRIPTION,
                onClick = { onNavigate(Screen.SUBSCRIPTION) }
            )
        }
        
        // Spacer
        Div(attrs = { style { property("flex", "1") } })
        
        // Settings at bottom
        Ul(attrs = { classes("nav-menu") }) {
            NavItem(
                label = "Settings",
                icon = Icons.SETTINGS,
                isActive = currentScreen == Screen.SETTINGS,
                onClick = { onNavigate(Screen.SETTINGS) }
            )
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Li(attrs = {
        classes("nav-item")
        if (isActive) classes("active")
        onClick { onClick() }
    }) {
        Span(attrs = {
            style {
                property("display", "flex")
                property("align-items", "center")
            }
        }) {
            RawHtml(icon)
        }
        Span { Text(label) }
    }
}

@Composable
private fun RawHtml(html: String) {
    Span(attrs = {
        ref { element ->
            element.innerHTML = html
            onDispose { }
        }
    })
}

private object Icons {
    const val DASHBOARD = """
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="7" height="9" rx="1"/>
            <rect x="14" y="3" width="7" height="5" rx="1"/>
            <rect x="14" y="12" width="7" height="9" rx="1"/>
            <rect x="3" y="16" width="7" height="5" rx="1"/>
        </svg>
    """
    
    const val DOCUMENT = """
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
            <line x1="16" y1="13" x2="8" y2="13"/>
            <line x1="16" y1="17" x2="8" y2="17"/>
            <polyline points="10 9 9 9 8 9"/>
        </svg>
    """
    
    const val LETTER = """
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
            <polyline points="22,6 12,13 2,6"/>
        </svg>
    """
    
    const val MICROPHONE = """
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
            <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
            <line x1="12" y1="19" x2="12" y2="23"/>
            <line x1="8" y1="23" x2="16" y2="23"/>
        </svg>
    """
    
    const val SETTINGS = """
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="3"/>
            <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
        </svg>
    """
    
    const val USER = """
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
            <circle cx="12" cy="7" r="4"/>
        </svg>
    """
    
    const val BRIEFCASE = """
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="2" y="7" width="20" height="14" rx="2" ry="2"/>
            <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/>
        </svg>
    """
    
    const val OPTIMIZE = """
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2L2 7l10 5 10-5-10-5z"/>
            <path d="M2 17l10 5 10-5"/>
            <path d="M2 12l10 5 10-5"/>
        </svg>
    """
    
    const val SEARCH = """
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/>
            <line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
    """
    
    const val DOLLAR = """
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="1" x2="12" y2="23"/>
            <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
        </svg>
    """
    
    const val STAR = """
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
        </svg>
    """
    
    const val CHART = """
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="20" x2="12" y2="10"/>
            <line x1="18" y1="20" x2="18" y2="4"/>
            <line x1="6" y1="20" x2="6" y2="16"/>
        </svg>
    """
}
