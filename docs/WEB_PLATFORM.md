# Web Platform Guide

This guide covers the Web-specific implementation details for VwaTek Apply using Kotlin/JS and Compose for Web.

## Overview

The Web version of VwaTek Apply uses **Compose for Web** (Compose HTML/DOM) to deliver a native-like experience in modern browsers while sharing business logic with the iOS and Android applications through Kotlin Multiplatform.

## Technical Stack

| Component | Technology |
|-----------|------------|
| UI Framework | Compose for Web |
| Language | Kotlin/JS |
| Build Tool | Gradle with Kotlin/JS plugin |
| Bundler | Webpack (via Kotlin/JS) |
| Storage | IndexedDB with encryption |
| Networking | Ktor (JS engine) |

## Project Structure

```
webApp/
|-- src/
|   |-- jsMain/
|   |   |-- kotlin/
|   |   |   +-- com/vwatek/apply/
|   |   |       |-- Main.kt              # Application entry point
|   |   |       |-- App.kt               # Root composable
|   |   |       |-- ui/
|   |   |       |   |-- theme/           # Web-specific theming
|   |   |       |   |-- components/      # Reusable UI components
|   |   |       |   +-- screens/         # Screen composables
|   |   |       +-- platform/
|   |   |           |-- Storage.kt       # IndexedDB implementation
|   |   |           +-- Platform.kt      # Platform-specific code
|   |   +-- resources/
|   |       |-- index.html               # HTML entry point
|   |       +-- styles.css               # Global styles
|   +-- jsTest/
|       +-- kotlin/                      # Web-specific tests
+-- build.gradle.kts
```

## Getting Started

### Prerequisites

- JDK 17+
- Node.js 18+ (for development server and tooling)
- Modern browser (Chrome, Firefox, Safari, Edge)

### Development

1. **Start the development server:**
   ```bash
   ./gradlew :webApp:jsBrowserDevelopmentRun
   ```

2. **Access the application:**
   Open `http://localhost:8080` in your browser

3. **Hot reload:**
   Changes to Kotlin code will automatically trigger a rebuild and refresh

### Production Build

```bash
# Build production bundle
./gradlew :webApp:jsBrowserProductionWebpack

# Output location
# webApp/build/distributions/
```

## Platform Implementation

### Entry Point

```kotlin
// Main.kt
fun main() {
    // Initialize Koin for dependency injection
    initKoin()
    
    // Render the application
    renderComposable(rootElementId = "root") {
        App()
    }
}

@Composable
fun App() {
    VwaTekTheme {
        Router()
    }
}
```

### Storage Implementation

The Web version uses IndexedDB for local storage with encryption:

```kotlin
// Platform.js.kt
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return WebWorkerDriver(
            schema = VwaTekDatabase.Schema,
            name = "vwatek.db"
        ).also { driver ->
            // Apply encryption using Web Crypto API
            applyEncryption(driver)
        }
    }
}

// Secure storage using Web Crypto API
actual class SecureStorage {
    private val cryptoKey: CryptoKey by lazy {
        getOrCreateEncryptionKey()
    }
    
    actual suspend fun store(key: String, value: String) {
        val encrypted = encrypt(value, cryptoKey)
        localStorage.setItem(key, encrypted)
    }
    
    actual suspend fun retrieve(key: String): String? {
        val encrypted = localStorage.getItem(key) ?: return null
        return decrypt(encrypted, cryptoKey)
    }
    
    private suspend fun encrypt(data: String, key: CryptoKey): String {
        val iv = window.crypto.getRandomValues(Uint8Array(12))
        val encoded = data.encodeToByteArray()
        
        val encrypted = window.crypto.subtle.encrypt(
            algorithm = AesGcmParams(name = "AES-GCM", iv = iv),
            key = key,
            data = encoded.toArrayBuffer()
        ).await()
        
        return Base64.encode(iv.toByteArray() + encrypted.toByteArray())
    }
    
    private suspend fun decrypt(data: String, key: CryptoKey): String {
        val decoded = Base64.decode(data)
        val iv = decoded.sliceArray(0 until 12)
        val ciphertext = decoded.sliceArray(12 until decoded.size)
        
        val decrypted = window.crypto.subtle.decrypt(
            algorithm = AesGcmParams(name = "AES-GCM", iv = iv.toUint8Array()),
            key = key,
            data = ciphertext.toArrayBuffer()
        ).await()
        
        return decrypted.toByteArray().decodeToString()
    }
}
```

