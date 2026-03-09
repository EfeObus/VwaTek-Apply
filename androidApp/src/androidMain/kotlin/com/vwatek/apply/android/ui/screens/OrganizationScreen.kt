package com.vwatek.apply.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vwatek.apply.data.api.OrganizationDto
import com.vwatek.apply.data.api.MemberDto
import com.vwatek.apply.data.api.TemplateDto
import com.vwatek.apply.presentation.organization.OrganizationIntent
import com.vwatek.apply.presentation.organization.OrganizationViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizationScreen() {
    val viewModel: OrganizationViewModel = koinInject()
    val state by viewModel.state.collectAsState()

    // Dialog state
    var showCreateDialog by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(message = error, duration = SnackbarDuration.Long)
            viewModel.onIntent(OrganizationIntent.ClearError)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onIntent(OrganizationIntent.LoadOrganizations)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (state.selectedOrganization != null) {
                        Text(state.selectedOrganization!!.name)
                    } else {
                        Text("Organizations")
                    }
                },
                navigationIcon = {
                    if (state.selectedOrganization != null) {
                        IconButton(onClick = { viewModel.onIntent(OrganizationIntent.ClearSelectedOrganization) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (state.selectedOrganization == null) {
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Create Organization")
                        }
                    } else {
                        IconButton(onClick = { showInviteDialog = true }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Invite Member")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (state.isLoading && state.organizations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (state.selectedOrganization != null) {
            OrganizationDetailContent(
                organization = state.selectedOrganization!!,
                members = state.members,
                templates = state.templates,
                isLoadingMembers = state.isLoadingMembers,
                modifier = Modifier.padding(paddingValues),
                onRemoveMember = { memberId ->
                    viewModel.onIntent(OrganizationIntent.RemoveMember(state.selectedOrganization!!.id, memberId))
                }
            )
        } else if (state.organizations.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Business,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No Organizations Yet", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Create an organization to manage your team's resumes and templates",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Organization")
                    }
                }
            }
        } else {
            // Organization List
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.organizations) { org ->
                    OrganizationCard(
                        organization = org,
                        onClick = { viewModel.onIntent(OrganizationIntent.SelectOrganization(org.id)) }
                    )
                }
            }
        }
    }

    // Create Organization Dialog
    if (showCreateDialog) {
        CreateOrganizationDialog(
            isCreating = state.isCreating,
            onDismiss = { showCreateDialog = false },
            onCreate = { name, description, industry ->
                viewModel.onIntent(OrganizationIntent.CreateOrganization(name, description, industry))
                showCreateDialog = false
            }
        )
    }

    // Invite Member Dialog
    if (showInviteDialog && state.selectedOrganization != null) {
        InviteMemberDialog(
            isInviting = state.isInviting,
            onDismiss = { showInviteDialog = false },
            onInvite = { email, role ->
                viewModel.onIntent(OrganizationIntent.InviteMember(state.selectedOrganization!!.id, email, role))
                showInviteDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrganizationCard(organization: OrganizationDto, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Business,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(organization.name, style = MaterialTheme.typography.titleMedium)
                val desc = organization.description
                if (!desc.isNullOrBlank()) {
                    Text(
                        desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val industry = organization.industry
                if (!industry.isNullOrBlank()) {
                    AssistChip(
                        onClick = { },
                        label = { Text(industry) },
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun OrganizationDetailContent(
    organization: OrganizationDto,
    members: List<MemberDto>,
    templates: List<TemplateDto>,
    isLoadingMembers: Boolean,
    modifier: Modifier = Modifier,
    onRemoveMember: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Members", "Templates", "Info")

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTab) {
            0 -> MembersTab(members, isLoadingMembers, onRemoveMember)
            1 -> TemplatesTab(templates)
            2 -> OrgInfoTab(organization)
        }
    }
}

@Composable
private fun MembersTab(members: List<MemberDto>, isLoading: Boolean, onRemove: (String) -> Unit) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (members.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No members yet. Invite someone!", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(members) { member ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(member.email ?: member.name ?: "Unknown", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Role: ${member.role} • ${member.status}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (member.role != "OWNER") {
                            IconButton(onClick = { onRemove(member.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFFF44336))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplatesTab(templates: List<TemplateDto>) {
    if (templates.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No shared templates yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(templates) { template ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(template.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Type: ${template.type}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val category = template.category
                        if (!category.isNullOrBlank()) {
                            Text(
                                "Category: $category",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrgInfoTab(organization: OrganizationDto) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Organization Details", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Name: ${organization.name}")
                    organization.description?.let { Text("Description: $it") }
                    organization.industry?.let { Text("Industry: $it") }
                    organization.size?.let { Text("Size: $it") }
                    Text("ID: ${organization.id}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun CreateOrganizationDialog(
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String?, industry: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var industry by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Organization") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Organization Name *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = industry,
                    onValueChange = { industry = it },
                    label = { Text("Industry") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, description.ifBlank { null }, industry.ifBlank { null }) },
                enabled = name.isNotBlank() && !isCreating
            ) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Create")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun InviteMemberDialog(
    isInviting: Boolean,
    onDismiss: () -> Unit,
    onInvite: (email: String, role: String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("MEMBER") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invite Member") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email *") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Role:", style = MaterialTheme.typography.labelLarge)
                listOf("MEMBER", "MANAGER", "ADMIN").forEach { r ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = role == r,
                            onClick = { role = r }
                        )
                        Text(r, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onInvite(email, role) },
                enabled = email.isNotBlank() && !isInviting
            ) {
                if (isInviting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Invite")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
