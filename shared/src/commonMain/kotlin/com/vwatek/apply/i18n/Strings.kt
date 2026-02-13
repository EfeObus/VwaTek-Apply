package com.vwatek.apply.i18n

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Internationalization Framework for VwaTek Apply
 * 
 * Supports English (en) and French (fr) for Canadian market
 */

enum class Locale(val code: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    FRENCH("fr", "French", "Français")
}

/**
 * String resources interface
 */
interface Strings {
    // Common
    val appName: String
    val save: String
    val cancel: String
    val delete: String
    val edit: String
    val loading: String
    val error: String
    val retry: String
    val search: String
    val filter: String
    val clear: String
    val apply: String
    val close: String
    val back: String
    val next: String
    val done: String
    val yes: String
    val no: String
    val ok: String
    val confirm: String
    val submit: String
    val required: String
    val optional: String
    
    // Navigation
    val navResume: String
    val navOptimizer: String
    val navCoverLetter: String
    val navInterview: String
    val navTracker: String
    val navNOC: String
    val navJobBank: String
    val navSettings: String
    val navProfile: String
    val navHelp: String
    
    // Auth
    val signIn: String
    val signUp: String
    val signOut: String
    val forgotPassword: String
    val email: String
    val password: String
    val confirmPassword: String
    val firstName: String
    val lastName: String
    val createAccount: String
    val welcomeBack: String
    val continueWithGoogle: String
    val continueWithLinkedIn: String
    val orContinueWith: String
    
    // Resume
    val resumeTitle: String
    val resumeCreate: String
    val resumeEdit: String
    val resumeAnalyze: String
    val resumeMatchScore: String
    val resumeKeywordsFound: String
    val resumeKeywordsMissing: String
    val resumeUpload: String
    val resumeImportLinkedIn: String
    val resumeExport: String
    val resumeVersion: String
    val resumeIndustry: String
    
    // Cover Letter
    val coverLetterTitle: String
    val coverLetterCreate: String
    val coverLetterGenerate: String
    val coverLetterTone: String
    val coverLetterJobTitle: String
    val coverLetterCompany: String
    
    // Interview
    val interviewTitle: String
    val interviewPractice: String
    val interviewQuestions: String
    val interviewFeedback: String
    val interviewTips: String
    
    // Job Tracker
    val trackerTitle: String
    val trackerSaved: String
    val trackerApplied: String
    val trackerScreening: String
    val trackerInterview: String
    val trackerOffer: String
    val trackerRejected: String
    val trackerAddJob: String
    val trackerNoJobs: String
    val trackerViewKanban: String
    val trackerViewList: String
    val trackerViewCalendar: String
    val trackerStats: String
    val trackerApplications: String
    val trackerInterviews: String
    val trackerOffers: String
    val trackerResponseRate: String
    
    // Canadian Specific - NOC
    val nocTitle: String
    val nocCode: String
    val nocSearch: String
    val nocMatchScore: String
    val nocTeerLevel: String
    val nocMainDuties: String
    val nocRequirements: String
    val nocSkills: String
    val nocExampleTitles: String
    val nocProvincialDemand: String
    val nocImmigrationPathways: String
    val nocAnalyzeResume: String
    val nocFitAnalysis: String
    
    // Canadian Specific - Immigration
    val workPermitRequired: String
    val lmiaRequired: String
    val province: String
    val expressEntry: String
    val provincialNominee: String
    val canadianExperience: String
    val crsPoints: String
    
    // Job Bank
    val jobBankTitle: String
    val jobBankSearch: String
    val jobBankSaveJob: String
    val jobBankApply: String
    val jobBankSalary: String
    val jobBankLocation: String
    val jobBankPostedDate: String
    val jobBankExpiry: String
    
    // Settings
    val settingsTitle: String
    val settingsLanguage: String
    val settingsTheme: String
    val settingsNotifications: String
    val settingsPrivacy: String
    val settingsDataExport: String
    val settingsDeleteAccount: String
    val settingsAbout: String
    val settingsVersion: String
    
    // Errors
    val errorNetwork: String
    val errorServer: String
    val errorUnauthorized: String
    val errorNotFound: String
    val errorValidation: String
    val errorUnknown: String
    
    // Success messages
    val successSaved: String
    val successDeleted: String
    val successUpdated: String
    val successCopied: String
}

/**
 * English Strings
 */
