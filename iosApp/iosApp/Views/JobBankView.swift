import SwiftUI
import shared

/// Phase 3: Job Bank Canada Search Screen for iOS
/// Allows users to search and explore Job Bank Canada job listings
struct JobBankView: View {
    @StateObject private var viewModel = JobBankViewModelWrapper()
    @State private var searchQuery = ""
    @State private var locationQuery = ""
    @State private var selectedProvinceCode: String? = nil
    @State private var showFilters = false
    @State private var showJobDetails = false
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Search Bar
                searchBar
                
                // Content
                ScrollView {
                    LazyVStack(spacing: 12) {
                        // Loading indicator
                        if viewModel.isSearching && viewModel.searchResults.isEmpty {
                            ProgressView()
                                .padding(.vertical, 40)
                        }
                        
                        // Results count
                        if viewModel.hasSearchResults {
                            HStack {
                                Text("\(viewModel.totalResults) jobs found")
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
                                Spacer()
                            }
                            .padding(.horizontal)
                        }
                        
                        // Search results
                        ForEach(viewModel.searchResults) { job in
                            JobCardView(job: job) {
                                viewModel.loadJobDetails(jobId: job.id)
                                showJobDetails = true
                            }
                        }
                        
                        // Load more
                        if viewModel.hasMoreResults && !viewModel.isSearching {
                            if viewModel.isLoadingMore {
                                ProgressView()
                                    .padding()
                            } else {
                                Button("Load More") {
                                    viewModel.loadMoreResults()
                                }
                                .padding()
                            }
                        }
                        
                        // Trending jobs
                        if viewModel.showTrending {
                            Section {
                                ForEach(viewModel.trendingJobs) { job in
                                    JobCardView(job: job) {
                                        viewModel.loadJobDetails(jobId: job.id)
                                        showJobDetails = true
                                    }
                                }
                            } header: {
                                HStack {
                                    Text("Trending Jobs")
                                        .font(.headline)
                                    Spacer()
                                }
                                .padding(.horizontal)
                            }
                        }
                        
                        // Empty state
                        if !viewModel.isSearching && !viewModel.hasSearchResults && !viewModel.showTrending {
                            emptyState
                        }
                    }
                    .padding()
                }
            }
            .navigationTitle("Job Bank Canada")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(viewModel.currentLocale == .english ? "FR" : "EN") {
                        viewModel.toggleLanguage()
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        showFilters = true
                    } label: {
                        Image(systemName: "line.3.horizontal.decrease.circle")
                    }
                }
            }
            .sheet(isPresented: $showFilters) {
                filterSheet
            }
            .sheet(isPresented: $showJobDetails) {
                jobDetailsSheet
            }
        }
    }
    
    // MARK: - Search Bar
    
    private var searchBar: some View {
        VStack(spacing: 8) {
            HStack {
                Image(systemName: "magnifyingglass")
                    .foregroundColor(.secondary)
                TextField("Job title or keyword", text: $searchQuery)
                    .textFieldStyle(.plain)
                    .submitLabel(.search)
                    .onSubmit {
                        performSearch()
                    }
            }
            .padding(10)
            .background(Color(.systemGray6))
            .cornerRadius(10)
            
            HStack {
                Image(systemName: "location")
                    .foregroundColor(.secondary)
                TextField("City or postal code", text: $locationQuery)
                    .textFieldStyle(.plain)
            }
            .padding(10)
            .background(Color(.systemGray6))
            .cornerRadius(10)
            
            Button(action: performSearch) {
                HStack {
                    if viewModel.isSearching {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            .scaleEffect(0.8)
                    }
                    Text("Search Jobs")
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
            }
            .buttonStyle(.borderedProminent)
            .disabled(viewModel.isSearching)
        }
        .padding()
        .background(Color(.systemBackground))
    }
    
    // MARK: - Filter Sheet
    
    private var filterSheet: some View {
        NavigationView {
            Form {
                Section("Province/Territory") {
                    Picker("Select Province", selection: $selectedProvinceCode) {
                        Text("All Provinces").tag(nil as String?)
                        ForEach(viewModel.provinces) { province in
                            Text("\(province.code) - \(province.name)")
                                .tag(province.code as String?)
                        }
                    }
                    .pickerStyle(.wheel)
                }
            }
            .navigationTitle("Filters")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Clear") {
                        selectedProvinceCode = nil
                        viewModel.clearSearch()
                        showFilters = false
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Apply") {
                        performSearch()
                        showFilters = false
                    }
                }
            }
        }
        .presentationDetents([.medium])
    }
    
    // MARK: - Job Details Sheet
    
    private var jobDetailsSheet: some View {
        NavigationView {
            Group {
                if viewModel.isLoadingDetails {
                    ProgressView()
                } else if let job = viewModel.selectedJob {
                    JobDetailsView(job: job)
                } else {
                    Text("No job selected")
                        .foregroundColor(.secondary)
                }
            }
            .navigationTitle("Job Details")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") {
                        viewModel.clearSelectedJob()
                        showJobDetails = false
                    }
                }
            }
        }
    }
    
    // MARK: - Empty State
    
    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "briefcase")
                .font(.system(size: 60))
                .foregroundColor(.secondary)
            
            Text("Search Job Bank Canada")
                .font(.headline)
            
            Text("Enter a job title, keyword, or location to search thousands of job listings from the Government of Canada Job Bank.")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(40)
    }
    
    // MARK: - Actions
    
    private func performSearch() {
        viewModel.searchJobs(
            query: searchQuery.isEmpty ? nil : searchQuery,
            location: locationQuery.isEmpty ? nil : locationQuery,
            provinceCode: selectedProvinceCode
        )
    }
}

