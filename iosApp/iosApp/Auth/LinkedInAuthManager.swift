import Foundation
import AuthenticationServices
import CryptoKit

/// Manager for LinkedIn OAuth authentication using ASWebAuthenticationSession
@MainActor
class LinkedInAuthManager: NSObject, ObservableObject {
    
    static let shared = LinkedInAuthManager()
    
    // LinkedIn OAuth configuration
    private let clientId = "86zpbbqqqa32et"
    private let redirectUri = "https://vwatek-backend-21443684777.us-central1.run.app/auth/linkedin/callback"
    private let scopes = ["openid", "profile", "email"]
    
    @Published var isSigningIn = false
    @Published var error: String?
    
    /// Result of LinkedIn Sign-In
    struct SignInResult {
        let authCode: String
    }
    
    private override init() {
        super.init()
    }
    
    /// Perform LinkedIn Sign-In using ASWebAuthenticationSession
    /// - Returns: SignInResult with authorization code
    func signIn() async -> Result<SignInResult, Error> {
        isSigningIn = true
        error = nil
        defer { isSigningIn = false }
        
        // Generate state for security
        let state = generateState()
        
        // Build authorization URL
        guard let authURL = buildAuthURL(state: state) else {
            let error = NSError(
                domain: "LinkedInAuth",
                code: -1,
                userInfo: [NSLocalizedDescriptionKey: "Failed to build authorization URL"]
            )
            self.error = error.localizedDescription
            return .failure(error)
        }
        
        return await withCheckedContinuation { continuation in
            let session = ASWebAuthenticationSession(
                url: authURL,
                callbackURLScheme: "vwatekapply"
            ) { [weak self] callbackURL, error in
                Task { @MainActor in
                    if let error = error {
                        // Check if user cancelled
                        if (error as NSError).code == ASWebAuthenticationSessionError.canceledLogin.rawValue {
                            let cancelError = NSError(
                                domain: "LinkedInAuth",
                                code: -999,
                                userInfo: [NSLocalizedDescriptionKey: "User cancelled sign-in"]
                            )
                            continuation.resume(returning: .failure(cancelError))
                        } else {
                            self?.error = error.localizedDescription
                            continuation.resume(returning: .failure(error))
                        }
                        return
                    }
                    
                    guard let callbackURL = callbackURL,
                          let code = self?.extractAuthCode(from: callbackURL) else {
                        let noCodeError = NSError(
                            domain: "LinkedInAuth",
                            code: -1,
                            userInfo: [NSLocalizedDescriptionKey: "No authorization code received"]
                        )
                        self?.error = noCodeError.localizedDescription
                        continuation.resume(returning: .failure(noCodeError))
                        return
                    }
                    
                    // Verify state if present
                    if let returnedState = self?.extractState(from: callbackURL),
                       returnedState != state {
                        let stateError = NSError(
                            domain: "LinkedInAuth",
                            code: -1,
                            userInfo: [NSLocalizedDescriptionKey: "State mismatch - possible CSRF attack"]
                        )
                        self?.error = stateError.localizedDescription
                        continuation.resume(returning: .failure(stateError))
                        return
                    }
                    
                    let result = SignInResult(authCode: code)
                    continuation.resume(returning: .success(result))
                }
            }
            
            session.presentationContextProvider = self
            session.prefersEphemeralWebBrowserSession = false
            
            if !session.start() {
                let startError = NSError(
                    domain: "LinkedInAuth",
                    code: -1,
                    userInfo: [NSLocalizedDescriptionKey: "Failed to start authentication session"]
                )
                self.error = startError.localizedDescription
                continuation.resume(returning: .failure(startError))
            }
        }
    }
    
    // MARK: - Private helpers
    
    private func buildAuthURL(state: String) -> URL? {
        var components = URLComponents(string: "https://www.linkedin.com/oauth/v2/authorization")
        components?.queryItems = [
            URLQueryItem(name: "response_type", value: "code"),
            URLQueryItem(name: "client_id", value: clientId),
            URLQueryItem(name: "redirect_uri", value: redirectUri),
            URLQueryItem(name: "scope", value: scopes.joined(separator: " ")),
            URLQueryItem(name: "state", value: state)
        ]
        return components?.url
    }
    
    private func extractAuthCode(from url: URL) -> String? {
        let components = URLComponents(url: url, resolvingAgainstBaseURL: false)
        return components?.queryItems?.first(where: { $0.name == "code" })?.value
    }
    
    private func extractState(from url: URL) -> String? {
        let components = URLComponents(url: url, resolvingAgainstBaseURL: false)
        return components?.queryItems?.first(where: { $0.name == "state" })?.value
    }
    
    private func generateState() -> String {
        var bytes = [UInt8](repeating: 0, count: 16)
        _ = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)
        return Data(bytes).base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }
}

// MARK: - ASWebAuthenticationPresentationContextProviding
extension LinkedInAuthManager: ASWebAuthenticationPresentationContextProviding {
    
    nonisolated func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let window = windowScene.windows.first else {
            return UIWindow()
        }
        return window
    }
}
