package com.vwatek.apply.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import com.vwatek.apply.domain.model.Resume
import com.vwatek.apply.domain.model.ResumeSourceType
import com.vwatek.apply.presentation.resume.ResumeIntent
import com.vwatek.apply.presentation.resume.ResumeViewModel
import com.vwatek.apply.util.DocumentParser
import com.vwatek.apply.util.OAuthHelper
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*
import org.koin.core.context.GlobalContext
import org.w3c.dom.HTMLInputElement
import org.w3c.files.File
import org.w3c.files.get

@Composable
fun ResumeScreen() {
    val viewModel = remember { GlobalContext.get().get<ResumeViewModel>() }
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    
    var showCreateModal by remember { mutableStateOf(false) }
    var showUploadModal by remember { mutableStateOf(false) }
    var showLinkedInModal by remember { mutableStateOf(false) }
    var showAnalyzeModal by remember { mutableStateOf(false) }
    var selectedResumeForAnalysis by remember { mutableStateOf<Resume?>(null) }
    
    Div {
        // Page Header
        Div(attrs = { classes("flex", "justify-between", "items-center", "mb-xl") }) {
            Div {
                H1(attrs = { classes("mb-xs") }) { Text("Resumes") }
                P(attrs = { classes("text-secondary", "m-0") }) {
                    Text("Manage and optimize your resumes for better job matches.")
                }
            }
            Div(attrs = { classes("flex", "gap-sm") }) {
                Button(attrs = {
                    classes("btn", "btn-secondary")
                    onClick { showUploadModal = true }
                }) {
                    Span(attrs = { classes("mr-sm") }) {
                        RawHtml("""<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>""")
                    }
                    Text("Upload Resume")
                }
                Button(attrs = {
                    classes("btn", "btn-linkedin")
                    onClick { showLinkedInModal = true }
                }) {
                    Span(attrs = { classes("mr-sm") }) {
                        RawHtml("""<svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433c-1.144 0-2.063-.926-2.063-2.065 0-1.138.92-2.063 2.063-2.063 1.14 0 2.064.925 2.064 2.063 0 1.139-.925 2.065-2.064 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z"/></svg>""")
                    }
                    Text("Import from LinkedIn")
                }
                Button(attrs = {
                    classes("btn", "btn-primary", "btn-lg")
                    onClick { showCreateModal = true }
                }) {
                    Text("+ New Resume")
                }
            }
        }
        
        // Analysis Result Display
        state.analysis?.let { analysis ->
            Div(attrs = { classes("analysis-section") }) {
                Div(attrs = { classes("analysis-header") }) {
                    H3(attrs = { classes("analysis-title") }) { Text("Analysis Results") }
                    Button(attrs = {
                        classes("btn", "btn-sm")
                        style { 
                            property("background", "rgba(255,255,255,0.2)")
                            property("color", "white")
                        }
                        onClick { viewModel.onIntent(ResumeIntent.ClearAnalysis) }
                    }) {
                        Text("✕ Close")
                    }
                }
                
                Div(attrs = { classes("analysis-content") }) {
                    // Score Circle
                    Div(attrs = { classes("analysis-score") }) {
                        Span(attrs = { classes("analysis-score-value") }) { 
                            Text("${analysis.matchScore}%") 
                        }
                        Span(attrs = { classes("analysis-score-label") }) { 
                            Text("Match Score") 
                        }
                    }
                    
                    Div {
                        P(attrs = { style { property("opacity", "0.9") } }) {
                            Text("Your resume matches ${analysis.matchScore}% of the job requirements.")
                        }
                        
                        // Progress bar
                        Div(attrs = { classes("progress-bar") }) {
                            Div(attrs = {
                                classes("progress-fill")
                                style { 
                                    property("width", "${analysis.matchScore}%")
                                    property("background", "rgba(255,255,255,0.5)")
                                }
                            })
                        }
                    }
                }
                
                // Missing Keywords
                if (analysis.missingKeywords.isNotEmpty()) {
                    Div(attrs = { classes("analysis-keywords") }) {
                        Span(attrs = { classes("analysis-keywords-title") }) { 
                            Text("Missing Keywords") 
                        }
                        Div {
                            analysis.missingKeywords.forEach { keyword ->
                                Span(attrs = { classes("keyword-tag") }) { Text(keyword) }
                            }
                        }
                    }
                }
                
                // Recommendations
                if (analysis.recommendations.isNotEmpty()) {
                    Ul(attrs = { classes("recommendations-list") }) {
                        analysis.recommendations.forEach { rec ->
                            Li { Text(rec) }
                        }
                    }
                }
            }
        }
        
        // Resume List
        if (state.isLoading) {
            Div(attrs = { classes("flex", "justify-center", "mt-xl") }) {
                Div(attrs = { classes("spinner") })
            }
        } else if (state.resumes.isEmpty()) {
            // Empty State
            Div(attrs = { classes("empty-state") }) {
                Div(attrs = { classes("empty-state-icon") }) {
                    RawHtml("""
                        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                            <polyline points="14 2 14 8 20 8"/>
                            <line x1="16" y1="13" x2="8" y2="13"/>
                            <line x1="16" y1="17" x2="8" y2="17"/>
                        </svg>
                    """)
                }
                H3(attrs = { classes("empty-state-title") }) { Text("No resumes yet") }
                P(attrs = { classes("empty-state-description") }) {
                    Text("Create your first resume to start analyzing and optimizing for job applications.")
                }
                Div(attrs = { classes("flex", "gap-md") }) {
                    Button(attrs = {
                        classes("btn", "btn-secondary", "btn-lg")
                        onClick { showUploadModal = true }
                    }) {
                        Text("Upload Resume")
                    }
                    Button(attrs = {
                        classes("btn", "btn-primary", "btn-lg")
                        onClick { showCreateModal = true }
                    }) {
                        Text("Create Resume")
                    }
                }
            }
        } else {
            // Resume Grid
            Div(attrs = { classes("grid", "grid-2") }) {
                state.resumes.forEach { resume ->
                    ResumeCard(
                        resume = resume,
                        onAnalyze = {
                            selectedResumeForAnalysis = resume
                            showAnalyzeModal = true
                        },
                        onDelete = {
                            viewModel.onIntent(ResumeIntent.DeleteResume(resume.id))
                        }
                    )
                }
            }
        }
        
        // Modals
        if (showCreateModal) {
            CreateResumeModal(
                onClose = { showCreateModal = false },
                onCreate = { name, content, industry ->
                    viewModel.onIntent(ResumeIntent.CreateResume(name, content, industry))
                    showCreateModal = false
                }
            )
        }
        
        if (showAnalyzeModal && selectedResumeForAnalysis != null) {
            AnalyzeResumeModal(
                resume = selectedResumeForAnalysis!!,
                isAnalyzing = state.isAnalyzing,
                onClose = { 
                    showAnalyzeModal = false
                    selectedResumeForAnalysis = null
                },
                onAnalyze = { jobDescription ->
                    viewModel.onIntent(ResumeIntent.AnalyzeResume(selectedResumeForAnalysis!!, jobDescription))
                    showAnalyzeModal = false
                }
            )
        }
        
        if (showUploadModal) {
            UploadResumeModal(
                onClose = { showUploadModal = false },
                onUpload = { name, content, _, _ ->
                    viewModel.onIntent(ResumeIntent.CreateResume(name, content, null))
                    showUploadModal = false
                }
            )
        }
        
        if (showLinkedInModal) {
            LinkedInImportModal(
                onClose = { showLinkedInModal = false },
                onImport = { name, content ->
                    viewModel.onIntent(ResumeIntent.CreateResume(name, content, null))
                    showLinkedInModal = false
                }
            )
        }
        
        // Error Toast
        state.error?.let { error ->
            Div(attrs = { classes("toast", "toast-error") }) {
                Text(error)
                Button(attrs = {
                    classes("btn", "btn-sm")
                    onClick { viewModel.onIntent(ResumeIntent.ClearError) }
                }) {
                    Text("Dismiss")
                }
            }
        }
    }
}

