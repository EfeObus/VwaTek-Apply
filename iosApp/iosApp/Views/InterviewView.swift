import SwiftUI

struct InterviewView: View {
    @State private var isInterviewActive = false
    @State private var showSetupSheet = false
    
    var body: some View {
        NavigationStack {
            if isInterviewActive {
                ActiveInterviewView(onEndInterview: {
                    isInterviewActive = false
                })
            } else {
                InterviewSetupView(onStartInterview: {
                    showSetupSheet = true
                })
            }
        }
        .sheet(isPresented: $showSetupSheet) {
            InterviewSetupSheet(onStart: { position, company, type in
                showSetupSheet = false
                isInterviewActive = true
            })
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
    var onEndInterview: () -> Void
    
    @State private var currentQuestion = "Tell me about yourself and why you're interested in this position."
    @State private var answer = ""
    @State private var questionNumber = 1
    @State private var totalQuestions = 5
    @State private var isSubmitting = false
    
    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                VStack(alignment: .leading) {
                    Text("Mock Interview")
                        .font(.headline)
                    
                    Text("Question \(questionNumber) of \(totalQuestions)")
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
            ProgressView(value: Double(questionNumber), total: Double(totalQuestions))
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
                        
                        Text(currentQuestion)
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
                                if isSubmitting {
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
                        .disabled(answer.isEmpty || isSubmitting)
                    }
                }
                .padding()
            }
        }
        .navigationBarHidden(true)
    }
    
    private func submitAnswer() {
        isSubmitting = true
        
        // Simulate submission
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            isSubmitting = false
            if questionNumber < totalQuestions {
                questionNumber += 1
                currentQuestion = "Can you describe a challenging project you worked on and how you handled it?"
                answer = ""
            } else {
                onEndInterview()
            }
        }
    }
}

struct InterviewSetupSheet: View {
    @Environment(\.dismiss) private var dismiss
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
                    .disabled(position.isEmpty || company.isEmpty)
                }
            }
        }
    }
}

#Preview {
    InterviewView()
}
