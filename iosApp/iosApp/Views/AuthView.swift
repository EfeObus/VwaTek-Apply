import SwiftUI

struct AuthView: View {
    @ObservedObject var viewModel: AuthViewModelWrapper
    @State private var currentView: AuthViewType = .login
    
    var body: some View {
        NavigationStack {
            ZStack {
                Color(.systemBackground)
                    .ignoresSafeArea()
                
                switch currentView {
                case .login:
                    LoginView(
                        viewModel: viewModel,
                        onSwitchToRegister: { currentView = .register },
                        onForgotPassword: { currentView = .forgotPassword }
                    )
                case .register:
                    RegisterView(
                        viewModel: viewModel,
                        onSwitchToLogin: { currentView = .login }
                    )
                case .forgotPassword:
                    ForgotPasswordView(
                        viewModel: viewModel,
                        onBackToLogin: { currentView = .login }
                    )
                case .profile:
                    EmptyView()
                }
            }
        }
    }
}

// MARK: - Login View
struct LoginView: View {
    @ObservedObject var viewModel: AuthViewModelWrapper
    @StateObject private var googleSignInManager = GoogleSignInManager.shared
    @StateObject private var appleSignInManager = AppleSignInManager.shared
    @StateObject private var linkedInAuthManager = LinkedInAuthManager.shared
    @State private var email = ""
    @State private var password = ""
    @State private var rememberMe = true
    @State private var showPassword = false
    @State private var isGoogleSigningIn = false
    @State private var isAppleSigningIn = false
    @State private var isLinkedInSigningIn = false
    
    var onSwitchToRegister: () -> Void
    var onForgotPassword: () -> Void
    
    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Logo
                VStack(spacing: 8) {
                    Image(systemName: "briefcase.fill")
                        .font(.system(size: 60))
                        .foregroundColor(.blue)
                    
                    Text("VwaTek Apply")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .foregroundColor(.blue)
                    
                    Text("AI-Powered Job Application Assistant")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .padding(.top, 40)
                
                // Error message
                if let error = viewModel.error {
                    HStack {
                        Image(systemName: "exclamationmark.triangle.fill")
                        Text(error)
                    }
                    .foregroundColor(.white)
                    .padding()
                    .background(Color.red.opacity(0.8))
                    .cornerRadius(10)
                }
                
                // Login form
                VStack(spacing: 16) {
                    // Email field
                    HStack {
                        Image(systemName: "envelope.fill")
                            .foregroundColor(.secondary)
                        TextField("Email", text: $email)
                            .keyboardType(.emailAddress)
                            .autocapitalization(.none)
                            .disableAutocorrection(true)
                    }
                    .padding()
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(10)
                    
                    // Password field
                    HStack {
                        Image(systemName: "lock.fill")
                            .foregroundColor(.secondary)
                        if showPassword {
                            TextField("Password", text: $password)
                        } else {
                            SecureField("Password", text: $password)
                        }
                        Button(action: { showPassword.toggle() }) {
                            Image(systemName: showPassword ? "eye.slash" : "eye")
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding()
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(10)
                    
                    // Remember me & Forgot password
                    HStack {
                        Toggle("Remember me", isOn: $rememberMe)
                            .toggleStyle(CheckboxToggleStyle())
                        
                        Spacer()
                        
                        Button("Forgot Password?") {
                            onForgotPassword()
                        }
                        .font(.footnote)
                    }
                    
                    // Login button
                    Button(action: {
                        viewModel.login(email: email, password: password, rememberMe: rememberMe)
                    }) {
                        HStack {
                            if viewModel.isLoading {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            }
                            Text("Log In")
                                .fontWeight(.semibold)
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(10)
                    }
                    .disabled(viewModel.isLoading || email.isEmpty || password.isEmpty)
                    
                    // Divider
                    HStack {
                        Rectangle()
                            .fill(Color.secondary.opacity(0.3))
                            .frame(height: 1)
                        Text("or continue with")
                            .font(.footnote)
                            .foregroundColor(.secondary)
                        Rectangle()
                            .fill(Color.secondary.opacity(0.3))
                            .frame(height: 1)
                    }
                    
                    // Social login buttons
                    HStack(spacing: 16) {
                        SocialLoginButton(
                            icon: "apple.logo",
                            title: "Apple",
                            isLoading: isAppleSigningIn,
                            action: {
                                Task {
                                    isAppleSigningIn = true
                                    defer { isAppleSigningIn = false }
                                    
                                    let result = await appleSignInManager.signIn()
                                    switch result {
                                    case .success(let signInResult):
                                        // Apple only provides name/email on first sign-in
                                        // For subsequent sign-ins, we might only get the user ID
                                        let email = signInResult.email ?? "\(signInResult.userId)@privaterelay.appleid.com"
                                        viewModel.appleSignIn(
                                            email: email,
                                            firstName: signInResult.givenName ?? "",
                                            lastName: signInResult.familyName ?? ""
                                        )
                                    case .failure(let error):
                                        // Check if user cancelled (code -999)
                                        if (error as NSError).code != -999 {
                                            print("Apple Sign-In failed: \(error.localizedDescription)")
                                        }
                                    }
                                }
                            }
                        )
                        
                        SocialLoginButton(
                            icon: "globe",
                            title: "Google",
                            isLoading: isGoogleSigningIn,
                            action: {
                                Task {
                                    isGoogleSigningIn = true
                                    defer { isGoogleSigningIn = false }
                                    
                                    // Get the root view controller for presenting
                                    guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                                          let rootVC = windowScene.windows.first?.rootViewController else {
                                        return
                                    }
                                    
                                    let result = await googleSignInManager.signIn(presentingViewController: rootVC)
                                    switch result {
                                    case .success(let signInResult):
                                        viewModel.googleSignIn(
                                            email: signInResult.email,
                                            firstName: signInResult.givenName ?? "",
                                            lastName: signInResult.familyName ?? "",
                                            profilePicture: signInResult.profilePictureURL
                                        )
                                    case .failure(let error):
                                        // Error is handled by GoogleSignInManager
                                        print("Google Sign-In failed: \(error.localizedDescription)")
                                    }
                                }
                            }
                        )
                    }
                    
                    // LinkedIn button (separate row for better UI)
                    SocialLoginButton(
                        icon: "link",
                        title: "LinkedIn",
                        isLoading: isLinkedInSigningIn,
                        action: {
                            Task {
                                isLinkedInSigningIn = true
                                defer { isLinkedInSigningIn = false }
                                
                                let result = await linkedInAuthManager.signIn()
                                switch result {
                                case .success(let signInResult):
                                    viewModel.linkedInSignIn(authCode: signInResult.authCode)
                                case .failure(let error):
                                    // Check if user cancelled (code -999)
                                    if (error as NSError).code != -999 {
                                        print("LinkedIn Sign-In failed: \(error.localizedDescription)")
                                    }
                                }
                            }
                        }
                    )
                }
                .padding(.horizontal)
                
                // Switch to register
                HStack {
                    Text("Don't have an account?")
                        .foregroundColor(.secondary)
                    Button("Register") {
                        onSwitchToRegister()
                    }
                    .fontWeight(.semibold)
                }
                .padding(.bottom, 40)
            }
        }
    }
}

// MARK: - Register View
struct RegisterView: View {
    @ObservedObject var viewModel: AuthViewModelWrapper
    @State private var firstName = ""
    @State private var lastName = ""
    @State private var email = ""
    @State private var phone = ""
    @State private var password = ""
    @State private var confirmPassword = ""
    @State private var showPassword = false
    
