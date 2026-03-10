import SwiftUI
import shared

@main
struct VwaTekApplyApp: App {
    
    init() {
        // Initialize Koin for iOS
        KoinHelperKt.doInitKoin()
        
        // Restore saved locale
        if let savedLocale = SettingsHelper.shared.getSetting(key: "locale") {
            LocaleManager.shared.setLocaleByCode(code: savedLocale)
        }
        
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
}
