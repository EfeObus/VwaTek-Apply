package com.vwatek.apply.ui.screens

import androidx.compose.runtime.*
import com.vwatek.apply.data.api.OrganizationDto
import com.vwatek.apply.data.api.MemberDto
import com.vwatek.apply.data.api.TemplateDto
import com.vwatek.apply.presentation.organization.OrganizationIntent
import com.vwatek.apply.presentation.organization.OrganizationState
import com.vwatek.apply.presentation.organization.OrganizationViewModel
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.attributes.*
import org.koin.core.context.GlobalContext

@Composable
fun OrganizationScreen() {
    val viewModel = remember { GlobalContext.get().get<OrganizationViewModel>() }
    val state by viewModel.state.collectAsState()
    
    var showCreateModal by remember { mutableStateOf(false) }
    var showInviteModal by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.onIntent(OrganizationIntent.LoadOrganizations)
    }
    
    Div(attrs = {
        style {
            property("max-width", "1200px")
            property("margin", "0 auto")
            property("padding", "32px 24px")
        }
    }) {
        // Header
        Div(attrs = {
            style {
                display(DisplayStyle.Flex)
                alignItems(AlignItems.Center)
                justifyContent(JustifyContent.SpaceBetween)
                property("margin-bottom", "24px")
            }
        }) {
            Div(attrs = {
                style {
                    display(DisplayStyle.Flex)
                    alignItems(AlignItems.Center)
                    property("gap", "12px")
                }
            }) {
                if (state.selectedOrganization != null) {
                    Button(attrs = {
                        style {
                            property("background", "none")
                            property("border", "1px solid var(--border-color, #e5e7eb)")
                            property("border-radius", "8px")
                            property("padding", "8px 16px")
                            property("cursor", "pointer")
                            property("font-size", "14px")
                            property("color", "var(--text-primary, #1f2937)")
                        }
                        onClick { viewModel.onIntent(OrganizationIntent.ClearSelectedOrganization) }
                    }) {
                        Text("← Back")
                    }
                }
                H2(attrs = {
                    style {
                        property("margin", "0")
                        property("font-size", "1.5rem")
                        property("font-weight", "700")
                    }
                }) {
                    Text(state.selectedOrganization?.name ?: "Organizations")
                }
            }
            
            if (state.selectedOrganization == null) {
                Button(attrs = {
                    style {
                        property("background", "linear-gradient(135deg, #6366f1, #8b5cf6)")
                        property("color", "white")
                        property("border", "none")
                        property("border-radius", "10px")
                        property("padding", "10px 20px")
                        property("cursor", "pointer")
                        property("font-weight", "600")
                        property("font-size", "14px")
                    }
                    onClick { showCreateModal = true }
                }) {
                    Text("+ Create Organization")
                }
            } else {
                Button(attrs = {
                    style {
                        property("background", "linear-gradient(135deg, #6366f1, #8b5cf6)")
                        property("color", "white")
                        property("border", "none")
                        property("border-radius", "10px")
                        property("padding", "10px 20px")
                        property("cursor", "pointer")
                        property("font-weight", "600")
                        property("font-size", "14px")
                    }
                    onClick { showInviteModal = true }
                }) {
                    Text("+ Invite Member")
                }
            }
        }
        
        // Error message
        state.error?.let { error ->
            Div(attrs = {
                style {
                    property("background", "#fef2f2")
                    property("border", "1px solid #fecaca")
                    property("border-radius", "10px")
                    property("padding", "12px 16px")
                    property("margin-bottom", "16px")
                    property("color", "#dc2626")
                    display(DisplayStyle.Flex)
                    justifyContent(JustifyContent.SpaceBetween)
                    alignItems(AlignItems.Center)
                }
            }) {
                Span { Text(error) }
                Button(attrs = {
                    style {
                        property("background", "none")
                        property("border", "none")
                        property("cursor", "pointer")
                        property("color", "#dc2626")
                        property("font-size", "18px")
                    }
                    onClick { viewModel.onIntent(OrganizationIntent.ClearError) }
                }) {
                    Text("✕")
                }
            }
        }
        
        // Content
        if (state.isLoading && state.organizations.isEmpty()) {
            Div(attrs = {
                style {
                    display(DisplayStyle.Flex)
                    justifyContent(JustifyContent.Center)
                    property("padding", "64px")
                }
            }) {
                Span { Text("Loading organizations...") }
            }
        } else if (state.selectedOrganization != null) {
            WebOrganizationDetail(
                state = state,
                onRemoveMember = { memberId ->
                    state.selectedOrganization?.let { org ->
                        viewModel.onIntent(OrganizationIntent.RemoveMember(org.id, memberId))
                    }
                }
            )
        } else if (state.organizations.isEmpty()) {
            // Empty state
            Div(attrs = {
                style {
                    property("text-align", "center")
                    property("padding", "64px 32px")
                }
            }) {
                Div(attrs = {
                    style {
                        property("font-size", "64px")
                        property("margin-bottom", "16px")
                    }
                }) { Text("🏢") }
                H3(attrs = {
                    style { property("margin", "0 0 8px 0") }
                }) { Text("No Organizations Yet") }
                P(attrs = {
                    style {
                        property("color", "var(--text-secondary, #6b7280)")
                        property("margin-bottom", "24px")
                    }
                }) { Text("Create an organization to manage your team's resumes and templates") }
                Button(attrs = {
                    style {
                        property("background", "linear-gradient(135deg, #6366f1, #8b5cf6)")
                        property("color", "white")
                        property("border", "none")
                        property("border-radius", "10px")
                        property("padding", "12px 24px")
                        property("cursor", "pointer")
                        property("font-weight", "600")
                    }
                    onClick { showCreateModal = true }
                }) { Text("+ Create Organization") }
            }
        } else {
            // Organization grid
            Div(attrs = {
                style {
                    display(DisplayStyle.Grid)
                    property("grid-template-columns", "repeat(auto-fill, minmax(340px, 1fr))")
                    property("gap", "16px")
                }
            }) {
                state.organizations.forEach { org ->
                    WebOrganizationCard(
                        organization = org,
                        onClick = { viewModel.onIntent(OrganizationIntent.SelectOrganization(org.id)) }
                    )
                }
            }
        }
    }
    
    // Create modal
    if (showCreateModal) {
        WebCreateOrganizationModal(
            isCreating = state.isCreating,
            onDismiss = { showCreateModal = false },
            onCreate = { name, desc, industry ->
                viewModel.onIntent(OrganizationIntent.CreateOrganization(name, desc, industry))
                showCreateModal = false
            }
        )
    }
    
    // Invite modal
    if (showInviteModal && state.selectedOrganization != null) {
        WebInviteMemberModal(
            isInviting = state.isInviting,
            onDismiss = { showInviteModal = false },
            onInvite = { email, role ->
                state.selectedOrganization?.let { org ->
                    viewModel.onIntent(OrganizationIntent.InviteMember(org.id, email, role))
                }
                showInviteModal = false
            }
        )
    }
}

