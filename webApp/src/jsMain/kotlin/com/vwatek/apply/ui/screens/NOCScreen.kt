package com.vwatek.apply.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.vwatek.apply.domain.model.*
import com.vwatek.apply.presentation.noc.*
import com.vwatek.apply.i18n.*
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.koin.core.context.GlobalContext

/**
 * Phase 3: NOC (National Occupational Classification) Screen for Web
 * Allows users to search and explore Canadian NOC codes
 */
@Composable
fun NOCScreen() {
    val viewModel = remember { GlobalContext.get().get<NOCViewModel>() }
    val state by viewModel.state.collectAsState()
    val strings = LocaleManager.strings
    
    var searchQuery by remember { mutableStateOf("") }
    var showTeerFilter by remember { mutableStateOf(false) }
    var showCategoryFilter by remember { mutableStateOf(false) }
    
    Div {
        // Header
        Div(attrs = { classes("flex", "justify-between", "items-center", "mb-lg") }) {
            Div {
                H1 { Text(strings.nocTitle) }
                P(attrs = { classes("text-secondary") }) {
                    Text("Search and explore Canadian National Occupational Classification codes")
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
        
        // Search Bar
        Div(attrs = { classes("card", "p-md", "mb-md") }) {
            Div(attrs = { classes("flex", "gap-md", "items-end") }) {
                // Search input
                Div(attrs = { style { flexGrow(1) } }) {
                    Label(attrs = { classes("form-label") }) { Text(strings.nocSearch) }
                    Input(InputType.Text) {
                        classes("form-input")
                        value(searchQuery)
                        placeholder("Search by code, title, or keyword...")
                        onInput { event -> 
                            searchQuery = event.value
                        }
                        onKeyDown { event ->
                            if (event.key == "Enter") {
                                viewModel.searchNOCCodes(
                                    query = searchQuery,
                                    teerLevels = state.selectedTeerLevels,
                                    category = state.selectedCategory
                                )
                            }
                        }
                    }
                }
                
                // TEER Filter
                Div(attrs = { classes("dropdown") }) {
                    Button(attrs = {
                        classes("btn", "btn-outline")
                        onClick { showTeerFilter = !showTeerFilter }
                    }) {
                        Text(strings.nocTeerLevel)
                        if (!state.selectedTeerLevels.isNullOrEmpty()) {
                            Span(attrs = { classes("badge", "badge-primary", "ml-xs") }) {
                                Text("${state.selectedTeerLevels?.size}")
                            }
                        }
                    }
                    
                    if (showTeerFilter) {
                        Div(attrs = { classes("dropdown-menu", "show") }) {
                            state.teerLevels.forEach { teer ->
                                Label(attrs = { classes("dropdown-item", "checkbox-item") }) {
                                    Input(InputType.Checkbox) {
                                        checked(state.selectedTeerLevels?.contains(teer.level) == true)
                                        onChange { 
                                            val current = state.selectedTeerLevels?.toMutableList() ?: mutableListOf()
                                            if (current.contains(teer.level)) {
                                                current.remove(teer.level)
                                            } else {
                                                current.add(teer.level)
                                            }
                                            viewModel.setTEERFilter(current.ifEmpty { null })
                                        }
                                    }
                                    Span { Text("TEER ${teer.level}: ${teer.getTitle(state.currentLocale)}") }
                                }
                            }
                        }
                    }
                }
                
                // Category Filter
                Div(attrs = { classes("dropdown") }) {
                    Button(attrs = {
                        classes("btn", "btn-outline")
                        onClick { showCategoryFilter = !showCategoryFilter }
                    }) {
                        Text("Category")
                        if (state.selectedCategory != null) {
                            Span(attrs = { classes("badge", "badge-primary", "ml-xs") }) {
                                Text("1")
                            }
                        }
                    }
                    
                    if (showCategoryFilter) {
                        Div(attrs = { classes("dropdown-menu", "show") }) {
                            Div(attrs = {
                                classes("dropdown-item")
                                onClick { 
                                    viewModel.setCategoryFilter(null)
                                    showCategoryFilter = false
                                }
                            }) {
                                Text("All Categories")
                            }
                            state.categories.forEach { cat ->
                                Div(attrs = {
                                    classes("dropdown-item", if (state.selectedCategory == cat.category) "active" else "")
                                    onClick { 
                                        viewModel.setCategoryFilter(cat.category)
                                        showCategoryFilter = false
                                    }
                                }) {
                                    Text("${cat.category} - ${cat.name}")
                                }
                            }
                        }
                    }
                }
                
                // Search Button
                Button(attrs = {
                    classes("btn", "btn-primary")
                    onClick {
                        viewModel.searchNOCCodes(
                            query = searchQuery,
                            teerLevels = state.selectedTeerLevels,
                            category = state.selectedCategory
                        )
                    }
                }) {
                    Text(strings.search)
                }
                
                // Clear Button
                if (searchQuery.isNotBlank() || state.selectedTeerLevels != null || state.selectedCategory != null) {
                    Button(attrs = {
                        classes("btn", "btn-outline")
                        onClick {
                            searchQuery = ""
                            viewModel.setTEERFilter(null)
                            viewModel.setCategoryFilter(null)
                            viewModel.clearSearch()
                        }
                    }) {
                        Text(strings.clear)
                    }
                }
            }
        }
        
        // Content Area
        Div(attrs = { classes("flex", "gap-lg") }) {
            // Search Results
            Div(attrs = { style { flexGrow(1) } }) {
                if (state.isSearching) {
                    Div(attrs = { classes("flex", "justify-center", "mt-lg") }) {
                        Div(attrs = { classes("spinner") })
                    }
                } else if (state.searchError != null) {
                    Div(attrs = { classes("alert", "alert-error") }) {
                        Text(state.searchError!!)
                    }
                } else if (state.searchResults.isEmpty() && state.lastSearchQuery.isNotBlank()) {
                    Div(attrs = { classes("text-center", "mt-lg", "text-secondary") }) {
                        P { Text("No NOC codes found for \"${state.lastSearchQuery}\"") }
                    }
                } else if (state.searchResults.isNotEmpty()) {
                    Div(attrs = { classes("mb-md", "text-secondary") }) {
                        Text("${state.totalResults} results found")
                    }
                    
                    Div(attrs = { classes("space-y-sm") }) {
                        state.searchResults.forEach { noc ->
                            NOCResultCard(
                                noc = noc,
                                currentLocale = state.currentLocale,
                                isSelected = state.selectedNOCDetails?.noc?.code == noc.code,
                                onClick = { viewModel.loadNOCDetails(noc.code) }
                            )
                        }
                    }
                    
                    // Load More
                    if (state.hasMoreResults) {
                        Div(attrs = { classes("flex", "justify-center", "mt-md") }) {
                            Button(attrs = {
                                classes("btn", "btn-outline")
                                disabled(state.isLoadingMore)
                                onClick { viewModel.loadMoreResults() }
                            }) {
                                if (state.isLoadingMore) {
                                    Span(attrs = { classes("spinner-sm", "mr-xs") })
                                }
                                Text("Load More")
                            }
                        }
                    }
                } else {
                    // Empty State / Initial
                    TEEROverviewGrid(teerLevels = state.teerLevels, currentLocale = state.currentLocale)
                }
            }
            
            // Details Panel
            if (state.selectedNOCDetails != null || state.isLoadingDetails) {
                Div(attrs = { 
                    classes("card", "p-md")
                    style { 
                        width(450.px)
                        minWidth(400.px)
                        maxHeight(80.vh)
                        overflow("auto")
                    }
                }) {
                    if (state.isLoadingDetails) {
                        Div(attrs = { classes("flex", "justify-center", "p-lg") }) {
                            Div(attrs = { classes("spinner") })
                        }
                    } else {
                        state.selectedNOCDetails?.let { details ->
                            NOCDetailsPanel(
                                details = details,
                                provincialDemand = state.provincialDemand,
                                immigrationPathways = state.immigrationPathways,
                                currentLocale = state.currentLocale,
                                onLoadDemand = { viewModel.loadProvincialDemand(details.noc.code) },
                                onLoadPathways = { viewModel.loadImmigrationPathways(details.noc.code) },
                                onClose = { viewModel.clearSelectedDetails() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageSwitcher(
    currentLocale: Locale,
    onLocaleChange: (Locale) -> Unit
) {
    Div(attrs = { classes("btn-group") }) {
        Button(attrs = {
            classes("btn", "btn-sm", if (currentLocale == Locale.ENGLISH) "btn-primary" else "btn-outline")
            onClick { onLocaleChange(Locale.ENGLISH) }
        }) {
            Text("EN")
        }
        Button(attrs = {
            classes("btn", "btn-sm", if (currentLocale == Locale.FRENCH) "btn-primary" else "btn-outline")
            onClick { onLocaleChange(Locale.FRENCH) }
        }) {
            Text("FR")
        }
    }
}

@Composable
private fun NOCResultCard(
    noc: NOCCode,
    currentLocale: Locale,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Div(attrs = {
        classes("card", "card-hover", "p-md", "cursor-pointer")
        if (isSelected) classes("border-primary")
        onClick { onClick() }
    }) {
        Div(attrs = { classes("flex", "justify-between", "items-start") }) {
            Div {
                Div(attrs = { classes("flex", "items-center", "gap-sm", "mb-xs") }) {
                    Span(attrs = { classes("badge", "badge-secondary") }) {
                        Text(noc.code)
                    }
                    Span(attrs = { classes("badge", getTeerBadgeClass(noc.teerLevel)) }) {
                        Text("TEER ${noc.teerLevel}")
                    }
                }
                H4(attrs = { classes("mb-xs") }) {
                    Text(if (currentLocale == Locale.FRENCH) noc.titleFr else noc.titleEn)
                }
                P(attrs = { classes("text-secondary", "text-sm") }) {
                    val description = if (currentLocale == Locale.FRENCH) noc.descriptionFr else noc.descriptionEn
                    Text(description.take(150) + if (description.length > 150) "..." else "")
                }
            }
            Span(attrs = { classes("text-secondary") }) {
                Text("→")
            }
        }
    }
}

@Composable
private fun TEEROverviewGrid(teerLevels: List<TEERLevelInfo>, currentLocale: Locale) {
    Div {
        H3(attrs = { classes("mb-md") }) { Text("TEER Classification System") }
        P(attrs = { classes("text-secondary", "mb-lg") }) {
            Text("The Training, Education, Experience and Responsibilities (TEER) system classifies occupations by the nature of training, education, and experience required.")
        }
        
        Div(attrs = { classes("grid", "grid-cols-2", "gap-md") }) {
            teerLevels.forEach { teer ->
                Div(attrs = { classes("card", "p-md") }) {
                    Div(attrs = { classes("flex", "items-center", "gap-sm", "mb-sm") }) {
                        Span(attrs = { classes("badge", getTeerBadgeClass(teer.level), "badge-lg") }) {
                            Text("TEER ${teer.level}")
                        }
                    }
                    H4(attrs = { classes("mb-xs") }) {
                        Text(teer.getTitle(currentLocale))
                    }
                    P(attrs = { classes("text-secondary", "text-sm") }) {
                        Text(teer.educationRequirement)
                    }
                }
            }
        }
    }
}

@Composable
private fun NOCDetailsPanel(
    details: NOCDetails,
    provincialDemand: List<ProvincialDemandInfo>,
    immigrationPathways: List<ImmigrationPathwayInfo>,
    currentLocale: Locale,
    onLoadDemand: () -> Unit,
    onLoadPathways: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocaleManager.strings
    val noc = details.noc
    
    Div {
        // Header
        Div(attrs = { classes("flex", "justify-between", "items-start", "mb-md") }) {
            Div {
                Div(attrs = { classes("flex", "items-center", "gap-sm", "mb-sm") }) {
                    Span(attrs = { classes("badge", "badge-secondary", "badge-lg") }) {
                        Text(noc.code)
                    }
                    Span(attrs = { classes("badge", getTeerBadgeClass(noc.teerLevel)) }) {
                        Text("TEER ${noc.teerLevel}")
                    }
                </Div>
                H3 { Text(if (currentLocale == Locale.FRENCH) noc.titleFr else noc.titleEn) }
            }
            Button(attrs = {
                classes("btn", "btn-icon", "btn-ghost")
                onClick { onClose() }
            }) {
                Text("×")
            }
        }
        
        // Description
        Div(attrs = { classes("mb-md") }) {
            P(attrs = { classes("text-secondary") }) {
                Text(if (currentLocale == Locale.FRENCH) noc.descriptionFr else noc.descriptionEn)
            }
        }
        
        // Main Duties
        if (details.mainDuties.isNotEmpty()) {
            Div(attrs = { classes("mb-md") }) {
                H5(attrs = { classes("mb-sm") }) { Text(strings.nocMainDuties) }
                Ul(attrs = { classes("list-disc", "pl-md") }) {
                    details.mainDuties.take(5).forEach { duty ->
                        Li {
                            Text(if (currentLocale == Locale.FRENCH) duty.dutyFr else duty.dutyEn)
                        }
                    }
                }
            }
        }
        
        // Employment Requirements
        if (details.employmentRequirements.isNotEmpty()) {
            Div(attrs = { classes("mb-md") }) {
                H5(attrs = { classes("mb-sm") }) { Text(strings.nocRequirements) }
                Ul(attrs = { classes("list-disc", "pl-md") }) {
                    details.employmentRequirements.forEach { req ->
                        Li {
                            Text(if (currentLocale == Locale.FRENCH) req.requirementFr else req.requirementEn)
                        }
                    }
                }
            }
        }
        
        // Example Job Titles
        details.additionalInfo?.let { info ->
            Div(attrs = { classes("mb-md") }) {
                H5(attrs = { classes("mb-sm") }) { Text(strings.nocExampleTitles) }
                Div(attrs = { classes("flex", "flex-wrap", "gap-xs") }) {
                    val titles = if (currentLocale == Locale.FRENCH) info.exampleTitlesFr else info.exampleTitlesEn
                    titles.take(10).forEach { title ->
                        Span(attrs = { classes("badge", "badge-outline") }) {
                            Text(title)
                        }
                    }
                }
            }
        }
        
        // Provincial Demand Section
        Div(attrs = { classes("mb-md") }) {
            Div(attrs = { classes("flex", "justify-between", "items-center", "mb-sm") }) {
                H5 { Text(strings.nocProvincialDemand) }
                if (provincialDemand.isEmpty()) {
                    Button(attrs = {
                        classes("btn", "btn-sm", "btn-outline")
                        onClick { onLoadDemand() }
                    }) {
                        Text("Load Data")
                    }
                }
            }
            
            if (provincialDemand.isNotEmpty()) {
                Div(attrs = { classes("space-y-xs") }) {
                    provincialDemand.forEach { demand ->
                        Div(attrs = { classes("flex", "justify-between", "items-center", "p-sm", "bg-surface", "rounded") }) {
                            Span { Text(demand.provinceCode) }
                            Div(attrs = { classes("flex", "gap-md", "items-center") }) {
                                Span(attrs = { classes("badge", getDemandBadgeClass(demand.demandLevel)) }) {
                                    Text(demand.demandLevel)
                                }
                                Span(attrs = { classes("text-sm") }) {
                                    Text(demand.formatSalary())
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Immigration Pathways Section
        Div(attrs = { classes("mb-md") }) {
            Div(attrs = { classes("flex", "justify-between", "items-center", "mb-sm") }) {
                H5 { Text(strings.nocImmigrationPathways) }
                if (immigrationPathways.isEmpty()) {
                    Button(attrs = {
                        classes("btn", "btn-sm", "btn-outline")
                        onClick { onLoadPathways() }
                    }) {
                        Text("Load Data")
                    }
                }
            }
            
            if (immigrationPathways.isNotEmpty()) {
                Div(attrs = { classes("space-y-xs") }) {
                    immigrationPathways.forEach { pathway ->
                        Div(attrs = { classes("p-sm", "bg-surface", "rounded") }) {
                            Div(attrs = { classes("flex", "justify-between", "items-center") }) {
                                Span(attrs = { classes("font-medium") }) { Text(pathway.pathwayName) }
                                Span(attrs = { 
                                    classes("badge", if (pathway.isEligible) "badge-success" else "badge-error") 
                                }) {
                                    Text(if (pathway.isEligible) "Eligible" else "Not Eligible")
                                }
                            }
                            pathway.provinceCode?.let { prov ->
                                Span(attrs = { classes("text-secondary", "text-sm") }) {
                                    Text("Province: $prov")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getTeerBadgeClass(level: Int): String = when (level) {
    0 -> "badge-purple"
    1 -> "badge-blue"
    2 -> "badge-teal"
    3 -> "badge-green"
    4 -> "badge-yellow"
    5 -> "badge-orange"
    else -> "badge-secondary"
}

private fun getDemandBadgeClass(demandLevel: String): String = when (demandLevel.uppercase()) {
    "HIGH" -> "badge-success"
    "MEDIUM" -> "badge-warning"
    "LOW" -> "badge-error"
    else -> "badge-secondary"
}
