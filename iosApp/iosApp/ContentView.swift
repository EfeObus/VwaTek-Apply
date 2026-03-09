import SwiftUI
import shared

struct ContentView: View {
    @StateObject private var authViewModel = AuthViewModelWrapper()
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    
    /// Deep link tab tag to navigate to (set by .onOpenURL)
    @State private var deepLinkTab: Int? = nil
    
    var body: some View {
        Group {
            if authViewModel.isAuthenticated {
                if horizontalSizeClass == .regular {
                    iPadMainView(authViewModel: authViewModel, deepLinkTab: $deepLinkTab)
                } else {
                    MainTabView(authViewModel: authViewModel, deepLinkTab: $deepLinkTab)
                }
            } else {
                AuthView(viewModel: authViewModel)
            }
        }
        .task {
            authViewModel.observeState()
        }
        .onOpenURL { url in
            handleDeepLink(url)
        }
    }
    
    /// Map vwatekapply://<host> to tab tag integers
    private func handleDeepLink(_ url: URL) {
        guard url.scheme == "vwatekapply" else { return }
        let host = url.host ?? ""
        let tabMap: [String: Int] = [
            "home": 0, "resume": 1, "optimizer": 2, "coverletter": 3,
            "interview": 4, "noc": 5, "jobbank": 6, "tracker": 7,
            "salary": 8, "linkedin": 9, "organization": 10,
            "subscription": 11, "profile": 12, "settings": 13
        ]
        if let tag = tabMap[host] {
            deepLinkTab = tag
        }
    }
}

// MARK: - Navigation Destination Enum

enum AppScreen: String, CaseIterable, Identifiable {
    case home, resume, optimizer, coverLetter, interview
    case noc, jobBank, tracker, salaryInsights, linkedInOptimizer
    case organization, subscription, profile, settings
    
    var id: String { rawValue }
    
    var label: String {
        switch self {
        case .home: return "Home"
        case .resume: return "Resumes"
        case .optimizer: return "Optimizer"
        case .coverLetter: return "Cover Letters"
        case .interview: return "Interview Prep"
        case .noc: return "NOC Codes"
        case .jobBank: return "Job Bank"
        case .tracker: return "Job Tracker"
        case .salaryInsights: return "Salary Insights"
        case .linkedInOptimizer: return "LinkedIn Optimizer"
        case .organization: return "Organization"
        case .subscription: return "Premium"
        case .profile: return "Profile"
        case .settings: return "Settings"
        }
    }
    
    var icon: String {
        switch self {
        case .home: return "house.fill"
        case .resume: return "doc.text.fill"
        case .optimizer: return "wand.and.stars"
        case .coverLetter: return "envelope.fill"
        case .interview: return "mic.fill"
        case .noc: return "briefcase.fill"
        case .jobBank: return "magnifyingglass"
        case .tracker: return "list.clipboard.fill"
        case .salaryInsights: return "chart.line.uptrend.xyaxis"
        case .linkedInOptimizer: return "link.circle.fill"
        case .organization: return "building.2"
        case .subscription: return "star.fill"
        case .profile: return "person.fill"
        case .settings: return "gear"
        }
    }
    
    /// Deep link tag index for backward compatibility
    var deepLinkTag: Int {
        switch self {
        case .home: return 0
        case .resume: return 1
        case .optimizer: return 2
        case .coverLetter: return 3
        case .interview: return 4
        case .noc: return 5
        case .jobBank: return 6
        case .tracker: return 7
        case .salaryInsights: return 8
        case .linkedInOptimizer: return 9
        case .organization: return 10
        case .subscription: return 11
        case .profile: return 12
        case .settings: return 13
        }
    }
    
    static func fromDeepLinkTag(_ tag: Int) -> AppScreen? {
        allCases.first { $0.deepLinkTag == tag }
    }
}

// MARK: - Menu Sections (for More tab and iPad sidebar)

struct MenuSection: Identifiable {
    let id = UUID()
    let title: String
    let items: [AppScreen]
}

private let menuSections: [MenuSection] = [
    MenuSection(title: "Career Tools", items: [.interview, .tracker, .jobBank, .noc]),
    MenuSection(title: "Insights", items: [.salaryInsights, .linkedInOptimizer]),
    MenuSection(title: "Account", items: [.organization, .subscription, .profile, .settings])
]

// MARK: - Main Tab View (Phone — 5 tabs)

struct MainTabView: View {
    @ObservedObject var authViewModel: AuthViewModelWrapper
    @Binding var deepLinkTab: Int?
    @State private var selectedTab: AppScreen = .home
    /// If user picks a "More" sub-screen, track it here
    @State private var moreSelection: AppScreen? = nil
    
