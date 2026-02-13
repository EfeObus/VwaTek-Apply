import Foundation

/// Helper class for managing app settings using UserDefaults
/// Provides a centralized way to persist and retrieve user preferences
class SettingsHelper {
    
    // MARK: - Singleton
    
    static let shared = SettingsHelper()
    
    private let defaults = UserDefaults.standard
    private let keyPrefix = "vwatek_"
    
    private init() {}
    
    // MARK: - Generic Methods
    
    /// Get a setting value by key
    /// - Parameter key: The setting key
    /// - Returns: The value as a String, or nil if not set
    func getSetting(key: String) -> String? {
        return defaults.string(forKey: prefixedKey(key))
    }
    
    /// Set a setting value
    /// - Parameters:
    ///   - key: The setting key
    ///   - value: The value to store
    func setSetting(key: String, value: String) {
        defaults.set(value, forKey: prefixedKey(key))
    }
    
    /// Remove a setting
    /// - Parameter key: The setting key to remove
    func removeSetting(key: String) {
        defaults.removeObject(forKey: prefixedKey(key))
    }
    
    /// Check if a setting exists
    /// - Parameter key: The setting key
    /// - Returns: True if the setting exists
    func hasSetting(key: String) -> Bool {
        return defaults.object(forKey: prefixedKey(key)) != nil
    }
    
    // MARK: - Boolean Helpers
    
    /// Get a boolean setting value
    /// - Parameters:
    ///   - key: The setting key
    ///   - defaultValue: Default value if not set
    /// - Returns: The boolean value
    func getBool(key: String, defaultValue: Bool = false) -> Bool {
        guard let value = getSetting(key: key) else {
            return defaultValue
        }
        return value.lowercased() == "true"
    }
    
    /// Set a boolean setting value
    /// - Parameters:
    ///   - key: The setting key
    ///   - value: The boolean value to store
    func setBool(key: String, value: Bool) {
        setSetting(key: key, value: String(value))
    }
    
    // MARK: - Int Helpers
    
    /// Get an integer setting value
    /// - Parameters:
    ///   - key: The setting key
    ///   - defaultValue: Default value if not set
    /// - Returns: The integer value
    func getInt(key: String, defaultValue: Int = 0) -> Int {
        guard let value = getSetting(key: key) else {
            return defaultValue
        }
        return Int(value) ?? defaultValue
    }
    
    /// Set an integer setting value
    /// - Parameters:
    ///   - key: The setting key
    ///   - value: The integer value to store
    func setInt(key: String, value: Int) {
        setSetting(key: key, value: String(value))
    }
    
    // MARK: - Clear All
    
    /// Clear all app settings
    func clearAll() {
        let allKeys = defaults.dictionaryRepresentation().keys
        for key in allKeys {
            if key.hasPrefix(keyPrefix) {
                defaults.removeObject(forKey: key)
            }
        }
    }
    
    // MARK: - Private
    
    private func prefixedKey(_ key: String) -> String {
        return "\(keyPrefix)\(key)"
    }
}

// MARK: - Setting Keys

extension SettingsHelper {
    
    struct Keys {
        // API Keys
        static let geminiApiKey = "gemini_api_key"
        static let openAiApiKey = "openai_api_key"
        
        // Locale
        static let locale = "locale"
        
        // Notifications
        static let pushNotifications = "push_notifications"
        static let emailNotifications = "email_notifications"
        static let interviewReminders = "interview_reminders"
        static let weeklyDigest = "weekly_digest"
        
        // Appearance
        static let darkMode = "dark_mode"
        
        // Privacy
        static let analyticsEnabled = "analytics_enabled"
        static let crashReportingEnabled = "crash_reporting_enabled"
    }
}
