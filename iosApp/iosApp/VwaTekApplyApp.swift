import SwiftUI
import shared

@main
struct VwaTekApplyApp: App {
    
    init() {
        // Initialize Koin for iOS
        KoinHelperKt.doInitKoin()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