@Composable
private fun ResumeCard(
    resume: Resume,
    onAnalyze: () -> Unit,
    onDelete: () -> Unit
) {
    Div(attrs = { classes("card") }) {
        Div(attrs = { classes("card-header") }) {
            Div(attrs = { classes("flex", "items-center", "gap-sm") }) {
                H3(attrs = { classes("card-title") }) { Text(resume.name) }
                when (resume.sourceType) {
                    ResumeSourceType.UPLOADED -> {
                        Span(attrs = { classes("badge", "badge-info") }) { Text("📄 Uploaded") }
                    }
                    ResumeSourceType.LINKEDIN -> {
                        Span(attrs = { classes("badge", "badge-linkedin") }) { Text("in LinkedIn") }
                    }
                    ResumeSourceType.MANUAL -> {}
                }
            }
            val industry = resume.industry
            if (industry != null) {
                Span(attrs = { classes("badge", "badge-primary") }) { Text(industry) }
            }
        }
        
        P(attrs = { 
            classes("text-secondary", "text-sm", "mb-lg")
            style {
                property("overflow", "hidden")
                property("text-overflow", "ellipsis")
                property("display", "-webkit-box")
                property("-webkit-line-clamp", "3")
                property("-webkit-box-orient", "vertical")
                property("line-height", "1.6")
            }
        }) {
            Text(resume.content.take(250) + if (resume.content.length > 250) "..." else "")
        }
        
        Div(attrs = { classes("flex", "gap-sm") }) {
            Button(attrs = {
                classes("btn", "btn-primary")
                onClick { onAnalyze() }
            }) {
                Text("🔍 Analyze")
            }
            Button(attrs = {
                classes("btn", "btn-outline")
            }) {
                Text("✏️ Edit")
            }
            Button(attrs = {
                classes("btn", "btn-ghost")
                style { property("color", "var(--color-error)") }
                onClick { onDelete() }
            }) {
                Text("🗑️ Delete")
            }
        }
    }
}

