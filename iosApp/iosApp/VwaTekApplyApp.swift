import SwiftUI
import shared
import FirebaseCore
import FirebaseCrashlytics

@main
struct VwaTekApplyApp: App {
    
    init() {
        // Initialize Firebase
        FirebaseApp.configure()
        
        // Configure Crashlytics
        #if DEBUG
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(false)
        #else
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(true)
        #endif
        
        // Set custom keys for crash context
        Crashlytics.crashlytics().setCustomValue(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "unknown", forKey: "app_version")
        Crashlytics.crashlytics().setCustomValue(Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "unknown", forKey: "build_number")
        
        // Initialize Koin for iOS
        KoinHelperKt.doInitKoin()
        
        // Configure Google Sign-In (if available)
        #if canImport(GoogleSignIn)
        GoogleSignInManager.shared.configure()
        #endif
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    // Handle Google Sign-In URL callback (if available)
                    #if canImport(GoogleSignIn)
                    _ = GoogleSignInManager.shared.handle(url)
                    #endif
                }
        }
    }
    
    // MARK: - Crashlytics Helper Functions
    
    /// Log a non-fatal exception to Crashlytics
    static func logException(_ error: Error, message: String? = nil) {
        if let message = message {
            Crashlytics.crashlytics().log(message)
        }
        Crashlytics.crashlytics().record(error: error)
    }
    
    /// Log a custom message to Crashlytics
    static func logMessage(_ message: String) {
        Crashlytics.crashlytics().log(message)
    }
    
    /// Set user identifier for crash reports
    static func setUserId(_ userId: String?) {
        Crashlytics.crashlytics().setUserID(userId ?? "")
    }
}
