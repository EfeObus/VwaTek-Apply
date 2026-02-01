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
            onClick { onAuthClick() }
            style {
                property("cursor", "pointer")
                property("padding", "var(--spacing-md)")
                property("margin", "var(--spacing-md)")
                property("border-radius", "var(--border-radius-md)")
                property("background", "var(--bg-secondary)")
            }
        }) {
            if (isAuthenticated && userName != null) {
                Div(attrs = { 
                    classes("flex", "items-center", "gap-sm")
                }) {
                    Div(attrs = {
                        style {
                            property("width", "36px")
                            property("height", "36px")
                            property("border-radius", "50%")
                            property("background", "var(--primary-color)")
                            property("color", "white")
                            property("display", "flex")
                            property("align-items", "center")
                            property("justify-content", "center")
                            property("font-weight", "600")
                        }
                    }) {
                        Text(userName.first().toString().uppercase())
                    }
                    Div {
                        P(attrs = { 
                            style { 
                                property("font-weight", "500")
                                property("font-size", "0.9rem")
                            } 
                        }) { 
                            Text(userName) 
                        }
                        P(attrs = { 
                            classes("text-secondary")
                            style { property("font-size", "0.75rem") }
                        }) { 
                            Text("View Profile") 
                        }
                    }
                }
            } else {
                Div(attrs = { classes("flex", "items-center", "gap-sm") }) {
                    RawHtml(Icons.USER)
                    Div {
                        P(attrs = { 
                            style { 
                                property("font-weight", "500")
                                property("font-size", "0.9rem")
                            } 
                        }) { 
                            Text("Sign In") 
                        }
                        P(attrs = { 
                            classes("text-secondary")
                            style { property("font-size", "0.75rem") }
                        }) { 
                            Text("Login or Register") 
                        }
                    }
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
}
