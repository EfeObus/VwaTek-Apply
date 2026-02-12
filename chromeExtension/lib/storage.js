/**
 * VwaTek Apply Chrome Extension - Storage Utility
 * Handles local and sync storage operations
 */

const DEFAULT_API_URL = 'https://api.vwatek.com/apply';

export class Storage {
    /**
     * Get authentication data
     */
    async getAuth() {
        const result = await chrome.storage.local.get(['authToken', 'authUser']);
        
        if (result.authToken && result.authUser) {
            return {
                token: result.authToken,
                user: result.authUser
            };
        }
        
        return null;
    }
    
    /**
     * Set authentication data
     */
    async setAuth(data) {
        await chrome.storage.local.set({
            authToken: data.token,
            authUser: data.user
        });
    }
    
    /**
     * Clear authentication data
     */
    async clearAuth() {
        await chrome.storage.local.remove(['authToken', 'authUser']);
    }
    
    /**
     * Get API base URL
     */
    async getApiBaseUrl() {
        const result = await chrome.storage.sync.get(['apiBaseUrl']);
        return result.apiBaseUrl || DEFAULT_API_URL;
    }
    
    /**
     * Set API base URL
     */
    async setApiBaseUrl(url) {
        await chrome.storage.sync.set({ apiBaseUrl: url });
    }
    
    /**
     * Get user preferences
     */
    async getPreferences() {
        const defaults = {
            dailySummaryEnabled: true,
            notificationsEnabled: true,
            autoDetectJobs: true,
            showSaveButton: true,
            theme: 'auto',
            defaultProvince: null,
            quickSaveConfirmation: true
        };
        
        const result = await chrome.storage.sync.get(['preferences']);
        return { ...defaults, ...result.preferences };
    }
    
    /**
     * Update user preferences
     */
    async setPreferences(preferences) {
        const current = await this.getPreferences();
        await chrome.storage.sync.set({
            preferences: { ...current, ...preferences }
        });
    }
    
    /**
     * Get saved jobs (offline cache)
     */
    async getSavedJobs() {
        const result = await chrome.storage.local.get(['savedJobs']);
        return result.savedJobs || [];
    }
    
    /**
     * Add a job to offline cache
     */
    async addSavedJob(job) {
        const jobs = await this.getSavedJobs();
        jobs.unshift({
            ...job,
            savedAt: new Date().toISOString(),
            synced: false
        });
        
        // Keep only last 100 jobs in cache
        if (jobs.length > 100) {
            jobs.pop();
        }
        
        await chrome.storage.local.set({ savedJobs: jobs });
    }
    
    /**
     * Mark job as synced
     */
    async markJobSynced(jobId) {
        const jobs = await this.getSavedJobs();
        const index = jobs.findIndex(j => j.id === jobId);
        
        if (index !== -1) {
            jobs[index].synced = true;
            await chrome.storage.local.set({ savedJobs: jobs });
        }
    }
    
    /**
     * Get unsynced jobs
     */
    async getUnsyncedJobs() {
        const jobs = await this.getSavedJobs();
        return jobs.filter(j => !j.synced);
    }
    
    /**
     * Get upcoming reminders (local cache)
     */
    async getUpcomingReminders() {
        const result = await chrome.storage.local.get(['upcomingReminders']);
        return result.upcomingReminders || [];
    }
    
    /**
     * Set upcoming reminders
     */
    async setUpcomingReminders(reminders) {
        await chrome.storage.local.set({ upcomingReminders: reminders });
    }
    
    /**
     * Get recently viewed jobs (for quick access)
     */
    async getRecentJobs() {
        const result = await chrome.storage.local.get(['recentJobs']);
        return result.recentJobs || [];
    }
    
    /**
     * Add to recent jobs
     */
    async addRecentJob(job) {
        const jobs = await this.getRecentJobs();
        
        // Remove if already exists
        const filtered = jobs.filter(j => j.url !== job.url);
        
        // Add to beginning
        filtered.unshift({
            title: job.title,
            company: job.company,
            url: job.url,
            viewedAt: new Date().toISOString()
        });
        
        // Keep only last 20
        const trimmed = filtered.slice(0, 20);
        
        await chrome.storage.local.set({ recentJobs: trimmed });
    }
    
    /**
     * Clear all local storage
     */
    async clearAll() {
        await chrome.storage.local.clear();
    }
    
    /**
     * Get storage usage
     */
    async getUsage() {
        return new Promise((resolve) => {
            chrome.storage.local.getBytesInUse(null, (bytesInUse) => {
                resolve({
                    bytesUsed: bytesInUse,
                    maxBytes: chrome.storage.local.QUOTA_BYTES,
                    percentUsed: (bytesInUse / chrome.storage.local.QUOTA_BYTES) * 100
                });
            });
        });
    }
}

export default Storage;
