import SwiftUI
import shared

struct PDFExportSheet: View {
    let resume: Resume
    let onDismiss: () -> Void
    
    @State private var selectedTemplate: PDFGenerator.Template = .professional
    @State private var isGenerating = false
    @State private var pdfURL: URL?
    @State private var showShareSheet = false
    @State private var showError = false
    @State private var errorMessage = ""
    
    private let pdfGenerator = PDFGenerator()
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                // Header
                VStack(spacing: 8) {
                    Image(systemName: "doc.richtext")
                        .font(.system(size: 48))
                        .foregroundColor(.blue)
                    
                    Text("Export to PDF")
                        .font(.title2)
                        .fontWeight(.semibold)
                    
                    Text("Choose a template for \"\(resume.name)\"")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }
                .padding(.top, 16)
                
                // Template Options
                ScrollView {
                    VStack(spacing: 12) {
                        ForEach(PDFGenerator.Template.allCases) { template in
                            TemplateOptionRow(
                                template: template,
                                isSelected: selectedTemplate == template,
                                onSelect: { selectedTemplate = template }
                            )
                        }
                    }
                    .padding(.horizontal)
                }
                
                Spacer()
                
                // Export Button
                Button(action: exportPDF) {
                    HStack {
                        if isGenerating {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        } else {
                            Image(systemName: "square.and.arrow.up")
                        }
                        Text(isGenerating ? "Generating..." : "Export & Share")
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(isGenerating ? Color.gray : Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(12)
                }
                .disabled(isGenerating)
                .padding(.horizontal)
                .padding(.bottom)
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        onDismiss()
                    }
                }
            }
        }
        .sheet(isPresented: $showShareSheet) {
            if let url = pdfURL {
                ShareSheet(activityItems: [url])
            }
        }
        .alert("Export Failed", isPresented: $showError) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(errorMessage)
        }
    }
    
    private func exportPDF() {
        isGenerating = true
        
        DispatchQueue.global(qos: .userInitiated).async {
            let url = pdfGenerator.generatePDF(from: resume, template: selectedTemplate)
            
            DispatchQueue.main.async {
                isGenerating = false
                
                if let url = url {
                    pdfURL = url
                    showShareSheet = true
                } else {
                    errorMessage = "Failed to generate PDF. Please try again."
                    showError = true
                }
            }
        }
    }
}

struct TemplateOptionRow: View {
    let template: PDFGenerator.Template
    let isSelected: Bool
    let onSelect: () -> Void
    
    var body: some View {
        Button(action: onSelect) {
            HStack(spacing: 12) {
                // Color indicator
                RoundedRectangle(cornerRadius: 4)
                    .fill(Color(template.accentColor))
                    .frame(width: 32, height: 32)
                
                VStack(alignment: .leading, spacing: 2) {
                    Text(template.rawValue)
                        .font(.headline)
                        .foregroundColor(isSelected ? Color(template.accentColor) : .primary)
                    
                    Text(template.description)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(2)
                }
                
                Spacer()
                
                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(Color(template.accentColor))
                }
            }
            .padding(12)
            .background(
                RoundedRectangle(cornerRadius: 10)
                    .stroke(
                        isSelected ? Color(template.accentColor) : Color.gray.opacity(0.3),
                        lineWidth: isSelected ? 2 : 1
                    )
                    .background(
                        isSelected 
                            ? Color(template.accentColor).opacity(0.08)
                            : Color.clear
                    )
                    .cornerRadius(10)
            )
        }
        .buttonStyle(PlainButtonStyle())
    }
}

struct ShareSheet: UIViewControllerRepresentable {
    let activityItems: [Any]
    var applicationActivities: [UIActivity]? = nil
    
    func makeUIViewController(context: Context) -> UIActivityViewController {
        let controller = UIActivityViewController(
            activityItems: activityItems,
            applicationActivities: applicationActivities
        )
        return controller
    }
    
    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

#Preview {
    PDFExportSheet(
        resume: Resume(
            id: "1",
            userId: "user1",
            name: "Software Engineer Resume",
            content: "Experience\nSoftware Engineer at Tech Corp\n\nEducation\nBS Computer Science",
            industry: "Technology",
            version: 1,
            createdAt: Kotlinx_datetimeInstant.companion.fromEpochMilliseconds(epochMilliseconds: Int64(Date().timeIntervalSince1970 * 1000)),
            updatedAt: Kotlinx_datetimeInstant.companion.fromEpochMilliseconds(epochMilliseconds: Int64(Date().timeIntervalSince1970 * 1000))
        ),
        onDismiss: {}
    )
}
