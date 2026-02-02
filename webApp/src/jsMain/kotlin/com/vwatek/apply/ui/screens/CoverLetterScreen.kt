package com.vwatek.apply.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.vwatek.apply.domain.model.CoverLetter
import com.vwatek.apply.domain.model.CoverLetterTone
import com.vwatek.apply.domain.model.Resume
import com.vwatek.apply.presentation.coverletter.CoverLetterIntent
import com.vwatek.apply.presentation.coverletter.CoverLetterViewModel
import com.vwatek.apply.presentation.resume.ResumeViewModel
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*
import org.koin.core.context.GlobalContext

@Composable
fun CoverLetterScreen() {
    val viewModel = remember { GlobalContext.get().get<CoverLetterViewModel>() }
    val resumeViewModel = remember { GlobalContext.get().get<ResumeViewModel>() }
    val state by viewModel.state.collectAsState()
    val resumeState by resumeViewModel.state.collectAsState()
    
    var showGenerateModal by remember { mutableStateOf(false) }
    var selectedCoverLetter by remember { mutableStateOf<CoverLetter?>(null) }
    
    Div {
        // Header
        Div(attrs = { classes("flex", "justify-between", "items-center", "mb-lg") }) {
            Div {
                H1 { Text("Cover Letters") }
                P(attrs = { classes("text-secondary") }) {
                    Text("Generate tailored cover letters for your job applications.")
                }
            }
            Button(attrs = {
                classes("btn", "btn-primary", "btn-lg")
                onClick { showGenerateModal = true }
            }) {
                Text("+ Generate New")
            }
        }
        
        // Generated Cover Letter Preview
        state.generatedCoverLetter?.let { coverLetter ->
            Div(attrs = { classes("card", "mb-lg") }) {
                Div(attrs = { classes("flex", "justify-between", "items-center", "mb-md") }) {
                    Div {
                        H3(attrs = { classes("card-title") }) { Text("Generated Cover Letter") }
                        P(attrs = { classes("text-secondary", "text-sm") }) {
                            Text("${coverLetter.jobTitle} at ${coverLetter.companyName}")
                        }
                    }
                    Button(attrs = {
                        classes("btn", "btn-secondary", "btn-sm")
                        onClick { viewModel.onIntent(CoverLetterIntent.ClearGenerated) }
                    }) {
                        Text("Close")
                    }
                }
                
                Div(attrs = {
                    style {
                        property("white-space", "pre-wrap")
                        property("font-family", "serif")
                        property("line-height", "1.8")
                        property("padding", "var(--spacing-lg)")
                        property("background-color", "var(--color-surface-variant)")
                        property("border-radius", "var(--radius-md)")
                    }
                }) {
                    Text(coverLetter.content)
                }
                
                Div(attrs = { classes("flex", "gap-sm", "mt-md") }) {
                    Button(attrs = {
                        classes("btn", "btn-primary")
                        onClick {
                            // Copy to clipboard
                            kotlinx.browser.window.navigator.clipboard.writeText(coverLetter.content)
                        }
                    }) {
                        Text("Copy to Clipboard")
                    }
                    Button(attrs = { classes("btn", "btn-outline") }) {
                        Text("Download as PDF")
                    }
                }
            }
        }
        
        // Cover Letter List
        if (state.isLoading) {
            Div(attrs = { classes("flex", "justify-center", "mt-lg") }) {
                Div(attrs = { classes("spinner") })
            }
        } else if (state.coverLetters.isEmpty() && state.generatedCoverLetter == null) {
            Div(attrs = { classes("empty-state") }) {
                Div(attrs = {
                    ref { element ->
                        element.innerHTML = """
                            <svg width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                                <polyline points="22,6 12,13 2,6"/>
                            </svg>
                        """
                        onDispose { }
                    }
                })
                H3(attrs = { classes("empty-state-title") }) { Text("No cover letters yet") }
                P(attrs = { classes("empty-state-description") }) {
                    Text("Generate your first cover letter tailored to a specific job posting.")
                }
                Button(attrs = {
                    classes("btn", "btn-primary", "btn-lg")
                    onClick { showGenerateModal = true }
                }) {
                    Text("Generate Cover Letter")
                }
            }
        } else {
            Div(attrs = { classes("grid", "grid-2") }) {
                state.coverLetters.forEach { coverLetter ->
                    CoverLetterCard(
                        coverLetter = coverLetter,
                        onView = { selectedCoverLetter = coverLetter },
                        onDelete = {
                            viewModel.onIntent(CoverLetterIntent.DeleteCoverLetter(coverLetter.id))
                        }
                    )
                }
            }
        }
        
        // Generate Modal
        if (showGenerateModal) {
            GenerateCoverLetterModal(
                isGenerating = state.isGenerating,
                resumes = resumeState.resumes,
                onClose = { showGenerateModal = false },
                onGenerate = { resume, jobTitle, company, jobDesc, tone ->
                    viewModel.onIntent(CoverLetterIntent.GenerateCoverLetter(
                        resumeContent = resume,
                        jobTitle = jobTitle,
                        companyName = company,
                        jobDescription = jobDesc,
                        tone = tone
                    ))
                    showGenerateModal = false
                }
            )
        }
        
        // View Modal
        selectedCoverLetter?.let { coverLetter ->
            ViewCoverLetterModal(
                coverLetter = coverLetter,
                onClose = { selectedCoverLetter = null }
            )
        }
        
        // Error Toast
        state.error?.let { error ->
            Div(attrs = { classes("toast", "toast-error") }) {
                Text(error)
                Button(attrs = {
                    classes("btn", "btn-sm")
                    onClick { viewModel.onIntent(CoverLetterIntent.ClearError) }
                }) {
                    Text("Dismiss")
                }
            }
        }
    }
}