@Composable
private fun WebOrganizationCard(organization: OrganizationDto, onClick: () -> Unit) {
    Div(attrs = {
        style {
            property("background", "var(--bg-secondary, white)")
            property("border", "1px solid var(--border-color, #e5e7eb)")
            property("border-radius", "12px")
            property("padding", "20px")
            property("cursor", "pointer")
            property("transition", "all 0.2s ease")
        }
        onClick { onClick() }
    }) {
        Div(attrs = {
            style {
                display(DisplayStyle.Flex)
                alignItems(AlignItems.Center)
                property("gap", "12px")
                property("margin-bottom", "12px")
            }
        }) {
            Div(attrs = {
                style {
                    property("width", "44px")
                    property("height", "44px")
                    property("border-radius", "10px")
                    property("background", "linear-gradient(135deg, #6366f1, #8b5cf6)")
                    property("color", "white")
                    display(DisplayStyle.Flex)
                    alignItems(AlignItems.Center)
                    justifyContent(JustifyContent.Center)
                    property("font-size", "20px")
                }
            }) { Text("🏢") }
            Div {
                Div(attrs = {
                    style {
                        property("font-weight", "600")
                        property("font-size", "1rem")
                    }
                }) { Text(organization.name) }
                organization.industry?.let { industry ->
                    Div(attrs = {
                        style {
                            property("font-size", "0.85rem")
                            property("color", "var(--text-secondary, #6b7280)")
                        }
                    }) { Text(industry) }
                }
            }
        }
        organization.description?.let { desc ->
            if (desc.isNotBlank()) {
                P(attrs = {
                    style {
                        property("font-size", "0.9rem")
                        property("color", "var(--text-secondary, #6b7280)")
                        property("margin", "0 0 12px 0")
                    }
                }) { Text(desc) }
            }
        }
        Div(attrs = {
            style {
                display(DisplayStyle.Flex)
                property("gap", "12px")
                property("font-size", "0.85rem")
                property("color", "var(--text-secondary, #6b7280)")
            }
        }) {
            Span { Text("👥 ${organization.memberCount} members") }
            Span { Text("📋 ${organization.subscriptionTier}") }
        }
    }
}

