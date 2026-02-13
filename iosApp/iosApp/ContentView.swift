import SwiftUI
import shared

struct ContentView: View {
    @StateObject private var authViewModel = AuthViewModelWrapper()
    
    var body: some View {
        Group {
            if authViewModel.isAuthenticated {
                MainTabView(authViewModel: authViewModel)
            } else {
                AuthView(viewModel: authViewModel)
            }
        }
        .task {
            authViewModel.observeState()
        }
    }
}

// MARK: - Main Tab View (for authenticated users)
struct MainTabView: View {
    @ObservedObject var authViewModel: AuthViewModelWrapper
    @State private var selectedTab = 0
    
    var body: some View {
        TabView(selection: $selectedTab) {
            HomeView(userName: authViewModel.fullName, selectedTab: $selectedTab)
                .tabItem {
                    Label("Home", systemImage: "house.fill")
                }
                .tag(0)
            
            ResumeView()
                .tabItem {
                    Label("Resume", systemImage: "doc.text.fill")
                }
                .tag(1)
            
            OptimizerView()
                .tabItem {
                    Label("Optimizer", systemImage: "wand.and.stars")
                }
                .tag(2)
            
            CoverLetterView()
                .tabItem {
                    Label("Cover Letter", systemImage: "envelope.fill")
                }
                .tag(3)
            
            InterviewView()
                .tabItem {
                    Label("Interview", systemImage: "mic.fill")
                }
                .tag(4)
            
            NOCView()
                .tabItem {
                    Label("NOC", systemImage: "briefcase.fill")
                }
                .tag(5)
            
            JobBankView()
                .tabItem {
                    Label("Job Bank", systemImage: "magnifyingglass")
                }
                .tag(6)
            
            ProfileView(viewModel: authViewModel)
                .tabItem {
                    Label("Profile", systemImage: "person.fill")
                }
                .tag(7)
            
            SettingsView()
                .tabItem {
                    Label("Settings", systemImage: "gear")
                }
                .tag(8)
        }
        .accentColor(.blue)
    }
}

// For iPad - use NavigationSplitView
struct iPadMainView: View {
    @ObservedObject var authViewModel: AuthViewModelWrapper
    @State private var selectedSection: NavigationSection? = .home
    @State private var selectedTab = 0
    
    enum NavigationSection: String, CaseIterable {
        case home = "Home"
        case resume = "Resume"
        case optimizer = "Optimizer"
        case coverLetter = "Cover Letter"
        case interview = "Interview"
        case noc = "NOC"
        case jobBank = "Job Bank"
        case profile = "Profile"
        case settings = "Settings"
        
        var icon: String {
            switch self {
            case .home: return "house.fill"
            case .resume: return "doc.text.fill"
            case .optimizer: return "wand.and.stars"
            case .coverLetter: return "envelope.fill"
            case .interview: return "mic.fill"
            case .noc: return "briefcase.fill"
            case .jobBank: return "magnifyingglass"
            case .profile: return "person.fill"
            case .settings: return "gear"
            }
        }
    }
    
    var body: some View {
        NavigationSplitView {
            List(NavigationSection.allCases, id: \.self, selection: $selectedSection) { section in
                Label(section.rawValue, systemImage: section.icon)
            }
            .navigationTitle("VwaTek Apply")
        } detail: {
            switch selectedSection {
            case .home:
                HomeView(userName: authViewModel.fullName, selectedTab: $selectedTab)
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
            case .profile:
                ProfileView(viewModel: authViewModel)
            case .settings:
                SettingsView()
            case .none:
                Text("Select a section")
            }
        }
    }
}

#Preview {
    ContentView()
}
