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
    @Published var userEmail: String = ""
    @Published var currentView: AuthViewType = .login
    
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
                self.userName = authState.user?.firstName ?? ""
                self.userEmail = authState.user?.email ?? ""
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
}

/// Swift enum to mirror Kotlin AuthView
enum AuthViewType: String {
    case login
    case register
    case forgotPassword
    case profile
}
