import Foundation
import UIKit

#if canImport(GoogleSignIn)
import GoogleSignIn
#endif

/// Manager for Google Sign-In using Google Identity Services SDK
@MainActor
class GoogleSignInManager: NSObject, ObservableObject {
    
    static let shared = GoogleSignInManager()
    
    // Google OAuth configuration - this should match the Web Client ID from Google Cloud Console
    private let clientID = "21443684777-vp58jf5fq1k7lvqk8m5eo5v5dv8m1h9t.apps.googleusercontent.com"
    
    @Published var isSigningIn = false
    @Published var error: String?
    
    /// Result of Google Sign-In
    struct SignInResult {
        let email: String
        let givenName: String?
        let familyName: String?
        let profilePictureURL: String?
        let idToken: String?
    }
    
    private override init() {
        super.init()
    }
    
    /// Configure Google Sign-In (call from App init)
    func configure() {
        // Configuration is set in Info.plist via GIDClientID key
    }
    
    /// Handle URL callback from Google Sign-In
    func handle(_ url: URL) -> Bool {
        #if canImport(GoogleSignIn)
        return GIDSignIn.sharedInstance.handle(url)
        #else
        return false
        #endif
    }
    
    /// Perform Google Sign-In
    /// - Parameter presentingViewController: The view controller to present sign-in UI from
    /// - Returns: SignInResult with user information
    func signIn(presentingViewController: Any) async -> Result<SignInResult, Error> {
        isSigningIn = true
        error = nil
        defer { isSigningIn = false }
        
        #if canImport(GoogleSignIn)
        guard let viewController = presentingViewController as? UIViewController else {
            let error = NSError(
                domain: "GoogleSignIn",
                code: -1,
                userInfo: [NSLocalizedDescriptionKey: "Invalid presenting view controller"]
            )
            self.error = error.localizedDescription
            return .failure(error)
        }
        
        return await withCheckedContinuation { continuation in
            GIDSignIn.sharedInstance.signIn(withPresenting: viewController) { [weak self] signInResult, error in
                if let error = error {
                    self?.error = error.localizedDescription
                    continuation.resume(returning: .failure(error))
                    return
                }
                
                guard let user = signInResult?.user,
                      let profile = user.profile else {
                    let noDataError = NSError(
                        domain: "GoogleSignIn",
                        code: -1,
                        userInfo: [NSLocalizedDescriptionKey: "No user data returned from Google"]
                    )
                    self?.error = noDataError.localizedDescription
                    continuation.resume(returning: .failure(noDataError))
                    return
                }
                
                let result = SignInResult(
                    email: profile.email,
                    givenName: profile.givenName,
                    familyName: profile.familyName,
                    profilePictureURL: profile.imageURL(withDimension: 200)?.absoluteString,
                    idToken: user.idToken?.tokenString
                )
                
                continuation.resume(returning: .success(result))
            }
        }
        #else
        let notAvailableError = NSError(
            domain: "GoogleSignIn",
            code: -1,
            userInfo: [NSLocalizedDescriptionKey: "Google Sign-In SDK not available"]
        )
        self.error = notAvailableError.localizedDescription
        return .failure(notAvailableError)
        #endif
    }
    
    /// Sign out from Google
    func signOut() {
        #if canImport(GoogleSignIn)
        GIDSignIn.sharedInstance.signOut()
        #endif
    }
    
    /// Check if user is already signed in
    func restorePreviousSignIn() async -> SignInResult? {
        #if canImport(GoogleSignIn)
        return await withCheckedContinuation { continuation in
            GIDSignIn.sharedInstance.restorePreviousSignIn { user, error in
                guard let user = user, let profile = user.profile else {
                    continuation.resume(returning: nil)
                    return
                }
                
                let result = SignInResult(
                    email: profile.email,
                    givenName: profile.givenName,
                    familyName: profile.familyName,
                    profilePictureURL: profile.imageURL(withDimension: 200)?.absoluteString,
                    idToken: user.idToken?.tokenString
                )
                continuation.resume(returning: result)
            }
        }
        #else
        return nil
        #endif
    }
}
