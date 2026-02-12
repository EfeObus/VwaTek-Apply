# Android Platform Guide

This guide covers Android-specific implementation details for VwaTek Apply using Kotlin Multiplatform and Jetpack Compose.

## Overview

The Android version of VwaTek Apply uses **Jetpack Compose** for the UI layer while sharing business logic with iOS and Web through the shared Kotlin Multiplatform module.

## Technical Stack

| Component | Technology |
|-----------|------------|
| UI Framework | Jetpack Compose |
| Language | Kotlin |
| Build Tool | Gradle (Kotlin DSL) |
| Secure Storage | Android Keystore + EncryptedSharedPreferences |
| Networking | Ktor (OkHttp engine) |
| Crash Reporting | Firebase Crashlytics (Phase 1) |
| Analytics | Firebase Analytics (Phase 1) |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |

## Project Structure

```
androidApp/
|-- src/
|   |-- main/
|   |   |-- kotlin/
|   |   |   +-- com/vwatek/apply/
|   |   |       |-- MainActivity.kt       # App entry point
|   |   |       |-- VwaTekApplication.kt  # Application class
|   |   |       |-- ui/
|   |   |       |   |-- theme/            # Material 3 theming
|   |   |       |   +-- components/       # Android-specific UI
|   |   |       +-- platform/
|   |   |           +-- AndroidPlatform.kt
|   |   |-- res/
|   |   |   |-- values/                   # Strings, colors, themes
|   |   |   |-- drawable/                 # Vector drawables
|   |   |   +-- mipmap-*/                 # App icons
|   |   +-- AndroidManifest.xml
|   +-- test/                             # Unit tests
|-- build.gradle.kts
+-- proguard-rules.pro

shared/src/androidMain/
|-- kotlin/
|   +-- com/vwatek/apply/
|       |-- platform/
|       |   |-- Platform.android.kt       # Android platform implementation
|       |   |-- Database.android.kt       # Android database driver
|       |   +-- Security.android.kt       # Keystore implementation
|       +-- di/
|           +-- Module.android.kt         # Android-specific DI
```

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17+
- Android SDK 34
- Kotlin 2.0+

### Development

1. **Open in Android Studio:**
   Open the root project folder in Android Studio.

2. **Sync Gradle:**
   Android Studio will automatically sync Gradle files.

3. **Select configuration:**
   Choose `androidApp` from the run configurations dropdown.

4. **Run:**
   Click the Run button or press Shift+F10.

### Building from Command Line

```bash
# Build debug APK
./gradlew :androidApp:assembleDebug

# Build release APK
./gradlew :androidApp:assembleRelease

# Build release AAB (for Play Store)
./gradlew :androidApp:bundleRelease

# Install on connected device
./gradlew :androidApp:installDebug
```

## Platform Implementation

### Application Class

```kotlin
// VwaTekApplication.kt
class VwaTekApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase Crashlytics (Phase 1)
        FirebaseCrashlytics.getInstance().apply {
            setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
            setCustomKey("app_version", BuildConfig.VERSION_NAME)
        }
        
        // Initialize Koin
        startKoin {
            androidContext(this@VwaTekApplication)
            modules(
                sharedModule,
                androidModule
            )
        }
        
        // Start network monitoring (Phase 1)
        val networkMonitor: NetworkMonitor = get()
        networkMonitor.startMonitoring()
    }
}
```

### Main Activity

```kotlin
// MainActivity.kt
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        enableEdgeToEdge()
        
        setContent {
            VwaTekTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VwaTekApp()
                }
            }
        }
    }
}
```

### Android Keystore Storage

```kotlin
// Security.android.kt
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore

actual class SecureStorage(private val context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "vwatek_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    actual fun store(key: String, value: String) {
        encryptedPrefs.edit()
            .putString(key, value)
            .apply()
    }
    
    actual fun retrieve(key: String): String? {
        return encryptedPrefs.getString(key, null)
    }
    
    actual fun delete(key: String) {
        encryptedPrefs.edit()
            .remove(key)
            .apply()
    }
    
    actual fun clear() {
        encryptedPrefs.edit()
            .clear()
            .apply()
    }
}
```

