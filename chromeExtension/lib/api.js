/**
 * VwaTek Apply Chrome Extension - API Client
 * Handles all communication with the VwaTek Apply backend
 * 
 * Backend API Endpoints:
 * - Job Tracker: /api/v1/jobs/*
 * - Notifications: /api/v1/notifications/*
 */

const DEFAULT_API_URL = 'https://api.vwatek.com/apply';

export class ApiClient {
    constructor() {
        this.baseUrl = null;
    }
    
    /**
     * Get the API base URL
     */
    async getBaseUrl() {
        if (this.baseUrl) return this.baseUrl;
        
        const result = await chrome.storage.sync.get(['apiBaseUrl']);
        this.baseUrl = result.apiBaseUrl || DEFAULT_API_URL;
        return this.baseUrl;
    }
    
    /**
     * Make an authenticated API request
     */
    async request(method, endpoint, token, data = null) {
        const baseUrl = await this.getBaseUrl();
        const url = `${baseUrl}${endpoint}`;
        
        const headers = {
            'Content-Type': 'application/json',
            'X-Extension-Version': chrome.runtime.getManifest().version
        };
        
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }
        
        const options = {
            method,
            headers
        };
        
        if (data && (method === 'POST' || method === 'PUT' || method === 'PATCH')) {
            options.body = JSON.stringify(data);
        }
        
