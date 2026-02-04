import SwiftUI
import shared

struct InterviewView: View {
    @StateObject private var viewModel = InterviewViewModelWrapper()
    @State private var showSetupSheet = false
    @State private var showError = false
    
    var body: some View {
        NavigationStack {
            if viewModel.currentSession != nil {
                ActiveInterviewView(
                    viewModel: viewModel,
                    onEndInterview: {
                        if let sessionId = viewModel.currentSession?.id {
                            viewModel.completeSession(sessionId: sessionId)
                        }
                        viewModel.selectSession(id: nil)
                    }
                )
            } else {
                InterviewSetupView(onStartInterview: {
                    showSetupSheet = true
                })
            }
        }
        .sheet(isPresented: $showSetupSheet) {
            InterviewSetupSheet(
                viewModel: viewModel,
                onStart: { position, company, type in
                    showSetupSheet = false
                    viewModel.startSession(
                        resumeContent: nil,
                        jobTitle: position,
                        jobDescription: "Position at \(company) - \(type) interview"
                    )
                }
            )
        }
        .alert("Error", isPresented: $showError) {
            Button("OK") {
                viewModel.clearError()
            }
        } message: {
            Text(viewModel.error ?? "An unknown error occurred")
        }
        .onChange(of: viewModel.error) { error in
            showError = error != nil
        }
    }
}

struct InterviewSetupView: View {
    var onStartInterview: () -> Void
    
    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Header card
                VStack(spacing: 16) {
                    Image(systemName: "mic.fill")
                        .font(.system(size: 50))
                        .foregroundColor(.blue)
                    
                    Text("AI Mock Interview")
                        .font(.title2)
                        .fontWeight(.bold)
                    
                    Text("Practice interviews with our AI interviewer. Get real-time feedback and improve your interview skills.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                    
                    Button(action: onStartInterview) {
                        Label("Start Mock Interview", systemImage: "play.fill")
                            .fontWeight(.semibold)
                            .frame(maxWidth: 250)
                            .padding()
                            .background(Color.blue)
                            .foregroundColor(.white)
                            .cornerRadius(10)
                    }
                }
                .padding(24)
                .background(Color.blue.opacity(0.1))
                .cornerRadius(16)
                .padding(.horizontal)
                
                // Interview types
                VStack(alignment: .leading, spacing: 12) {
                    Text("Interview Types")
                        .font(.headline)
                        .padding(.horizontal)
                    
                    LazyVGrid(columns: [
                        GridItem(.flexible()),
                        GridItem(.flexible())
                    ], spacing: 12) {
                        InterviewTypeCard(
                            icon: "brain.head.profile",
                            title: "Behavioral",
                            description: "STAR method questions"
                        )
                        
                        InterviewTypeCard(
                            icon: "chevron.left.forwardslash.chevron.right",
                            title: "Technical",
                            description: "Skills assessment"
                        )
                        
                        InterviewTypeCard(
                            icon: "chart.line.uptrend.xyaxis",
                            title: "Case Study",
                            description: "Problem solving"
                        )
                        
                        InterviewTypeCard(
                            icon: "person.3.fill",
                            title: "Leadership",
                            description: "Management questions"
                        )
                    }
                    .padding(.horizontal)
                }
                
                // Tips
                VStack(alignment: .leading, spacing: 12) {
                    Text("Interview Tips")
                        .font(.headline)
                        .padding(.horizontal)
                    
                    InterviewTipCard(
                        number: 1,
                        title: "Research the Company",
                        description: "Know their mission, products, and recent news."
                    )
                    
                    InterviewTipCard(
                        number: 2,
                        title: "Use the STAR Method",
                        description: "Structure answers: Situation, Task, Action, Result."
                    )
                    
                    InterviewTipCard(
                        number: 3,
                        title: "Prepare Questions",
                        description: "Have thoughtful questions ready for the interviewer."
                    )
                }
            }
            .padding(.vertical)
        }
        .navigationTitle("Interview")
    }
}

struct InterviewTypeCard: View {
    let icon: String
    let title: String
    let description: String
    
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 28))
                .foregroundColor(.blue)
            
            Text(title)
                .font(.subheadline)
                .fontWeight(.semibold)
            
            Text(description)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
}

struct InterviewTipCard: View {
    let number: Int
    let title: String
    let description: String
    
    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(Color.orange)
                    .frame(width: 28, height: 28)
                
                Text("\(number)")
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
            }
            
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                Text(description)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
        .padding(.horizontal)
    }
}

