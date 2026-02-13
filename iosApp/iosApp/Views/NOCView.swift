import SwiftUI
import shared

/**
 * Phase 3: NOC (National Occupational Classification) View for iOS
 * Allows users to search and explore Canadian NOC codes
 */
struct NOCView: View {
    @StateObject private var viewModel = NOCViewModelWrapper()
    @State private var searchQuery = ""
    @State private var showFilterSheet = false
    @State private var showDetailsSheet = false
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Search bar
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.secondary)
                    
                    TextField("Search NOC codes...", text: $searchQuery)
                        .textFieldStyle(.plain)
                        .onSubmit {
                            viewModel.search(
                                query: searchQuery,
                                teerLevels: viewModel.selectedTeerLevels,
                                category: viewModel.selectedCategory
                            )
                        }
                    
                    if !searchQuery.isEmpty {
                        Button(action: {
                            searchQuery = ""
                            viewModel.clearSearch()
                        }) {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(10)
                .padding()
                
                // Content
                if viewModel.isSearching {
                    ProgressView()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if viewModel.searchResults.isEmpty && searchQuery.isEmpty {
                    // TEER Overview
                    TEEROverviewView(
                        teerLevels: viewModel.teerLevels,
                        currentLocale: viewModel.currentLocale
                    )
                } else if viewModel.searchResults.isEmpty {
                    // No results
                    VStack(spacing: 16) {
                        Image(systemName: "magnifyingglass")
                            .font(.system(size: 48))
                            .foregroundColor(.secondary)
                        
                        Text("No NOC codes found for \"\(searchQuery)\"")
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    // Search results
                    VStack(alignment: .leading, spacing: 8) {
                        Text("\(viewModel.totalResults) results found")
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .padding(.horizontal)
                        
                        List {
                            ForEach(viewModel.searchResults, id: \.code) { noc in
                                NOCResultRow(
                                    noc: noc,
                                    currentLocale: viewModel.currentLocale
                                )
                                .onTapGesture {
                                    viewModel.loadDetails(code: noc.code)
                                    showDetailsSheet = true
                                }
                            }
                            
                            if viewModel.hasMoreResults {
                                HStack {
                                    Spacer()
                                    if viewModel.isLoadingMore {
                                        ProgressView()
                                    } else {
                                        Button("Load More") {
                                            viewModel.loadMoreResults()
                                        }
                                    }
                                    Spacer()
                                }
                                .padding()
                            }
                        }
                        .listStyle(.plain)
                    }
                }
            }
            .navigationTitle(viewModel.currentLocale == .english ? "NOC Codes" : "Codes CNP")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    HStack {
                        // Language toggle
                        Button(action: {
                            viewModel.toggleLanguage()
                        }) {
                            Text(viewModel.currentLocale == .english ? "FR" : "EN")
                                .fontWeight(.bold)
                        }
                        
                        // Filter button
                        Button(action: {
                            showFilterSheet = true
                        }) {
                            Image(systemName: viewModel.hasActiveFilters 
                                ? "line.3.horizontal.decrease.circle.fill" 
                                : "line.3.horizontal.decrease.circle")
                        }
                    }
                }
            }
        }
        .sheet(isPresented: $showFilterSheet) {
            NOCFilterSheet(
                viewModel: viewModel,
                searchQuery: searchQuery,
                isPresented: $showFilterSheet
            )
        }
        .sheet(isPresented: $showDetailsSheet) {
            if let details = viewModel.selectedDetails {
                NOCDetailsSheet(
                    viewModel: viewModel,
                    details: details,
                    isPresented: $showDetailsSheet
                )
            } else if viewModel.isLoadingDetails {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
    }
}

// MARK: - TEER Overview

struct TEEROverviewView: View {
    let teerLevels: [TEERLevelInfo]
    let currentLocale: AppLocale
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("TEER Classification System")
                    .font(.title2)
                    .fontWeight(.bold)
                
                Text("The Training, Education, Experience and Responsibilities (TEER) system classifies occupations by the nature of training, education, and experience required.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                
                ForEach(teerLevels, id: \.level) { teer in
                    TEERCard(teer: teer, currentLocale: currentLocale)
                }
            }
            .padding()
        }
    }
}

struct TEERCard: View {
    let teer: TEERLevelInfo
    let currentLocale: AppLocale
    
