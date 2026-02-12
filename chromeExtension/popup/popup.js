/**
 * VwaTek Apply Chrome Extension - Popup Script
 * Handles user authentication, job detection, and saving jobs to tracker
 */

import { ApiClient } from '../lib/api.js';
import { Storage } from '../lib/storage.js';

const api = new ApiClient();
const storage = new Storage();

// DOM Elements
const elements = {
    loadingSection: document.getElementById('loading-section'),
    loginSection: document.getElementById('login-section'),
    mainSection: document.getElementById('main-section'),
    
    // Auth
    loginBtn: document.getElementById('login-btn'),
    signupLink: document.getElementById('signup-link'),
    logoutBtn: document.getElementById('logout-btn'),
    
    // User info
    userAvatar: document.getElementById('user-avatar'),
    userName: document.getElementById('user-name'),
    userEmail: document.getElementById('user-email'),
    
    // Job detection
    jobDetected: document.getElementById('job-detected'),
    noJobDetected: document.getElementById('no-job-detected'),
    detectedJobTitle: document.getElementById('detected-job-title'),
    detectedJobCompany: document.getElementById('detected-job-company'),
    detectedJobLocation: document.getElementById('detected-job-location'),
    
    // Actions
    quickSaveBtn: document.getElementById('quick-save-btn'),
    saveDetailsBtn: document.getElementById('save-details-btn'),
    
    // Form
    saveForm: document.getElementById('save-form'),
    jobForm: document.getElementById('job-form'),
    cancelFormBtn: document.getElementById('cancel-form-btn'),
    
    // Stats
    statsSection: document.getElementById('stats-section'),
    statTotal: document.getElementById('stat-total'),
    statActive: document.getElementById('stat-active'),
    statInterviews: document.getElementById('stat-interviews'),
    statOffers: document.getElementById('stat-offers'),
    openDashboardLink: document.getElementById('open-dashboard-link'),
    
    // Messages
    successMessage: document.getElementById('success-message'),
    errorMessage: document.getElementById('error-message'),
    errorText: document.getElementById('error-text'),
    
    // Footer
    settingsLink: document.getElementById('settings-link'),
    helpLink: document.getElementById('help-link')
};

// State
let currentUser = null;
let detectedJob = null;

/**
 * Initialize the popup
 */
async function init() {
    try {
        showLoading();
        
        // Check authentication
        const authData = await storage.getAuth();
        
        if (authData && authData.token) {
            // Validate token
            const isValid = await validateToken(authData.token);
            if (isValid) {
                currentUser = authData.user;
                await showMainSection();
            } else {
                await storage.clearAuth();
                showLoginSection();
            }
        } else {
            showLoginSection();
        }
        
        setupEventListeners();
    } catch (error) {
        console.error('Initialization error:', error);
        showLoginSection();
    }
}

/**
 * Validate the stored token
 */
async function validateToken(token) {
    try {
        const response = await api.verifyToken(token);
        return response.valid;
    } catch {
        return false;
    }
}

/**
 * Show loading state
 */
function showLoading() {
    elements.loadingSection.classList.remove('hidden');
    elements.loginSection.classList.add('hidden');
    elements.mainSection.classList.add('hidden');
}

/**
 * Show login section
 */
function showLoginSection() {
    elements.loadingSection.classList.add('hidden');
    elements.loginSection.classList.remove('hidden');
    elements.mainSection.classList.add('hidden');
}

/**
 * Show main section with user info
 */
