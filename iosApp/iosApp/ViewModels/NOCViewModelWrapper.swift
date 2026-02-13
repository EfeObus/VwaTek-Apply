import Foundation
import shared
import Combine
import SwiftUI

/// UI models for NOC data
struct NOCCodeUI: Identifiable {
    let id: String
    let code: String
    let titleEn: String
    let titleFr: String
    let descriptionEn: String
    let descriptionFr: String
    let teerLevel: Int
    let category: String
}

struct NOCDetailsUI {
    let noc: NOCCodeUI
    let mainDuties: [NOCMainDutyUI]
    let employmentRequirements: [NOCEmploymentRequirementUI]
    let skills: [NOCSkillUI]
}

struct NOCMainDutyUI: Identifiable {
    let id: String
    let dutyEn: String
    let dutyFr: String
}

struct NOCEmploymentRequirementUI: Identifiable {
    let id: String
    let requirementEn: String
    let requirementFr: String
}

struct NOCSkillUI: Identifiable {
    let id: String
    let skillNameEn: String
    let skillNameFr: String
    let skillLevel: Int
}

struct TEERLevelInfo: Identifiable {
    let id: Int
    let level: Int
    let educationRequirement: String
    
    func getTitle(locale: AppLocale) -> String {
        switch level {
        case 0: return locale == .english ? "Management" : "Gestion"
        case 1: return locale == .english ? "Professional (University)" : "Professionnel (Université)"
        case 2: return locale == .english ? "Technical (College)" : "Technique (Collège)"
        case 3: return locale == .english ? "Intermediate (Apprenticeship)" : "Intermédiaire (Apprentissage)"
        case 4: return locale == .english ? "Entry-Level (High School)" : "Niveau d'entrée (Secondaire)"
        case 5: return locale == .english ? "Labour (Short Training)" : "Main d'œuvre (Formation courte)"
        default: return "Unknown"
        }
    }
}

struct NOCCategoryUI: Identifiable {
    let id: String
    let category: String
    let name: String
}

struct ProvincialDemandUI: Identifiable {
    let id: String
    let provinceCode: String
    let demandLevel: String
    let medianSalary: Double
    
    func formatSalary() -> String {
        return String(format: "$%.0f/yr", medianSalary)
    }
}

struct ImmigrationPathwayUI: Identifiable {
    let id: String
    let pathwayName: String
    let provinceCode: String?
    let isEligible: Bool
}

enum AppLocale {
    case english
    case french
}

/// Wrapper class to make Kotlin NOCViewModel observable in SwiftUI
@MainActor
class NOCViewModelWrapper: ObservableObject {
    private let viewModel: NOCViewModel
    
    @Published var searchResults: [NOCCodeUI] = []
    @Published var selectedDetails: NOCDetailsUI? = nil
    @Published var teerLevels: [TEERLevelInfo] = []
    @Published var categories: [NOCCategoryUI] = []
    @Published var provincialDemand: [ProvincialDemandUI] = []
    @Published var immigrationPathways: [ImmigrationPathwayUI] = []
    @Published var totalResults: Int = 0
    @Published var currentPage: Int = 0
    @Published var hasMoreResults: Bool = false
    
    @Published var selectedTeerLevels: [Int]? = nil
    @Published var selectedCategory: String? = nil
    @Published var currentLocale: AppLocale = .english
    
    @Published var isSearching: Bool = false
    @Published var isLoadingDetails: Bool = false
    @Published var isLoadingMore: Bool = false
    @Published var error: String? = nil
    
    private var stateWatcher: Closeable?
    private var currentQuery: String = ""
    
    var hasActiveFilters: Bool {
        (selectedTeerLevels != nil && !selectedTeerLevels!.isEmpty) || selectedCategory != nil
    }
    
    init() {
        // Get NOCViewModel from Koin
        self.viewModel = KoinHelperKt.getNOCViewModel()
        setupTeerLevels()
        setupCategories()
        observeState()
    }
    
    deinit {
        stateWatcher?.close()
    }
    
    private func setupTeerLevels() {
        teerLevels = [
            TEERLevelInfo(id: 0, level: 0, educationRequirement: "Management experience"),
            TEERLevelInfo(id: 1, level: 1, educationRequirement: "University degree"),
            TEERLevelInfo(id: 2, level: 2, educationRequirement: "College diploma or apprenticeship"),
            TEERLevelInfo(id: 3, level: 3, educationRequirement: "College certificate or apprenticeship"),
            TEERLevelInfo(id: 4, level: 4, educationRequirement: "High school diploma"),
            TEERLevelInfo(id: 5, level: 5, educationRequirement: "Short-term training")
        ]
    }
    
    private func setupCategories() {
        categories = [
            NOCCategoryUI(id: "0", category: "0", name: "Legislative and senior management"),
            NOCCategoryUI(id: "1", category: "1", name: "Business, finance and administration"),
            NOCCategoryUI(id: "2", category: "2", name: "Natural and applied sciences"),
            NOCCategoryUI(id: "3", category: "3", name: "Health"),
            NOCCategoryUI(id: "4", category: "4", name: "Education, law and social"),
            NOCCategoryUI(id: "5", category: "5", name: "Art, culture, recreation and sport"),
            NOCCategoryUI(id: "6", category: "6", name: "Sales and service"),
            NOCCategoryUI(id: "7", category: "7", name: "Trades, transport and equipment"),
            NOCCategoryUI(id: "8", category: "8", name: "Natural resources, agriculture"),
            NOCCategoryUI(id: "9", category: "9", name: "Manufacturing and utilities")
        ]
    }
    