    var body: some View {
        HStack(spacing: 16) {
            Text("TEER \(teer.level)")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundColor(.white)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(teerColor(level: teer.level))
                .cornerRadius(8)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(teer.getTitle(locale: currentLocale))
                    .font(.headline)
                
                Text(teer.educationRequirement)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
        }
        .padding()
        .background(teerColor(level: teer.level).opacity(0.1))
        .cornerRadius(12)
    }
}

// MARK: - NOC Result Row

struct NOCResultRow: View {
    let noc: NOCCodeUI
    let currentLocale: AppLocale
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Text(noc.code)
                    .font(.caption)
                    .fontWeight(.semibold)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color(.systemGray5))
                    .cornerRadius(4)
                
                Text("TEER \(noc.teerLevel)")
                    .font(.caption2)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(teerColor(level: noc.teerLevel))
                    .cornerRadius(4)
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .foregroundColor(.secondary)
            }
            
            Text(currentLocale == .english ? noc.titleEn : noc.titleFr)
                .font(.headline)
            
            Text(currentLocale == .english ? noc.descriptionEn : noc.descriptionFr)
                .font(.caption)
                .foregroundColor(.secondary)
                .lineLimit(2)
        }
        .padding(.vertical, 8)
    }
}

// MARK: - Filter Sheet

struct NOCFilterSheet: View {
    @ObservedObject var viewModel: NOCViewModelWrapper
    let searchQuery: String
    @Binding var isPresented: Bool
    
    var body: some View {
        NavigationStack {
            List {
                Section("TEER Level") {
                    ForEach(viewModel.teerLevels, id: \.level) { teer in
                        Button(action: {
                            viewModel.toggleTeerFilter(level: teer.level)
                        }) {
                            HStack {
                                Image(systemName: viewModel.selectedTeerLevels?.contains(teer.level) == true 
                                    ? "checkmark.square.fill" 
                                    : "square")
                                    .foregroundColor(.accentColor)
                                
                                Text("TEER \(teer.level): \(teer.getTitle(locale: viewModel.currentLocale))")
                            }
                        }
                        .foregroundColor(.primary)
                    }
                }
                
                Section("Category") {
                    Button(action: {
                        viewModel.setCategoryFilter(category: nil)
                    }) {
                        HStack {
                            Image(systemName: viewModel.selectedCategory == nil 
                                ? "largecircle.fill.circle" 
                                : "circle")
                                .foregroundColor(.accentColor)
                            
                            Text("All Categories")
                        }
                    }
                    .foregroundColor(.primary)
                    
                    ForEach(viewModel.categories, id: \.category) { cat in
                        Button(action: {
                            viewModel.setCategoryFilter(category: cat.category)
                        }) {
                            HStack {
                                Image(systemName: viewModel.selectedCategory == cat.category 
                                    ? "largecircle.fill.circle" 
                                    : "circle")
                                    .foregroundColor(.accentColor)
                                
                                Text("\(cat.category) - \(cat.name)")
                                    .lineLimit(1)
                            }
                        }
                        .foregroundColor(.primary)
                    }
                }
            }
            .navigationTitle("Filter")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Clear") {
                        viewModel.clearFilters()
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Apply") {
                        viewModel.search(
                            query: searchQuery,
                            teerLevels: viewModel.selectedTeerLevels,
                            category: viewModel.selectedCategory
                        )
                        isPresented = false
                    }
                    .fontWeight(.semibold)
                }
            }
        }
    }
}

// MARK: - Details Sheet

struct NOCDetailsSheet: View {
    @ObservedObject var viewModel: NOCViewModelWrapper
    let details: NOCDetailsUI
    @Binding var isPresented: Bool
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    // Header
                    HStack(spacing: 8) {
                        Text(details.noc.code)
                            .font(.title3)
                            .fontWeight(.bold)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color.accentColor.opacity(0.2))
                            .cornerRadius(8)
                        