### HTTP Client Configuration

```kotlin
// HttpClient.js.kt
actual fun createHttpClient(): HttpClient = HttpClient(Js) {
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
    
    // CORS handling is managed by browser
    // Ensure API supports CORS headers
}
```

## UI Components

### Compose for Web Basics

```kotlin
@Composable
fun ResumeCard(
    resume: Resume,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Div(
        attrs = {
            classes("resume-card")
            style {
                padding(16.px)
                borderRadius(8.px)
                backgroundColor(Color.white)
                boxShadow("0 2px 4px rgba(0,0,0,0.1)")
            }
        }
    ) {
        H3 { Text(resume.name) }
        P { Text(resume.industry ?: "No industry specified") }
        
        Div(attrs = { classes("button-group") }) {
            Button(
                attrs = {
                    onClick { onEditClick() }
                    classes("btn", "btn-primary")
                }
            ) {
                Text("Edit")
            }
            
            Button(
                attrs = {
                    onClick { onDeleteClick() }
                    classes("btn", "btn-danger")
                }
            ) {
                Text("Delete")
            }
        }
    }
}
```

### Responsive Design

```kotlin
@Composable
fun ResponsiveLayout(content: @Composable () -> Unit) {
    val windowWidth = remember { mutableStateOf(window.innerWidth) }
    
    DisposableEffect(Unit) {
        val listener: (Event) -> Unit = {
            windowWidth.value = window.innerWidth
        }
        window.addEventListener("resize", listener)
        onDispose {
            window.removeEventListener("resize", listener)
        }
    }
    
    val layoutClass = when {
        windowWidth.value < 768 -> "mobile-layout"
        windowWidth.value < 1024 -> "tablet-layout"
        else -> "desktop-layout"
    }
    
    Div(attrs = { classes(layoutClass) }) {
        content()
    }
}
```

## Routing

```kotlin
sealed class Route(val path: String) {
    object Home : Route("/")
    object ResumeList : Route("/resumes")
    object ResumeDetail : Route("/resumes/{id}")
    object CoverLetter : Route("/cover-letters")
    object Interview : Route("/interview")
    object Settings : Route("/settings")
}

@Composable
fun Router() {
    val currentRoute = remember { mutableStateOf<Route>(Route.Home) }
    
    // Listen to browser history
    DisposableEffect(Unit) {
        val listener: (Event) -> Unit = {
            currentRoute.value = parseRoute(window.location.pathname)
        }
        window.addEventListener("popstate", listener)
        onDispose {
            window.removeEventListener("popstate", listener)
        }
    }
    
    when (val route = currentRoute.value) {
        is Route.Home -> HomeScreen()
        is Route.ResumeList -> ResumeListScreen()
        is Route.ResumeDetail -> ResumeDetailScreen(route.path)
        is Route.CoverLetter -> CoverLetterScreen()
        is Route.Interview -> InterviewScreen()
        is Route.Settings -> SettingsScreen()
    }
}

fun navigateTo(route: Route) {
    window.history.pushState(null, "", route.path)
    window.dispatchEvent(PopStateEvent("popstate"))
}
```

## File Handling

### PDF Export

```kotlin
suspend fun exportToPdf(resume: Resume): Blob {
    // Use jsPDF library for PDF generation
    val doc = jsPDF()
    
    doc.setFontSize(24)
    doc.text(resume.name, 20, 30)
    
    doc.setFontSize(12)
    var yPosition = 50
    
    resume.sections.forEach { section ->
        doc.setFontSize(16)
        doc.text(section.title, 20, yPosition)
        yPosition += 10
        
        doc.setFontSize(12)
        section.content.forEach { line ->
            doc.text(line, 25, yPosition)
            yPosition += 7
        }
        yPosition += 10
    }
    
    return doc.output("blob")
}

fun downloadPdf(blob: Blob, filename: String) {
    val url = URL.createObjectURL(blob)
    val link = document.createElement("a") as HTMLAnchorElement
    link.href = url
    link.download = filename
    link.click()
    URL.revokeObjectURL(url)
}
```