// MARK: - Job Card View

struct JobCardView: View {
    let job: JobBankJobUI
    let onTap: () -> Void
    
    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 8) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(job.title)
                            .font(.headline)
                            .foregroundColor(.primary)
                            .multilineTextAlignment(.leading)
                        
                        Text(job.employer)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    
                    Spacer()
                    
                    Button {
                        // Save job action
                    } label: {
                        Image(systemName: "bookmark")
                            .foregroundColor(.accentColor)
                    }
                }
                
                // Location
                HStack {
                    Image(systemName: "location")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text(job.location.displayName)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                // Salary
                if let salary = job.salary {
                    HStack {
                        Image(systemName: "dollarsign.circle")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text(salary.displayRange)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                
                // NOC Code
                if let noc = job.nocCode {
                    HStack {
                        Text("NOC: \(noc)")
                            .font(.caption2)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color(.systemGray5))
                            .cornerRadius(4)
                    }
                }
                
                // Posted date
                Text("Posted: \(job.postingDate)")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            .padding()
            .background(Color(.systemBackground))
            .cornerRadius(12)
            .shadow(color: .black.opacity(0.05), radius: 5, x: 0, y: 2)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Job Details View

struct JobDetailsView: View {
    let job: JobBankJobUI
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // Header
                Text(job.title)
                    .font(.title2)
                    .fontWeight(.bold)
                
                Text(job.employer)
                    .font(.headline)
                    .foregroundColor(.accentColor)
                
                // Info chips
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Image(systemName: "location")
                        Text(job.location.displayName)
                    }
                    
                    if let salary = job.salary {
                        HStack {
                            Image(systemName: "dollarsign.circle")
                            Text(salary.displayRange)
                        }
                    }
                    
                    if let noc = job.nocCode {
                        HStack {
                            Image(systemName: "tag")
                            Text("NOC Code: \(noc)")
                        }
                    }
                    
                    HStack {
                        Image(systemName: "person.2")
                        Text("\(job.vacancies) \(job.vacancies > 1 ? "vacancies" : "vacancy")")
                    }
                }
                .font(.subheadline)
                .foregroundColor(.secondary)
                
                Divider()
                
                // Description
                VStack(alignment: .leading, spacing: 8) {
                    Text("Description")
                        .font(.headline)
                    Text(job.description)
                        .font(.body)
                }
                
                // Requirements
                if !job.requirements.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Requirements")
                            .font(.headline)
                        ForEach(job.requirements, id: \.self) { req in
                            HStack(alignment: .top) {
                                Text("•")
                                Text(req)
                            }
                            .font(.body)
                        }
                    }
                }
                
                // Benefits
                if !job.benefits.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Benefits")
                            .font(.headline)
                        ForEach(job.benefits, id: \.self) { benefit in
                            HStack(alignment: .top) {
                                Text("•")
                                Text(benefit)
                            }
                            .font(.body)
                        }
                    }
                }
                
                Divider()
                
                // Action buttons
                HStack(spacing: 12) {
                    Button {
                        // Save action
                    } label: {
                        Label("Save", systemImage: "bookmark")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                    
                    Button {
                        // Apply action - open URL
                        if let url = URL(string: job.url) {
                            UIApplication.shared.open(url)
                        }
                    } label: {
                        Label("Apply", systemImage: "arrow.up.right")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                }
                
                // Posted date
                HStack {
                    Spacer()
                    Text("Posted: \(job.postingDate)")
                    if let expiry = job.expiryDate {
                        Text("• Expires: \(expiry)")
                    }
                    Spacer()
                }
                .font(.caption)
                .foregroundColor(.secondary)
            }
            .padding()
        }
    }
}

#Preview {
    JobBankView()
}