@Composable
private fun WebOrganizationDetail(state: OrganizationState, onRemoveMember: (String) -> Unit) {
    var activeTab by remember { mutableStateOf("members") }
    
    // Tabs
    Div(attrs = {
        style {
            display(DisplayStyle.Flex)
            property("gap", "8px")
            property("margin-bottom", "20px")
            property("border-bottom", "1px solid var(--border-color, #e5e7eb)")
            property("padding-bottom", "0")
        }
    }) {
        listOf("members" to "Members", "templates" to "Templates", "info" to "Info").forEach { (key, label) ->
            Button(attrs = {
                style {
                    property("background", "none")
                    property("border", "none")
                    property("border-bottom", if (activeTab == key) "2px solid #6366f1" else "2px solid transparent")
                    property("padding", "10px 16px")
                    property("cursor", "pointer")
                    property("font-weight", if (activeTab == key) "600" else "400")
                    property("color", if (activeTab == key) "#6366f1" else "var(--text-secondary, #6b7280)")
                }
                onClick { activeTab = key }
            }) { Text(label) }
        }
    }
    
    when (activeTab) {
        "members" -> {
            if (state.isLoadingMembers) {
                Div(attrs = { style { property("text-align", "center"); property("padding", "32px") } }) {
                    Text("Loading members...")
                }
            } else if (state.members.isEmpty()) {
                Div(attrs = { style { property("text-align", "center"); property("padding", "32px"); property("color", "var(--text-secondary, #6b7280)") } }) {
                    Text("No members yet. Invite someone!")
                }
            } else {
                Div(attrs = { style { display(DisplayStyle.Flex); flexDirection(FlexDirection.Column); property("gap", "8px") } }) {
                    state.members.forEach { member ->
                        WebMemberRow(member = member, onRemove = { onRemoveMember(member.id) })
                    }
                }
            }
        }
        "templates" -> {
            if (state.templates.isEmpty()) {
                Div(attrs = { style { property("text-align", "center"); property("padding", "32px"); property("color", "var(--text-secondary, #6b7280)") } }) {
                    Text("No shared templates yet.")
                }
            } else {
                Div(attrs = {
                    style {
                        display(DisplayStyle.Grid)
                        property("grid-template-columns", "repeat(auto-fill, minmax(300px, 1fr))")
                        property("gap", "12px")
                    }
                }) {
                    state.templates.forEach { template ->
                        Div(attrs = {
                            style {
                                property("background", "var(--bg-secondary, white)")
                                property("border", "1px solid var(--border-color, #e5e7eb)")
                                property("border-radius", "10px")
                                property("padding", "16px")
                            }
                        }) {
                            Div(attrs = { style { property("font-weight", "600"); property("margin-bottom", "4px") } }) { Text(template.name) }
                            Div(attrs = {
                                style {
                                    display(DisplayStyle.Flex)
                                    property("gap", "8px")
                                    property("margin-bottom", "4px")
                                }
                            }) {
                                Span(attrs = {
                                    style {
                                        property("font-size", "0.75rem")
                                        property("background", "#eff6ff")
                                        property("color", "#3b82f6")
                                        property("padding", "2px 8px")
                                        property("border-radius", "999px")
                                    }
                                }) { Text(template.type) }
                                template.category?.let { cat ->
                                    Span(attrs = {
                                        style {
                                            property("font-size", "0.75rem")
                                            property("color", "var(--text-secondary, #6b7280)")
                                        }
                                    }) { Text(cat) }
                                }
                            }
                            template.description?.let { desc ->
                                if (desc.isNotBlank()) {
                                    P(attrs = {
                                        style {
                                            property("font-size", "0.85rem")
                                            property("color", "var(--text-secondary, #6b7280)")
                                            property("margin", "4px 0 0 0")
                                        }
                                    }) { Text(desc) }
                                }
                            }
                        }
                    }
                }
            }
        }
        "info" -> {
            val org = state.selectedOrganization
            if (org != null) {
                Div(attrs = {
                    style {
                        property("background", "var(--bg-secondary, white)")
                        property("border", "1px solid var(--border-color, #e5e7eb)")
                        property("border-radius", "12px")
                        property("padding", "24px")
                        property("max-width", "600px")
                    }
                }) {
                    H3(attrs = { style { property("margin", "0 0 16px 0") } }) { Text("Organization Details") }
                    WebInfoRow("Name", org.name)
                    org.description?.let { if (it.isNotBlank()) WebInfoRow("Description", it) }
                    org.industry?.let { if (it.isNotBlank()) WebInfoRow("Industry", it) }
                    org.size?.let { if (it.isNotBlank()) WebInfoRow("Size", it) }
                    WebInfoRow("Members", org.memberCount.toString())
                    WebInfoRow("Subscription", org.subscriptionTier)
                    WebInfoRow("SSO", if (org.ssoEnabled) "Enabled" else "Disabled")
                }
            }
        }
    }
}

