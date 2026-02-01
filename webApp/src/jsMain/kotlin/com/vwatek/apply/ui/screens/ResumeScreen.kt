package com.vwatek.apply.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.vwatek.apply.domain.model.Resume
import com.vwatek.apply.domain.model.ResumeSourceType
import com.vwatek.apply.presentation.resume.ResumeIntent
import com.vwatek.apply.presentation.resume.ResumeViewModel
import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*
import org.koin.core.context.GlobalContext
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import org.w3c.files.get

@Composable
fun ResumeScreen() {
    val viewModel = remember { GlobalContext.get().get<ResumeViewModel>() }
    val state by viewModel.state.collectAsState()
    
    var showCreateModal by remember { mutableStateOf(false) }
    var showUploadModal by remember { mutableStateOf(false) }
    var showLinkedInModal by remember { mutableStateOf(false) }
    var showAnalyzeModal by remember { mutableStateOf(false) }
    var selectedResumeForAnalysis by remember { mutableStateOf<Resume?>(null) }
    
    Div {
        // Header
        Div(attrs = { classes("flex", "justify-between", "items-center", "mb-lg") }) {
            Div {
                H1 { Text("Resumes") }
                P(attrs = { classes("text-secondary") }) {
                    Text("Manage and optimize your resumes for better job matches.")
                }
            }
            Div(attrs = { classes("flex", "gap-sm") }) {
                Button(attrs = {
                    classes("btn", "btn-secondary")
                    onClick { showUploadModal = true }
                }) {
                    Text("Upload Resume")
                }
                Button(attrs = {
                    classes("btn", "btn-linkedin")
                    onClick { showLinkedInModal = true }
                }) {
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
        
        // Analysis Result
        state.analysis?.let { analysis ->
            Div(attrs = { classes("card", "mb-lg") }) {
                Div(attrs = { classes("flex", "justify-between", "items-center", "mb-md") }) {
                    H3(attrs = { classes("card-title") }) { Text("Analysis Results") }
                    Button(attrs = {
                        classes("btn", "btn-secondary", "btn-sm")
                        onClick { viewModel.onIntent(ResumeIntent.ClearAnalysis) }
                    }) {
                        Text("Close")
                    }
                }
                
                // Score Display
                Div(attrs = { classes("flex", "items-center", "gap-lg", "mb-lg") }) {
                    Div(attrs = {
                        classes("score-circle")
                        style { property("--score", "${analysis.matchScore}") }
                    }) {
                        Span(attrs = { classes("score-value") }) { Text("${analysis.matchScore}") }
                        Span(attrs = { classes("score-label") }) { Text("Match Score") }
                    }
                    Div {
                        P(attrs = { classes("text-secondary", "mb-sm") }) {
                            Text("Your resume matches ${analysis.matchScore}% of the job requirements.")
                        }
                        Div(attrs = { classes("progress-bar") }) {
                            Div(attrs = {
                                classes("progress-fill")
                                style { property("width", "${analysis.matchScore}%") }
                            })
                        }
                    }
                }
                
                // Missing Keywords
                if (analysis.missingKeywords.isNotEmpty()) {
                    Div(attrs = { classes("mb-md") }) {
                        H4(attrs = { classes("mb-sm") }) { Text("Missing Keywords") }
                        Div(attrs = { classes("flex", "gap-sm") }) {
                            analysis.missingKeywords.forEach { keyword ->
                                Span(attrs = { classes("badge", "badge-warning") }) {
                                    Text(keyword)
                                }
                            }
                        }
                    }
                }
                
                // Recommendations
                if (analysis.recommendations.isNotEmpty()) {
                    Div {
                        H4(attrs = { classes("mb-sm") }) { Text("Recommendations") }
                        Ul(attrs = { style { property("padding-left", "20px") } }) {
                            analysis.recommendations.forEach { rec ->
                                Li(attrs = { classes("mb-sm", "text-secondary") }) {
                                    Text(rec)
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Resume List
        if (state.isLoading) {
            Div(attrs = { classes("flex", "justify-center", "mt-lg") }) {
                Div(attrs = { classes("spinner") })
            }
        } else if (state.resumes.isEmpty()) {
            Div(attrs = { classes("empty-state") }) {
                Div(attrs = {
                    ref { element ->
                        element.innerHTML = """
                            <svg width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                                <polyline points="14 2 14 8 20 8"/>
                                <line x1="16" y1="13" x2="8" y2="13"/>
                                <line x1="16" y1="17" x2="8" y2="17"/>
                            </svg>
                        """
                        onDispose { }
                    }
                })
                H3(attrs = { classes("empty-state-title") }) { Text("No resumes yet") }
                P(attrs = { classes("empty-state-description") }) {
                    Text("Create your first resume to start analyzing and optimizing for job applications.")
                }
                Button(attrs = {
                    classes("btn", "btn-primary", "btn-lg")
                    onClick { showCreateModal = true }
                }) {
                    Text("Create Resume")
                }
            }
        } else {
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
        
        // Create Modal
        if (showCreateModal) {
            CreateResumeModal(
                onClose = { showCreateModal = false },
                onCreate = { name, content, industry ->
                    viewModel.onIntent(ResumeIntent.CreateResume(name, content, industry))
                    showCreateModal = false
                }
            )
        }
        
        // Analyze Modal
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
        
        // Upload Modal
        if (showUploadModal) {
            UploadResumeModal(
                onClose = { showUploadModal = false },
                onUpload = { name, content, fileName, fileType ->
                    viewModel.onIntent(ResumeIntent.CreateResume(name, content, null))
                    showUploadModal = false
                }
            )
        }
        
        // LinkedIn Import Modal
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
                // Source type badge
                when (resume.sourceType) {
                    ResumeSourceType.UPLOADED -> {
                        Span(attrs = { classes("badge", "badge-info") }) {
                            Text("Uploaded")
                        }
                    }
                    ResumeSourceType.LINKEDIN -> {
                        Span(attrs = { classes("badge", "badge-linkedin") }) {
                            Text("LinkedIn")
                        }
                    }
                    ResumeSourceType.MANUAL -> { /* No badge for manual */ }
                }
            }
            val industry = resume.industry
            if (industry != null) {
                Span(attrs = { classes("badge", "badge-primary") }) {
                    Text(industry)
                }
            }
        }
        
        P(attrs = { 
            classes("text-secondary", "text-sm", "mb-md")
            style {
                property("overflow", "hidden")
                property("text-overflow", "ellipsis")
                property("display", "-webkit-box")
                property("-webkit-line-clamp", "3")
                property("-webkit-box-orient", "vertical")
            }
        }) {
            Text(resume.content.take(200) + if (resume.content.length > 200) "..." else "")
        }
        
        Div(attrs = { classes("flex", "gap-sm") }) {
            Button(attrs = {
                classes("btn", "btn-primary")
                onClick { onAnalyze() }
            }) {
                Text("Analyze")
            }
            Button(attrs = {
                classes("btn", "btn-outline")
            }) {
                Text("Edit")
            }
            Button(attrs = {
                classes("btn", "btn-danger")
                onClick { onDelete() }
            }) {
                Text("Delete")
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
        Div(attrs = { classes("modal") }) {
            Div(attrs = { classes("modal-header") }) {
                H3(attrs = { classes("modal-title") }) { Text("Create New Resume") }
                Button(attrs = {
                    classes("modal-close")
                    onClick { onClose() }
                }) {
                    Text("X")
                }
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
                        placeholder("Paste your resume content here...")
                        value(content)
                        onInput { content = it.value }
                        style { property("min-height", "200px") }
                    }
                    P(attrs = { classes("form-helper") }) {
                        Text("Paste your full resume text including work experience, skills, and education.")
                    }
                }
            }
            
            Div(attrs = { classes("modal-footer") }) {
                Button(attrs = {
                    classes("btn", "btn-secondary")
                    onClick { onClose() }
                }) {
                    Text("Cancel")
                }
                Button(attrs = {
                    classes("btn", "btn-primary")
                    onClick { 
                        if (name.isNotBlank() && content.isNotBlank()) {
                            onCreate(name, content, industry.ifBlank { null })
                        }
                    }
                }) {
                    Text("Create Resume")
                }
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
        Div(attrs = { classes("modal") }) {
            Div(attrs = { classes("modal-header") }) {
                H3(attrs = { classes("modal-title") }) { Text("Analyze Resume") }
                Button(attrs = {
                    classes("modal-close")
                    onClick { onClose() }
                }) {
                    Text("X")
                }
            }
            
            Div(attrs = { classes("modal-body") }) {
                P(attrs = { classes("mb-md") }) {
                    Text("Analyzing: ")
                    B { Text(resume.name) }
                }
                
                Div(attrs = { classes("form-group") }) {
                    Label(attrs = { classes("form-label") }) { Text("Job Description") }
                    TextArea {
                        classes("form-textarea")
                        placeholder("Paste the job description you want to match against...")
                        value(jobDescription)
                        onInput { jobDescription = it.value }
                        style { property("min-height", "200px") }
                    }
                    P(attrs = { classes("form-helper") }) {
                        Text("Include the full job posting for best results.")
                    }
                }
            }
            
            Div(attrs = { classes("modal-footer") }) {
                Button(attrs = {
                    classes("btn", "btn-secondary")
                    onClick { onClose() }
                }) {
                    Text("Cancel")
                }
                Button(attrs = {
                    classes("btn", "btn-primary")
                    if (isAnalyzing || jobDescription.isBlank()) {
                        attr("disabled", "true")
                    }
                    onClick { 
                        if (jobDescription.isNotBlank()) {
                            onAnalyze(jobDescription)
                        }
                    }
                }) {
                    if (isAnalyzing) {
                        Text("Analyzing...")
                    } else {
                        Text("Analyze")
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
    var error by remember { mutableStateOf<String?>(null) }
    
    Div(attrs = { classes("modal-backdrop") }) {
        Div(attrs = { classes("modal") }) {
            Div(attrs = { classes("modal-header") }) {
                H3(attrs = { classes("modal-title") }) { Text("Upload Resume") }
                Button(attrs = {
                    classes("modal-close")
                    onClick { onClose() }
                }) {
                    Text("X")
                }
            }
            
            Div(attrs = { classes("modal-body") }) {
                P(attrs = { classes("text-secondary", "mb-md") }) {
                    Text("Upload your resume file (PDF, DOCX, DOC, or TXT). We'll extract the text content automatically.")
                }
                
                error?.let { err ->
                    Div(attrs = { classes("alert", "alert-error", "mb-md") }) {
                        Text(err)
                    }
                }
                
                // File upload area
                Div(attrs = { 
                    classes("upload-area")
                    style {
                        property("border", "2px dashed var(--border-color)")
                        property("border-radius", "var(--border-radius-lg)")
                        property("padding", "var(--spacing-xl)")
                        property("text-align", "center")
                        property("cursor", "pointer")
                        property("transition", "all 0.2s")
                    }
                }) {
                    if (fileName != null) {
                        Div(attrs = { classes("flex", "items-center", "justify-center", "gap-md") }) {
                            RawHtml("""
                                <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="var(--success-color)" stroke-width="2">
                                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                                    <polyline points="14 2 14 8 20 8"/>
                                    <polyline points="9 15 12 18 15 15"/>
                                    <line x1="12" y1="12" x2="12" y2="18"/>
                                </svg>
                            """)
                            Div {
                                P(attrs = { style { property("font-weight", "600") } }) {
                                    Text(fileName!!)
                                }
                                P(attrs = { classes("text-secondary", "text-sm") }) {
                                    Text("File loaded successfully")
                                }
                            }
                        }
                    } else {
                        RawHtml("""
                            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="margin: 0 auto var(--spacing-md);">
                                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                                <polyline points="17 8 12 3 7 8"/>
                                <line x1="12" y1="3" x2="12" y2="15"/>
                            </svg>
                        """)
                        P(attrs = { style { property("font-weight", "600") } }) {
                            Text("Click to select a file")
                        }
                        P(attrs = { classes("text-secondary", "text-sm") }) {
                            Text("or drag and drop here")
                        }
                        P(attrs = { classes("text-secondary", "text-sm", "mt-sm") }) {
                            Text("Supported: PDF, DOCX, DOC, TXT (Max 10MB)")
                        }
                    }
                    
                    Input(InputType.File) {
                        id("resume-file-input")
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
                                if (file.size.toInt() > maxSize) {
                                    error = "File too large. Maximum size is 10MB."
                                    return@onChange
                                }
                                
                                fileName = file.name
                                fileType = file.name.substringAfterLast(".")
                                resumeName = file.name.substringBeforeLast(".")
                                isProcessing = true
                                error = null
                                
                                val reader = FileReader()
                                reader.onload = { loadEvent ->
                                    val result = loadEvent.target.asDynamic().result as? String
                                    if (result != null) {
                                        // For text files, use directly
                                        // For PDF/DOCX, we'd need a parser library
                                        if (fileType == "txt") {
                                            fileContent = result
                                        } else {
                                            // For now, indicate that parsing is needed
                                            fileContent = "Content from: $fileName\n\nNote: PDF and DOCX parsing requires additional setup. Please paste your resume content manually or use a text file."
                                        }
                                    }
                                    isProcessing = false
                                }
                                reader.onerror = {
                                    error = "Error reading file"
                                    isProcessing = false
                                }
                                reader.readAsText(file)
                            }
                        }
                    }
                }
                
                if (fileName != null) {
                    Div(attrs = { classes("form-group", "mt-md") }) {
                        Label(attrs = { classes("form-label") }) { Text("Resume Name") }
                        Input(InputType.Text) {
                            classes("form-input")
                            value(resumeName)
                            onInput { resumeName = it.value }
                        }
                    }
                    
                    Div(attrs = { classes("form-group") }) {
                        Label(attrs = { classes("form-label") }) { Text("Resume Content") }
                        TextArea {
                            classes("form-textarea")
                            value(fileContent)
                            onInput { fileContent = it.value }
                            style { property("min-height", "200px") }
                        }
                        P(attrs = { classes("form-helper") }) {
                            Text("Review and edit the extracted content as needed.")
                        }
                    }
                }
            }
            
            Div(attrs = { classes("modal-footer") }) {
                Button(attrs = {
                    classes("btn", "btn-secondary")
                    onClick { onClose() }
                }) {
                    Text("Cancel")
                }
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
                        Text("Processing...")
                    } else {
                        Text("Upload Resume")
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
    
    Div(attrs = { classes("modal-backdrop") }) {
        Div(attrs = { classes("modal") }) {
            Div(attrs = { classes("modal-header") }) {
                H3(attrs = { classes("modal-title") }) { Text("Import from LinkedIn") }
                Button(attrs = {
                    classes("modal-close")
                    onClick { onClose() }
                }) {
                    Text("X")
                }
            }
            
            Div(attrs = { classes("modal-body") }) {
                when (step) {
                    1 -> {
                        // Instructions
                        Div(attrs = { classes("linkedin-instructions") }) {
                            P(attrs = { classes("text-secondary", "mb-md") }) {
                                Text("Follow these steps to export your LinkedIn profile:")
                            }
                            
                            Ol(attrs = { 
                                style { 
                                    property("padding-left", "20px")
                                    property("margin-bottom", "var(--spacing-lg)")
                                }
                            }) {
                                Li(attrs = { classes("mb-sm") }) {
                                    Text("Go to your LinkedIn profile")
                                }
                                Li(attrs = { classes("mb-sm") }) {
                                    Text("Click 'More' button below your profile photo")
                                }
                                Li(attrs = { classes("mb-sm") }) {
                                    Text("Select 'Save to PDF'")
                                }
                                Li(attrs = { classes("mb-sm") }) {
                                    Text("Open the PDF and copy all the text")
                                }
                                Li(attrs = { classes("mb-sm") }) {
                                    Text("Paste it in the text area on the next step")
                                }
                            }
                            
                            Div(attrs = { classes("alert", "alert-info", "mb-md") }) {
                                B { Text("Alternative: ") }
                                Text("You can also manually copy your profile information directly from your LinkedIn page.")
                            }
                        }
                    }
                    2 -> {
                        // Paste profile content
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
                            Label(attrs = { classes("form-label") }) { Text("LinkedIn Profile Content") }
                            TextArea {
                                classes("form-textarea")
                                placeholder("Paste your LinkedIn profile content here...")
                                value(profileContent)
                                onInput { profileContent = it.value }
                                style { property("min-height", "300px") }
                            }
                            P(attrs = { classes("form-helper") }) {
                                Text("Include your headline, summary, experience, education, and skills.")
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
                    }) {
                        Text("Back")
                    }
                } else {
                    Button(attrs = {
                        classes("btn", "btn-secondary")
                        onClick { onClose() }
                    }) {
                        Text("Cancel")
                    }
                }
                
                if (step < 2) {
                    Button(attrs = {
                        classes("btn", "btn-primary")
                        onClick { step++ }
                    }) {
                        Text("Next")
                    }
                } else {
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
                    }) {
                        Text("Import Resume")
                    }
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