### Database Driver

```kotlin
// Database.android.kt
import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

actual class DatabaseDriverFactory(private val context: Context) {
    
    actual fun createDriver(): SqlDriver {
        // Load SQLCipher native libraries
        SQLiteDatabase.loadLibs(context)
        
        // Get or create encryption key
        val passphrase = getOrCreateDatabaseKey()
        val factory = SupportFactory(passphrase)
        
        return AndroidSqliteDriver(
            schema = VwaTekDatabase.Schema,
            context = context,
            name = "vwatek.db",
            factory = factory
        )
    }
    
    private fun getOrCreateDatabaseKey(): ByteArray {
        val secureStorage = SecureStorage(context)
        val existingKey = secureStorage.retrieve("db_key")
        
        return if (existingKey != null) {
            existingKey.encodeToByteArray()
        } else {
            val newKey = generateSecureKey()
            secureStorage.store("db_key", newKey.decodeToString())
            newKey
        }
    }
    
    private fun generateSecureKey(): ByteArray {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES)
        keyGenerator.init(256)
        return keyGenerator.generateKey().encoded
    }
}
```

## Phase 1 Components (February 2026)

### Network Monitor

```kotlin
// NetworkMonitor.android.kt
actual class NetworkMonitorFactory actual constructor() {
    actual fun create(): NetworkMonitor = AndroidNetworkMonitor(context)
}

class AndroidNetworkMonitor(context: Context) : NetworkMonitor {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) 
        as ConnectivityManager
    
    private val _networkState = MutableStateFlow(NetworkState(
        status = NetworkStatus.UNKNOWN,
        type = NetworkType.UNKNOWN
    ))
    override val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateNetworkState()
        }
        override fun onLost(network: Network) {
            _networkState.value = NetworkState(NetworkStatus.UNAVAILABLE, NetworkType.UNKNOWN)
        }
    }
    
    override fun startMonitoring() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
        updateNetworkState()
    }
    
    override fun stopMonitoring() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}
```

### Sync Engine

```kotlin
// SyncEngine.android.kt
actual class SyncEngineFactory actual constructor() {
    actual fun create(
        syncApiClient: SyncApiClient,
        networkMonitor: NetworkMonitor,
        coroutineScope: CoroutineScope
    ): SyncEngine = AndroidSyncEngine(syncApiClient, networkMonitor, coroutineScope)
}

class AndroidSyncEngine(
    private val syncApiClient: SyncApiClient,
    private val networkMonitor: NetworkMonitor,
    private val scope: CoroutineScope
) : SyncEngine {
    // Automatically syncs when network becomes available
    init {
        scope.launch {
            networkMonitor.networkState.collect { state ->
                if (state.status == NetworkStatus.AVAILABLE) {
                    syncPendingChanges()
                }
            }
        }
    }
}
```

### Consent Manager

```kotlin
// ConsentManager.android.kt
actual class ConsentManagerFactory actual constructor() {
    actual fun create(
        privacyApiClient: PrivacyApiClient,
        coroutineScope: CoroutineScope
    ): ConsentManager = AndroidConsentManager(context, privacyApiClient, coroutineScope)
}

class AndroidConsentManager(
    context: Context,
    private val privacyApiClient: PrivacyApiClient,
    private val scope: CoroutineScope
) : ConsentManager {
    // Uses EncryptedSharedPreferences for consent storage
    private val storage = SecureStorage(context)
}
```

### HTTP Client

```kotlin
// HttpClient.android.kt
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import java.util.concurrent.TimeUnit

actual fun createHttpClient(): HttpClient = HttpClient(OkHttp) {
    engine {
        config {
            connectTimeout(15, TimeUnit.SECONDS)
            readTimeout(60, TimeUnit.SECONDS)
            writeTimeout(60, TimeUnit.SECONDS)
            
            // Enable TLS 1.3
            connectionSpecs(listOf(
                ConnectionSpec.MODERN_TLS,
                ConnectionSpec.COMPATIBLE_TLS
            ))
        }
    }
    
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
        })
    }
    
    install(HttpTimeout) {
        requestTimeoutMillis = 60_000
        connectTimeoutMillis = 15_000
    }
}
```

