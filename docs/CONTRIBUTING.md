# Contributing to VwaTek Apply

First off, thank you for considering contributing to VwaTek Apply! It's people like you that make this project a great tool for job seekers everywhere.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [How to Contribute](#how-to-contribute)
- [Pull Request Process](#pull-request-process)
- [Coding Standards](#coding-standards)
- [Testing Guidelines](#testing-guidelines)
- [Documentation](#documentation)

## Code of Conduct

This project and everyone participating in it is governed by our Code of Conduct. By participating, you are expected to uphold this code. Please report unacceptable behavior to [conduct@vwatekapply.com](mailto:conduct@vwatekapply.com).

### Our Pledge

We pledge to make participation in our project a harassment-free experience for everyone, regardless of age, body size, disability, ethnicity, gender identity and expression, level of experience, nationality, personal appearance, race, religion, or sexual identity and orientation.

## Ground Rules

| Rule | Description |
|------|-------------|
| **No Emojis** | Emojis are not permitted in the application UI or codebase |
| **Clipart Allowed** | Vector icons and clipart graphics may be used for visual elements |

## Getting Started

### Prerequisites

Before you begin, ensure you have the following installed:

- **Xcode 16+** (for iOS development)
- **Android Studio** or **JetBrains Fleet** (for Kotlin Multiplatform)
- **JDK 17+** (for Kotlin compilation)
- **Node.js 18+** (for Web development)
- **Git** (for version control)

### Fork and Clone

1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/YOUR-USERNAME/vwatek-apply.git
   cd vwatek-apply
   ```
3. Add the upstream remote:
   ```bash
   git remote add upstream https://github.com/vwatek/vwatek-apply.git
   ```

## Development Setup

### 1. Configure API Credentials

Create a `secrets.properties` file in the root directory:

```properties
GEMINI_API_KEY=your_development_api_key
```

> Warning: Never commit API keys to the repository!

### 2. Install Dependencies

Open the project in Android Studio or Fleet and sync Gradle:

```bash
./gradlew build
```

### 3. Run the Applications

**iOS:**
Open `iosApp/iosApp.xcodeproj` in Xcode and run on a simulator or device.

**Android:**
Open the project in Android Studio, select the `androidApp` configuration, and run on an emulator or device.

**Web:**
Run the development server:
```bash
./gradlew :webApp:jsBrowserDevelopmentRun
```

### 4. Run Tests

```bash
# Run all tests
./gradlew allTests

# Run shared module tests only
./gradlew :shared:testDebugUnitTest

# Run Android tests
./gradlew :androidApp:testDebugUnitTest
```

## How to Contribute

### Reporting Bugs

Before creating bug reports, please check the existing issues to avoid duplicates.

When creating a bug report, include:

- **Clear title** describing the issue
- **Steps to reproduce** the behavior
- **Expected behavior** vs **actual behavior**
- **Screenshots** if applicable
- **Environment details**:
  - Platform (iOS/Android/Web)
  - OS version / Browser version
  - Device model
  - App version

**Bug Report Template:**

```markdown
## Bug Description
A clear description of the bug.

## Steps to Reproduce
1. Go to '...'
2. Click on '...'
3. Scroll down to '...'
4. See error

## Expected Behavior
What you expected to happen.

## Actual Behavior
What actually happened.

## Screenshots
If applicable, add screenshots.

## Environment
- Platform: [e.g., iOS, Android, Web]
- OS Version / Browser: [e.g., iOS 17.0, Android 14, Chrome 120]
- Device: [e.g., iPhone 15 Pro, Pixel 8, Desktop]
- App Version: [e.g., 1.0.0]
```

### Suggesting Features

Feature requests are welcome! Please provide:

- **Clear description** of the feature
- **Use case** explaining why this feature would be useful
- **Possible implementation** approach (optional)

**Feature Request Template:**

```markdown
## Feature Description
A clear description of the feature.

## Use Case
Explain how this feature would benefit users.

## Proposed Solution
Describe how you envision this feature working.

## Alternatives Considered
Any alternative solutions you've considered.

## Additional Context
Add any other context or screenshots.
```

### Code Contributions

1. **Check existing issues** for something you'd like to work on
2. **Create an issue** if one doesn't exist for your contribution
3. **Comment on the issue** to let others know you're working on it
4. **Create a feature branch** from `main`
5. **Make your changes** following our coding standards
6. **Write tests** for new functionality
7. **Submit a pull request**

## Pull Request Process

### Branch Naming

Use descriptive branch names:

- `feature/resume-pdf-export`
- `bugfix/interview-crash-on-empty-response`
- `docs/api-documentation-update`
- `refactor/viewmodel-architecture`

### Commit Messages

Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>(<scope>): <description>

[optional body]

[optional footer(s)]
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

**Examples:**

```
feat(resume): add PDF export functionality

Implement native Share Sheet integration for exporting
resumes as PDF documents on both iOS and Android.

Closes #123
```

```
fix(interview): handle empty AI response gracefully

- Add null check for response content
- Display user-friendly error message
- Log error for debugging

Fixes #456
```

### PR Checklist

Before submitting a PR, ensure:

- [ ] Code follows the project's coding standards
- [ ] No emojis used in code or UI strings
- [ ] All tests pass locally
- [ ] New code has appropriate test coverage
- [ ] Documentation is updated if needed
- [ ] Commit messages follow conventions
- [ ] PR description clearly explains the changes
- [ ] Screenshots included for UI changes

### PR Review Process

1. A maintainer will review your PR within 48 hours
2. Address any requested changes
3. Once approved, a maintainer will merge your PR
4. Your contribution will be included in the next release!

## Coding Standards

### Kotlin Style Guide

We follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) with some additions:

#### Naming Conventions

```kotlin
// Classes: PascalCase
class ResumeAnalyzer

// Functions: camelCase
fun analyzeResume()

// Variables: camelCase
val resumeContent: String

// Constants: SCREAMING_SNAKE_CASE
const val MAX_RESUME_LENGTH = 10000

// Private properties: prefix with underscore for backing fields
private val _state = MutableStateFlow(State())
val state: StateFlow<State> = _state.asStateFlow()
```

#### Code Organization

```kotlin
class MyViewModel(
    private val useCase: MyUseCase  // Constructor parameters
) : ViewModel() {

    // 1. Companion object
    companion object {
        private const val TAG = "MyViewModel"
    }

    // 2. Private properties
    private val _state = MutableStateFlow(State())

    // 3. Public properties
    val state: StateFlow<State> = _state.asStateFlow()

    // 4. Init block
    init {
        loadData()
    }

    // 5. Public functions
    fun onAction(action: Action) {
        // ...
    }

    // 6. Private functions
    private fun loadData() {
        // ...
    }
}
```

#### Coroutines

```kotlin
// Use viewModelScope for ViewModel coroutines
viewModelScope.launch {
    // ...
}

// Use appropriate dispatchers
withContext(Dispatchers.IO) {
    // IO operations
}

// Handle errors with Result or try-catch
val result = runCatching {
    repository.fetchData()
}.onFailure { error ->
    handleError(error)
}
```

### UI String Guidelines

**IMPORTANT: No emojis are allowed in UI strings or code comments.**

```kotlin
// CORRECT
val successMessage = "Resume analyzed successfully"
val errorMessage = "Failed to connect to server"

// INCORRECT - Do not use emojis
val successMessage = "Resume analyzed successfully!"  // No emojis
val errorMessage = "Failed to connect to server"      // No emojis
```

### File Organization

```
feature/
|-- data/
|   |-- repository/
|   |   +-- FeatureRepositoryImpl.kt
|   +-- model/
|       +-- FeatureDto.kt
|-- domain/
|   |-- model/
|   |   +-- Feature.kt
|   |-- repository/
|   |   +-- FeatureRepository.kt
|   +-- usecase/
|       +-- GetFeatureUseCase.kt
+-- presentation/
    |-- FeatureScreen.kt
    |-- FeatureViewModel.kt
    |-- FeatureState.kt
    +-- FeatureIntent.kt
```

### Compose UI Guidelines

```kotlin
// Use meaningful parameter names
@Composable
fun ResumeCard(
    resume: Resume,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier  // Always include modifier parameter
) {
    // ...
}

// Extract reusable components
@Composable
private fun ScoreIndicator(
    score: Int,
    modifier: Modifier = Modifier
) {
    // ...
}

// Use preview annotations
@Preview(showBackground = true)
@Composable
private fun ResumeCardPreview() {
    VwaTekTheme {
        ResumeCard(
            resume = sampleResume,
            onEditClick = {},
            onDeleteClick = {}
        )
    }
}
```

## Testing Guidelines

### Test Structure

```kotlin
class ResumeAnalyzerTest {
    
    // Arrange
    private lateinit var analyzer: ResumeAnalyzer
    private lateinit var mockRepository: MockResumeRepository
    
    @BeforeTest
    fun setup() {
        mockRepository = MockResumeRepository()
        analyzer = ResumeAnalyzer(mockRepository)
    }
    
    @Test
    fun `analyzeResume returns high score for matching keywords`() = runTest {
        // Arrange
        val resume = createTestResume(skills = listOf("Kotlin", "Android"))
        val jobDescription = createTestJob(requirements = listOf("Kotlin", "Android"))
        
        // Act
        val result = analyzer.analyze(resume, jobDescription)
        
        // Assert
        assertTrue(result.matchScore >= 80)
        assertTrue(result.matchedKeywords.containsAll(listOf("Kotlin", "Android")))
    }
    
    @Test
    fun `analyzeResume identifies missing keywords`() = runTest {
        // ...
    }
}
```

### Test Naming

Use descriptive test names with backticks:

```kotlin
@Test
fun `generateCoverLetter throws exception when resume is empty`()

@Test
fun `mockInterview asks follow-up questions based on response`()

@Test
fun `exportPDF creates valid PDF document`()
```

### What to Test

- **Use Cases**: Business logic and edge cases
- **ViewModels**: State management and intent handling
- **Repositories**: Data mapping and error handling
- **Utilities**: Helper functions and extensions

### Mocking

Use constructor injection for easy mocking:

```kotlin
class MockGeminiService : GeminiService {
    var mockResponse: String = ""
    var shouldThrow: Boolean = false
    
    override suspend fun generateContent(prompt: String): String {
        if (shouldThrow) throw ApiException("Mock error")
        return mockResponse
    }
}
```

## Documentation

### Code Documentation

Document public APIs with KDoc:

```kotlin
/**
 * Analyzes a resume against a job description to calculate compatibility.
 *
 * @param resume The candidate's resume content
 * @param jobDescription The target job description
 * @return [ResumeAnalysis] containing match score and recommendations
 * @throws ApiException if the AI service is unavailable
 *
 * @sample
 * ```
 * val analysis = analyzer.analyze(myResume, jobDescription)
 * println("Match score: ${analysis.matchScore}%")
 * ```
 */
suspend fun analyze(resume: Resume, jobDescription: JobDescription): ResumeAnalysis
```

### README Updates

Update documentation when:

- Adding new features
- Changing API interfaces
- Modifying setup requirements
- Updating dependencies

## Questions?

If you have questions about contributing:

1. Check the [FAQ](docs/FAQ.md)
2. Search existing issues
3. Open a new discussion on GitHub
4. Reach out to maintainers at [dev@vwatekapply.com](mailto:dev@vwatekapply.com)

---

Thank you for contributing to VwaTek Apply!