object EnglishStrings : Strings {
    // Common
    override val appName = "VwaTek Apply"
    override val save = "Save"
    override val cancel = "Cancel"
    override val delete = "Delete"
    override val edit = "Edit"
    override val loading = "Loading..."
    override val error = "An error occurred"
    override val retry = "Retry"
    override val search = "Search"
    override val filter = "Filter"
    override val clear = "Clear"
    override val apply = "Apply"
    override val close = "Close"
    override val back = "Back"
    override val next = "Next"
    override val done = "Done"
    override val yes = "Yes"
    override val no = "No"
    override val ok = "OK"
    override val confirm = "Confirm"
    override val submit = "Submit"
    override val required = "Required"
    override val optional = "Optional"
    
    // Navigation
    override val navResume = "Resume"
    override val navOptimizer = "Optimizer"
    override val navCoverLetter = "Cover Letter"
    override val navInterview = "Interview"
    override val navTracker = "Tracker"
    override val navNOC = "NOC Codes"
    override val navJobBank = "Job Bank"
    override val navSettings = "Settings"
    override val navProfile = "Profile"
    override val navHelp = "Help"
    
    // Auth
    override val signIn = "Sign In"
    override val signUp = "Sign Up"
    override val signOut = "Sign Out"
    override val forgotPassword = "Forgot Password?"
    override val email = "Email"
    override val password = "Password"
    override val confirmPassword = "Confirm Password"
    override val firstName = "First Name"
    override val lastName = "Last Name"
    override val createAccount = "Create Account"
    override val welcomeBack = "Welcome Back"
    override val continueWithGoogle = "Continue with Google"
    override val continueWithLinkedIn = "Continue with LinkedIn"
    override val orContinueWith = "or continue with"
    
    // Resume
    override val resumeTitle = "My Resumes"
    override val resumeCreate = "Create Resume"
    override val resumeEdit = "Edit Resume"
    override val resumeAnalyze = "Analyze Resume"
    override val resumeMatchScore = "Match Score"
    override val resumeKeywordsFound = "Keywords Found"
    override val resumeKeywordsMissing = "Missing Keywords"
    override val resumeUpload = "Upload Resume"
    override val resumeImportLinkedIn = "Import from LinkedIn"
    override val resumeExport = "Export"
    override val resumeVersion = "Version"
    override val resumeIndustry = "Industry"
    
    // Cover Letter
    override val coverLetterTitle = "Cover Letters"
    override val coverLetterCreate = "Create Cover Letter"
    override val coverLetterGenerate = "Generate with AI"
    override val coverLetterTone = "Tone"
    override val coverLetterJobTitle = "Job Title"
    override val coverLetterCompany = "Company"
    
    // Interview
    override val interviewTitle = "Interview Prep"
    override val interviewPractice = "Practice Interview"
    override val interviewQuestions = "Common Questions"
    override val interviewFeedback = "Feedback"
    override val interviewTips = "Tips"
    
    // Job Tracker
    override val trackerTitle = "Job Tracker"
    override val trackerSaved = "Saved"
    override val trackerApplied = "Applied"
    override val trackerScreening = "Screening"
    override val trackerInterview = "Interview"
    override val trackerOffer = "Offer"
    override val trackerRejected = "Rejected"
    override val trackerAddJob = "Add Job"
    override val trackerNoJobs = "No jobs tracked yet"
    override val trackerViewKanban = "Kanban"
    override val trackerViewList = "List"
    override val trackerViewCalendar = "Calendar"
    override val trackerStats = "Statistics"
    override val trackerApplications = "Applications"
    override val trackerInterviews = "Interviews"
    override val trackerOffers = "Offers"
    override val trackerResponseRate = "Response Rate"
    
    // Canadian Specific - NOC
    override val nocTitle = "NOC Codes"
    override val nocCode = "NOC Code"
    override val nocSearch = "Search NOC Codes"
    override val nocMatchScore = "NOC Match Score"
    override val nocTeerLevel = "TEER Level"
    override val nocMainDuties = "Main Duties"
    override val nocRequirements = "Employment Requirements"
    override val nocSkills = "Skills"
    override val nocExampleTitles = "Example Job Titles"
    override val nocProvincialDemand = "Provincial Demand"
    override val nocImmigrationPathways = "Immigration Pathways"
    override val nocAnalyzeResume = "Analyze Resume Fit"
    override val nocFitAnalysis = "NOC Fit Analysis"
    
    // Canadian Specific - Immigration
    override val workPermitRequired = "Work Permit Required"
    override val lmiaRequired = "LMIA Required"
    override val province = "Province"
    override val expressEntry = "Express Entry"
    override val provincialNominee = "Provincial Nominee Program"
    override val canadianExperience = "Canadian Experience Class"
    override val crsPoints = "CRS Points"
    