struct ActiveInterviewView: View {
    @ObservedObject var viewModel: InterviewViewModelWrapper
    var onEndInterview: () -> Void
    
    @State private var answer = ""
    
    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                VStack(alignment: .leading) {
                    Text("Mock Interview")
                        .font(.headline)
                    
                    Text("Question \(viewModel.currentQuestionIndex + 1) of \(viewModel.totalQuestions)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                Button("End") {
                    onEndInterview()
                }
                .foregroundColor(.red)
            }
            .padding()
            .background(Color.blue.opacity(0.1))
            
            // Progress
            ProgressView(value: Double(viewModel.currentQuestionIndex + 1), total: Double(max(viewModel.totalQuestions, 1)))
                .tint(.blue)
            
            ScrollView {
                VStack(spacing: 20) {
                    // Question card
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Image(systemName: "bubble.left.fill")
                                .foregroundColor(.blue)
                            
                            Text("Interview Question")
                                .font(.caption)
                                .fontWeight(.semibold)
                                .foregroundColor(.blue)
                        }
                        
                        Text(viewModel.currentQuestion?.question ?? "Loading question...")
                            .font(.body)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding()
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(12)
                    
                    // Answer input
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Your Answer")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        
                        TextEditor(text: $answer)
                            .frame(minHeight: 200)
                            .padding(8)
                            .background(Color(.secondarySystemBackground))
                            .cornerRadius(10)
                    }
                    
                    // Action buttons
                    HStack(spacing: 12) {
                        Button(action: { /* Get hint */ }) {
                            Label("Get Hint", systemImage: "lightbulb")
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color(.secondarySystemBackground))
                                .cornerRadius(10)
                        }
                        
                        Button(action: submitAnswer) {
                            HStack {
                                if viewModel.isSubmittingAnswer {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                }
                                
                                Label("Submit", systemImage: "paperplane.fill")
                            }
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(answer.isEmpty ? Color.gray : Color.blue)
                            .foregroundColor(.white)
                            .cornerRadius(10)
                        }
                        .disabled(answer.isEmpty || viewModel.isSubmittingAnswer)
                    }
                    
                    // Show feedback if available
                    if let feedback = viewModel.lastFeedback {
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Image(systemName: "sparkles")
                                    .foregroundColor(.purple)
                                
                                Text("AI Feedback")
                                    .font(.caption)
                                    .fontWeight(.semibold)
                                    .foregroundColor(.purple)
                            }
                            
                            Text(feedback)
                                .font(.body)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding()
                        .background(Color.purple.opacity(0.1))
                        .cornerRadius(12)
                    }
                }
                .padding()
            }
        }
        .navigationBarHidden(true)
    }
    
    private func submitAnswer() {
        guard let question = viewModel.currentQuestion else { return }
        
        viewModel.submitAnswer(
            question: question,
            answer: answer,
            jobTitle: viewModel.currentSession?.jobTitle ?? "Job"
        )
        
        // Move to next question after submission
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            if viewModel.currentQuestionIndex < viewModel.totalQuestions - 1 {
                viewModel.setCurrentQuestion(index: viewModel.currentQuestionIndex + 1)
                answer = ""
            }
        }
    }
}

struct InterviewSetupSheet: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var viewModel: InterviewViewModelWrapper
    @State private var position = ""
    @State private var company = ""
    @State private var selectedType = "behavioral"
    
    var onStart: (String, String, String) -> Void
    
    let interviewTypes = [
        ("behavioral", "Behavioral"),
        ("technical", "Technical"),
        ("case_study", "Case Study")
    ]
    
    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Position", text: $position)
                    TextField("Company", text: $company)
                }
                
                Section("Interview Type") {
                    Picker("Type", selection: $selectedType) {
                        ForEach(interviewTypes, id: \.0) { type in
                            Text(type.1).tag(type.0)
                        }
                    }
                    .pickerStyle(.inline)
                    .labelsHidden()
                }
                
                if viewModel.isStartingSession {
                    Section {
                        HStack {
                            ProgressView()
                            Text("Starting interview session...")
                                .foregroundColor(.secondary)
                        }
                    }
                }
            }
            .navigationTitle("Setup Interview")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
                
                ToolbarItem(placement: .confirmationAction) {
                    Button("Start") {
                        onStart(position, company, selectedType)
                    }
                    .disabled(position.isEmpty || company.isEmpty || viewModel.isStartingSession)
                }
            }
        }
    }
}

#Preview {
    // Note: Preview won't work without Koin initialization
    // InterviewView()
    Text("Interview Preview - Run on device/simulator")
}