        try {
            const response = await fetch(url, options);
            
            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || `HTTP error ${response.status}`);
            }
            
            // Handle empty response
            const text = await response.text();
            return text ? JSON.parse(text) : {};
        } catch (error) {
            console.error(`API error (${method} ${endpoint}):`, error);
            throw error;
        }
    }
    
    /**
     * Verify if a token is still valid
     */
    async verifyToken(token) {
        try {
            const result = await this.request('GET', '/api/v1/auth/verify', token);
            return { valid: true, user: result.user };
        } catch {
            return { valid: false };
        }
    }
    
    /**
     * Get tracker stats
     * Backend: GET /api/v1/jobs/stats
     * Returns: { totalApplications, savedCount, appliedCount, interviewCount, offerCount, rejectedCount, interviewRate, offerRate }
     */
    async getStats(token) {
        const stats = await this.request('GET', '/api/v1/jobs/stats', token);
        // Map backend response to extension expected format
        return {
            totalApplications: stats.totalApplications || 0,
            activeApplications: (stats.appliedCount || 0) + (stats.interviewCount || 0),
            interviewsScheduled: stats.interviewCount || 0,
            offersReceived: stats.offerCount || 0,
            savedCount: stats.savedCount || 0,
            appliedCount: stats.appliedCount || 0,
            rejectedCount: stats.rejectedCount || 0,
            interviewRate: stats.interviewRate || 0,
            offerRate: stats.offerRate || 0
        };
    }
    
    /**
     * Quick save a job (minimal data)
     * Backend: POST /api/v1/jobs/quick
     * Request: { jobTitle, companyName, jobUrl, jobBoardSource, externalJobId }
     */
    async quickSaveJob(token, jobData) {
        return this.request('POST', '/api/v1/jobs/quick', token, {
            jobTitle: jobData.title,
            companyName: jobData.company,
            jobUrl: jobData.jobUrl,
            jobBoardSource: jobData.source || 'OTHER',
            externalJobId: jobData.externalJobId || null
        });
    }
    
    /**
     * Create a full job application
     * Backend: POST /api/v1/jobs
     */
    async createApplication(token, applicationData) {
        // Map extension fields to backend expected fields
        return this.request('POST', '/api/v1/jobs', token, {
            jobTitle: applicationData.title || applicationData.jobTitle,
            companyName: applicationData.company || applicationData.companyName,
            companyLogo: applicationData.companyLogo,
            jobUrl: applicationData.jobUrl,
            jobDescription: applicationData.description || applicationData.jobDescription,
            jobBoardSource: applicationData.source || applicationData.jobBoardSource,
            externalJobId: applicationData.externalJobId,
            city: applicationData.city,
            province: applicationData.province,
            country: applicationData.country || 'Canada',
            isRemote: applicationData.workModel === 'REMOTE' || applicationData.isRemote,
            isHybrid: applicationData.workModel === 'HYBRID' || applicationData.isHybrid,
            salaryMin: applicationData.salaryMin ? parseInt(applicationData.salaryMin) : null,
            salaryMax: applicationData.salaryMax ? parseInt(applicationData.salaryMax) : null,
            salaryCurrency: applicationData.currency || 'CAD',
            salaryPeriod: applicationData.salaryPeriod,
            status: applicationData.status || 'SAVED',
            nocCode: applicationData.nocCode,
            contactName: applicationData.contactName,
            contactEmail: applicationData.contactEmail,
            contactPhone: applicationData.contactPhone
        });
    }
    
    /**
     * Get all applications
     * Backend: GET /api/v1/jobs
     * Query params: status, source, province, search, limit, offset
     */
    async getApplications(token, params = {}) {
        const queryString = new URLSearchParams(params).toString();
        const endpoint = `/api/v1/jobs${queryString ? `?${queryString}` : ''}`;
        return this.request('GET', endpoint, token);
    }
    
    /**
     * Get a single application with details
     * Backend: GET /api/v1/jobs/{id}
     */
    async getApplication(token, applicationId) {
        return this.request('GET', `/api/v1/jobs/${applicationId}`, token);
    }
    
    /**
     * Update application status
     * Backend: PATCH /api/v1/jobs/{id}/status
     * Request: { status, notes }
     */
    async updateStatus(token, applicationId, status, notes = null) {
        return this.request('PATCH', `/api/v1/jobs/${applicationId}/status`, token, {
            status,
            notes  // Note: backend uses 'notes' not 'note'
        });
    }
    
    /**
     * Add a note to an application
     * Backend: POST /api/v1/jobs/{id}/notes
     * Request: { content, noteType }
     */
    async addNote(token, applicationId, content, noteType = 'GENERAL') {
        return this.request('POST', `/api/v1/jobs/${applicationId}/notes`, token, {
            content,
            noteType  // Backend uses noteType, not isPinned
        });
    }
    
    /**
     * Add a reminder to an application
     * Backend: POST /api/v1/jobs/{id}/reminders
     * Request: { reminderType, title, message, reminderAt }
     */
    async addReminder(token, applicationId, reminderData) {
        return this.request('POST', `/api/v1/jobs/${applicationId}/reminders`, token, {
            reminderType: reminderData.reminderType || 'FOLLOW_UP',
            title: reminderData.title,
            message: reminderData.message,
            reminderAt: reminderData.reminderAt  // ISO 8601 datetime string
        });
    }
    
    /**
     * Get upcoming reminders across all applications
     * Backend: GET /api/v1/jobs/reminders/upcoming
     */
    async getUpcomingReminders(token) {
        return this.request('GET', '/api/v1/jobs/reminders/upcoming', token);
    }
    
    /**
     * Mark a reminder as complete
     * Backend: PATCH /api/v1/jobs/{applicationId}/reminders/{reminderId}/complete
     */
    async completeReminder(token, applicationId, reminderId) {
        return this.request('PATCH', `/api/v1/jobs/${applicationId}/reminders/${reminderId}/complete`, token);
    }
    
    /**
     * Get notification preferences
     * Backend: GET /api/v1/notifications/preferences
     */
    async getNotificationPreferences(token) {
        return this.request('GET', '/api/v1/notifications/preferences', token);
    }
    
    /**
     * Update notification preferences
     * Backend: PUT /api/v1/notifications/preferences
     */
    async updateNotificationPreferences(token, preferences) {
        return this.request('PUT', '/api/v1/notifications/preferences', token, preferences);
    }
    
    /**
     * Register device token for push notifications
     * Backend: POST /api/v1/notifications/devices
     * Request: { token, platform, deviceName }
     */
    async registerDeviceToken(token, deviceToken, platform) {
        return this.request('POST', '/api/v1/notifications/devices', token, {
            token: deviceToken,
            platform,
            deviceName: 'Chrome Extension'
        });
    }
    
    /**
     * Check if a job URL already exists (uses quick save duplicate check)
     * Note: Backend's POST /api/v1/jobs/quick returns alreadyExists flag
     * For checking only, we do a lightweight GET with the URL
     */
    async checkDuplicate(token, jobUrl) {
        try {
            // Check via applications list with URL filter
            const result = await this.request('GET', `/api/v1/jobs?search=${encodeURIComponent(jobUrl)}&limit=1`, token);
            return result.applications && result.applications.length > 0 && 
                   result.applications.some(app => app.jobUrl === jobUrl);
        } catch {
            return false;
        }
    }
}

export default ApiClient;
