import Foundation
import shared
import Combine
import SwiftUI

/// UI models for Job Bank data
struct JobBankJobUI: Identifiable {
    let id: String
    let title: String
    let employer: String
    let location: JobBankLocationUI
    let salary: JobBankSalaryUI?
    let nocCode: String?
    let postingDate: String
    let expiryDate: String?
    let description: String
    let requirements: [String]
    let benefits: [String]
    let hours: String?
    let jobType: String?
    let vacancies: Int
    let url: String
}

struct JobBankLocationUI {
    let city: String
    let province: String
    let postalCode: String?
    let isRemote: Bool
    
    var displayName: String {
        isRemote ? "\(city), \(province) (Remote)" : "\(city), \(province)"
    }
}

struct JobBankSalaryUI {
    let min: Double?
    let max: Double?
    let period: String
    let currency: String
    
    var displayRange: String {
        let minStr = min.map { String(format: "$%.2f", $0) } ?? ""
        let maxStr = max.map { String(format: "$%.2f", $0) } ?? ""
        let periodStr = period == "HOURLY" ? "/hr" : "/yr"
        
        if !minStr.isEmpty && !maxStr.isEmpty {
            return "\(minStr) - \(maxStr)\(periodStr)"
        } else if !minStr.isEmpty {
            return "From \(minStr)\(periodStr)"
        } else if !maxStr.isEmpty {
            return "Up to \(maxStr)\(periodStr)"
        }
        return "Negotiable"
    }
}

struct CanadianProvinceUI: Identifiable {
    let id: String
    let code: String
    let name: String
    let nameFr: String
    
    static let allProvinces: [CanadianProvinceUI] = [
        CanadianProvinceUI(id: "AB", code: "AB", name: "Alberta", nameFr: "Alberta"),
        CanadianProvinceUI(id: "BC", code: "BC", name: "British Columbia", nameFr: "Colombie-Britannique"),
        CanadianProvinceUI(id: "MB", code: "MB", name: "Manitoba", nameFr: "Manitoba"),
        CanadianProvinceUI(id: "NB", code: "NB", name: "New Brunswick", nameFr: "Nouveau-Brunswick"),
        CanadianProvinceUI(id: "NL", code: "NL", name: "Newfoundland and Labrador", nameFr: "Terre-Neuve-et-Labrador"),
        CanadianProvinceUI(id: "NS", code: "NS", name: "Nova Scotia", nameFr: "Nouvelle-Écosse"),
        CanadianProvinceUI(id: "NT", code: "NT", name: "Northwest Territories", nameFr: "Territoires du Nord-Ouest"),
        CanadianProvinceUI(id: "NU", code: "NU", name: "Nunavut", nameFr: "Nunavut"),
        CanadianProvinceUI(id: "ON", code: "ON", name: "Ontario", nameFr: "Ontario"),
        CanadianProvinceUI(id: "PE", code: "PE", name: "Prince Edward Island", nameFr: "Île-du-Prince-Édouard"),
        CanadianProvinceUI(id: "QC", code: "QC", name: "Quebec", nameFr: "Québec"),
        CanadianProvinceUI(id: "SK", code: "SK", name: "Saskatchewan", nameFr: "Saskatchewan"),
        CanadianProvinceUI(id: "YT", code: "YT", name: "Yukon", nameFr: "Yukon")
    ]
}

enum AppLocaleJB {
    case english
    case french
}

/// Wrapper class to make Kotlin JobBankViewModel observable in SwiftUI
@MainActor
class JobBankViewModelWrapper: ObservableObject {
    private let viewModel: JobBankViewModel
    
    @Published var searchResults: [JobBankJobUI] = []
    @Published var trendingJobs: [JobBankJobUI] = []
    @Published var selectedJob: JobBankJobUI? = nil
    @Published var provinces: [CanadianProvinceUI] = CanadianProvinceUI.allProvinces
    
    @Published var totalResults: Int = 0
    @Published var currentPage: Int = 0
    @Published var hasMoreResults: Bool = false
    