@Composable
private fun WebMemberRow(member: MemberDto, onRemove: () -> Unit) {
    Div(attrs = {
        style {
            property("background", "var(--bg-secondary, white)")
            property("border", "1px solid var(--border-color, #e5e7eb)")
            property("border-radius", "10px")
            property("padding", "12px 16px")
            display(DisplayStyle.Flex)
            alignItems(AlignItems.Center)
            property("gap", "12px")
        }
    }) {
        Span(attrs = { style { property("font-size", "24px") } }) { Text("👤") }
        Div(attrs = { style { property("flex", "1") } }) {
            Div(attrs = { style { property("font-weight", "500") } }) {
                Text(member.email ?: member.name ?: "Unknown")
            }
            Div(attrs = {
                style {
                    display(DisplayStyle.Flex)
                    property("gap", "8px")
                    property("font-size", "0.85rem")
                    property("margin-top", "2px")
                }
            }) {
                Span(attrs = {
                    style {
                        property("padding", "2px 8px")
                        property("border-radius", "999px")
                        property("font-size", "0.75rem")
                        property("background", when (member.role) {
                            "OWNER" -> "#f3e8ff"
                            "ADMIN" -> "#fef2f2"
                            "MANAGER" -> "#fff7ed"
                            else -> "#eff6ff"
                        })
                        property("color", when (member.role) {
                            "OWNER" -> "#9333ea"
                            "ADMIN" -> "#dc2626"
                            "MANAGER" -> "#ea580c"
                            else -> "#3b82f6"
                        })
                    }
                }) { Text(member.role) }
                Span(attrs = { style { property("color", "var(--text-secondary, #6b7280)") } }) { Text(member.status) }
            }
        }
        if (member.role != "OWNER") {
            Button(attrs = {
                style {
                    property("background", "none")
                    property("border", "1px solid #fecaca")
                    property("border-radius", "6px")
                    property("padding", "4px 8px")
                    property("cursor", "pointer")
                    property("color", "#dc2626")
                    property("font-size", "12px")
                }
                onClick { onRemove() }
            }) { Text("Remove") }
        }
    }
}