### File Upload

```kotlin
@Composable
fun FileUpload(
    onFileSelected: (File) -> Unit
) {
    Input(
        type = InputType.File,
        attrs = {
            accept(".pdf,.doc,.docx,.txt")
            onChange { event ->
                val files = (event.target as HTMLInputElement).files
                files?.item(0)?.let { file ->
                    onFileSelected(file)
                }
            }
        }
    )
}

suspend fun readFileAsText(file: File): String {
    return suspendCoroutine { continuation ->
        val reader = FileReader()
        reader.onload = { event ->
            val result = (event.target as FileReader).result as String
            continuation.resume(result)
        }
        reader.onerror = { _ ->
            continuation.resumeWithException(Exception("Failed to read file"))
        }
        reader.readAsText(file)
    }
}
```

## Browser Compatibility

### Supported Browsers

| Browser | Minimum Version |
|---------|----------------|
| Chrome | 90+ |
| Firefox | 88+ |
| Safari | 14+ |
| Edge | 90+ |

### Polyfills

The build automatically includes necessary polyfills for:
- Web Crypto API
- IndexedDB
- Fetch API
- ES6+ features

## Performance Optimization

### Code Splitting

```kotlin
// build.gradle.kts
kotlin {
    js(IR) {
        browser {
            webpackTask {
                cssSupport {
                    enabled.set(true)
                }
                // Enable code splitting
                outputFileName = "[name].[contenthash].js"
            }
        }
        binaries.executable()
    }
}
```

### Lazy Loading

```kotlin
@Composable
fun LazyScreen(
    loader: suspend () -> @Composable () -> Unit
) {
    var content by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
    
    LaunchedEffect(loader) {
        content = loader()
    }
    
    content?.invoke() ?: LoadingSpinner()
}
```

## Testing

### Unit Tests

```kotlin
// ResumeViewModelTest.kt
class ResumeViewModelTest {
    @Test
    fun testResumeAnalysis() = runTest {
        val mockService = MockGeminiService()
        val viewModel = ResumeReviewViewModel(mockService)
        
        viewModel.onIntent(ResumeReviewIntent.AnalyzeResume(
            resume = testResume,
            jobDescription = testJob
        ))
        
        val state = viewModel.state.value
        assertTrue(state.analysis != null)
        assertTrue(state.analysis!!.matchScore > 0)
    }
}
```

### Browser Tests

```bash
# Run tests in headless browser
./gradlew :webApp:jsBrowserTest
```

## Deployment

### Static Hosting

The production build generates static files suitable for:
- GitHub Pages
- Netlify
- Vercel
- AWS S3 + CloudFront
- Any static file server

### Build and Deploy

```bash
# Build production bundle
./gradlew :webApp:jsBrowserProductionWebpack

# Files are in webApp/build/distributions/
# Deploy contents to your hosting provider
```

### Environment Configuration

```kotlin
// Config.kt
object WebConfig {
    val apiBaseUrl: String
        get() = when {
            js("window.location.hostname") == "localhost" -> "http://localhost:8080"
            else -> "https://api.vwatek.com"
        }
    
    val isDevelopment: Boolean
        get() = js("window.location.hostname") == "localhost"
}
```

## Troubleshooting

### Common Issues

| Issue | Solution |
|-------|----------|
| CORS errors | Ensure API server includes proper CORS headers |
| IndexedDB not available | Check browser privacy settings |
| Build fails | Clear Gradle cache and rebuild |
| Hot reload not working | Restart development server |

### Debug Mode

Enable verbose logging in development:

```kotlin
if (WebConfig.isDevelopment) {
    console.log("Debug: $message")
}
```