    @Published var isSearching: Bool = false
    @Published var isLoadingMore: Bool = false
    @Published var isLoadingDetails: Bool = false
    @Published var isLoadingTrending: Bool = false
    @Published var searchError: String? = nil
    @Published var detailsError: String? = nil
    
    @Published var currentLocale: AppLocaleJB = .english
    
    private var stateWatcher: Closeable?
    
    var hasActiveFilters: Bool {
        return false // Will be updated based on state
    }
    
    var hasSearchResults: Bool {
        !searchResults.isEmpty
    }
    
    var showTrending: Bool {
        !isSearching && !hasSearchResults && !trendingJobs.isEmpty
    }
    
    init() {
        self.viewModel = KoinHelperKt.getJobBankViewModel()
        observeState()
    }
    
    deinit {
        stateWatcher?.close()
    }
    
    private func observeState() {
        stateWatcher = FlowExtensionsKt.watch(viewModel.state) { [weak self] (state: Any?) in
            guard let self = self, let jbState = state as? JobBankState else { return }
            Task { @MainActor in
                self.searchResults = jbState.searchResults.map { self.convertJob($0) }
                self.trendingJobs = jbState.trendingJobs.map { self.convertJob($0) }
                self.selectedJob = jbState.selectedJob != nil ? self.convertJob(jbState.selectedJob!) : nil
                self.provinces = jbState.provinces.map { self.convertProvince($0) }
                self.totalResults = Int(jbState.totalResults)
                self.currentPage = Int(jbState.currentPage)
                self.hasMoreResults = jbState.hasMoreResults
                self.isSearching = jbState.isSearching
                self.isLoadingMore = jbState.isLoadingMore
                self.isLoadingDetails = jbState.isLoadingDetails
                self.isLoadingTrending = jbState.isLoadingTrending
                self.searchError = jbState.searchError
                self.detailsError = jbState.detailsError
                self.currentLocale = jbState.currentLocale == .english ? .english : .french
            }
        }
    }
    
    private func convertJob(_ job: JobBankJob) -> JobBankJobUI {
        return JobBankJobUI(
            id: job.id,
            title: job.title,
            employer: job.employer,
            location: JobBankLocationUI(
                city: job.location.city,
                province: job.location.province,
                postalCode: job.location.postalCode,
                isRemote: job.location.isRemote
            ),
            salary: job.salary != nil ? JobBankSalaryUI(
                min: job.salary!.min?.doubleValue,
                max: job.salary!.max?.doubleValue,
                period: job.salary!.period,
                currency: job.salary!.currency
            ) : nil,
            nocCode: job.nocCode,
            postingDate: job.postingDate,
            expiryDate: job.expiryDate,
            description: job.description_,
            requirements: job.requirements as? [String] ?? [],
            benefits: job.benefits as? [String] ?? [],
            hours: job.hours,
            jobType: job.jobType,
            vacancies: Int(job.vacancies),
            url: job.url
        )
    }
    
    private func convertProvince(_ province: CanadianProvince) -> CanadianProvinceUI {
        return CanadianProvinceUI(
            id: province.code,
            code: province.code,
            name: province.name,
            nameFr: province.nameFr
        )
    }
    
    // MARK: - Public Methods
    
    func searchJobs(query: String?, location: String?, provinceCode: String?) {
        viewModel.searchJobs(
            query: query,
            location: location,
            provinceCode: provinceCode,
            nocCode: nil
        )
    }
    
    func searchByNOC(nocCode: String) {
        viewModel.searchByNOC(nocCode: nocCode)
    }
    
    func searchByProvince(provinceCode: String) {
        viewModel.searchByProvince(provinceCode: provinceCode)
    }
    
    func loadMoreResults() {
        viewModel.loadMoreResults()
    }
    
    func loadJobDetails(jobId: String) {
        viewModel.loadJobDetails(jobId: jobId)
    }
    
    func clearSelectedJob() {
        viewModel.clearSelectedJob()
    }
    
    func clearSearch() {
        viewModel.clearSearch()
    }
    
    func toggleLanguage() {
        viewModel.toggleLanguage()
    }
    
    func setLanguage(_ locale: AppLocaleJB) {
        viewModel.setLanguage(locale: locale == .english ? Locale.english : Locale.french)
    }
}