@Composable
private fun WebInfoRow(label: String, value: String) {
    Div(attrs = {
        style {
            display(DisplayStyle.Flex)
            property("padding", "8px 0")
            property("border-bottom", "1px solid var(--border-color, #f3f4f6)")
        }
    }) {
        Span(attrs = {
            style {
                property("width", "140px")
                property("font-weight", "500")
                property("color", "var(--text-secondary, #6b7280)")
                property("font-size", "0.9rem")
            }
        }) { Text(label) }
        Span(attrs = { style { property("font-size", "0.9rem") } }) { Text(value) }
    }
}

// MARK: - Modals

@Composable
private fun WebCreateOrganizationModal(
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String?, industry: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var industry by remember { mutableStateOf("") }
    
    ModalOverlay(onDismiss) {
        H3(attrs = { style { property("margin", "0 0 20px 0") } }) { Text("Create Organization") }
        
        WebFormField("Organization Name *") {
            Input(InputType.Text) {
                value(name)
                onInput { name = it.value }
                style {
                    property("width", "100%")
                    property("padding", "10px 12px")
                    property("border", "1px solid var(--border-color, #e5e7eb)")
                    property("border-radius", "8px")
                    property("font-size", "14px")
                    property("box-sizing", "border-box")
                }
            }
        }
        WebFormField("Description") {
            Input(InputType.Text) {
                value(description)
                onInput { description = it.value }
                style {
                    property("width", "100%")
                    property("padding", "10px 12px")
                    property("border", "1px solid var(--border-color, #e5e7eb)")
                    property("border-radius", "8px")
                    property("font-size", "14px")
                    property("box-sizing", "border-box")
                }
            }
        }
        WebFormField("Industry") {
            Input(InputType.Text) {
                value(industry)
                onInput { industry = it.value }
                style {
                    property("width", "100%")
                    property("padding", "10px 12px")
                    property("border", "1px solid var(--border-color, #e5e7eb)")
                    property("border-radius", "8px")
                    property("font-size", "14px")
                    property("box-sizing", "border-box")
                }
            }
        }
        
        Div(attrs = {
            style {
                display(DisplayStyle.Flex)
                justifyContent(JustifyContent.FlexEnd)
                property("gap", "12px")
                property("margin-top", "20px")
            }
        }) {
            Button(attrs = {
                style {
                    property("background", "none")
                    property("border", "1px solid var(--border-color, #e5e7eb)")
                    property("border-radius", "8px")
                    property("padding", "10px 20px")
                    property("cursor", "pointer")
                }
                onClick { onDismiss() }
            }) { Text("Cancel") }
            Button(attrs = {
                style {
                    property("background", if (name.isNotBlank() && !isCreating) "linear-gradient(135deg, #6366f1, #8b5cf6)" else "#d1d5db")
                    property("color", "white")
                    property("border", "none")
                    property("border-radius", "8px")
                    property("padding", "10px 20px")
                    property("cursor", if (name.isNotBlank() && !isCreating) "pointer" else "not-allowed")
                    property("font-weight", "600")
                }
                onClick {
                    if (name.isNotBlank() && !isCreating) {
                        onCreate(name, description.ifBlank { null }, industry.ifBlank { null })
                    }
                }
            }) { Text(if (isCreating) "Creating..." else "Create") }
        }
    }
}

