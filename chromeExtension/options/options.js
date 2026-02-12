/**
 * VwaTek Apply Chrome Extension - Options Page Script
 * Handles extension settings and preferences
 */

import { Storage } from '../lib/storage.js';

const storage = new Storage();

// DOM Elements
const elements = {
    // Account
    statusBadge: document.getElementById('status-badge'),
    accountInfo: document.getElementById('account-info'),
    accountAvatar: document.getElementById('account-avatar'),
    accountName: document.getElementById('account-name'),
    accountEmail: document.getElementById('account-email'),
    loginPrompt: document.getElementById('login-prompt'),
    loginBtn: document.getElementById('login-btn'),
    logoutBtn: document.getElementById('logout-btn'),
    openDashboardBtn: document.getElementById('open-dashboard-btn'),
    
    // Notifications
    notificationsEnabled: document.getElementById('notifications-enabled'),
    dailySummary: document.getElementById('daily-summary'),
    interviewReminders: document.getElementById('interview-reminders'),
    
    // Job Boards
    autoDetect: document.getElementById('auto-detect'),
    showSaveButton: document.getElementById('show-save-button'),
    quickSaveConfirm: document.getElementById('quick-save-confirm'),
    defaultProvince: document.getElementById('default-province'),
    
    // Advanced
    apiUrl: document.getElementById('api-url'),
    clearDataBtn: document.getElementById('clear-data-btn'),
    
    // Toast
    toast: document.getElementById('toast')
};

/**
 * Initialize options page
 */
async function init() {
    await loadAuthStatus();
    await loadPreferences();
    setupEventListeners();
}

/**
 * Load authentication status
 */
async function loadAuthStatus() {
    try {
        const auth = await storage.getAuth();
        
        if (auth && auth.user) {
            // Show logged in state
            elements.accountInfo.classList.remove('hidden');
            elements.loginPrompt.classList.add('hidden');
            elements.logoutBtn.classList.remove('hidden');
            elements.openDashboardBtn.classList.remove('hidden');
            
            // Update user info
            const initials = getInitials(auth.user.name || auth.user.email);
            elements.accountAvatar.textContent = initials;
            elements.accountName.textContent = auth.user.name || 'User';
            elements.accountEmail.textContent = auth.user.email;
            
            // Update status badge
            elements.statusBadge.className = 'status-badge status-connected';
            elements.statusBadge.innerHTML = '<span>●</span> Connected';
        } else {
            // Show logged out state
            elements.accountInfo.classList.add('hidden');
            elements.loginPrompt.classList.remove('hidden');
            elements.logoutBtn.classList.add('hidden');
            elements.openDashboardBtn.classList.add('hidden');
            
            // Update status badge
            elements.statusBadge.className = 'status-badge status-disconnected';
            elements.statusBadge.innerHTML = '<span>●</span> Not signed in';
        }
    } catch (error) {
        console.error('Error loading auth status:', error);
    }
}

/**
 * Get user initials from name
 */
function getInitials(name) {
    if (!name) return 'U';
    const parts = name.trim().split(' ');
    if (parts.length >= 2) {
        return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
}

/**
 * Load user preferences
 */
async function loadPreferences() {
    try {
        const prefs = await storage.getPreferences();
        
        // Notifications
        elements.notificationsEnabled.checked = prefs.notificationsEnabled !== false;
        elements.dailySummary.checked = prefs.dailySummaryEnabled !== false;
        elements.interviewReminders.checked = prefs.interviewRemindersEnabled !== false;
        
        // Job Boards
        elements.autoDetect.checked = prefs.autoDetectJobs !== false;
        elements.showSaveButton.checked = prefs.showSaveButton !== false;
        elements.quickSaveConfirm.checked = prefs.quickSaveConfirmation !== false;
        elements.defaultProvince.value = prefs.defaultProvince || '';
        
        // Advanced
        const apiUrl = await storage.getApiBaseUrl();
        if (apiUrl && apiUrl !== 'https://api.vwatek.com/apply') {
            elements.apiUrl.value = apiUrl;
        }
    } catch (error) {
        console.error('Error loading preferences:', error);
    }
}

/**
 * Save preferences
 */
async function savePreferences() {
    try {
        await storage.setPreferences({
            notificationsEnabled: elements.notificationsEnabled.checked,
            dailySummaryEnabled: elements.dailySummary.checked,
            interviewRemindersEnabled: elements.interviewReminders.checked,
            autoDetectJobs: elements.autoDetect.checked,
            showSaveButton: elements.showSaveButton.checked,
            quickSaveConfirmation: elements.quickSaveConfirm.checked,
            defaultProvince: elements.defaultProvince.value || null
        });
        
        // Save API URL if provided
        const apiUrl = elements.apiUrl.value.trim();
        if (apiUrl) {
            await storage.setApiBaseUrl(apiUrl);
        }
        
        showToast('Settings saved', 'success');
    } catch (error) {
        console.error('Error saving preferences:', error);
        showToast('Failed to save settings', 'error');
    }
}

/**
 * Handle login
 */
async function handleLogin() {
    const baseUrl = await storage.getApiBaseUrl();
    chrome.tabs.create({ url: `${baseUrl}/login?source=extension` });
}

/**
 * Handle logout
 */
async function handleLogout() {
    try {
        await storage.clearAuth();
        showToast('Signed out successfully', 'success');
        await loadAuthStatus();
    } catch (error) {
        showToast('Failed to sign out', 'error');
    }
}

/**
 * Open dashboard
 */
async function openDashboard() {
    const baseUrl = await storage.getApiBaseUrl();
    chrome.tabs.create({ url: `${baseUrl}/tracker` });
}

/**
 * Clear local data
 */
async function clearLocalData() {
    if (confirm('Are you sure you want to clear all local data? This will not affect your account on the server.')) {
        try {
            await storage.clearAll();
            showToast('Local data cleared', 'success');
            await loadPreferences();
            await loadAuthStatus();
        } catch (error) {
            showToast('Failed to clear data', 'error');
        }
    }
}

/**
 * Show toast notification
 */
function showToast(message, type = 'info') {
    elements.toast.textContent = message;
    elements.toast.className = `toast toast-${type}`;
    elements.toast.classList.remove('hidden');
    
    setTimeout(() => {
        elements.toast.classList.add('hidden');
    }, 3000);
}

/**
 * Setup event listeners
 */
function setupEventListeners() {
    // Account
    elements.loginBtn.addEventListener('click', handleLogin);
    elements.logoutBtn.addEventListener('click', handleLogout);
    elements.openDashboardBtn.addEventListener('click', openDashboard);
    
    // Auto-save preferences on change
    const preferenceInputs = [
        elements.notificationsEnabled,
        elements.dailySummary,
        elements.interviewReminders,
        elements.autoDetect,
        elements.showSaveButton,
        elements.quickSaveConfirm,
        elements.defaultProvince
    ];
    
    preferenceInputs.forEach(input => {
        input.addEventListener('change', savePreferences);
    });
    
    // API URL - save on blur
    elements.apiUrl.addEventListener('blur', savePreferences);
    
    // Clear data
    elements.clearDataBtn.addEventListener('click', clearLocalData);
}

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', init);