    // Job Bank
    override val jobBankTitle = "Job Bank Canada"
    override val jobBankSearch = "Search Jobs"
    override val jobBankSaveJob = "Save Job"
    override val jobBankApply = "Apply"
    override val jobBankSalary = "Salary"
    override val jobBankLocation = "Location"
    override val jobBankPostedDate = "Posted"
    override val jobBankExpiry = "Expires"
    
    // Settings
    override val settingsTitle = "Settings"
    override val settingsLanguage = "Language"
    override val settingsTheme = "Theme"
    override val settingsNotifications = "Notifications"
    override val settingsPrivacy = "Privacy"
    override val settingsDataExport = "Export Data"
    override val settingsDeleteAccount = "Delete Account"
    override val settingsAbout = "About"
    override val settingsVersion = "Version"
    
    // Errors
    override val errorNetwork = "Network error. Please check your connection."
    override val errorServer = "Server error. Please try again later."
    override val errorUnauthorized = "Please sign in to continue."
    override val errorNotFound = "The requested resource was not found."
    override val errorValidation = "Please check your input and try again."
    override val errorUnknown = "An unexpected error occurred."
    
    // Success messages
    override val successSaved = "Saved successfully"
    override val successDeleted = "Deleted successfully"
    override val successUpdated = "Updated successfully"
    override val successCopied = "Copied to clipboard"
}

/**
 * French Strings
 */
object FrenchStrings : Strings {
    // Common
    override val appName = "VwaTek Apply"
    override val save = "Enregistrer"
    override val cancel = "Annuler"
    override val delete = "Supprimer"
    override val edit = "Modifier"
    override val loading = "Chargement..."
    override val error = "Une erreur s'est produite"
    override val retry = "Réessayer"
    override val search = "Rechercher"
    override val filter = "Filtrer"
    override val clear = "Effacer"
    override val apply = "Appliquer"
    override val close = "Fermer"
    override val back = "Retour"
    override val next = "Suivant"
    override val done = "Terminé"
    override val yes = "Oui"
    override val no = "Non"
    override val ok = "OK"
    override val confirm = "Confirmer"
    override val submit = "Soumettre"
    override val required = "Requis"
    override val optional = "Facultatif"
    
    // Navigation
    override val navResume = "CV"
    override val navOptimizer = "Optimiseur"
    override val navCoverLetter = "Lettre de motivation"
    override val navInterview = "Entrevue"
    override val navTracker = "Suivi"
    override val navNOC = "Codes CNP"
    override val navJobBank = "Guichet-Emplois"
    override val navSettings = "Paramètres"
    override val navProfile = "Profil"
    override val navHelp = "Aide"
    
    // Auth
    override val signIn = "Connexion"
    override val signUp = "Inscription"
    override val signOut = "Déconnexion"
    override val forgotPassword = "Mot de passe oublié?"
    override val email = "Courriel"
    override val password = "Mot de passe"
    override val confirmPassword = "Confirmer le mot de passe"
    override val firstName = "Prénom"
    override val lastName = "Nom de famille"
    override val createAccount = "Créer un compte"
    override val welcomeBack = "Bon retour"
    override val continueWithGoogle = "Continuer avec Google"
    override val continueWithLinkedIn = "Continuer avec LinkedIn"
    override val orContinueWith = "ou continuer avec"
    
    // Resume
    override val resumeTitle = "Mes CV"
    override val resumeCreate = "Créer un CV"
    override val resumeEdit = "Modifier le CV"
    override val resumeAnalyze = "Analyser le CV"
    override val resumeMatchScore = "Score de correspondance"
    override val resumeKeywordsFound = "Mots-clés trouvés"
    override val resumeKeywordsMissing = "Mots-clés manquants"
    override val resumeUpload = "Téléverser un CV"
    override val resumeImportLinkedIn = "Importer de LinkedIn"
    override val resumeExport = "Exporter"
    override val resumeVersion = "Version"
    override val resumeIndustry = "Industrie"
    
    // Cover Letter
    override val coverLetterTitle = "Lettres de motivation"
    override val coverLetterCreate = "Créer une lettre"
    override val coverLetterGenerate = "Générer avec l'IA"
    override val coverLetterTone = "Ton"
    override val coverLetterJobTitle = "Titre du poste"
    override val coverLetterCompany = "Entreprise"
    
    // Interview
    override val interviewTitle = "Préparation d'entrevue"
    override val interviewPractice = "Pratiquer l'entrevue"
    override val interviewQuestions = "Questions courantes"
    override val interviewFeedback = "Rétroaction"
    override val interviewTips = "Conseils"
    
