import SwiftUI
import shared
import GoogleSignIn

@main
struct VwaTekApplyApp: App {
    
    init() {
        // Initialize Koin for iOS
        KoinHelperKt.doInitKoin()
        
        // Configure Google Sign-In
        GoogleSignInManager.shared.configure()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    // Handle Google Sign-In URL callback
                    _ = GoogleSignInManager.shared.handle(url)
                }
        }
    }
}