    private func observeState() {
        stateWatcher = FlowExtensionsKt.watch(viewModel.state) { [weak self] (state: Any?) in
            guard let self = self, let nocState = state as? NOCState else { return }
            Task { @MainActor in
                self.searchResults = nocState.searchResults.map { self.convertNOCCode($0) }
                self.selectedDetails = nocState.selectedNOCDetails != nil ? self.convertNOCDetails(nocState.selectedNOCDetails!) : nil
                self.provincialDemand = nocState.provincialDemand.map { self.convertProvincialDemand($0) }
                self.immigrationPathways = nocState.immigrationPathways.map { self.convertImmigrationPathway($0) }
                self.totalResults = Int(nocState.totalResults)
                self.currentPage = Int(nocState.currentPage)
                self.hasMoreResults = nocState.hasMoreResults
                self.currentLocale = nocState.currentLocale == .english ? .english : .french
                self.isSearching = nocState.isSearching
                self.isLoadingDetails = nocState.isLoadingDetails
                self.error = nocState.searchError
            }
        }
    }
    
    private func convertNOCCode(_ noc: NOCCode) -> NOCCodeUI {
        return NOCCodeUI(
            id: noc.code,
            code: noc.code,
            titleEn: noc.titleEn,
            titleFr: noc.titleFr,
            descriptionEn: noc.descriptionEn,
            descriptionFr: noc.descriptionFr,
            teerLevel: Int(noc.teerLevel),
            category: noc.category
        )
    }
    
    private func convertNOCDetails(_ details: NOCDetails) -> NOCDetailsUI {
        return NOCDetailsUI(
            noc: convertNOCCode(details.noc),
            mainDuties: details.mainDuties.map { duty in
                NOCMainDutyUI(
                    id: duty.id,
                    dutyEn: duty.dutyTextEn,
                    dutyFr: duty.dutyTextFr
                )
            },
            employmentRequirements: details.employmentRequirements.map { req in
                NOCEmploymentRequirementUI(
                    id: req.id,
                    requirementEn: req.requirementTextEn,
                    requirementFr: req.requirementTextFr
                )
            },
            skills: details.skills.map { skill in
                NOCSkillUI(
                    id: skill.id,
                    skillNameEn: skill.skillNameEn,
                    skillNameFr: skill.skillNameFr,
                    skillLevel: Int(skill.skillLevel)
                )
            }
        )
    }
    
    private func convertProvincialDemand(_ demand: NOCProvincialDemand) -> ProvincialDemandUI {
        return ProvincialDemandUI(
            id: "\(demand.nocCode)_\(demand.provinceCode)",
            provinceCode: demand.provinceCode,
            demandLevel: demand.demandLevel,
            medianSalary: demand.medianSalary
        )
    }
    
    private func convertImmigrationPathway(_ pathway: NOCImmigrationPathway) -> ImmigrationPathwayUI {
        return ImmigrationPathwayUI(
            id: pathway.pathwayName,
            pathwayName: pathway.pathwayName,
            provinceCode: pathway.provinceCode,
            isEligible: pathway.isEligible
        )
    }
    
    // MARK: - Public Methods
    
    func search(query: String, teerLevels: [Int]?, category: String?) {
        currentQuery = query
        if query.isEmpty && teerLevels == nil && category == nil {
            searchResults = []
            totalResults = 0
            hasMoreResults = false
            return
        }
        
        let filters = NOCSearchFilters(
            query: query.isEmpty ? nil : query,
            teerLevels: teerLevels?.map { Int32($0) },
            category: category,
            page: 0,
            perPage: 20
        )
        viewModel.searchNOCCodes(filters: filters)
    }
    
    func clearSearch() {
        currentQuery = ""
        searchResults = []
        totalResults = 0
        hasMoreResults = false
    }
    
    func loadDetails(code: String) {
        viewModel.loadNOCDetails(code: code)
    }
    
    func loadProvincialDemand(code: String) {
        viewModel.loadProvincialDemand(code: code)
    }
    
    func loadImmigrationPathways(code: String) {
        viewModel.loadImmigrationPathways(code: code)
    }
    
    func clearSelectedDetails() {
        // Clear details when closing sheet
        provincialDemand = []
        immigrationPathways = []
    }
    
    func loadMoreResults() {
        guard hasMoreResults && !isLoadingMore else { return }
        isLoadingMore = true
        
        let filters = NOCSearchFilters(
            query: currentQuery.isEmpty ? nil : currentQuery,
            teerLevels: selectedTeerLevels?.map { Int32($0) },
            category: selectedCategory,
            page: Int32(currentPage + 1),
            perPage: 20
        )
        viewModel.searchNOCCodes(filters: filters)
        isLoadingMore = false
    }
    
    func toggleLanguage() {
        viewModel.toggleLanguage()
    }
    
    func toggleTeerFilter(level: Int) {
        if var levels = selectedTeerLevels {
            if let index = levels.firstIndex(of: level) {
                levels.remove(at: index)
                selectedTeerLevels = levels.isEmpty ? nil : levels
            } else {
                levels.append(level)
                selectedTeerLevels = levels
            }
        } else {
            selectedTeerLevels = [level]
        }
    }
    
    func setCategoryFilter(category: String?) {
        selectedCategory = category
    }
    
    func clearFilters() {
        selectedTeerLevels = nil
        selectedCategory = nil
    }
}