@Composable
private fun CoverLetterCard(
    coverLetter: CoverLetter,
    onView: () -> Unit,
    onDelete: () -> Unit
) {
    Div(attrs = { classes("card") }) {
        Div(attrs = { classes("card-header") }) {
            H3(attrs = { classes("card-title") }) { Text(coverLetter.jobTitle) }
            Span(attrs = { classes("badge", "badge-primary") }) {
                Text(coverLetter.tone.name.lowercase().replaceFirstChar { it.uppercase() })
            }
        }
        
        P(attrs = { classes("text-secondary", "mb-sm") }) {
            Text(coverLetter.companyName)
        }
        
        P(attrs = { 
            classes("text-sm", "mb-md")
            style {
                property("overflow", "hidden")
                property("text-overflow", "ellipsis")
                property("display", "-webkit-box")
                property("-webkit-line-clamp", "3")
                property("-webkit-box-orient", "vertical")
            }
        }) {
            Text(coverLetter.content.take(150) + "...")
        }
        
        Div(attrs = { classes("flex", "gap-sm") }) {
            Button(attrs = {
                classes("btn", "btn-primary")
                onClick { onView() }
            }) {
                Text("View")
            }
            Button(attrs = {
                classes("btn", "btn-outline")
                onClick {
                    kotlinx.browser.window.navigator.clipboard.writeText(coverLetter.content)
                }
            }) {
                Text("Copy")
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
private fun GenerateCoverLetterModal(
    isGenerating: Boolean,
    resumes: List<Resume>,
    onClose: () -> Unit,
    onGenerate: (resume: String, jobTitle: String, company: String, jobDesc: String, tone: CoverLetterTone) -> Unit
) {
    var selectedResumeId by remember { mutableStateOf<String?>(null) }
    var jobTitle by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var jobDescription by remember { mutableStateOf("") }
    var selectedTone by remember { mutableStateOf(CoverLetterTone.PROFESSIONAL) }
    
    val selectedResume = resumes.find { it.id == selectedResumeId }
    
    Div(attrs = { classes("modal-backdrop") }) {
        Div(attrs = { 
            classes("modal")
            style { property("max-width", "600px") }
        }) {
            Div(attrs = { classes("modal-header") }) {
                H3(attrs = { classes("modal-title") }) { Text("Generate Cover Letter") }
                Button(attrs = {
                    classes("modal-close")
                    onClick { onClose() }
                }) {
                    Text("X")
                }
            }
            
            Div(attrs = { classes("modal-body") }) {
                Div(attrs = { classes("grid", "grid-2", "mb-md") }) {
                    Div(attrs = { classes("form-group") }) {
                        Label(attrs = { classes("form-label") }) { Text("Job Title") }
                        Input(InputType.Text) {
                            classes("form-input")
                            placeholder("e.g., Software Engineer")
                            value(jobTitle)
                            onInput { jobTitle = it.value }
                        }
                    }
                    
                    Div(attrs = { classes("form-group") }) {
                        Label(attrs = { classes("form-label") }) { Text("Company Name") }
                        Input(InputType.Text) {
                            classes("form-input")
                            placeholder("e.g., Google")
                            value(companyName)
                            onInput { companyName = it.value }
                        }
                    }
                }
                
                Div(attrs = { classes("form-group") }) {
                    Label(attrs = { classes("form-label") }) { Text("Tone") }
                    Select(attrs = {
                        classes("form-select")
                        onChange { 
                            selectedTone = CoverLetterTone.valueOf(it.value!!)
                        }
                    }) {
                        CoverLetterTone.entries.forEach { tone ->
                            Option(
                                value = tone.name,
                                attrs = {
                                    if (tone == selectedTone) selected()
                                }
                            ) {
                                Text(tone.name.lowercase().replaceFirstChar { it.uppercase() })
                            }
                        }
                    }
                }
                
                Div(attrs = { classes("form-group") }) {
                    Label(attrs = { classes("form-label") }) { Text("Select Resume") }
                    if (resumes.isEmpty()) {
                        Div(attrs = { 
                            classes("form-helper")
                            style { 
                                property("padding", "var(--spacing-md)")
                                property("background-color", "var(--color-surface-variant)")
                                property("border-radius", "var(--radius-md)")
                                property("text-align", "center")
                            }
                        }) {
                            Text("No resumes uploaded yet. Please upload a resume on the Resume page first.")
                        }
                    } else {
                        Select(attrs = {
                            classes("form-select")
                            onChange { event ->
                                val value = event.target.value
                                selectedResumeId = if (value.isEmpty()) null else value
                            }
                        }) {
                            Option(value = "") {
                                Text("-- Select a resume --")
                            }
                            resumes.forEach { resume ->
                                Option(
                                    value = resume.id,
                                    attrs = {
                                        if (resume.id == selectedResumeId) selected()
                                    }
                                ) {
                                    Text(resume.name)
                                }
                            }
                        }
                        selectedResume?.let { resume ->
                            Div(attrs = { 
                                classes("mt-sm")
                                style { 
                                    property("padding", "var(--spacing-sm)")
                                    property("background-color", "var(--color-surface-variant)")
                                    property("border-radius", "var(--radius-sm)")
                                    property("font-size", "0.85rem")
                                }
                            }) {
                                Text("✓ ${resume.name} selected")
                            }
                        }
                    }
                }
                
                Div(attrs = { classes("form-group") }) {
                    Label(attrs = { classes("form-label") }) { Text("Job Description") }
                    TextArea {
                        classes("form-textarea")
                        placeholder("Paste the job description...")
                        value(jobDescription)
                        onInput { jobDescription = it.value }
                        style { property("min-height", "120px") }
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
                    if (isGenerating || selectedResume == null || jobTitle.isBlank() || companyName.isBlank() || jobDescription.isBlank()) {
                        attr("disabled", "true")
                    }
                    onClick { 
                        selectedResume?.let { resume ->
                            onGenerate(resume.content, jobTitle, companyName, jobDescription, selectedTone)
                        }
                    }
                }) {
                    if (isGenerating) {
                        Text("Generating...")
                    } else {
                        Text("Generate")
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewCoverLetterModal(
    coverLetter: CoverLetter,
    onClose: () -> Unit
) {
    Div(attrs = { classes("modal-backdrop") }) {
        Div(attrs = { 
            classes("modal")
            style { property("max-width", "700px") }
        }) {
            Div(attrs = { classes("modal-header") }) {
                Div {
                    H3(attrs = { classes("modal-title") }) { Text(coverLetter.jobTitle) }
                    P(attrs = { classes("text-secondary", "text-sm") }) {
                        Text(coverLetter.companyName)
                    }
                }
                Button(attrs = {
                    classes("modal-close")
                    onClick { onClose() }
                }) {
                    Text("X")
                }
            }
            
            Div(attrs = { classes("modal-body") }) {
                Div(attrs = {
                    style {
                        property("white-space", "pre-wrap")
                        property("font-family", "serif")
                        property("line-height", "1.8")
                    }
                }) {
                    Text(coverLetter.content)
                }
            }
            
            Div(attrs = { classes("modal-footer") }) {
                Button(attrs = {
                    classes("btn", "btn-primary")
                    onClick {
                        kotlinx.browser.window.navigator.clipboard.writeText(coverLetter.content)
                    }
                }) {
                    Text("Copy to Clipboard")
                }
                Button(attrs = {
                    classes("btn", "btn-secondary")
                    onClick { onClose() }
                }) {
                    Text("Close")
                }
            }
        }
    }
}