                        Text("TEER \(details.noc.teerLevel)")
                            .font(.caption)
                            .fontWeight(.bold)
                            .foregroundColor(.white)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(teerColor(level: details.noc.teerLevel))
                            .cornerRadius(8)
                    }
                    
                    Text(viewModel.currentLocale == .english ? details.noc.titleEn : details.noc.titleFr)
                        .font(.title2)
                        .fontWeight(.bold)
                    
                    Text(viewModel.currentLocale == .english ? details.noc.descriptionEn : details.noc.descriptionFr)
                        .font(.body)
                        .foregroundColor(.secondary)
                    
                    // Main Duties
                    if !details.mainDuties.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(viewModel.currentLocale == .english ? "Main Duties" : "Fonctions principales")
                                .font(.headline)
                            
                            ForEach(details.mainDuties.prefix(5), id: \.id) { duty in
                                HStack(alignment: .top, spacing: 8) {
                                    Text("•")
                                        .fontWeight(.bold)
                                    Text(viewModel.currentLocale == .english ? duty.dutyEn : duty.dutyFr)
                                        .font(.subheadline)
                                }
                            }
                        }
                    }
                    
                    // Requirements
                    if !details.employmentRequirements.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(viewModel.currentLocale == .english ? "Employment Requirements" : "Conditions d'accès")
                                .font(.headline)
                            
                            ForEach(details.employmentRequirements, id: \.id) { req in
                                HStack(alignment: .top, spacing: 8) {
                                    Text("•")
                                        .fontWeight(.bold)
                                    Text(viewModel.currentLocale == .english ? req.requirementEn : req.requirementFr)
                                        .font(.subheadline)
                                }
                            }
                        }
                    }
                    
                    // Provincial Demand
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text(viewModel.currentLocale == .english ? "Provincial Demand" : "Demande provinciale")
                                .font(.headline)
                            
                            Spacer()
                            
                            if viewModel.provincialDemand.isEmpty {
                                Button("Load") {
                                    viewModel.loadProvincialDemand(code: details.noc.code)
                                }
                                .font(.caption)
                            }
                        }
                        
                        if !viewModel.provincialDemand.isEmpty {
                            ForEach(viewModel.provincialDemand, id: \.provinceCode) { demand in
                                HStack {
                                    Text(demand.provinceCode)
                                        .fontWeight(.medium)
                                    
                                    Spacer()
                                    
                                    Text(demand.demandLevel)
                                        .font(.caption)
                                        .fontWeight(.semibold)
                                        .foregroundColor(.white)
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 4)
                                        .background(demandColor(level: demand.demandLevel))
                                        .cornerRadius(4)
                                    
                                    Text(demand.formatSalary())
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                                .padding(.vertical, 4)
                            }
                        }
                    }
                    .padding()
                    .background(Color(.systemGray6))
                    .cornerRadius(12)
                    
                    // Immigration Pathways
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text(viewModel.currentLocale == .english ? "Immigration Pathways" : "Voies d'immigration")
                                .font(.headline)
                            
                            Spacer()
                            
                            if viewModel.immigrationPathways.isEmpty {
                                Button("Load") {
                                    viewModel.loadImmigrationPathways(code: details.noc.code)
                                }
                                .font(.caption)
                            }
                        }
                        
                        if !viewModel.immigrationPathways.isEmpty {
                            ForEach(viewModel.immigrationPathways, id: \.pathwayName) { pathway in
                                HStack {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(pathway.pathwayName)
                                            .font(.subheadline)
                                        
                                        if let province = pathway.provinceCode {
                                            Text("Province: \(province)")
                                                .font(.caption)
                                                .foregroundColor(.secondary)
                                        }
                                    }
                                    
                                    Spacer()
                                    
                                    Text(pathway.isEligible ? "Eligible" : "Not Eligible")
                                        .font(.caption)
                                        .fontWeight(.semibold)
                                        .foregroundColor(.white)
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 4)
                                        .background(pathway.isEligible ? Color.green : Color.red)
                                        .cornerRadius(4)
                                }
                                .padding(.vertical, 4)
                            }
                        }
                    }
                    .padding()
                    .background(Color(.systemGray6))
                    .cornerRadius(12)
                }
                .padding()
            }
            .navigationTitle("NOC Details")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        viewModel.clearSelectedDetails()
                        isPresented = false
                    }
                }
            }
        }
    }
}

// MARK: - Helper Functions

func teerColor(level: Int) -> Color {
    switch level {
    case 0: return Color.purple
    case 1: return Color.blue
    case 2: return Color.teal
    case 3: return Color.green
    case 4: return Color.yellow
    case 5: return Color.orange
    default: return Color.gray
    }
}

func demandColor(level: String) -> Color {
    switch level.uppercased() {
    case "HIGH": return Color.green
    case "MEDIUM": return Color.yellow
    case "LOW": return Color.red
    default: return Color.gray
    }
}