    // Job Tracker
    override val trackerTitle = "Suivi des emplois"
    override val trackerSaved = "Enregistré"
    override val trackerApplied = "Postulé"
    override val trackerScreening = "Présélection"
    override val trackerInterview = "Entrevue"
    override val trackerOffer = "Offre"
    override val trackerRejected = "Refusé"
    override val trackerAddJob = "Ajouter un emploi"
    override val trackerNoJobs = "Aucun emploi suivi"
    override val trackerViewKanban = "Kanban"
    override val trackerViewList = "Liste"
    override val trackerViewCalendar = "Calendrier"
    override val trackerStats = "Statistiques"
    override val trackerApplications = "Candidatures"
    override val trackerInterviews = "Entrevues"
    override val trackerOffers = "Offres"
    override val trackerResponseRate = "Taux de réponse"
    
    // Canadian Specific - NOC
    override val nocTitle = "Codes CNP"
    override val nocCode = "Code CNP"
    override val nocSearch = "Rechercher les codes CNP"
    override val nocMatchScore = "Score CNP"
    override val nocTeerLevel = "Niveau FEER"
    override val nocMainDuties = "Fonctions principales"
    override val nocRequirements = "Conditions d'accès"
    override val nocSkills = "Compétences"
    override val nocExampleTitles = "Exemples d'appellations d'emploi"
    override val nocProvincialDemand = "Demande provinciale"
    override val nocImmigrationPathways = "Voies d'immigration"
    override val nocAnalyzeResume = "Analyser l'adéquation du CV"
    override val nocFitAnalysis = "Analyse d'adéquation CNP"
    
    // Canadian Specific - Immigration
    override val workPermitRequired = "Permis de travail requis"
    override val lmiaRequired = "EIMT requise"
    override val province = "Province"
    override val expressEntry = "Entrée express"
    override val provincialNominee = "Programme des candidats des provinces"
    override val canadianExperience = "Catégorie de l'expérience canadienne"
    override val crsPoints = "Points SGC"
    
    // Job Bank
    override val jobBankTitle = "Guichet-Emplois"
    override val jobBankSearch = "Rechercher des emplois"
    override val jobBankSaveJob = "Sauvegarder l'emploi"
    override val jobBankApply = "Postuler"
    override val jobBankSalary = "Salaire"
    override val jobBankLocation = "Lieu"
    override val jobBankPostedDate = "Publié le"
    override val jobBankExpiry = "Date limite"
    
    // Settings
    override val settingsTitle = "Paramètres"
    override val settingsLanguage = "Langue"
    override val settingsTheme = "Thème"
    override val settingsNotifications = "Notifications"
    override val settingsPrivacy = "Confidentialité"
    override val settingsDataExport = "Exporter les données"
    override val settingsDeleteAccount = "Supprimer le compte"
    override val settingsAbout = "À propos"
    override val settingsVersion = "Version"
    
    // Errors
    override val errorNetwork = "Erreur réseau. Vérifiez votre connexion."
    override val errorServer = "Erreur serveur. Veuillez réessayer plus tard."
    override val errorUnauthorized = "Veuillez vous connecter pour continuer."
    override val errorNotFound = "La ressource demandée est introuvable."
    override val errorValidation = "Vérifiez vos données et réessayez."
    override val errorUnknown = "Une erreur inattendue s'est produite."
    
    // Success messages
    override val successSaved = "Enregistré avec succès"
    override val successDeleted = "Supprimé avec succès"
    override val successUpdated = "Mis à jour avec succès"
    override val successCopied = "Copié dans le presse-papiers"
}

/**
 * Locale Manager - manages current locale and provides localized strings
 */
object LocaleManager {
    private val _currentLocale = MutableStateFlow(Locale.ENGLISH)
    val currentLocale: StateFlow<Locale> = _currentLocale.asStateFlow()
    
    val strings: Strings
        get() = when (_currentLocale.value) {
            Locale.FRENCH -> FrenchStrings
            Locale.ENGLISH -> EnglishStrings
        }
    
    fun setLocale(locale: Locale) {
        _currentLocale.value = locale
    }
    
    fun setLocaleByCode(code: String) {
        val locale = Locale.entries.find { it.code == code } ?: Locale.ENGLISH
        setLocale(locale)
    }
    
    fun toggleLocale() {
        _currentLocale.value = when (_currentLocale.value) {
            Locale.ENGLISH -> Locale.FRENCH
            Locale.FRENCH -> Locale.ENGLISH
        }
    }
    
    /**
     * Get localized string for bilingual content
     */
    fun getLocalizedString(english: String, french: String): String {
        return when (_currentLocale.value) {
            Locale.ENGLISH -> english
            Locale.FRENCH -> french
        }
    }
}

/**
 * Extension for easy access to localized strings in Composables
 */
val strings: Strings get() = LocaleManager.strings
