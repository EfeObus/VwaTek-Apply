import SwiftUI

struct ResumeView: View {
    @State private var resumes: [ResumeItem] = []
    @State private var showCreateSheet = false
    
    var body: some View {
        NavigationStack {
            Group {
                if resumes.isEmpty {
                    EmptyResumeView(
                        onCreateNew: { showCreateSheet = true },
                        onUpload: { /* Handle upload */ }
                    )
                } else {
                    ResumeListView(resumes: resumes)
                }
            }
            .navigationTitle("Resume")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button(action: { showCreateSheet = true }) {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $showCreateSheet) {
                CreateResumeSheet(onSave: { name, industry in
                    // Create resume
                    let newResume = ResumeItem(
                        id: UUID().uuidString,
                        name: name,
                        industry: industry,
                        updatedAt: Date()
                    )
                    resumes.append(newResume)
                    showCreateSheet = false
                })
            }
        }
    }
}

struct ResumeItem: Identifiable {
    let id: String
    let name: String
    let industry: String?
    let updatedAt: Date
}

struct EmptyResumeView: View {
    var onCreateNew: () -> Void
    var onUpload: () -> Void
    
    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            
            Image(systemName: "doc.text")
                .font(.system(size: 70))
                .foregroundColor(.secondary.opacity(0.5))
            
            VStack(spacing: 8) {
                Text("No Resumes Yet")
                    .font(.title2)
                    .fontWeight(.semibold)
                
                Text("Create your first professional resume or upload an existing one")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
            }
            
            VStack(spacing: 12) {
                Button(action: onCreateNew) {
                    Label("Create New Resume", systemImage: "plus")
                        .frame(maxWidth: 250)
                        .padding()
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(10)
                }
                
                Button(action: onUpload) {
                    Label("Upload Existing", systemImage: "arrow.up.doc")
                        .frame(maxWidth: 250)
                        .padding()
                        .background(Color(.secondarySystemBackground))
                        .foregroundColor(.primary)
                        .cornerRadius(10)
                }
            }
            
            Spacer()
        }
    }
}

struct ResumeListView: View {
    let resumes: [ResumeItem]
    
    var body: some View {
        List {
            ForEach(resumes) { resume in
                ResumeRow(resume: resume)
            }
        }
        .listStyle(.insetGrouped)
    }
}

struct ResumeRow: View {
    let resume: ResumeItem
    @State private var showActions = false
    
    var body: some View {
        HStack(spacing: 12) {
            // Icon
            ZStack {
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color.blue.opacity(0.1))
                    .frame(width: 44, height: 44)
                
                Image(systemName: "doc.text.fill")
                    .foregroundColor(.blue)
            }
            
            VStack(alignment: .leading, spacing: 4) {
                Text(resume.name)
                    .font(.headline)
                
                HStack {
                    if let industry = resume.industry {
                        Text(industry)
                            .font(.caption)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 2)
                            .background(Color.blue.opacity(0.1))
                            .cornerRadius(4)
                    }
                    
                    Text("Updated \(resume.updatedAt, style: .date)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            
            Spacer()
            
            Menu {
                Button(action: { /* Edit */ }) {
                    Label("Edit", systemImage: "pencil")
                }
                
                Button(action: { /* Optimize */ }) {
                    Label("Optimize with AI", systemImage: "sparkles")
                }
                
                Divider()
                
                Button(role: .destructive, action: { /* Delete */ }) {
                    Label("Delete", systemImage: "trash")
                }
            } label: {
                Image(systemName: "ellipsis.circle")
                    .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}

struct CreateResumeSheet: View {
    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var industry = ""
    
    var onSave: (String, String?) -> Void
    
    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Resume Name", text: $name)
                    TextField("Industry (Optional)", text: $industry)
                } footer: {
                    Text("Give your resume a descriptive name to easily identify it later.")
                }
            }
            .navigationTitle("New Resume")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
                
                ToolbarItem(placement: .confirmationAction) {
                    Button("Create") {
                        onSave(name, industry.isEmpty ? nil : industry)
                    }
                    .disabled(name.isEmpty)
                }
            }
        }
    }
}

#Preview {
    ResumeView()
}
