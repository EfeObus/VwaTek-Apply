import SwiftUI
import shared

struct OrganizationView: View {
    @StateObject private var viewModel = OrganizationViewModelWrapper()
    @State private var showCreateSheet = false
    @State private var showInviteSheet = false
    
    var body: some View {
        NavigationView {
            Group {
                if viewModel.isLoading && viewModel.organizations.isEmpty {
                    ProgressView("Loading organizations...")
                } else if let selected = viewModel.selectedOrganization {
                    OrganizationDetailView(
                        organization: selected,
                        members: viewModel.members,
                        templates: viewModel.templates,
                        isLoadingMembers: viewModel.isLoadingMembers,
                        onBack: { viewModel.clearSelectedOrganization() },
                        onInvite: { showInviteSheet = true },
                        onRemoveMember: { memberId in
                            viewModel.removeMember(orgId: selected.id, memberId: memberId)
                        }
                    )
                } else if viewModel.organizations.isEmpty {
                    VStack(spacing: 16) {
                        Image(systemName: "building.2")
                            .font(.system(size: 64))
                            .foregroundColor(.blue.opacity(0.5))
                        Text("No Organizations Yet")
                            .font(.title2.bold())
                        Text("Create an organization to manage your team's resumes and templates")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 32)
                        Button(action: { showCreateSheet = true }) {
                            Label("Create Organization", systemImage: "plus")
                                .padding(.horizontal, 24)
                                .padding(.vertical, 12)
                        }
                        .buttonStyle(.borderedProminent)
                    }
                } else {
                    OrganizationListView(
                        organizations: viewModel.organizations,
                        onSelect: { org in viewModel.selectOrganization(orgId: org.id) },
                        onCreate: { showCreateSheet = true }
                    )
                }
            }
            .navigationTitle(viewModel.selectedOrganization?.name ?? "Organizations")
            .toolbar {
                if viewModel.selectedOrganization == nil {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button(action: { showCreateSheet = true }) {
                            Image(systemName: "plus")
                        }
                    }
                }
            }
            .alert("Error", isPresented: .constant(viewModel.error != nil)) {
                Button("OK") { viewModel.clearError() }
            } message: {
                if let error = viewModel.error {
                    Text(error)
                }
            }
            .sheet(isPresented: $showCreateSheet) {
                CreateOrganizationSheet(
                    isCreating: viewModel.isCreating,
                    onCreate: { name, description, industry in
                        viewModel.createOrganization(name: name, description: description, industry: industry)
                        showCreateSheet = false
                    }
                )
            }
            .sheet(isPresented: $showInviteSheet) {
                if let org = viewModel.selectedOrganization {
                    InviteMemberSheet(
                        isInviting: viewModel.isInviting,
                        onInvite: { email, role in
                            viewModel.inviteMember(orgId: org.id, email: email, role: role)
                            showInviteSheet = false
                        }
                    )
                }
            }
            .onAppear {
                viewModel.loadOrganizations()
            }
        }
    }
}

// MARK: - Organization List
struct OrganizationListView: View {
    let organizations: [OrganizationDto]
    let onSelect: (OrganizationDto) -> Void
    let onCreate: () -> Void
    
    var body: some View {
        List {
            ForEach(organizations, id: \.id) { org in
                Button(action: { onSelect(org) }) {
                    HStack(spacing: 12) {
                        Image(systemName: "building.2.fill")
                            .font(.title2)
                            .foregroundColor(.blue)
                            .frame(width: 44, height: 44)
                            .background(Color.blue.opacity(0.1))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                        
                        VStack(alignment: .leading, spacing: 4) {
                            Text(org.name)
                                .font(.headline)
                                .foregroundColor(.primary)
                            if let desc = org.description_, !desc.isEmpty {
                                Text(desc)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                    .lineLimit(1)
                            }
                            HStack(spacing: 8) {
                                if let industry = org.industry, !industry.isEmpty {
                                    Label(industry, systemImage: "briefcase")
                                        .font(.caption2)
                                        .foregroundColor(.blue)
                                }
                                Label("\(org.memberCount) members", systemImage: "person.2")
                                    .font(.caption2)
                                    .foregroundColor(.secondary)
                            }
                        }
                        
                        Spacer()
                        Image(systemName: "chevron.right")
                            .foregroundColor(.secondary)
                    }
                    .padding(.vertical, 4)
                }
            }
        }
    }
}

// MARK: - Organization Detail
struct OrganizationDetailView: View {
    let organization: OrganizationDto
    let members: [MemberDto]
    let templates: [TemplateDto]
    let isLoadingMembers: Bool
    let onBack: () -> Void
    let onInvite: () -> Void
    let onRemoveMember: (String) -> Void
    
    @State private var selectedTab = 0
    
