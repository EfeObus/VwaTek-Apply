package com.vwatek.apply.presentation.organization

import com.vwatek.apply.data.api.OrganizationApiClient
import com.vwatek.apply.data.api.CreateOrganizationApiRequest
import com.vwatek.apply.data.api.OrganizationDto
import com.vwatek.apply.data.api.MemberDto
import com.vwatek.apply.data.api.InvitationDto
import com.vwatek.apply.data.api.TemplateDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OrganizationViewModel(
    private val apiClient: OrganizationApiClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(OrganizationState())
    val state: StateFlow<OrganizationState> = _state.asStateFlow()

    fun onIntent(intent: OrganizationIntent) {
        when (intent) {
            is OrganizationIntent.LoadOrganizations -> loadOrganizations()
            is OrganizationIntent.SelectOrganization -> selectOrganization(intent.orgId)
            is OrganizationIntent.CreateOrganization -> createOrganization(intent.name, intent.description, intent.industry)
            is OrganizationIntent.LoadMembers -> loadMembers(intent.orgId)
            is OrganizationIntent.InviteMember -> inviteMember(intent.orgId, intent.email, intent.role)
            is OrganizationIntent.RemoveMember -> removeMember(intent.orgId, intent.memberId)
            is OrganizationIntent.AcceptInvitation -> acceptInvitation(intent.token)
            is OrganizationIntent.LoadTemplates -> loadTemplates(intent.orgId)
            is OrganizationIntent.ClearError -> clearError()
            is OrganizationIntent.ClearSelectedOrganization -> clearSelectedOrganization()
        }
    }

    private fun loadOrganizations() {
        scope.launch {
            _state.update { it.copy(isLoading = true) }
            apiClient.getOrganizations()
                .onSuccess { response ->
                    _state.update { it.copy(isLoading = false, organizations = response.organizations) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    private fun selectOrganization(orgId: String) {
        scope.launch {
            _state.update { it.copy(isLoading = true) }
            apiClient.getOrganization(orgId)
                .onSuccess { response ->
                    _state.update { it.copy(isLoading = false, selectedOrganization = response.organization) }
                    // Also load members
                    loadMembers(orgId)
                    loadTemplates(orgId)
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    private fun createOrganization(name: String, description: String?, industry: String?) {
        scope.launch {
            _state.update { it.copy(isCreating = true) }
            apiClient.createOrganization(
                CreateOrganizationApiRequest(name = name, description = description, industry = industry)
            )
                .onSuccess { response ->
                    _state.update { state ->
                        state.copy(
                            isCreating = false,
                            organizations = state.organizations + response.organization,
                            selectedOrganization = response.organization
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isCreating = false, error = e.message) }
                }
        }
    }

    private fun loadMembers(orgId: String) {
        scope.launch {
            _state.update { it.copy(isLoadingMembers = true) }
            apiClient.getMembers(orgId)
                .onSuccess { response ->
                    _state.update { it.copy(isLoadingMembers = false, members = response.members) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoadingMembers = false, error = e.message) }
                }
        }
    }

    private fun inviteMember(orgId: String, email: String, role: String) {
        scope.launch {
            _state.update { it.copy(isInviting = true) }
            apiClient.inviteMember(orgId, email, role)
                .onSuccess { response ->
                    _state.update { state ->
                        state.copy(
                            isInviting = false,
                            invitations = state.invitations + response.invitation
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isInviting = false, error = e.message) }
                }
        }
    }

    private fun removeMember(orgId: String, memberId: String) {
        scope.launch {
            apiClient.removeMember(orgId, memberId)
                .onSuccess {
                    _state.update { state ->
                        state.copy(members = state.members.filter { it.id != memberId })
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                }
        }
    }

    private fun acceptInvitation(token: String) {
        scope.launch {
            _state.update { it.copy(isAcceptingInvitation = true) }
            apiClient.acceptInvitation(token)
                .onSuccess {
                    _state.update { it.copy(isAcceptingInvitation = false) }
                    loadOrganizations()
                }
                .onFailure { e ->
                    _state.update { it.copy(isAcceptingInvitation = false, error = e.message) }
                }
        }
    }

    private fun loadTemplates(orgId: String) {
        scope.launch {
            _state.update { it.copy(isLoadingTemplates = true) }
            apiClient.getTemplates(orgId)
                .onSuccess { response ->
                    _state.update { it.copy(isLoadingTemplates = false, templates = response.templates) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoadingTemplates = false, error = e.message) }
                }
        }
    }

    private fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun clearSelectedOrganization() {
        _state.update { it.copy(selectedOrganization = null, members = emptyList(), templates = emptyList()) }
    }
}

data class OrganizationState(
    val organizations: List<OrganizationDto> = emptyList(),
    val selectedOrganization: OrganizationDto? = null,
    val members: List<MemberDto> = emptyList(),
    val invitations: List<InvitationDto> = emptyList(),
    val templates: List<TemplateDto> = emptyList(),
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val isLoadingMembers: Boolean = false,
    val isInviting: Boolean = false,
    val isAcceptingInvitation: Boolean = false,
    val isLoadingTemplates: Boolean = false,
    val error: String? = null
)

sealed class OrganizationIntent {
    data object LoadOrganizations : OrganizationIntent()
    data class SelectOrganization(val orgId: String) : OrganizationIntent()
    data class CreateOrganization(val name: String, val description: String? = null, val industry: String? = null) : OrganizationIntent()
    data class LoadMembers(val orgId: String) : OrganizationIntent()
    data class InviteMember(val orgId: String, val email: String, val role: String) : OrganizationIntent()
    data class RemoveMember(val orgId: String, val memberId: String) : OrganizationIntent()
    data class AcceptInvitation(val token: String) : OrganizationIntent()
    data class LoadTemplates(val orgId: String) : OrganizationIntent()
    data object ClearError : OrganizationIntent()
    data object ClearSelectedOrganization : OrganizationIntent()
}
