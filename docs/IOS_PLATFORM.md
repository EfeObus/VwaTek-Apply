# iOS Platform Guide

This guide covers iOS-specific implementation details for VwaTek Apply using Kotlin Multiplatform and SwiftUI.

## Overview

The iOS version of VwaTek Apply uses **Compose Multiplatform for iOS** with a **SwiftUI** wrapper, delivering a native iOS experience while sharing business logic with Android and Web.

## Technical Stack

| Component | Technology |
|-----------|------------|
| UI Framework | Compose Multiplatform (with SwiftUI integration) |
| Language | Kotlin (shared), Swift (iOS-specific) |
| Build Tool | Gradle + Xcode |
| Secure Storage | iOS Keychain Services |
| Networking | Ktor (Darwin engine) |
| Crash Reporting | Firebase Crashlytics (Phase 1) |
| Analytics | Firebase Analytics (Phase 1) |

## Project Structure

```
iosApp/
|-- iosApp/
|   |-- iOSApp.swift              # App entry point
|   |-- ContentView.swift         # SwiftUI wrapper for Compose
|   |-- Info.plist                # App configuration
|   +-- Assets.xcassets/          # iOS assets and icons
|-- iosApp.xcodeproj/             # Xcode project
+-- Podfile                       # CocoaPods dependencies (if used)

shared/src/iosMain/
|-- kotlin/
|   +-- com/vwatek/apply/
|       |-- platform/
|       |   |-- Platform.ios.kt   # iOS platform implementation
|       |   |-- Database.ios.kt   # iOS database driver
|       |   +-- Security.ios.kt   # Keychain implementation
|       +-- di/
|           +-- Module.ios.kt     # iOS-specific DI
```

## Getting Started

### Prerequisites

- macOS 13+ (Ventura or later)
- Xcode 16+
- JDK 17+
- CocoaPods (optional, for additional dependencies)

### Development

1. **Open the project in Xcode:**
   ```bash
   open iosApp/iosApp.xcodeproj
   ```
   
   Or use Android Studio/Fleet with the Kotlin Multiplatform plugin.

2. **Select target device:**
   Choose a simulator or connected iPhone from the device menu.

3. **Build and run:**
   Press Cmd+R or click the Run button.

### Building from Command Line

```bash
# Build the shared framework
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# Build release framework
./gradlew :shared:linkReleaseFrameworkIosArm64
```

## Platform Implementation

### App Entry Point

```swift
// iOSApp.swift
import SwiftUI
import shared
import FirebaseCore
import FirebaseCrashlytics

@main
struct VwaTekApp: App {
    init() {
        // Initialize Firebase (Phase 1)
        FirebaseApp.configure()
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(true)
        
        // Initialize Koin
        KoinHelperKt.doInitKoin()
        
        // Start network monitoring (Phase 1)
        NetworkMonitorHelper.shared.startMonitoring()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

### Compose Integration

```swift
// ContentView.swift
import SwiftUI
import shared

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.keyboard)
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }
    
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
```

### Keychain Storage

```kotlin
// Security.ios.kt
import platform.Security.*
import platform.Foundation.*
import kotlinx.cinterop.*

actual class SecureStorage {
    
    actual fun store(key: String, value: String) {
        val query = mapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrAccount to key.toNSString(),
            kSecValueData to value.encodeToByteArray().toNSData(),
            kSecAttrAccessible to kSecAttrAccessibleWhenUnlocked
        ).toCFDictionary()
        
        // Delete existing item
        SecItemDelete(query)
        
        // Add new item
        val status = SecItemAdd(query, null)
        if (status != errSecSuccess) {
            throw SecurityException("Failed to store item: $status")
        }
    }
    
    actual fun retrieve(key: String): String? {
        val query = mapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrAccount to key.toNSString(),
            kSecReturnData to true,
            kSecMatchLimit to kSecMatchLimitOne
        ).toCFDictionary()
        
        memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)
            
            if (status == errSecSuccess) {
                val data = result.value as? NSData
                return data?.toByteArray()?.decodeToString()
            }
            return null
        }
    }
    
    actual fun delete(key: String) {
        val query = mapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrAccount to key.toNSString()
        ).toCFDictionary()
        
        SecItemDelete(query)
    }
}
```

### Database Driver

```kotlin
// Database.ios.kt
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = VwaTekDatabase.Schema,
            name = "vwatek.db",
            onConfiguration = { config: DatabaseConfiguration ->
                config.copy(
                    extendedConfig = DatabaseConfiguration.Extended(
                        foreignKeyConstraints = true
                    )
                )
            }
        )
    }
}
```

## Phase 1 Components (February 2026)

### Network Monitor

```kotlin
// NetworkMonitor.ios.kt
import platform.Network.*
import platform.darwin.dispatch_get_main_queue

actual class NetworkMonitorFactory actual constructor() {
    actual fun create(): NetworkMonitor = IOSNetworkMonitor()
}

class IOSNetworkMonitor : NetworkMonitor {
    private val monitor = nw_path_monitor_create()
    
    private val _networkState = MutableStateFlow(NetworkState(
        status = NetworkStatus.UNKNOWN,
        type = NetworkType.UNKNOWN
    ))
    override val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()
    
    override fun startMonitoring() {
        nw_path_monitor_set_update_handler(monitor) { path ->
            val status = when (nw_path_get_status(path)) {
                nw_path_status_satisfied -> NetworkStatus.AVAILABLE
                nw_path_status_unsatisfied -> NetworkStatus.UNAVAILABLE
                else -> NetworkStatus.UNKNOWN
            }
            _networkState.value = NetworkState(status, detectNetworkType(path))
        }
        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
        nw_path_monitor_start(monitor)
    }
    
