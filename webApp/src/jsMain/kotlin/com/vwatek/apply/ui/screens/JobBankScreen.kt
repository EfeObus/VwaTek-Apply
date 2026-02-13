package com.vwatek.apply.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.vwatek.apply.domain.model.*
import com.vwatek.apply.presentation.jobbank.*
import com.vwatek.apply.i18n.*
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.koin.core.context.GlobalContext

/**
 * Phase 3: Job Bank Canada Search Screen for Web
 * Allows users to search and explore Job Bank Canada job listings
 */
@Composable
fun JobBankScreen() {
    val viewModel = remember { GlobalContext.get().get<JobBankViewModel>() }
    val state by viewModel.state.collectAsState()
    val strings = LocaleManager.strings
    
    var searchQuery by remember { mutableStateOf("") }
    var locationQuery by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    var selectedProvinceCode by remember { mutableStateOf<String?>(null) }
    var selectedJobId by remember { mutableStateOf<String?>(null) }
    
    Div {
        // Header
        Div(attrs = { classes("flex", "justify-between", "items-center", "mb-lg") }) {
            Div {
                H1 { Text(strings.jobBankTitle) }
                P(attrs = { classes("text-secondary") }) {
                    Text("Search jobs from the Government of Canada Job Bank")
                }
            }
            
            // Language toggle
            Div(attrs = { classes("flex", "gap-sm", "items-center") }) {
                LanguageSwitcher(
                    currentLocale = state.currentLocale,
                    onLocaleChange = { viewModel.setLanguage(it) }
                )
            }
        }
        
        // Search Card
        Div(attrs = { classes("card", "p-md", "mb-lg") }) {
            Div(attrs = { classes("grid", "grid-cols-1", "md:grid-cols-3", "gap-md") }) {
                // Job search input
                Div {
                    Label(attrs = { classes("form-label") }) { Text("Job Title or Keyword") }
                    Input(InputType.Text) {
                        classes("form-input")
                        value(searchQuery)
                        placeholder("e.g. Software Developer, Nurse...")
                        onInput { event -> searchQuery = event.value }
                        onKeyDown { event ->
                            if (event.key == "Enter") {
                                viewModel.searchJobs(
                                    query = searchQuery.ifBlank { null },
                                    location = locationQuery.ifBlank { null },
                                    provinceCode = selectedProvinceCode
                                )
                            }
                        }
                    }
                }
                
                // Location input
                Div {
                    Label(attrs = { classes("form-label") }) { Text("Location") }
                    Input(InputType.Text) {
                        classes("form-input")
                        value(locationQuery)
                        placeholder("City or postal code...")
                        onInput { event -> locationQuery = event.value }
                    }
                }
                
                // Province filter
                Div {
                    Label(attrs = { classes("form-label") }) { Text("Province/Territory") }
                    Select(attrs = {
                        classes("form-select")
                        onChange { event ->
                            selectedProvinceCode = event.value?.ifBlank { null }
                        }
                    }) {
                        Option(value = "") { Text("All Provinces") }
                        state.provinces.forEach { province ->
                            Option(value = province.code) {
                                Text("${province.code} - ${province.name}")
                            }
                        }
                    }
                }
            }
            
            // Search button
            Div(attrs = { classes("mt-md", "flex", "gap-sm") }) {
                Button(attrs = {
                    classes("btn", "btn-primary")
                    onClick {
                        viewModel.searchJobs(
                            query = searchQuery.ifBlank { null },
                            location = locationQuery.ifBlank { null },
                            provinceCode = selectedProvinceCode
                        )
                    }
                    if (state.isSearching) attr("disabled", "true")
                }) {
                    if (state.isSearching) {
                        Span(attrs = { classes("spinner", "spinner-sm", "mr-sm") })
                    }
                    Text("Search Jobs")
                }
                
                if (state.hasActiveFilters || searchQuery.isNotBlank() || locationQuery.isNotBlank()) {
                    Button(attrs = {
                        classes("btn", "btn-outline")
                        onClick {
                            searchQuery = ""
                            locationQuery = ""
                            selectedProvinceCode = null
                            viewModel.clearSearch()
                        }
                    }) {
                        Text("Clear")
                    }
                }
            }
        }
        
        // Main content
        Div(attrs = { classes("grid", "grid-cols-1", "lg:grid-cols-3", "gap-lg") }) {
            // Left: Job List
            Div(attrs = { classes("lg:col-span-2") }) {
                
                // Results header
                if (state.hasSearchResults) {
                    Div(attrs = { classes("flex", "justify-between", "items-center", "mb-md") }) {
                        Span(attrs = { classes("text-secondary") }) {
                            Text("${state.totalResults} jobs found")
                        }
                    }
                }
                
                // Loading state
                if (state.isSearching && state.searchResults.isEmpty()) {
                    Div(attrs = { classes("flex", "justify-center", "p-xl") }) {
                        Span(attrs = { classes("spinner") })
                    }
                }
                
                // Job list
                if (state.hasSearchResults) {
                    Div(attrs = { classes("space-y-md") }) {
                        state.searchResults.forEach { job ->
                            JobCard(
                                job = job,
                                isSelected = selectedJobId == job.id,
                                onClick = {
                                    selectedJobId = job.id
                                    viewModel.loadJobDetails(job.id)
                                }
                            )
                        }
                        
                        // Load more
                        if (state.hasMoreResults) {
                            Div(attrs = { classes("flex", "justify-center", "mt-md") }) {
                                Button(attrs = {
                                    classes("btn", "btn-outline")
                                    onClick { viewModel.loadMoreResults() }
                                    if (state.isLoadingMore) attr("disabled", "true")
                                }) {
                                    if (state.isLoadingMore) {
                                        Span(attrs = { classes("spinner", "spinner-sm", "mr-sm") })
                                    }
                                    Text("Load More")
                                }
                            }
                        }
                    }
                }
                
                // Trending jobs (when no search)
                if (state.showTrending) {
                    H3(attrs = { classes("mb-md") }) { Text("Trending Jobs") }
                    
                    if (state.isLoadingTrending) {
                        Div(attrs = { classes("flex", "justify-center", "p-xl") }) {
                            Span(attrs = { classes("spinner") })
                        }
                    } else {
                        Div(attrs = { classes("space-y-md") }) {
                            state.trendingJobs.forEach { job ->
                                JobCard(
                                    job = job,
                                    isSelected = selectedJobId == job.id,
                                    onClick = {
                                        selectedJobId = job.id
                                        viewModel.loadJobDetails(job.id)
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Empty state
                if (!state.isSearching && !state.hasSearchResults && !state.showTrending) {
                    Div(attrs = { classes("card", "p-xl", "text-center") }) {
                        Div(attrs = { classes("text-4xl", "mb-md") }) { Text("🍁") }
                        H3 { Text("Search Job Bank Canada") }
                        P(attrs = { classes("text-secondary") }) {
                            Text("Enter a job title, keyword, or location to search thousands of job listings from the Government of Canada Job Bank.")
                        }
                    }
                }
            }
            
            // Right: Job Details Panel
            Div(attrs = { classes("hidden", "lg:block") }) {
                if (state.isLoadingDetails) {
                    Div(attrs = { classes("card", "p-xl", "flex", "justify-center") }) {
                        Span(attrs = { classes("spinner") })
                    }
                } else if (state.selectedJob != null) {
                    JobDetailsPanel(
                        job = state.selectedJob!!,
                        onSave = { /* Save to tracker */ },
                        onApply = { /* Open job URL */ }
                    )
                } else {
                    Div(attrs = { classes("card", "p-lg", "text-center", "text-secondary") }) {
                        Div(attrs = { classes("text-3xl", "mb-md") }) { Text("📋") }
                        P { Text("Select a job to view details") }
                    }
                }
            }
        }
    }
}

@Composable
private fun JobCard(
    job: JobBankJob,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Div(attrs = {
        classes("card", "p-md", "cursor-pointer", "hover:shadow-md", "transition-shadow")
        if (isSelected) classes("border-primary", "border-2")
        onClick { onClick() }
    }) {
        // Title and employer
        Div(attrs = { classes("flex", "justify-between", "items-start") }) {
            Div {
                H4(attrs = { classes("font-semibold", "mb-xs") }) { Text(job.title) }
                P(attrs = { classes("text-secondary") }) { Text(job.employer) }
            }
            Button(attrs = {
                classes("btn", "btn-icon", "btn-ghost")
                onClick { it.stopPropagation() }
            }) {
                Text("🔖")
            }
        }
        
        // Location and salary
        Div(attrs = { classes("flex", "gap-md", "mt-sm", "text-sm", "text-secondary") }) {
            Div(attrs = { classes("flex", "items-center", "gap-xs") }) {
                Text("📍")
                Text(job.location.displayName)
            }
            
            job.salary?.let { salary ->
                Div(attrs = { classes("flex", "items-center", "gap-xs") }) {
                    Text("💰")
                    Text(salary.displayRange)
                }
            }
        }
        
        // NOC code chip
        job.nocCode?.let { noc ->
            Div(attrs = { classes("mt-sm") }) {
                Span(attrs = { classes("badge", "badge-outline") }) {
                    Text("NOC: $noc")
                }
            }
        }
        
        // Posted date
        Div(attrs = { classes("mt-sm", "text-xs", "text-secondary") }) {
            Text("Posted: ${job.postingDate}")
        }
    }
}

@Composable
private fun JobDetailsPanel(
    job: JobBankJob,
    onSave: () -> Unit,
    onApply: () -> Unit
) {
    Div(attrs = { classes("card", "p-lg", "sticky", "top-md") }) {
        // Header
        H2(attrs = { classes("text-xl", "font-bold", "mb-xs") }) { Text(job.title) }
        P(attrs = { classes("text-primary", "font-medium", "mb-md") }) { Text(job.employer) }
        
        // Location and salary
        Div(attrs = { classes("space-y-sm", "mb-md") }) {
            Div(attrs = { classes("flex", "items-center", "gap-sm") }) {
                Text("📍")
                Text(job.location.displayName)
            }
            
            job.salary?.let { salary ->
                Div(attrs = { classes("flex", "items-center", "gap-sm") }) {
                    Text("💰")
                    Text(salary.displayRange)
                }
            }
            
            job.nocCode?.let { noc ->
                Div(attrs = { classes("flex", "items-center", "gap-sm") }) {
                    Text("🏷️")
                    Text("NOC Code: $noc")
                }
            }
            
            job.vacancies.let { count ->
                Div(attrs = { classes("flex", "items-center", "gap-sm") }) {
                    Text("👥")
                    Text("$count vacancy${if (count > 1) "ies" else ""}")
                }
            }
        }
        
        Hr()
        
        // Description
        Div(attrs = { classes("my-md") }) {
            H4(attrs = { classes("font-semibold", "mb-sm") }) { Text("Description") }
            P(attrs = { classes("text-secondary", "text-sm") }) { Text(job.description) }
        }
        
        // Requirements
        if (job.requirements.isNotEmpty()) {
            Div(attrs = { classes("my-md") }) {
                H4(attrs = { classes("font-semibold", "mb-sm") }) { Text("Requirements") }
                Ul(attrs = { classes("list-disc", "list-inside", "text-sm", "text-secondary") }) {
                    job.requirements.forEach { req ->
                        Li { Text(req) }
                    }
                }
            }
        }
        
        // Benefits
        if (job.benefits.isNotEmpty()) {
            Div(attrs = { classes("my-md") }) {
                H4(attrs = { classes("font-semibold", "mb-sm") }) { Text("Benefits") }
                Ul(attrs = { classes("list-disc", "list-inside", "text-sm", "text-secondary") }) {
                    job.benefits.forEach { benefit ->
                        Li { Text(benefit) }
                    }
                }
            }
        }
        
        Hr()
        
        // Action buttons
        Div(attrs = { classes("flex", "gap-sm", "mt-md") }) {
            Button(attrs = {
                classes("btn", "btn-outline", "flex-1")
                onClick { onSave() }
            }) {
                Text("🔖 Save")
            }
            
            Button(attrs = {
                classes("btn", "btn-primary", "flex-1")
                onClick { onApply() }
            }) {
                Text("Apply →")
            }
        }
        
        // Posted date
        Div(attrs = { classes("mt-md", "text-xs", "text-secondary", "text-center") }) {
            Text("Posted: ${job.postingDate}")
            job.expiryDate?.let { Text(" • Expires: $it") }
        }
    }
}