    var body: some View {
        TabView(selection: $selectedTab) {
            NavigationStack {
                HomeView(userName: authViewModel.fullName, selectedTab: tabBinding)
            }
            .tabItem { Label("Home", systemImage: "house.fill") }
            .tag(AppScreen.home)
            
            NavigationStack {
                ResumeView()
            }
            .tabItem { Label("Resumes", systemImage: "doc.text.fill") }
            .tag(AppScreen.resume)
            
            NavigationStack {
                OptimizerView()
            }
            .tabItem { Label("Optimizer", systemImage: "wand.and.stars") }
            .tag(AppScreen.coverLetter)
            
            NavigationStack {
                CoverLetterView()
            }
            .tabItem { Label("Letters", systemImage: "envelope.fill") }
            .tag(AppScreen.optimizer)
            
            // "More" tab with all remaining features
            NavigationStack {
                MoreMenuView(
                    authViewModel: authViewModel,
                    selection: $moreSelection
                )
            }
            .tabItem { Label("More", systemImage: "ellipsis.circle.fill") }
            .tag(AppScreen.profile) // Use profile as tag for More tab
        }
        .accentColor(.blue)
        .onChange(of: deepLinkTab) { newTab in
            guard let tag = newTab else { return }
            if let screen = AppScreen.fromDeepLinkTag(tag) {
                let primaryTabs: [AppScreen] = [.home, .resume, .optimizer, .coverLetter]
                if primaryTabs.contains(screen) {
                    selectedTab = screen
                } else {
                    // Navigate to More tab and select the sub-screen
                    selectedTab = .profile
                    moreSelection = screen
                }
            }
            deepLinkTab = nil
        }
    }
    
    /// Bridge for HomeView's selectedTab binding (Int-based)
    private var tabBinding: Binding<Int> {
        Binding(
            get: { selectedTab.deepLinkTag },
            set: { tag in
                if let screen = AppScreen.fromDeepLinkTag(tag) {
                    let primaryTabs: [AppScreen] = [.home, .resume, .optimizer, .coverLetter]
                    if primaryTabs.contains(screen) {
                        selectedTab = screen
                    } else {
                        selectedTab = .profile
                        moreSelection = screen
                    }
                }
            }
        )
    }
}

// MARK: - More Menu View

struct MoreMenuView: View {
    @ObservedObject var authViewModel: AuthViewModelWrapper
    @Binding var selection: AppScreen?
    
    var body: some View {
        List {
            ForEach(menuSections) { section in
                Section(section.title) {
                    ForEach(section.items) { screen in
                        NavigationLink(value: screen) {
                            Label(screen.label, systemImage: screen.icon)
                        }
                    }
                }
            }
        }
        .navigationTitle("More")
        .navigationDestination(for: AppScreen.self) { screen in
            screenView(for: screen)
        }
        .onChange(of: selection) { newValue in
            // Auto-navigate to deep-linked screen — handled by NavigationStack
        }
    }
    
    @ViewBuilder
    private func screenView(for screen: AppScreen) -> some View {
        switch screen {
        case .interview:
            InterviewView()
        case .tracker:
            TrackerView()
        case .jobBank:
            JobBankView()
        case .noc:
            NOCView()
        case .salaryInsights:
            SalaryInsightsView()
        case .linkedInOptimizer:
            LinkedInOptimizerView()
        case .organization:
            OrganizationView()
        case .subscription:
            SubscriptionView()
        case .profile:
            ProfileView(viewModel: authViewModel)
        case .settings:
            SettingsView()
        default:
            Text(screen.label)
        }
    }
}

// MARK: - iPad Layout (Sidebar + Detail)

struct iPadMainView: View {
    @ObservedObject var authViewModel: AuthViewModelWrapper
    @Binding var deepLinkTab: Int?
    @State private var selectedScreen: AppScreen? = .home

    private let sidebarSections: [MenuSection] = [
        MenuSection(title: "Main", items: [.home, .resume, .optimizer, .coverLetter]),
        MenuSection(title: "Career Tools", items: [.interview, .tracker, .jobBank, .noc]),
        MenuSection(title: "Insights", items: [.salaryInsights, .linkedInOptimizer]),
        MenuSection(title: "Account", items: [.organization, .subscription, .profile, .settings])
    ]
    
    var body: some View {
        NavigationSplitView {
            List(selection: $selectedScreen) {
                ForEach(sidebarSections) { section in
                    Section(section.title) {
                        ForEach(section.items) { screen in
                            Label(screen.label, systemImage: screen.icon)
                                .tag(screen)
                        }
                    }
                }
            }
            .navigationTitle("VwaTek Apply")
        } detail: {
            Group {
                switch selectedScreen {
                case .home:
                    HomeView(userName: authViewModel.fullName, selectedTab: tabBinding)
                case .resume:
                    ResumeView()
                case .optimizer:
                    OptimizerView()
                case .coverLetter:
                    CoverLetterView()
                case .interview:
                    InterviewView()
                case .noc:
                    NOCView()
                case .jobBank:
                    JobBankView()
                case .tracker:
                    TrackerView()
                case .salaryInsights:
                    SalaryInsightsView()
                case .linkedInOptimizer:
                    LinkedInOptimizerView()
                case .organization:
                    OrganizationView()
                case .subscription:
                    SubscriptionView()
                case .profile:
                    ProfileView(viewModel: authViewModel)
                case .settings:
                    SettingsView()
                case .none:
                    Text("Select a section")
                }
            }
        }
        .onChange(of: deepLinkTab) { newTab in
            guard let tab = newTab else { return }
            if let screen = AppScreen.fromDeepLinkTag(tab) {
                selectedScreen = screen
            }
            deepLinkTab = nil
        }
    }
    
    /// Bridge for HomeView's selectedTab binding (Int-based)
    private var tabBinding: Binding<Int> {
        Binding(
            get: { selectedScreen?.deepLinkTag ?? 0 },
            set: { tag in
                if let screen = AppScreen.fromDeepLinkTag(tag) {
                    selectedScreen = screen
                }
            }
        )
    }
}

#Preview {
    ContentView()
}