    override fun stopMonitoring() {
        nw_path_monitor_cancel(monitor)
    }
}
```

### Sync Engine

```kotlin
// SyncEngine.ios.kt
actual class SyncEngineFactory actual constructor() {
    actual fun create(
        syncApiClient: SyncApiClient,
        networkMonitor: NetworkMonitor,
        coroutineScope: CoroutineScope
    ): SyncEngine = IOSSyncEngine(syncApiClient, networkMonitor, coroutineScope)
}
```

### Consent Manager

```kotlin
// ConsentManager.ios.kt
actual class ConsentManagerFactory actual constructor() {
    actual fun create(
        privacyApiClient: PrivacyApiClient,
        coroutineScope: CoroutineScope
    ): ConsentManager = IOSConsentManager(privacyApiClient, coroutineScope)
}

class IOSConsentManager(
    private val privacyApiClient: PrivacyApiClient,
    private val scope: CoroutineScope
) : ConsentManager {
    // Uses iOS Keychain for consent storage
    private val storage = SecureStorage()
}
```

### HTTP Client

```kotlin
// HttpClient.ios.kt
import io.ktor.client.*
import io.ktor.client.engine.darwin.*

actual fun createHttpClient(): HttpClient = HttpClient(Darwin) {
    engine {
        configureRequest {
            setAllowsCellularAccess(true)
            setTimeoutInterval(60.0)
        }
    }
    
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
        })
    }
}
```

## iOS-Specific Features

### Share Sheet Integration

```kotlin
// Share.ios.kt
import platform.UIKit.*

actual class ShareService {
    actual fun sharePdf(data: ByteArray, filename: String) {
        val url = writeTempFile(data, filename)
        
        val activityController = UIActivityViewController(
            activityItems = listOf(url),
            applicationActivities = null
        )
        
        UIApplication.sharedApplication.keyWindow?.rootViewController
            ?.presentViewController(activityController, animated = true, completion = null)
    }
    
    private fun writeTempFile(data: ByteArray, filename: String): NSURL {
        val tempDir = NSTemporaryDirectory()
        val filePath = "$tempDir/$filename"
        data.toNSData().writeToFile(filePath, atomically = true)
        return NSURL.fileURLWithPath(filePath)
    }
}
```

### Haptic Feedback

```kotlin
// Haptics.ios.kt
import platform.UIKit.*

actual class HapticFeedback {
    private val impactGenerator = UIImpactFeedbackGenerator(
        style = UIImpactFeedbackStyleMedium
    )
    
    actual fun light() {
        UIImpactFeedbackGenerator(style = UIImpactFeedbackStyleLight)
            .impactOccurred()
    }
    
    actual fun medium() {
        impactGenerator.impactOccurred()
    }
    
    actual fun success() {
        UINotificationFeedbackGenerator()
            .notificationOccurred(UINotificationFeedbackTypeSuccess)
    }
    
    actual fun error() {
        UINotificationFeedbackGenerator()
            .notificationOccurred(UINotificationFeedbackTypeError)
    }
}
```

## App Store Submission

### Requirements

1. **App Icon:** 1024x1024 PNG without alpha channel
2. **Screenshots:** Required sizes for each device type
3. **Privacy Policy:** Required for apps collecting user data
4. **App Review Guidelines:** Ensure compliance

### Build for Release

```bash
# Archive the app
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Release \
  -archivePath build/VwaTekApply.xcarchive \
  archive

# Export for App Store
xcodebuild -exportArchive \
  -archivePath build/VwaTekApply.xcarchive \
  -exportPath build/AppStore \
  -exportOptionsPlist ExportOptions.plist
```

### Info.plist Configuration

```xml
<key>NSAppTransportSecurity</key>
<dict>
    <key>NSAllowsArbitraryLoads</key>
    <false/>
</dict>

<key>UILaunchStoryboardName</key>
<string>LaunchScreen</string>

<key>UIRequiredDeviceCapabilities</key>
<array>
    <string>arm64</string>
</array>

<key>UISupportedInterfaceOrientations</key>
<array>
    <string>UIInterfaceOrientationPortrait</string>
    <string>UIInterfaceOrientationLandscapeLeft</string>
    <string>UIInterfaceOrientationLandscapeRight</string>
</array>
```

## Testing

### Unit Tests

```bash
# Run iOS tests
./gradlew :shared:iosSimulatorArm64Test
```

### UI Tests

Use Xcode's XCTest framework for UI testing:

```swift
// VwaTekApplyUITests.swift
import XCTest

class VwaTekApplyUITests: XCTestCase {
    let app = XCUIApplication()
    
    override func setUpWithError() throws {
        continueAfterFailure = false
        app.launch()
    }
    
    func testResumeAnalysis() throws {
        // Navigate to resume screen
        app.buttons["Analyze Resume"].tap()
        
        // Verify screen loaded
        XCTAssertTrue(app.staticTexts["Resume Analysis"].exists)
    }
}
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Framework not found | Run `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` |
| Simulator not starting | Reset Content and Settings in Simulator menu |
| Keychain access denied | Check entitlements in Xcode |
| Build fails in Xcode | Clean build folder (Cmd+Shift+K) and rebuild |

## Performance Tips

1. **Use release builds** for performance testing
2. **Profile with Instruments** to identify bottlenecks
3. **Minimize interop calls** between Kotlin and Swift
4. **Cache heavy computations** in the shared module
