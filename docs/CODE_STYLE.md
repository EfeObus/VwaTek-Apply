# Code Style Guide

This document outlines the coding standards and style guidelines for the VwaTek Apply project. All contributors must adhere to these guidelines to maintain code consistency and quality.

## Ground Rules

| Rule | Description |
|------|-------------|
| **No Emojis** | Emojis are NOT permitted in code, comments, UI strings, or documentation |
| **Clipart Allowed** | Vector icons and clipart graphics may be used for visual elements |

## Kotlin Style Guide

We follow the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) with the following additions and clarifications.

### Naming Conventions

#### Classes and Interfaces

```kotlin
// Classes: PascalCase
class ResumeAnalyzer
class UserPreferences
class GeminiApiService

// Interfaces: PascalCase, no "I" prefix
interface ResumeRepository
interface AiService

// Abstract classes: PascalCase with "Abstract" prefix or "Base" prefix
abstract class BaseViewModel
abstract class AbstractRepository
```

#### Functions

```kotlin
// Functions: camelCase, verb-first
fun analyzeResume()
fun getUserPreferences()
fun saveDocument()
fun isValid(): Boolean
fun hasPermission(): Boolean

// Suspend functions: same convention
suspend fun fetchAnalysis()
suspend fun uploadResume()
```

#### Variables and Properties

```kotlin
// Variables: camelCase
val resumeContent: String
var isLoading: Boolean
val userPreferences: Preferences

// Constants: SCREAMING_SNAKE_CASE
const val MAX_RESUME_LENGTH = 10000
const val API_TIMEOUT_MS = 60_000L
const val DEFAULT_PAGE_SIZE = 20

// Companion object constants
companion object {
    private const val TAG = "ResumeViewModel"
    const val EXTRA_RESUME_ID = "resume_id"
}
```

#### Backing Properties

```kotlin
// Use underscore prefix for private backing properties
private val _state = MutableStateFlow(UiState())
val state: StateFlow<UiState> = _state.asStateFlow()

private val _events = Channel<Event>()
val events: Flow<Event> = _events.receiveAsFlow()
```

### Code Organization

#### File Structure

Each Kotlin file should be organized in this order:

1. Package statement
2. Import statements (sorted alphabetically, no wildcards)
3. Top-level declarations

#### Class Structure

```kotlin
class MyViewModel(
    private val useCase: MyUseCase,      // Constructor parameters
    private val repository: Repository
) : ViewModel() {

    // 1. Companion object (if needed)
    companion object {
        private const val TAG = "MyViewModel"
        private const val DEBOUNCE_MS = 300L
    }

    // 2. Private properties
    private val _state = MutableStateFlow(State())
    private var currentJob: Job? = null

    // 3. Public properties
    val state: StateFlow<State> = _state.asStateFlow()

    // 4. Init block
    init {
        loadInitialData()
    }

    // 5. Public functions (most important first)
    fun onIntent(intent: Intent) {
        when (intent) {
            is Intent.Load -> loadData()
            is Intent.Refresh -> refresh()
        }
    }

    fun retry() {
        loadData()
    }

    // 6. Private functions
    private fun loadData() {
        viewModelScope.launch {
            // ...
        }
    }

    private fun loadInitialData() {
        // ...
    }

    // 7. Nested classes and interfaces
    sealed class State {
        object Loading : State()
        data class Success(val data: Data) : State()
        data class Error(val message: String) : State()
    }

    sealed class Intent {
        object Load : Intent()
        object Refresh : Intent()
    }
}
```

### Formatting

#### Indentation

- Use 4 spaces for indentation (no tabs)
- Continuation indent: 4 spaces

#### Line Length

- Maximum line length: 120 characters
- Break long lines at logical points

#### Braces

```kotlin
// Opening brace on same line
if (condition) {
    // code
} else {
    // code
}

// Single-line bodies: braces optional but be consistent
if (condition) return early

// For lambdas, prefer inline for short expressions
list.map { it.name }

// Multi-line lambdas
list.map { item ->
    val processed = process(item)
    processed.toResult()
}
```

#### Blank Lines

```kotlin
class MyClass {
    
    // One blank line between property groups
    private val property1: String = ""
    private val property2: Int = 0
    
    private var mutableProperty: Boolean = false
    
    // One blank line before functions
    fun function1() {
        // ...
    }
    
    fun function2() {
        // ...
    }
}
```

### Coroutines

#### Scope Usage

```kotlin
// ViewModels: use viewModelScope
class MyViewModel : ViewModel() {
    fun loadData() {
        viewModelScope.launch {
            // ...
        }
    }
}

// Repositories: accept scope or use withContext
class MyRepository {
    suspend fun fetchData(): Result<Data> = withContext(Dispatchers.IO) {
        // ...
    }
}
```

#### Error Handling

```kotlin
// Prefer Result type for recoverable errors
suspend fun analyze(): Result<Analysis> {
    return runCatching {
        apiService.analyze(resume)
    }.onFailure { error ->
        logger.error("Analysis failed", error)
    }
}

// Use try-catch for specific handling
try {
    val result = apiService.call()
    handleSuccess(result)
} catch (e: NetworkException) {
    handleNetworkError(e)
} catch (e: ApiException) {
    handleApiError(e)
}
```

#### Flow Usage

```kotlin
// Prefer Flow for reactive streams
fun observeResumes(): Flow<List<Resume>> = flow {
    while (true) {
        emit(repository.getResumes())
        delay(REFRESH_INTERVAL)
    }
}.flowOn(Dispatchers.IO)

// Collect safely in ViewModels
viewModelScope.launch {
    resumeFlow
        .catch { error -> handleError(error) }
        .collect { resumes -> updateState(resumes) }
}
```