## Android-Specific Features

### Share Sheet Integration

```kotlin
// Share.android.kt
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

actual class ShareService(private val context: Context) {
    
    actual fun sharePdf(data: ByteArray, filename: String) {
        // Write to cache directory
        val file = File(context.cacheDir, filename)
        file.writeBytes(data)
        
        // Get content URI via FileProvider
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        // Create share intent
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        // Start chooser
        val chooser = Intent.createChooser(intent, "Share Resume")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
```

### Haptic Feedback

```kotlin
// Haptics.android.kt
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

actual class HapticFeedback(private val context: Context) {
    
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    
    actual fun light() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        }
    }
    
    actual fun medium() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        }
    }
    
    actual fun success() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
        }
    }
    
    actual fun error() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        }
    }
}
```

### Material 3 Theming

```kotlin
// Theme.kt
@Composable
fun VwaTekTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),
    onPrimary = Color.White,
    secondary = Color(0xFF03DAC6),
    onSecondary = Color.Black,
    background = Color(0xFFFAFAFA),
    surface = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color.Black,
    secondary = Color(0xFF03DAC6),
    onSecondary = Color.Black,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E)
)
```

## Gradle Configuration

### App-level build.gradle.kts

```kotlin
// androidApp/build.gradle.kts
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.vwatek.apply"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.vwatek.apply"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":shared"))
    
    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose)
    
    // Security
    implementation(libs.security.crypto)
    implementation(libs.sqlcipher)
    
    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.espresso.core)
}
```

## Play Store Submission

### Requirements

1. **App signing:** Use Play App Signing
2. **Target API level:** Must target API 34+
3. **64-bit support:** Required for all apps
4. **Privacy policy:** Required URL in Play Console

### Release Checklist

- [ ] Update versionCode and versionName
- [ ] Enable R8/ProGuard minification
- [ ] Test release build thoroughly
- [ ] Create signed AAB (not APK)
- [ ] Prepare store listing (screenshots, descriptions)
- [ ] Complete content rating questionnaire

### ProGuard Rules

```proguard
# proguard-rules.pro

# Keep Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# SQLDelight
-keep class com.vwatek.apply.db.** { *; }

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Koin
-keep class org.koin.** { *; }
```

## Testing

### Unit Tests

```bash
# Run unit tests
./gradlew :androidApp:testDebugUnitTest

# Run with coverage
./gradlew :androidApp:testDebugUnitTestCoverage
```

### Instrumentation Tests

```bash
# Run on connected device/emulator
./gradlew :androidApp:connectedDebugAndroidTest
```

### UI Tests with Compose

```kotlin
// ResumeScreenTest.kt
class ResumeScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun analyzeButton_isDisplayed() {
        composeTestRule.setContent {
            VwaTekTheme {
                ResumeScreen()
            }
        }
        
        composeTestRule
            .onNodeWithText("Analyze Resume")
            .assertIsDisplayed()
    }
    
    @Test
    fun resumeInput_acceptsText() {
        composeTestRule.setContent {
            VwaTekTheme {
                ResumeScreen()
            }
        }
        
        composeTestRule
            .onNodeWithTag("resume_input")
            .performTextInput("My resume content")
            
        composeTestRule
            .onNodeWithText("My resume content")
            .assertIsDisplayed()
    }
}
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Build fails with SDK error | Update Android SDK to version 34 |
| Compose preview not working | Sync Gradle and rebuild project |
| SQLCipher crash | Ensure native libraries are loaded before database access |
| Keystore errors | Clear app data or reinstall |

## Performance Tips

1. **Use Baseline Profiles** for faster startup
2. **Enable R8 full mode** for better optimization
3. **Use LazyColumn/LazyRow** for lists
4. **Profile with Android Studio Profiler**
5. **Avoid recomposition** by using stable types