async function showMainSection() {
    elements.loadingSection.classList.add('hidden');
    elements.loginSection.classList.add('hidden');
    elements.mainSection.classList.remove('hidden');
    
    // Update user info
    if (currentUser) {
        const initials = getInitials(currentUser.name || currentUser.email);
        elements.userAvatar.textContent = initials;
        elements.userName.textContent = currentUser.name || 'User';
        elements.userEmail.textContent = currentUser.email;
    }
    
    // Detect job on current page
    await detectCurrentJob();
    
    // Load stats
    await loadStats();
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
 * Detect job on current tab
 */
async function detectCurrentJob() {
    try {
        // Query the active tab and send message to content script
        const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
        
        if (!tab.id) {
            showNoJobDetected();
            return;
        }
        
        // Check if we're on a supported site
        const supportedSites = [
            'linkedin.com/jobs',
            'indeed.com',
            'indeed.ca',
            'glassdoor.com',
            'glassdoor.ca',
            'jobbank.gc.ca'
        ];
        
        const isSupported = supportedSites.some(site => tab.url?.includes(site));
        
        if (!isSupported) {
            showNoJobDetected();
            return;
        }
        
        // Request job data from content script
        chrome.tabs.sendMessage(tab.id, { action: 'getJobData' }, (response) => {
            if (chrome.runtime.lastError) {
                console.log('Content script not ready:', chrome.runtime.lastError);
                showNoJobDetected();
                return;
            }
            
            if (response && response.job) {
                detectedJob = response.job;
                showDetectedJob(detectedJob);
            } else {
                showNoJobDetected();
            }
        });
    } catch (error) {
        console.error('Error detecting job:', error);
        showNoJobDetected();
    }
}

/**
 * Show detected job card
 */
function showDetectedJob(job) {
    elements.jobDetected.classList.remove('hidden');
    elements.noJobDetected.classList.add('hidden');
    
    elements.detectedJobTitle.textContent = job.title || 'Unknown Title';
    elements.detectedJobCompany.textContent = job.company || 'Unknown Company';
    
    const locationParts = [job.city, job.province].filter(Boolean);
    elements.detectedJobLocation.textContent = locationParts.length > 0 
        ? locationParts.join(', ') 
        : 'Location not specified';
}

/**
 * Show no job detected state
 */
function showNoJobDetected() {
    elements.jobDetected.classList.add('hidden');
    elements.noJobDetected.classList.remove('hidden');
    elements.saveForm.classList.add('hidden');
}

/**
 * Load user stats
 */
async function loadStats() {
    try {
        const authData = await storage.getAuth();
        const stats = await api.getStats(authData.token);
        
        elements.statTotal.textContent = stats.totalApplications || 0;
        elements.statActive.textContent = stats.activeApplications || 0;
        elements.statInterviews.textContent = stats.interviewsScheduled || 0;
        elements.statOffers.textContent = stats.offersReceived || 0;
    } catch (error) {
        console.error('Error loading stats:', error);
        // Show zeros on error
        elements.statTotal.textContent = '0';
        elements.statActive.textContent = '0';
        elements.statInterviews.textContent = '0';
        elements.statOffers.textContent = '0';
    }
}

/**
 * Quick save the detected job
 */
async function quickSaveJob() {
    if (!detectedJob) return;
    
    try {
        elements.quickSaveBtn.disabled = true;
        elements.quickSaveBtn.textContent = 'Saving...';
        
        const authData = await storage.getAuth();
        
        const jobData = {
            title: detectedJob.title,
            company: detectedJob.company,
            city: detectedJob.city,
            province: detectedJob.province,
            jobUrl: detectedJob.url,
            source: detectedJob.source,
            salaryMin: detectedJob.salaryMin,
            salaryMax: detectedJob.salaryMax,
            jobType: detectedJob.jobType,
            workModel: detectedJob.workModel,
            description: detectedJob.description
        };
        
        await api.quickSaveJob(authData.token, jobData);
        
        showSuccess('Job saved successfully!');
        await loadStats();
        
    } catch (error) {
        console.error('Error saving job:', error);
        showError(error.message || 'Failed to save job');
    } finally {
        elements.quickSaveBtn.disabled = false;
        elements.quickSaveBtn.innerHTML = `
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="5" x2="12" y2="19"></line>
                <line x1="5" y1="12" x2="19" y2="12"></line>
            </svg>
            Quick Save
        `;
    }
}

/**
 * Show save with details form
 */
function showSaveForm() {
    elements.saveForm.classList.remove('hidden');
    elements.jobDetected.classList.add('hidden');
    elements.statsSection.classList.add('hidden');
    
    // Pre-fill form with detected job data
    if (detectedJob) {
        document.getElementById('job-title').value = detectedJob.title || '';
        document.getElementById('company-name').value = detectedJob.company || '';
        document.getElementById('job-city').value = detectedJob.city || '';
        document.getElementById('job-province').value = detectedJob.province || '';
        document.getElementById('salary-min').value = detectedJob.salaryMin || '';
        document.getElementById('salary-max').value = detectedJob.salaryMax || '';
        document.getElementById('job-type').value = detectedJob.jobType || 'FULL_TIME';
        document.getElementById('work-model').value = detectedJob.workModel || 'ON_SITE';
    }
}

/**
 * Hide save form and show job card
 */
function hideSaveForm() {
    elements.saveForm.classList.add('hidden');
    elements.statsSection.classList.remove('hidden');
    
    if (detectedJob) {
        elements.jobDetected.classList.remove('hidden');
    } else {
        elements.noJobDetected.classList.remove('hidden');
    }
}

/**
 * Save job with full details
 */
async function saveJobWithDetails(event) {
    event.preventDefault();
    
    try {
        const formData = new FormData(elements.jobForm);
        const authData = await storage.getAuth();
        
        const jobData = {
            title: formData.get('title'),
            company: formData.get('company'),
            city: formData.get('city'),
            province: formData.get('province'),
            salaryMin: formData.get('salaryMin') ? parseInt(formData.get('salaryMin')) : null,
            salaryMax: formData.get('salaryMax') ? parseInt(formData.get('salaryMax')) : null,
            currency: 'CAD',
            jobType: formData.get('jobType'),
            workModel: formData.get('workModel'),
            lmiaRequired: formData.get('lmiaRequired') === 'on',
            jobUrl: detectedJob?.url,
            source: detectedJob?.source || 'OTHER',
            notes: formData.get('notes')
        };
        
        await api.createApplication(authData.token, jobData);
        
        showSuccess('Job saved successfully!');
        hideSaveForm();
        await loadStats();
        
    } catch (error) {
        console.error('Error saving job:', error);
        showError(error.message || 'Failed to save job');
    }
}

/**
 * Handle login
 */
async function handleLogin() {
    // Open the web app login page in a new tab
    // The web app will handle OAuth and store auth data
    const webAppUrl = await storage.getApiBaseUrl();
    chrome.tabs.create({ url: `${webAppUrl}/login?source=extension` });
}

/**
 * Handle logout
 */
async function handleLogout() {
    try {
        await storage.clearAuth();
        currentUser = null;
        showLoginSection();
    } catch (error) {
        console.error('Logout error:', error);
        showError('Failed to sign out');
    }
}

/**
 * Show success message
 */
function showSuccess(message) {
    elements.successMessage.querySelector('span').textContent = message;
    elements.successMessage.classList.remove('hidden');
    
    setTimeout(() => {
        elements.successMessage.classList.add('hidden');
    }, 3000);
}

/**
 * Show error message
 */
function showError(message) {
    elements.errorText.textContent = message;
    elements.errorMessage.classList.remove('hidden');
    
    setTimeout(() => {
        elements.errorMessage.classList.add('hidden');
    }, 4000);
}

/**
 * Setup event listeners
 */
function setupEventListeners() {
    // Auth
    elements.loginBtn.addEventListener('click', handleLogin);
    elements.signupLink.addEventListener('click', (e) => {
        e.preventDefault();
        chrome.tabs.create({ url: 'https://vwatek.com/apply/signup' });
    });
    elements.logoutBtn.addEventListener('click', handleLogout);
    
    // Job actions
    elements.quickSaveBtn.addEventListener('click', quickSaveJob);
    elements.saveDetailsBtn.addEventListener('click', showSaveForm);
    elements.cancelFormBtn.addEventListener('click', hideSaveForm);
    elements.jobForm.addEventListener('submit', saveJobWithDetails);
    
    // Dashboard link
    elements.openDashboardLink.addEventListener('click', async (e) => {
        e.preventDefault();
        const webAppUrl = await storage.getApiBaseUrl();
        chrome.tabs.create({ url: `${webAppUrl}/tracker` });
    });
    
    // Footer links
    elements.settingsLink.addEventListener('click', (e) => {
        e.preventDefault();
        chrome.runtime.openOptionsPage();
    });
    
    elements.helpLink.addEventListener('click', (e) => {
        e.preventDefault();
        chrome.tabs.create({ url: 'https://vwatek.com/apply/help' });
    });
    
    // Listen for auth updates from background script
    chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
        if (message.action === 'authUpdated') {
            init();
        }
    });
}

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', init);