    var onSwitchToLogin: () -> Void
    
    var passwordsMatch: Bool {
        password == confirmPassword
    }
    
    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Header
                VStack(spacing: 8) {
                    Text("Create Account")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .foregroundColor(.blue)
                    
                    Text("Start your job search journey")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .padding(.top, 40)
                
                // Error message
                if let error = viewModel.error {
                    HStack {
                        Image(systemName: "exclamationmark.triangle.fill")
                        Text(error)
                    }
                    .foregroundColor(.white)
                    .padding()
                    .background(Color.red.opacity(0.8))
                    .cornerRadius(10)
                }
                
                // Registration form
                VStack(spacing: 16) {
                    // Name row
                    HStack(spacing: 12) {
                        HStack {
                            Image(systemName: "person.fill")
                                .foregroundColor(.secondary)
                            TextField("First Name", text: $firstName)
                        }
                        .padding()
                        .background(Color(.secondarySystemBackground))
                        .cornerRadius(10)
                        
                        HStack {
                            TextField("Last Name", text: $lastName)
                        }
                        .padding()
                        .background(Color(.secondarySystemBackground))
                        .cornerRadius(10)
                    }
                    
                    // Email
                    HStack {
                        Image(systemName: "envelope.fill")
                            .foregroundColor(.secondary)
                        TextField("Email", text: $email)
                            .keyboardType(.emailAddress)
                            .autocapitalization(.none)
                    }
                    .padding()
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(10)
                    
                    // Phone
                    HStack {
                        Image(systemName: "phone.fill")
                            .foregroundColor(.secondary)
                        TextField("Phone (Optional)", text: $phone)
                            .keyboardType(.phonePad)
                    }
                    .padding()
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(10)
                    
                    // Password
                    HStack {
                        Image(systemName: "lock.fill")
                            .foregroundColor(.secondary)
                        if showPassword {
                            TextField("Password", text: $password)
                        } else {
                            SecureField("Password", text: $password)
                        }
                        Button(action: { showPassword.toggle() }) {
                            Image(systemName: showPassword ? "eye.slash" : "eye")
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding()
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(10)
                    
                    // Confirm password
                    HStack {
                        Image(systemName: "lock.fill")
                            .foregroundColor(.secondary)
                        if showPassword {
                            TextField("Confirm Password", text: $confirmPassword)
                        } else {
                            SecureField("Confirm Password", text: $confirmPassword)
                        }
                    }
                    .padding()
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(10)
                    .overlay(
                        RoundedRectangle(cornerRadius: 10)
                            .stroke(!confirmPassword.isEmpty && !passwordsMatch ? Color.red : Color.clear, lineWidth: 2)
                    )
                    
                    if !confirmPassword.isEmpty && !passwordsMatch {
                        Text("Passwords do not match")
                            .font(.footnote)
                            .foregroundColor(.red)
                    }
                    
                    // Register button
                    Button(action: {
                        viewModel.register(
                            email: email,
                            password: password,
                            confirmPassword: confirmPassword,
                            firstName: firstName,
                            lastName: lastName,
                            phone: phone.isEmpty ? nil : phone
                        )
                    }) {
                        HStack {
                            if viewModel.isLoading {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            }
                            Text("Create Account")
                                .fontWeight(.semibold)
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(10)
                    }
                    .disabled(viewModel.isLoading || !isFormValid)
                }
                .padding(.horizontal)
                
                // Switch to login
                HStack {
                    Text("Already have an account?")
                        .foregroundColor(.secondary)
                    Button("Log In") {
                        onSwitchToLogin()
                    }
                    .fontWeight(.semibold)
                }
                .padding(.bottom, 40)
            }
        }
    }
    
    private var isFormValid: Bool {
        !firstName.isEmpty && !lastName.isEmpty && !email.isEmpty && 
        !password.isEmpty && passwordsMatch
    }
}

// MARK: - Forgot Password View
struct ForgotPasswordView: View {
    @ObservedObject var viewModel: AuthViewModelWrapper
    @State private var email = ""
    @State private var resetSent = false
    