    var body: some View {
        VStack(spacing: 0) {
            // Back button + invite button
            HStack {
                Button(action: onBack) {
                    HStack(spacing: 4) {
                        Image(systemName: "chevron.left")
                        Text("Back")
                    }
                }
                Spacer()
                Button(action: onInvite) {
                    Label("Invite", systemImage: "person.badge.plus")
                }
                .buttonStyle(.bordered)
            }
            .padding(.horizontal)
            .padding(.bottom, 8)
            
            Picker("Tab", selection: $selectedTab) {
                Text("Members").tag(0)
                Text("Templates").tag(1)
                Text("Info").tag(2)
            }
            .pickerStyle(.segmented)
            .padding(.horizontal)
            
            switch selectedTab {
            case 0:
                MembersTabView(members: members, isLoading: isLoadingMembers, onRemove: onRemoveMember)
            case 1:
                TemplatesTabView(templates: templates)
            case 2:
                OrgInfoTabView(organization: organization)
            default:
                EmptyView()
            }
        }
    }
}

// MARK: - Members Tab
struct MembersTabView: View {
    let members: [MemberDto]
    let isLoading: Bool
    let onRemove: (String) -> Void
    
    var body: some View {
        if isLoading {
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if members.isEmpty {
            VStack(spacing: 12) {
                Image(systemName: "person.2.slash")
                    .font(.largeTitle)
                    .foregroundColor(.secondary)
                Text("No members yet")
                    .foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            List {
                ForEach(members, id: \.id) { member in
                    HStack {
                        Image(systemName: "person.circle.fill")
                            .font(.title2)
                            .foregroundColor(.blue)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(member.email ?? member.name ?? "Unknown")
                                .font(.body)
                            HStack(spacing: 8) {
                                Text(member.role)
                                    .font(.caption)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 2)
                                    .background(roleColor(member.role).opacity(0.15))
                                    .foregroundColor(roleColor(member.role))
                                    .clipShape(Capsule())
                                Text(member.status)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                        Spacer()
                        if member.role != "OWNER" {
                            Button(action: { onRemove(member.id) }) {
                                Image(systemName: "xmark.circle.fill")
                                    .foregroundColor(.red.opacity(0.7))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
        }
    }
    
    private func roleColor(_ role: String) -> Color {
        switch role {
        case "OWNER": return .purple
        case "ADMIN": return .red
        case "MANAGER": return .orange
        default: return .blue
        }
    }
}

// MARK: - Templates Tab
struct TemplatesTabView: View {
    let templates: [TemplateDto]
    
    var body: some View {
        if templates.isEmpty {
            VStack(spacing: 12) {
                Image(systemName: "doc.on.doc")
                    .font(.largeTitle)
                    .foregroundColor(.secondary)
                Text("No shared templates yet")
                    .foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            List {
                ForEach(templates, id: \.id) { template in
                    VStack(alignment: .leading, spacing: 4) {
                        Text(template.name)
                            .font(.headline)
                        HStack(spacing: 8) {
                            Text(template.type)
                                .font(.caption)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(Color.blue.opacity(0.1))
                                .foregroundColor(.blue)
                                .clipShape(Capsule())
                            if let category = template.category, !category.isEmpty {
                                Text(category)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                        if let desc = template.description_, !desc.isEmpty {
                            Text(desc)
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .lineLimit(2)
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
        }
    }
}

// MARK: - Info Tab
struct OrgInfoTabView: View {
    let organization: OrganizationDto
    
    var body: some View {
        List {
            Section("Details") {
                LabeledContent("Name", value: organization.name)
                if let desc = organization.description_, !desc.isEmpty {
                    LabeledContent("Description", value: desc)
                }
                if let industry = organization.industry, !industry.isEmpty {
                    LabeledContent("Industry", value: industry)
                }
                if let size = organization.size, !size.isEmpty {
                    LabeledContent("Size", value: size)
                }
                LabeledContent("Members", value: "\(organization.memberCount)")
                LabeledContent("Subscription", value: organization.subscriptionTier)
                LabeledContent("SSO", value: organization.ssoEnabled ? "Enabled" : "Disabled")
            }
        }
    }
}

// MARK: - Create Sheet
struct CreateOrganizationSheet: View {
    let isCreating: Bool
    let onCreate: (String, String?, String?) -> Void
    
    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var description = ""
    @State private var industry = ""
    
    var body: some View {
        NavigationView {
            Form {
                Section("Organization Details") {
                    TextField("Name *", text: $name)
                    TextField("Description", text: $description)
                    TextField("Industry", text: $industry)
                }
            }
            .navigationTitle("New Organization")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Create") {
                        onCreate(
                            name,
                            description.isEmpty ? nil : description,
                            industry.isEmpty ? nil : industry
                        )
                    }
                    .disabled(name.isEmpty || isCreating)
                }
            }
        }
    }
}

// MARK: - Invite Sheet
struct InviteMemberSheet: View {
    let isInviting: Bool
    let onInvite: (String, String) -> Void
    
    @Environment(\.dismiss) private var dismiss
    @State private var email = ""
    @State private var role = "MEMBER"
    
    private let roles = ["MEMBER", "MANAGER", "ADMIN"]
    
    var body: some View {
        NavigationView {
            Form {
                Section("Invite Details") {
                    TextField("Email *", text: $email)
                        .keyboardType(.emailAddress)
                        .autocapitalization(.none)
                    Picker("Role", selection: $role) {
                        ForEach(roles, id: \.self) { r in
                            Text(r).tag(r)
                        }
                    }
                }
            }
            .navigationTitle("Invite Member")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Invite") {
                        onInvite(email, role)
                    }
                    .disabled(email.isEmpty || isInviting)
                }
            }
        }
    }
}