@Composable
private fun WebInviteMemberModal(
    isInviting: Boolean,
    onDismiss: () -> Unit,
    onInvite: (email: String, role: String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("MEMBER") }
    
    ModalOverlay(onDismiss) {
        H3(attrs = { style { property("margin", "0 0 20px 0") } }) { Text("Invite Member") }
        
        WebFormField("Email *") {
            Input(InputType.Email) {
                value(email)
                onInput { email = it.value }
                style {
                    property("width", "100%")
                    property("padding", "10px 12px")
                    property("border", "1px solid var(--border-color, #e5e7eb)")
                    property("border-radius", "8px")
                    property("font-size", "14px")
                    property("box-sizing", "border-box")
                }
            }
        }
        WebFormField("Role") {
            Div(attrs = {
                style {
                    display(DisplayStyle.Flex)
                    property("gap", "8px")
                }
            }) {
                listOf("MEMBER", "MANAGER", "ADMIN").forEach { r ->
                    Button(attrs = {
                        style {
                            property("background", if (role == r) "#6366f1" else "var(--bg-secondary, white)")
                            property("color", if (role == r) "white" else "var(--text-primary, #1f2937)")
                            property("border", if (role == r) "1px solid #6366f1" else "1px solid var(--border-color, #e5e7eb)")
                            property("border-radius", "8px")
                            property("padding", "8px 16px")
                            property("cursor", "pointer")
                            property("font-size", "13px")
                        }
                        onClick { role = r }
                    }) { Text(r) }
                }
            }
        }
        
        Div(attrs = {
            style {
                display(DisplayStyle.Flex)
                justifyContent(JustifyContent.FlexEnd)
                property("gap", "12px")
                property("margin-top", "20px")
            }
        }) {
            Button(attrs = {
                style {
                    property("background", "none")
                    property("border", "1px solid var(--border-color, #e5e7eb)")
                    property("border-radius", "8px")
                    property("padding", "10px 20px")
                    property("cursor", "pointer")
                }
                onClick { onDismiss() }
            }) { Text("Cancel") }
            Button(attrs = {
                style {
                    property("background", if (email.isNotBlank() && !isInviting) "linear-gradient(135deg, #6366f1, #8b5cf6)" else "#d1d5db")
                    property("color", "white")
                    property("border", "none")
                    property("border-radius", "8px")
                    property("padding", "10px 20px")
                    property("cursor", if (email.isNotBlank() && !isInviting) "pointer" else "not-allowed")
                    property("font-weight", "600")
                }
                onClick {
                    if (email.isNotBlank() && !isInviting) {
                        onInvite(email, role)
                    }
                }
            }) { Text(if (isInviting) "Inviting..." else "Invite") }
        }
    }
}

@Composable
private fun ModalOverlay(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Div(attrs = {
        style {
            position(Position.Fixed)
            property("top", "0")
            property("left", "0")
            property("right", "0")
            property("bottom", "0")
            property("background", "rgba(0,0,0,0.5)")
            display(DisplayStyle.Flex)
            alignItems(AlignItems.Center)
            justifyContent(JustifyContent.Center)
            property("z-index", "1000")
        }
        onClick { onDismiss() }
    }) {
        Div(attrs = {
            style {
                property("background", "var(--bg-primary, white)")
                property("border-radius", "16px")
                property("padding", "24px")
                property("min-width", "400px")
                property("max-width", "500px")
                property("box-shadow", "0 20px 60px rgba(0,0,0,0.3)")
            }
            onClick { it.stopPropagation() }
        }) {
            content()
        }
    }
}

@Composable
private fun WebFormField(label: String, content: @Composable () -> Unit) {
    Div(attrs = {
        style {
            property("margin-bottom", "16px")
        }
    }) {
        Label(attrs = {
            style {
                property("font-weight", "500")
                property("font-size", "0.9rem")
                property("margin-bottom", "6px")
                display(DisplayStyle.Block)
            }
        }) { Text(label) }
        content()
    }
}
