import SwiftUI
import shared

struct VersionHistorySheet: View {
    let resume: Resume
    @ObservedObject var viewModel: ResumeViewModelWrapper
    let onDismiss: () -> Void
    
    @State private var showRestoreConfirmation = false
    @State private var versionToRestore: ResumeVersion?
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Header
                VStack(spacing: 8) {
                    Image(systemName: "clock.arrow.circlepath")
                        .font(.system(size: 40))
                        .foregroundColor(.blue)
                    
                    Text(resume.name)
                        .font(.headline)
                        .foregroundColor(.secondary)
                }
                .padding(.top, 16)
                .padding(.bottom, 12)
                
                Divider()
                
                // Content
                if viewModel.isLoadingVersions {
                    Spacer()
                    ProgressView("Loading versions...")
                    Spacer()
                } else if viewModel.versionHistory.isEmpty {
                    Spacer()
                    EmptyVersionHistoryView()
                    Spacer()
                } else {
                    List(viewModel.versionHistory, id: \.id) { version in
                        VersionHistoryRow(
                            version: version,
                            isRestoring: viewModel.isRestoringVersion,
                            onRestore: {
                                versionToRestore = version
                                showRestoreConfirmation = true
                            }
                        )
                    }
                    .listStyle(.insetGrouped)
                }
            }
            .navigationTitle("Version History")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") {
                        onDismiss()
                    }
                    .disabled(viewModel.isRestoringVersion)
                }
            }
            .confirmationDialog(
                "Restore Version \(versionToRestore?.versionNumber ?? 0)?",
                isPresented: $showRestoreConfirmation,
                titleVisibility: .visible
            ) {
                Button("Restore", role: .destructive) {
                    if let version = versionToRestore {
                        viewModel.restoreVersion(resumeId: resume.id, versionId: version.id)
                    }
                }
                Button("Cancel", role: .cancel) {
                    versionToRestore = nil
                }
            } message: {
                Text("This will restore your resume to this version. A new version will be created with the current content before restoring.")
            }
            .onChange(of: viewModel.versionRestoreSuccess) { success in
                if success {
                    onDismiss()
                }
            }
        }
        .onAppear {
            viewModel.loadVersionHistory(resumeId: resume.id)
        }
        .onDisappear {
            viewModel.clearVersionHistory()
        }
    }
}

struct EmptyVersionHistoryView: View {
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "clock.arrow.circlepath")
                .font(.system(size: 50))
                .foregroundColor(.secondary.opacity(0.5))
            
            VStack(spacing: 4) {
                Text("No Version History")
                    .font(.headline)
                
                Text("Versions are created automatically when you edit your resume")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }
        }
    }
}

struct VersionHistoryRow: View {
    let version: ResumeVersion
    let isRestoring: Bool
    let onRestore: () -> Void
    
    var body: some View {
        HStack(spacing: 12) {
            // Version info
            VStack(alignment: .leading, spacing: 4) {
                Text("Version \(version.versionNumber)")
                    .font(.headline)
                
                Text(version.changeDescription)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .lineLimit(2)
                
                Text(version.createdAtFormatted)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            // Restore button
            Button(action: onRestore) {
                if isRestoring {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle())
                } else {
                    Text("Restore")
                        .font(.subheadline)
                        .fontWeight(.medium)
                }
            }
            .buttonStyle(.bordered)
            .disabled(isRestoring)
        }
        .padding(.vertical, 4)
    }
}

#Preview {
    VersionHistorySheet(
        resume: Resume(
            id: "1",
            userId: "user1",
            name: "Software Engineer Resume",
            content: "Sample content",
            industry: "Technology",
            sourceType: .manual,
            fileName: nil,
            fileType: nil,
            originalFileData: nil,
            createdAt: Kotlinx_datetimeInstant.companion.fromEpochMilliseconds(epochMilliseconds: Int64(Date().timeIntervalSince1970 * 1000)),
            updatedAt: Kotlinx_datetimeInstant.companion.fromEpochMilliseconds(epochMilliseconds: Int64(Date().timeIntervalSince1970 * 1000)),
            currentVersionId: nil
        ),
        viewModel: ResumeViewModelWrapper(),
        onDismiss: {}
    )
}