@Composable
private fun CreateResumeModal(
    onClose: () -> Unit,
    onCreate: (name: String, content: String, industry: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var industry by remember { mutableStateOf("") }
    
    Div(attrs = { classes("modal-backdrop") }) {
        Div(attrs = { classes("modal", "modal-lg") }) {
            Div(attrs = { classes("modal-header") }) {
                H3(attrs = { classes("modal-title") }) { Text("Create New Resume") }
                Button(attrs = {
                    classes("modal-close")
                    onClick { onClose() }
                }) { Text("✕") }
            }
            
            Div(attrs = { classes("modal-body") }) {
                Div(attrs = { classes("form-group") }) {
                    Label(attrs = { classes("form-label") }) { Text("Resume Name") }
                    Input(InputType.Text) {
                        classes("form-input")
                        placeholder("e.g., Software Engineer Resume")
                        value(name)
                        onInput { name = it.value }
                    }
                }
                
                Div(attrs = { classes("form-group") }) {
                    Label(attrs = { classes("form-label") }) { Text("Industry (Optional)") }
                    Input(InputType.Text) {
                        classes("form-input")
                        placeholder("e.g., Technology, Finance, Healthcare")
                        value(industry)
                        onInput { industry = it.value }
                    }
                }
                
                Div(attrs = { classes("form-group") }) {
                    Label(attrs = { classes("form-label") }) { Text("Resume Content") }
                    TextArea {
                        classes("form-textarea")
                        placeholder("Paste your resume content here including work experience, skills, and education...")
                        value(content)
                        onInput { content = it.value }
                        style { property("min-height", "250px") }
                    }
                    P(attrs = { classes("form-helper") }) {
                        Text("Include your professional summary, work experience, skills, and education for best AI analysis results.")
                    }
                }
            }
            
            Div(attrs = { classes("modal-footer") }) {
                Button(attrs = {
                    classes("btn", "btn-secondary")
                    onClick { onClose() }
                }) { Text("Cancel") }
                Button(attrs = {
                    classes("btn", "btn-primary")
                    if (name.isBlank() || content.isBlank()) attr("disabled", "true")
                    onClick { 
                        if (name.isNotBlank() && content.isNotBlank()) {
                            onCreate(name, content, industry.ifBlank { null })
                        }
                    }
                }) { Text("Create Resume") }
            }
        }
    }
}

@Composable
private fun AnalyzeResumeModal(
    resume: Resume,
    isAnalyzing: Boolean,
    onClose: () -> Unit,
    onAnalyze: (jobDescription: String) -> Unit
) {
    var jobDescription by remember { mutableStateOf("") }
    
    Div(attrs = { classes("modal-backdrop") }) {
        Div(attrs = { classes("modal", "modal-lg") }) {
            Div(attrs = { classes("modal-header") }) {
                H3(attrs = { classes("modal-title") }) { Text("Analyze Resume") }
                Button(attrs = {
                    classes("modal-close")
                    onClick { onClose() }
                }) { Text("✕") }
            }
            
            Div(attrs = { classes("modal-body") }) {
                Div(attrs = { classes("alert", "alert-info", "mb-lg") }) {
                    B { Text("Analyzing: ") }
                    Text(resume.name)
                }
                
                Div(attrs = { classes("form-group") }) {
                    Label(attrs = { classes("form-label") }) { Text("Job Description") }
                    TextArea {
                        classes("form-textarea")
                        placeholder("Paste the full job description you want to match your resume against...")
                        value(jobDescription)
                        onInput { jobDescription = it.value }
                        style { property("min-height", "250px") }
                    }
                    P(attrs = { classes("form-helper") }) {
                        Text("Include the complete job posting with requirements and qualifications for accurate analysis.")
                    }
                }
            }
            
            Div(attrs = { classes("modal-footer") }) {
                Button(attrs = {
                    classes("btn", "btn-secondary")
                    onClick { onClose() }
                }) { Text("Cancel") }
                Button(attrs = {
                    classes("btn", "btn-primary")
                    if (isAnalyzing || jobDescription.isBlank()) attr("disabled", "true")
                    onClick { 
                        if (jobDescription.isNotBlank()) {
                            onAnalyze(jobDescription)
                        }
                    }
                }) {
                    if (isAnalyzing) {
                        Span(attrs = { classes("spinner-sm", "mr-sm") })
                        Text("Analyzing...")
                    } else {
                        Text("🔍 Analyze with AI")
                    }
                }
            }
        }
    }
}

@Composable
private fun UploadResumeModal(
    onClose: () -> Unit,
    onUpload: (name: String, content: String, fileName: String?, fileType: String?) -> Unit
) {
    var resumeName by remember { mutableStateOf("") }
    var fileContent by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf<String?>(null) }
    var fileType by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var processingMessage by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    Div(attrs = { classes("modal-backdrop") }) {
        Div(attrs = { classes("modal", "modal-lg") }) {
            Div(attrs = { classes("modal-header") }) {
                H3(attrs = { classes("modal-title") }) { Text("Upload Resume") }
                Button(attrs = {
                    classes("modal-close")
                    onClick { onClose() }
                }) { Text("✕") }
            }
            
            Div(attrs = { classes("modal-body") }) {
                P(attrs = { classes("text-secondary", "mb-lg") }) {
                    Text("Upload your resume file and we'll automatically extract the text content using AI-powered parsing.")
                }
                
                error?.let { err ->
                    Div(attrs = { classes("alert", "alert-error", "mb-lg") }) {
                        Text(err)
                        Button(attrs = {
                            classes("btn", "btn-sm", "ml-md")
                            onClick { error = null }
                        }) { Text("✕") }
                    }
                }
                
                // Upload Area
                Div(attrs = { 
                    classes("upload-area")
                    if (fileName != null) classes("has-file")
                    style { property("position", "relative") }
                }) {
                    // Processing overlay
                    if (isProcessing) {
                        Div(attrs = { classes("processing-overlay") }) {
                            Div(attrs = { classes("spinner") })
                            P(attrs = { classes("processing-text") }) { Text(processingMessage) }
                        }
                    }
                    
                    if (fileName != null && !isProcessing) {
                        // File loaded state
                        Div(attrs = { classes("file-preview") }) {
                            Div(attrs = { classes("file-icon") }) {
                                val icon = when (fileType?.lowercase()) {
                                    "pdf" -> "📕"
                                    "docx", "doc" -> "📘"
                                    "txt" -> "📄"
                                    else -> "📁"
                                }
                                Text(icon)
                            }
                            Div(attrs = { classes("file-info") }) {
                                Div(attrs = { classes("file-name") }) { Text(fileName!!) }
                                Div(attrs = { classes("file-size", "text-success") }) { 
                                    Text("✓ Content extracted successfully") 
                                }
                            }
                            Button(attrs = {
                                classes("file-remove")
                                onClick {
                                    fileName = null
                                    fileContent = ""
                                    fileType = null
                                    resumeName = ""
                                }
                            }) { Text("✕") }
                        }
                    } else if (!isProcessing) {
                        // Upload prompt
                        Div(attrs = { classes("upload-icon") }) {
                            RawHtml("""<svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>""")
                        }
                        P(attrs = { classes("upload-title") }) { Text("Click to select a file") }
                        P(attrs = { classes("upload-subtitle") }) { Text("or drag and drop here") }
                        P(attrs = { classes("upload-supported") }) { 
                            Text("Supported: PDF, DOCX, DOC, TXT • Max 10MB") 
                        }
                    }
                    
                    Input(InputType.File) {
                        id("resume-upload-input")
                        attr("accept", ".pdf,.docx,.doc,.txt")
                        style {
                            property("position", "absolute")
                            property("width", "100%")
                            property("height", "100%")
                            property("top", "0")
                            property("left", "0")
                            property("opacity", "0")
                            property("cursor", "pointer")
                        }
                        onChange { event ->
                            val input = event.target as? HTMLInputElement
                            val file = input?.files?.get(0)
                            if (file != null) {
                                val maxSize = 10 * 1024 * 1024 // 10MB
                                if (file.size.toDouble() > maxSize) {
                                    error = "File too large. Maximum size is 10MB."
                                    return@onChange
                                }
                                
                                val type = file.name.substringAfterLast(".").lowercase()
                                if (!DocumentParser.isSupported(type)) {
                                    error = "Unsupported file type. Please use PDF, DOCX, DOC, or TXT."
                                    return@onChange
                                }
                                
                                fileName = file.name
                                fileType = type
                                resumeName = file.name.substringBeforeLast(".")
                                isProcessing = true
                                processingMessage = "Extracting text from ${file.name}..."
                                error = null
                                
                                scope.launch {
                                    try {
                                        val result = DocumentParser.parseFile(file)
                                        result.fold(
                                            onSuccess = { text ->
                                                fileContent = text
                                                isProcessing = false
                                            },
                                            onFailure = { e ->
                                                error = e.message ?: "Failed to parse file"
                                                isProcessing = false
                                                fileName = null
                                            }
                                        )
                                    } catch (e: Exception) {
                                        error = "Error processing file: ${e.message}"
                                        isProcessing = false
                                        fileName = null
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Content preview and edit
                if (fileName != null && fileContent.isNotBlank() && !isProcessing) {
                    Div(attrs = { classes("mt-lg") }) {
                        Div(attrs = { classes("form-group") }) {
                            Label(attrs = { classes("form-label") }) { Text("Resume Name") }
                            Input(InputType.Text) {
                                classes("form-input")
                                value(resumeName)
                                onInput { resumeName = it.value }
                            }
                        }
                        
                        Div(attrs = { classes("form-group") }) {
                            Label(attrs = { classes("form-label") }) { 
                                Text("Extracted Content") 
                            }
                            TextArea {
                                classes("form-textarea")
                                value(fileContent)
                                onInput { fileContent = it.value }
                                style { property("min-height", "200px") }
                            }
                            P(attrs = { classes("form-helper") }) {
                                Text("Review and edit the extracted content as needed before saving.")
                            }
                        }
                    }
                }
            }
            
            Div(attrs = { classes("modal-footer") }) {
                Button(attrs = {
                    classes("btn", "btn-secondary")
                    onClick { onClose() }
                }) { Text("Cancel") }
                Button(attrs = {
                    classes("btn", "btn-primary")
                    if (resumeName.isBlank() || fileContent.isBlank() || isProcessing) {
                        attr("disabled", "true")
                    }
                    onClick {
                        if (resumeName.isNotBlank() && fileContent.isNotBlank()) {
                            onUpload(resumeName, fileContent, fileName, fileType)
                        }
                    }
                }) {
                    if (isProcessing) {
                        Span(attrs = { classes("spinner-sm", "mr-sm") })
                        Text("Processing...")
                    } else {
                        Text("Save Resume")
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkedInImportModal(
    onClose: () -> Unit,
    onImport: (name: String, content: String) -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var profileContent by remember { mutableStateOf("") }
    var resumeName by remember { mutableStateOf("LinkedIn Import") }
    var isConnecting by remember { mutableStateOf(false) }
    var connectionError by remember { mutableStateOf<String?>(null) }
    
    Div(attrs = { classes("modal-backdrop") }) {
        Div(attrs = { classes("modal", "modal-lg") }) {
            Div(attrs = { classes("modal-header") }) {
                H3(attrs = { classes("modal-title") }) { Text("Import from LinkedIn") }
                Button(attrs = {
                    classes("modal-close")
                    onClick { onClose() }
                }) { Text("✕") }
            }
            
            Div(attrs = { classes("modal-body") }) {
                when (step) {
                    1 -> {
                        // Option Selection
                        Div(attrs = { classes("text-center", "mb-xl") }) {
                            Div(attrs = { 
                                classes("upload-icon")
                                style { property("margin", "0 auto var(--spacing-md)") }
                            }) {
                                RawHtml("""<svg width="32" height="32" viewBox="0 0 24 24" fill="#0A66C2"><path d="M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433c-1.144 0-2.063-.926-2.063-2.065 0-1.138.92-2.063 2.063-2.063 1.14 0 2.064.925 2.064 2.063 0 1.139-.925 2.065-2.064 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z"/></svg>""")
                            }
                            H4 { Text("Import Your LinkedIn Profile") }
                            P(attrs = { classes("text-secondary") }) {
                                Text("Choose how you'd like to import your LinkedIn profile")
                            }
                        }
                        
                        connectionError?.let { err ->
                            Div(attrs = { classes("alert", "alert-error", "mb-lg") }) {
                                Text(err)
                            }
                        }
                        
                        // Connect with LinkedIn button
                        Div(attrs = { classes("mb-lg") }) {
                            Button(attrs = {
                                classes("btn", "btn-linkedin", "btn-lg", "btn-block")
                                if (isConnecting) attr("disabled", "true")
                                onClick {
                                    isConnecting = true
                                    connectionError = null
                                    
                                    OAuthHelper.openLinkedInPopup(
                                        onSuccess = { code ->
                                            isConnecting = false
                                            // In production, exchange code for token via backend
                                            // For now, proceed to manual entry with success message
                                            connectionError = null
                                            step = 2
                                        },
                                        onError = { error ->
                                            isConnecting = false
                                            connectionError = error
                                        }
                                    )
                                }
                            }) {
                                if (isConnecting) {
                                    Span(attrs = { classes("spinner-sm", "mr-sm") })
                                    Text("Connecting...")
                                } else {
                                    Text("Connect with LinkedIn")
                                }
                            }
                        }
                        
                        Div(attrs = { classes("auth-divider", "mb-lg") }) {
                            Span { Text("or import manually") }
                        }
                        
                        // Manual import instructions
                        Div(attrs = { classes("card") }) {
                            H5(attrs = { classes("mb-md") }) { Text("Manual Import Steps:") }
                            Ol(attrs = { 
                                style { 
                                    property("padding-left", "24px")
                                    property("color", "var(--color-text-secondary)")
                                }
                            }) {
                                Li(attrs = { classes("mb-sm") }) {
                                    Text("Go to your LinkedIn profile")
                                }
                                Li(attrs = { classes("mb-sm") }) {
                                    Text("Click 'More' → 'Save to PDF'")
                                }
                                Li(attrs = { classes("mb-sm") }) {
                                    Text("Open the downloaded PDF")
                                }
                                Li(attrs = { classes("mb-sm") }) {
                                    Text("Copy and paste the content in the next step")
                                }
                            }
                            Button(attrs = {
                                classes("btn", "btn-secondary", "mt-md")
                                onClick { step = 2 }
                            }) {
                                Text("Continue with Manual Import →")
                            }
                        }
                    }
                    
                    2 -> {
                        // Profile content entry
                        Div(attrs = { classes("form-group") }) {
                            Label(attrs = { classes("form-label") }) { Text("Resume Name") }
                            Input(InputType.Text) {
                                classes("form-input")
                                placeholder("My LinkedIn Resume")
                                value(resumeName)
                                onInput { resumeName = it.value }
                            }
                        }
                        
                        Div(attrs = { classes("form-group") }) {
                            Label(attrs = { classes("form-label") }) { 
                                Text("LinkedIn Profile Content") 
                            }
                            TextArea {
                                classes("form-textarea")
                                placeholder("""Paste your LinkedIn profile content here...

Include:
• Professional headline
• About/Summary section
• Work experience
• Education
• Skills""")
                                value(profileContent)
                                onInput { profileContent = it.value }
                                style { property("min-height", "300px") }
                            }
                            P(attrs = { classes("form-helper") }) {
                                Text("Include all sections from your LinkedIn profile for the best resume conversion.")
                            }
                        }
                    }
                }
            }
            
            Div(attrs = { classes("modal-footer") }) {
                if (step > 1) {
                    Button(attrs = {
                        classes("btn", "btn-secondary")
                        onClick { step-- }
                    }) { Text("← Back") }
                } else {
                    Button(attrs = {
                        classes("btn", "btn-secondary")
                        onClick { onClose() }
                    }) { Text("Cancel") }
                }
                
                if (step == 2) {
                    Button(attrs = {
                        classes("btn", "btn-linkedin")
                        if (resumeName.isBlank() || profileContent.isBlank()) {
                            attr("disabled", "true")
                        }
                        onClick {
                            if (resumeName.isNotBlank() && profileContent.isNotBlank()) {
                                onImport(resumeName, profileContent)
                            }
                        }
                    }) { Text("Import Resume") }
                }
            }
        }
    }
}

@Composable
private fun RawHtml(html: String) {
    Span(attrs = {
        ref { element ->
            element.innerHTML = html
            onDispose { }
        }
    })
}