    var onBackToLogin: () -> Void
    
    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            
            // Icon
            Image(systemName: "lock.rotation")
                .font(.system(size: 60))
                .foregroundColor(.blue)
            
            // Header
            VStack(spacing: 8) {
                Text("Reset Password")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                
                Text("Enter your email to receive a reset link")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            
            if resetSent {
                // Success message
                VStack(spacing: 12) {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 50))
                        .foregroundColor(.green)
                    
                    Text("Reset link sent!")
                        .font(.headline)
                    
                    Text("Check your email for instructions.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .padding()
                .background(Color.green.opacity(0.1))
                .cornerRadius(10)
            } else {
                // Email field
                HStack {
                    Image(systemName: "envelope.fill")
                        .foregroundColor(.secondary)
                    TextField("Email", text: $email)
                        .keyboardType(.emailAddress)
                        .autocapitalization(.none)
                }
                .padding()
                .background(Color(.secondarySystemBackground))
                .cornerRadius(10)
                .padding(.horizontal)
                
                // Send button
                Button(action: {
                    viewModel.resetPassword(email: email)
                    resetSent = true
                }) {
                    HStack {
                        if viewModel.isLoading {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        }
                        Text("Send Reset Link")
                            .fontWeight(.semibold)
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(10)
                }
                .disabled(viewModel.isLoading || email.isEmpty)
                .padding(.horizontal)
            }
            
            // Back to login
            Button(action: { onBackToLogin() }) {
                HStack {
                    Image(systemName: "arrow.left")
                    Text("Back to Login")
                }
            }
            
            Spacer()
        }
    }
}

// MARK: - Helper Views
struct SocialLoginButton: View {
    let icon: String
    let title: String
    var isLoading: Bool = false
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack {
                if isLoading {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle())
                        .scaleEffect(0.8)
                } else {
                    Image(systemName: icon)
                }
                Text(title)
            }
            .frame(maxWidth: .infinity)
            .padding()
            .background(Color(.secondarySystemBackground))
            .cornerRadius(10)
        }
        .foregroundColor(.primary)
        .disabled(isLoading)
    }
}

struct CheckboxToggleStyle: ToggleStyle {
    func makeBody(configuration: Configuration) -> some View {
        HStack {
            Image(systemName: configuration.isOn ? "checkmark.square.fill" : "square")
                .foregroundColor(configuration.isOn ? .blue : .secondary)
                .onTapGesture { configuration.isOn.toggle() }
            configuration.label
                .font(.footnote)
        }
    }
}

#Preview {
    AuthView(viewModel: AuthViewModelWrapper())
}
