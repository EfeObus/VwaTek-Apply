package com.vwatek.apply.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vwatek.apply.domain.model.*
import com.vwatek.apply.presentation.noc.*
import com.vwatek.apply.i18n.*
import org.koin.compose.koinInject

/**
 * Phase 3: NOC (National Occupational Classification) Screen
 * Allows users to search and explore Canadian NOC codes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NOCScreen(
    onNavigateBack: () -> Unit = {}
) {
    val viewModel: NOCViewModel = koinInject()
    val state by viewModel.state.collectAsState()
    val strings = LocaleManager.strings
    
    var searchQuery by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showDetailsSheet by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(state.searchError) {
        state.searchError?.let {
            snackbarHostState.showSnackbar(it)
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(strings.nocTitle) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Language toggle
                    IconButton(onClick = { viewModel.toggleLanguage() }) {
                        Text(
                            text = if (state.currentLocale == Locale.ENGLISH) "FR" else "EN",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Filter button
                    IconButton(onClick = { showFilterSheet = true }) {
                        BadgedBox(
                            badge = {
                                if (state.selectedTeerLevels != null || state.selectedCategory != null) {
                                    Badge { Text("!") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.FilterList, "Filter")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = {
                    viewModel.searchNOCCodes(
                        query = searchQuery,
                        teerLevels = state.selectedTeerLevels,
                        category = state.selectedCategory
                    )
                },
                active = false,
                onActiveChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(strings.nocSearch) },
                leadingIcon = { Icon(Icons.Default.Search, "Search") },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            viewModel.clearSearch()
                        }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                }
            ) {}
            
            // Content
            if (state.isSearching) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.searchResults.isEmpty() && state.lastSearchQuery.isBlank()) {
                // Show TEER overview
                TEEROverviewContent(
                    teerLevels = state.teerLevels,
                    currentLocale = state.currentLocale,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            } else {
                // Show search results
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.searchResults.isNotEmpty()) {
                        item {
                            Text(
                                "${state.totalResults} results found",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (state.lastSearchQuery.isNotBlank()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.SearchOff,
                                        "No results",
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "No NOC codes found for \"${state.lastSearchQuery}\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    
                    items(state.searchResults) { noc ->
                        NOCResultCard(
                            noc = noc,
                            currentLocale = state.currentLocale,
                            onClick = {
                                viewModel.loadNOCDetails(noc.code)
                                showDetailsSheet = true
                            }
                        )
                    }
                    
                    if (state.hasMoreResults) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (state.isLoadingMore) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else {
                                    TextButton(onClick = { viewModel.loadMoreResults() }) {
                                        Text("Load More")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Filter Bottom Sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false }
        ) {
            FilterSheetContent(
                teerLevels = state.teerLevels,
                categories = state.categories,
                selectedTeerLevels = state.selectedTeerLevels,
                selectedCategory = state.selectedCategory,
                currentLocale = state.currentLocale,
                onTeerFilterChange = { viewModel.setTEERFilter(it) },
                onCategoryFilterChange = { viewModel.setCategoryFilter(it) },
                onApply = {
                    viewModel.searchNOCCodes(
                        query = searchQuery,
                        teerLevels = state.selectedTeerLevels,
                        category = state.selectedCategory
                    )
                    showFilterSheet = false
                },
                onClear = {
                    viewModel.setTEERFilter(null)
                    viewModel.setCategoryFilter(null)
                }
            )
        }
    }
    
    // Details Bottom Sheet
    if (showDetailsSheet && (state.selectedNOCDetails != null || state.isLoadingDetails)) {
        ModalBottomSheet(
            onDismissRequest = {
                showDetailsSheet = false
                viewModel.clearSelectedDetails()
            }
        ) {
            DetailsSheetContent(
                details = state.selectedNOCDetails,
                isLoading = state.isLoadingDetails,
                provincialDemand = state.provincialDemand,
                immigrationPathways = state.immigrationPathways,
                currentLocale = state.currentLocale,
                onLoadDemand = { state.selectedNOCDetails?.let { viewModel.loadProvincialDemand(it.noc.code) } },
                onLoadPathways = { state.selectedNOCDetails?.let { viewModel.loadImmigrationPathways(it.noc.code) } }
            )
        }
    }
}

@Composable
private fun TEEROverviewContent(
    teerLevels: List<TEERLevelInfo>,
    currentLocale: Locale,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        item {
            Text(
                "TEER Classification System",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "The Training, Education, Experience and Responsibilities (TEER) system classifies occupations by the nature of training, education, and experience required.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
        }
        
        items(teerLevels) { teer ->
            TEERCard(teer = teer, currentLocale = currentLocale)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TEERCard(
    teer: TEERLevelInfo,
    currentLocale: Locale
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = getTeerColor(teer.level).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = getTeerColor(teer.level)
            ) {
                Text(
                    "TEER ${teer.level}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    teer.getTitle(currentLocale),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    teer.educationRequirement,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NOCResultCard(
    noc: NOCCode,
    currentLocale: Locale,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        noc.code,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = getTeerColor(noc.teerLevel)
                ) {
                    Text(
                        "TEER ${noc.teerLevel}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                if (currentLocale == Locale.FRENCH) noc.titleFr else noc.titleEn,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(Modifier.height(4.dp))
            
            val description = if (currentLocale == Locale.FRENCH) noc.descriptionFr else noc.descriptionEn
            Text(
                description.take(120) + if (description.length > 120) "..." else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FilterSheetContent(
    teerLevels: List<TEERLevelInfo>,
    categories: List<NOCCategoryInfo>,
    selectedTeerLevels: List<Int>?,
    selectedCategory: String?,
    currentLocale: Locale,
    onTeerFilterChange: (List<Int>?) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            "Filter NOC Codes",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(Modifier.height(16.dp))
        
        // TEER Level Filter
        Text(
            "TEER Level",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        
        teerLevels.forEach { teer ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val current = selectedTeerLevels?.toMutableList() ?: mutableListOf()
                        if (current.contains(teer.level)) {
                            current.remove(teer.level)
                        } else {
                            current.add(teer.level)
                        }
                        onTeerFilterChange(current.ifEmpty { null })
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = selectedTeerLevels?.contains(teer.level) == true,
                    onCheckedChange = null
                )
                Spacer(Modifier.width(8.dp))
                Text("TEER ${teer.level}: ${teer.getTitle(currentLocale)}")
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Category Filter
        Text(
            "Category",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        
        LazyColumn(
            modifier = Modifier.height(200.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCategoryFilterChange(null) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedCategory == null,
                        onClick = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("All Categories")
                }
            }
            
            items(categories) { cat ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCategoryFilterChange(cat.category) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedCategory == cat.category,
                        onClick = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("${cat.category} - ${cat.name}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onClear,
                modifier = Modifier.weight(1f)
            ) {
                Text("Clear")
            }
            
            Button(
                onClick = onApply,
                modifier = Modifier.weight(1f)
            ) {
                Text("Apply")
            }
        }
        
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun DetailsSheetContent(
    details: NOCDetails?,
    isLoading: Boolean,
    provincialDemand: List<ProvincialDemandInfo>,
    immigrationPathways: List<ImmigrationPathwayInfo>,
    currentLocale: Locale,
    onLoadDemand: () -> Unit,
    onLoadPathways: () -> Unit
) {
    val strings = LocaleManager.strings
    
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxWidth().height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (details != null) {
        val noc = details.noc
        
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            noc.code,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = getTeerColor(noc.teerLevel)
                    ) {
                        Text(
                            "TEER ${noc.teerLevel}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }
            }
            
            item {
                Text(
                    if (currentLocale == Locale.FRENCH) noc.titleFr else noc.titleEn,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                Text(
                    if (currentLocale == Locale.FRENCH) noc.descriptionFr else noc.descriptionEn,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Main Duties
            if (details.mainDuties.isNotEmpty()) {
                item {
                    Text(
                        strings.nocMainDuties,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                items(details.mainDuties.take(5)) { duty ->
                    Row {
                        Text("• ", fontWeight = FontWeight.Bold)
                        Text(
                            if (currentLocale == Locale.FRENCH) duty.dutyFr else duty.dutyEn,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            // Requirements
            if (details.employmentRequirements.isNotEmpty()) {
                item {
                    Text(
                        strings.nocRequirements,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                items(details.employmentRequirements) { req ->
                    Row {
                        Text("• ", fontWeight = FontWeight.Bold)
                        Text(
                            if (currentLocale == Locale.FRENCH) req.requirementFr else req.requirementEn,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            // Provincial Demand
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                strings.nocProvincialDemand,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            if (provincialDemand.isEmpty()) {
                                TextButton(onClick = onLoadDemand) {
                                    Text("Load")
                                }
                            }
                        }
                        
                        if (provincialDemand.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            provincialDemand.forEach { demand ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(demand.provinceCode)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = getDemandColor(demand.demandLevel)
                                        ) {
                                            Text(
                                                demand.demandLevel,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White
                                            )
                                        }
                                        Text(
                                            demand.formatSalary(),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Immigration Pathways
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                strings.nocImmigrationPathways,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            if (immigrationPathways.isEmpty()) {
                                TextButton(onClick = onLoadPathways) {
                                    Text("Load")
                                }
                            }
                        }
                        
                        if (immigrationPathways.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            immigrationPathways.forEach { pathway ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pathway.pathwayName, style = MaterialTheme.typography.bodyMedium)
                                        pathway.provinceCode?.let {
                                            Text("Province: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (pathway.isEligible) Color(0xFF22C55E) else Color(0xFFEF4444)
                                    ) {
                                        Text(
                                            if (pathway.isEligible) "Eligible" else "Not Eligible",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

private fun getTeerColor(level: Int): Color = when (level) {
    0 -> Color(0xFF9333EA) // Purple
    1 -> Color(0xFF3B82F6) // Blue
    2 -> Color(0xFF14B8A6) // Teal
    3 -> Color(0xFF22C55E) // Green
    4 -> Color(0xFFEAB308) // Yellow
    5 -> Color(0xFFF97316) // Orange
    else -> Color.Gray
}

private fun getDemandColor(demandLevel: String): Color = when (demandLevel.uppercase()) {
    "HIGH" -> Color(0xFF22C55E) // Green
    "MEDIUM" -> Color(0xFFEAB308) // Yellow
    "LOW" -> Color(0xFFEF4444) // Red
    else -> Color.Gray
}
