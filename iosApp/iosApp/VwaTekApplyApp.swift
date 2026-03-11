import SwiftUI
import shared

@main
struct VwaTekApplyApp: App {
    
    @State private var isInitialized = false
    @State private var initError: String? = nil
    
    init() {
        print("🟡 [BOOT] VwaTekApplyApp.init() START")
        
        // Initialize Koin for iOS
        print("🟡 [BOOT] Calling doInitKoin...")
        KoinHelperKt.doInitKoin()
        print("🟢 [BOOT] doInitKoin completed")
        
        // Restore saved locale
        if let savedLocale = SettingsHelper.shared.getSetting(key: "locale") {
            LocaleManager.shared.setLocaleByCode(code: savedLocale)
        }
        print("🟢 [BOOT] Locale restored")
        
        // Configure Google Sign-In (if available)
        #if canImport(GoogleSignIn)
        GoogleSignInManager.shared.configure()
        #endif
        
        print("🟢 [BOOT] VwaTekApplyApp.init() DONE")
    }
    
    var body: some Scene {
        WindowGroup {
            if isInitialized {
                ContentView()
                    .onOpenURL { url in
                        #if canImport(GoogleSignIn)
                        _ = GoogleSignInManager.shared.handle(url)
                        #endif
                    }
            } else if let error = initError {
                VStack(spacing: 16) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.system(size: 50))
                        .foregroundColor(.red)
                    Text("Initialization Error")
                        .font(.title2)
                        .fontWeight(.bold)
                    Text(error)
                        .font(.body)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Color(.systemBackground))
            } else {
                VStack(spacing: 16) {
                    Image(systemName: "briefcase.fill")
                        .font(.system(size: 60))
                        .foregroundColor(.blue)
                    Text("VwaTek Apply")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .foregroundColor(.blue)
                    ProgressView("Loading...")
                        .padding(.top, 8)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Color(.systemBackground))
                .task {
                    print("🟡 [BOOT] Splash .task - testing Koin resolution...")
                    // Test Koin resolution in a deferred context
                    do {
                        let _ = KoinHelperKt.getAuthViewModel()
                        print("🟢 [BOOT] Koin resolution succeeded!")
                        await MainActor.run {
                            isInitialized = true
                        }
                    } catch {
                        print("🔴 [BOOT] Koin resolution FAILED: \(error)")
                        await MainActor.run {
                            initError = "Failed to initialize: \(error.localizedDescription)"
                        }
                    }
                }
            }
        }
    }
}
