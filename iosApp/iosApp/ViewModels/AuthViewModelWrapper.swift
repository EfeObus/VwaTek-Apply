import Foundation
import shared
import Combine

/// Wrapper class to make Kotlin ViewModel observable in SwiftUI
@MainActor
class AuthViewModelWrapper: ObservableObject {
    private let viewModel: AuthViewModel
    
    @Published var isLoading: Bool = false
    @Published var isAuthenticated: Bool = false
    @Published var error: String? = nil
    @Published var userName: String = ""
    @Published var userLastName: String = ""
    @Published var userEmail: String = ""
    @Published var userPhone: String = ""
    @Published var currentView: AuthViewType = .login
    
    // Store the current user object for updates
    private var currentUser: User? = nil
    
    var fullName: String {
        let first = userName
        let last = userLastName
        if first.isEmpty && last.isEmpty {
            return "User"
        }
        return "\(first) \(last)".trimmingCharacters(in: .whitespaces)
    }
    
    var initials: String {
        let first = userName.first.map(String.init) ?? ""
        let last = userLastName.first.map(String.init) ?? ""
        let result = (first + last).uppercased()
        return result.isEmpty ? "U" : result
    }
    
    private var stateWatcher: Closeable?
    
    init() {
        // Get AuthViewModel from Koin
        self.viewModel = KoinHelperKt.getAuthViewModel()
    }
    
    deinit {
        stateWatcher?.close()
    }
    
    func observeState() {
        // Use FlowExtensionsKt.watch to observe StateFlow
        stateWatcher = FlowExtensionsKt.watch(viewModel.state) { [weak self] (state: Any?) in
            guard let self = self, let authState = state as? AuthViewState else { return }
            Task { @MainActor in
                self.isLoading = authState.isLoading
                self.isAuthenticated = authState.isAuthenticated
                self.error = authState.error
                self.currentUser = authState.user
                self.userName = authState.user?.firstName ?? ""
                self.userLastName = authState.user?.lastName ?? ""
                self.userEmail = authState.user?.email ?? ""
                self.userPhone = authState.user?.phone ?? ""
            }
        }
    }
    
    func login(email: String, password: String, rememberMe: Bool = true) {
        let intent = AuthIntent.Login(email: email, password: password, rememberMe: rememberMe)
        viewModel.onIntent(intent: intent)
    }
    
    func register(email: String, password: String, confirmPassword: String, 
                  firstName: String, lastName: String, phone: String?) {
        let intent = AuthIntent.Register(
            email: email,
            password: password,
            confirmPassword: confirmPassword,
            firstName: firstName,
            lastName: lastName,
            phone: phone,
            street: nil,
            city: nil,
            state: nil,
            zipCode: nil,
            country: nil
        )
        viewModel.onIntent(intent: intent)
    }
    
    func updateProfile(firstName: String, lastName: String, phone: String?) {
        guard let user = currentUser else { return }
        
        // Create updated user with new values
        // Keep the existing updatedAt - the repository will update the timestamp
        let updatedUser = User(
            id: user.id,
            email: user.email,
            firstName: firstName,
            lastName: lastName,
            phone: phone,
            address: user.address,
            profileImageUrl: user.profileImageUrl,
            authProvider: user.authProvider,
            linkedInProfileUrl: user.linkedInProfileUrl,
            emailVerified: user.emailVerified,
            createdAt: user.createdAt,
            updatedAt: user.updatedAt
        )
        
        let intent = AuthIntent.UpdateProfile(user: updatedUser)
        viewModel.onIntent(intent: intent)
    }
    
    func resetPassword(email: String) {
        let intent = AuthIntent.ResetPassword(email: email)
        viewModel.onIntent(intent: intent)
    }
    
    func logout() {
        viewModel.onIntent(intent: AuthIntent.Logout.shared)
    }
    
    func switchView(_ view: AuthViewType) {
        let kotlinView: shared.AuthView
        switch view {
        case .login:
            kotlinView = shared.AuthView.login
        case .register:
            kotlinView = shared.AuthView.register_
        case .forgotPassword:
            kotlinView = shared.AuthView.forgotPassword
        case .profile:
            kotlinView = shared.AuthView.profile
        }
        let intent = AuthIntent.SwitchView(view: kotlinView)
        viewModel.onIntent(intent: intent)
    }
    
    func clearError() {
        viewModel.onIntent(intent: AuthIntent.ClearError.shared)
    }
    
    // MARK: - Social Sign-In
    
    /// Handle Google Sign-In result
    func googleSignIn(email: String, firstName: String, lastName: String, profilePicture: String?) {
        let intent = AuthIntent.GoogleSignIn(
            email: email,
            firstName: firstName,
            lastName: lastName,
            profilePicture: profilePicture
        )
        viewModel.onIntent(intent: intent)
    }
    
    /// Handle Apple Sign-In result
    func appleSignIn(email: String, firstName: String, lastName: String) {
        let intent = AuthIntent.GoogleSignIn( // Reusing GoogleSignIn intent for now
            email: email,
            firstName: firstName,
            lastName: lastName,
            profilePicture: nil
        )
        viewModel.onIntent(intent: intent)
    }
    
    /// Handle LinkedIn Sign-In callback with auth code
    func linkedInSignIn(authCode: String) {
        let intent = AuthIntent.LinkedInCallback(authCode: authCode)
        viewModel.onIntent(intent: intent)
    }
}

/// Swift enum to mirror Kotlin AuthView
enum AuthViewType: String {
    case login
    case register
    case forgotPassword
    case profile
}