### Compose UI Guidelines

#### Composable Functions

```kotlin
// Top-level composables: PascalCase
@Composable
fun ResumeCard(
    resume: Resume,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier  // Always include modifier parameter
) {
    // ...
}

// Private helper composables
@Composable
private fun ScoreIndicator(
    score: Int,
    modifier: Modifier = Modifier
) {
    // ...
}
```

#### State Management

```kotlin
// Prefer hoisting state
@Composable
fun ResumeScreen(
    viewModel: ResumeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    ResumeContent(
        state = state,
        onIntent = viewModel::onIntent
    )
}

@Composable
private fun ResumeContent(
    state: ResumeState,
    onIntent: (ResumeIntent) -> Unit
) {
    // Stateless composable
}
```

#### Preview Annotations

```kotlin
@Preview(
    name = "Light Mode",
    showBackground = true
)
@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ResumeCardPreview() {
    VwaTekTheme {
        ResumeCard(
            resume = previewResume,
            onEditClick = {},
            onDeleteClick = {}
        )
    }
}
```

### Documentation

#### KDoc Comments

```kotlin
/**
 * Analyzes a resume against a job description to calculate compatibility.
 *
 * This function performs keyword matching, skills alignment analysis,
 * and generates actionable recommendations for improvement.
 *
 * @param resume The candidate's resume content
 * @param jobDescription The target job description to analyze against
 * @return [Result] containing [ResumeAnalysis] on success or an error
 * @throws IllegalArgumentException if resume or jobDescription is blank
 *
 * Example usage:
 * ```
 * val result = analyzer.analyze(myResume, jobDescription)
 * result.onSuccess { analysis ->
 *     println("Match score: ${analysis.matchScore}%")
 * }
 * ```
 *
 * @see ResumeAnalysis
 * @see KeywordMatcher
 */
suspend fun analyze(
    resume: Resume,
    jobDescription: JobDescription
): Result<ResumeAnalysis>
```

#### When to Document

- All public APIs
- Complex algorithms
- Non-obvious behavior
- Configuration options
- Deprecated functionality

#### When NOT to Document

```kotlin
// Avoid redundant documentation
/** Returns the user's name. */  // Redundant
fun getUserName(): String

// Prefer self-documenting code
fun getUserName(): String  // Clear from signature
```

### String Handling

#### No Emojis in Strings

```kotlin
// CORRECT: No emojis
val successMessage = "Resume analyzed successfully"
val errorMessage = "Failed to connect to server"
val welcomeText = "Welcome to VwaTek Apply"

// INCORRECT: Contains emojis - DO NOT USE
val successMessage = "Resume analyzed successfully!"  // No emojis allowed
```

#### String Resources

```kotlin
// Use string resources for UI text
// strings.xml (Android)
<string name="resume_analyzed">Resume analyzed successfully</string>
<string name="error_network">Failed to connect to server</string>

// Access in code
stringResource(R.string.resume_analyzed)
```

### Testing

#### Test Naming

```kotlin
class ResumeAnalyzerTest {
    
    @Test
    fun `analyze returns high score for matching keywords`() = runTest {
        // ...
    }
    
    @Test
    fun `analyze throws exception when resume is empty`() = runTest {
        // ...
    }
    
    @Test
    fun `analyze identifies missing required skills`() = runTest {
        // ...
    }
}
```

#### Test Structure

```kotlin
@Test
fun `analyze returns correct match score`() = runTest {
    // Arrange
    val resume = createTestResume(skills = listOf("Kotlin", "Android"))
    val job = createTestJob(requirements = listOf("Kotlin", "Android"))
    
    // Act
    val result = analyzer.analyze(resume, job)
    
    // Assert
    assertTrue(result.isSuccess)
    assertEquals(100, result.getOrNull()?.matchScore)
}
```

### Import Organization

```kotlin
// Standard library imports first
import kotlin.coroutines.CoroutineContext

// Third-party imports
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.inject

// Project imports last
import com.vwatek.apply.domain.model.Resume
import com.vwatek.apply.data.repository.ResumeRepository

// No wildcard imports
// INCORRECT: import kotlinx.coroutines.*
// CORRECT: import kotlinx.coroutines.launch
```

### Dependency Injection

```kotlin
// Module definition
val dataModule = module {
    single<ResumeRepository> { ResumeRepositoryImpl(get(), get()) }
    single { GeminiApiService(get()) }
}

val domainModule = module {
    factory { AnalyzeResumeUseCase(get()) }
    factory { GenerateCoverLetterUseCase(get()) }
}

val presentationModule = module {
    viewModel { ResumeViewModel(get(), get()) }
}

// Injection in ViewModels
class ResumeViewModel(
    private val analyzeUseCase: AnalyzeResumeUseCase,
    private val repository: ResumeRepository
) : ViewModel()

// Injection in Composables
@Composable
fun ResumeScreen(
    viewModel: ResumeViewModel = koinViewModel()
) {
    // ...
}
```

## Linting and Formatting

### Tools

- **ktlint**: Kotlin linter and formatter
- **detekt**: Static code analysis

### Running Checks

```bash
# Run ktlint check
./gradlew ktlintCheck

# Auto-format code
./gradlew ktlintFormat

# Run detekt analysis
./gradlew detekt
```

### IDE Configuration

Configure your IDE to:
- Use 4 spaces for indentation
- Enable "Optimize imports on the fly"
- Set line length to 120 characters
- Enable ktlint plugin

## Enforcement

- All PRs must pass linting checks
- Code review will enforce style guidelines
- CI/CD pipeline runs automated style checks
