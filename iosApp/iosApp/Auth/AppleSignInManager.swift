import Foundation
import AuthenticationServices
import CryptoKit

/// Manager for Sign in with Apple
@MainActor
class AppleSignInManager: NSObject, ObservableObject {
    
    static let shared = AppleSignInManager()
    
    @Published var isSigningIn = false
    @Published var error: String?
    
    private var continuation: CheckedContinuation<Result<SignInResult, Error>, Never>?
    
    /// Result of Apple Sign-In
    struct SignInResult {
        let userId: String
        let email: String?
        let givenName: String?
        let familyName: String?
        let identityToken: String?
        let authorizationCode: String?
    }
    
    private override init() {
        super.init()
    }
    
    /// Perform Apple Sign-In
    /// - Returns: SignInResult with user information
    func signIn() async -> Result<SignInResult, Error> {
        isSigningIn = true
        error = nil
        
        // Generate nonce for security
        let nonce = randomNonceString()
        
        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.fullName, .email]
        request.nonce = sha256(nonce)
        
        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self
        
        return await withCheckedContinuation { continuation in
            self.continuation = continuation
            controller.performRequests()
        }
    }
    
    /// Check if Apple credentials are still valid
    func checkCredentialState(userId: String) async -> ASAuthorizationAppleIDProvider.CredentialState {
        await withCheckedContinuation { continuation in
            ASAuthorizationAppleIDProvider().getCredentialState(forUserID: userId) { state, error in
                continuation.resume(returning: state)
            }
        }
    }
    
    // MARK: - Nonce Generation
    
    private func randomNonceString(length: Int = 32) -> String {
        precondition(length > 0)
        var randomBytes = [UInt8](repeating: 0, count: length)
        let errorCode = SecRandomCopyBytes(kSecRandomDefault, randomBytes.count, &randomBytes)
        if errorCode != errSecSuccess {
            fatalError("Unable to generate nonce. SecRandomCopyBytes failed with OSStatus \(errorCode)")
        }
        
        let charset: [Character] = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
        let nonce = randomBytes.map { byte in
            charset[Int(byte) % charset.count]
        }
        return String(nonce)
    }
    
    private func sha256(_ input: String) -> String {
        let inputData = Data(input.utf8)
        let hashedData = SHA256.hash(data: inputData)
        let hashString = hashedData.compactMap {
            String(format: "%02x", $0)
        }.joined()
        return hashString
    }
}

// MARK: - ASAuthorizationControllerDelegate
extension AppleSignInManager: ASAuthorizationControllerDelegate {
    
    nonisolated func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        Task { @MainActor in
            isSigningIn = false
            
            guard let appleIDCredential = authorization.credential as? ASAuthorizationAppleIDCredential else {
                let error = NSError(
                    domain: "AppleSignIn",
                    code: -1,
                    userInfo: [NSLocalizedDescriptionKey: "Invalid credential type"]
                )
                self.error = error.localizedDescription
                continuation?.resume(returning: .failure(error))
                continuation = nil
                return
            }
            
            let identityToken: String?
            if let tokenData = appleIDCredential.identityToken {
                identityToken = String(data: tokenData, encoding: .utf8)
            } else {
                identityToken = nil
            }
            
            let authCode: String?
            if let codeData = appleIDCredential.authorizationCode {
                authCode = String(data: codeData, encoding: .utf8)
            } else {
                authCode = nil
            }
            
            let result = SignInResult(
                userId: appleIDCredential.user,
                email: appleIDCredential.email,
                givenName: appleIDCredential.fullName?.givenName,
                familyName: appleIDCredential.fullName?.familyName,
                identityToken: identityToken,
                authorizationCode: authCode
            )
            
            continuation?.resume(returning: .success(result))
            continuation = nil
        }
    }
    
    nonisolated func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        Task { @MainActor in
            isSigningIn = false
            
            // Check if user cancelled
            if let authError = error as? ASAuthorizationError {
                switch authError.code {
                case .canceled:
                    // User cancelled, don't treat as error
                    let cancelError = NSError(
                        domain: "AppleSignIn",
                        code: -999,
                        userInfo: [NSLocalizedDescriptionKey: "User cancelled sign-in"]
                    )
                    continuation?.resume(returning: .failure(cancelError))
                default:
                    self.error = error.localizedDescription
                    continuation?.resume(returning: .failure(error))
                }
            } else {
                self.error = error.localizedDescription
                continuation?.resume(returning: .failure(error))
            }
            continuation = nil
        }
    }
}

// MARK: - ASAuthorizationControllerPresentationContextProviding
extension AppleSignInManager: ASAuthorizationControllerPresentationContextProviding {
    
    nonisolated func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        // Get the first window scene's window
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let window = windowScene.windows.first else {
            return UIWindow()
        }
        return window
    }
}
