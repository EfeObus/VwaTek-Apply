/**
 * VwaTek Apply Chrome Extension - Background Service Worker
 * Handles authentication flow, alarms for reminders, and cross-tab communication
 */

import { ApiClient } from '../lib/api.js';
import { Storage } from '../lib/storage.js';

const api = new ApiClient();
const storage = new Storage();

/**
 * Extension installation handler
 */
chrome.runtime.onInstalled.addListener(async (details) => {
    console.log('VwaTek Apply extension installed:', details.reason);
    
    if (details.reason === 'install') {
        // First install - open welcome page
        chrome.tabs.create({
            url: chrome.runtime.getURL('welcome/welcome.html')
        });
    }
    
    // Setup daily alarm for notification sync
    await setupAlarms();
});

/**
 * Setup recurring alarms
 */
async function setupAlarms() {
    // Clear existing alarms
    await chrome.alarms.clearAll();
    
    // Create alarm for syncing reminders (every hour)
    chrome.alarms.create('syncReminders', {
        periodInMinutes: 60
    });
    
    // Create alarm for daily summary (9 AM)
    chrome.alarms.create('dailySummary', {
        periodInMinutes: 24 * 60,
        when: getNext9AM()
    });
}

/**
 * Get timestamp for next 9 AM
 */
function getNext9AM() {
    const now = new Date();
    const next9AM = new Date(now);
    next9AM.setHours(9, 0, 0, 0);
    
    if (now.getHours() >= 9) {
        next9AM.setDate(next9AM.getDate() + 1);
    }
    
    return next9AM.getTime();
}

/**
 * Handle alarms
 */
chrome.alarms.onAlarm.addListener(async (alarm) => {
    console.log('Alarm triggered:', alarm.name);
    
    const authData = await storage.getAuth();
    if (!authData || !authData.token) {
        return; // Not logged in
    }
    
    switch (alarm.name) {
        case 'syncReminders':
            await syncReminders(authData.token);
            break;
        case 'dailySummary':
            await showDailySummary(authData.token);
            break;
    }
});

/**
 * Sync reminders from server
 */
async function syncReminders(token) {
    try {
        const reminders = await api.getUpcomingReminders(token);
        
        // Store upcoming reminders locally
        await chrome.storage.local.set({ upcomingReminders: reminders });
        
        // Schedule local notifications for imminent reminders
        const now = Date.now();
        const oneHourFromNow = now + (60 * 60 * 1000);
        
        for (const reminder of reminders) {
            const reminderTime = new Date(reminder.scheduledAt).getTime();
            
            if (reminderTime > now && reminderTime <= oneHourFromNow) {
                scheduleNotification(reminder);
            }
        }
    } catch (error) {
        console.error('Error syncing reminders:', error);
    }
}

/**
 * Schedule a local notification
 */
function scheduleNotification(reminder) {
    const delay = new Date(reminder.scheduledAt).getTime() - Date.now();
    
    if (delay > 0) {
        setTimeout(() => {
            chrome.notifications.create(reminder.id, {
                type: 'basic',
                iconUrl: chrome.runtime.getURL('icons/icon128.png'),
                title: reminder.title,
                message: reminder.message,
                priority: 2,
                buttons: [
                    { title: 'Open Tracker' },
                    { title: 'Dismiss' }
                ]
            });
        }, delay);
    }
}

/**
 * Show daily summary notification
 */
async function showDailySummary(token) {
    try {
        const preferences = await storage.getPreferences();
        
        if (!preferences.dailySummaryEnabled) {
            return;
        }
        
        const stats = await api.getStats(token);
        
        const message = buildSummaryMessage(stats);
        
        chrome.notifications.create('dailySummary', {
            type: 'basic',
            iconUrl: chrome.runtime.getURL('icons/icon128.png'),
            title: 'Your Job Application Summary',
            message: message,
            priority: 1,
            buttons: [
                { title: 'Open Tracker' }
            ]
        });
    } catch (error) {
        console.error('Error showing daily summary:', error);
    }
}

/**
 * Build summary message from stats
 */
function buildSummaryMessage(stats) {
    const parts = [];
    
    if (stats.interviewsToday > 0) {
        parts.push(`🎯 ${stats.interviewsToday} interview(s) today!`);
    }
    
    if (stats.pendingReminders > 0) {
        parts.push(`📋 ${stats.pendingReminders} pending follow-ups`);
    }
    
    if (stats.applicationsThisWeek > 0) {
        parts.push(`📝 ${stats.applicationsThisWeek} applications this week`);
    }
    
    if (parts.length === 0) {
        parts.push('Keep up the great work with your job search!');
    }
    
    return parts.join('\n');
}

/**
 * Handle notification button clicks
 */
chrome.notifications.onButtonClicked.addListener((notificationId, buttonIndex) => {
    if (buttonIndex === 0) {
        // Open tracker
        storage.getApiBaseUrl().then(baseUrl => {
            chrome.tabs.create({ url: `${baseUrl}/tracker` });
        });
    }
    
    chrome.notifications.clear(notificationId);
});

/**
 * Handle messages from popup and content scripts
 */
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    handleMessage(message, sender, sendResponse);
    return true; // Keep channel open for async response
});

/**
 * Process incoming messages
 */
async function handleMessage(message, sender, sendResponse) {
    try {
        switch (message.action) {
            case 'getAuthStatus':
                const authData = await storage.getAuth();
                sendResponse({ 
                    isAuthenticated: !!(authData && authData.token),
                    user: authData?.user 
                });
                break;
                
            case 'setAuth':
                await storage.setAuth(message.data);
                // Notify all tabs about auth change
                chrome.runtime.sendMessage({ action: 'authUpdated' });
                sendResponse({ success: true });
                break;
                
            case 'quickSave':
                const token = (await storage.getAuth())?.token;
                if (!token) {
                    sendResponse({ error: 'Not authenticated' });
                    return;
                }
                const result = await api.quickSaveJob(token, message.job);
                sendResponse({ success: true, data: result });
                break;
                
            case 'getStats':
                const authToken = (await storage.getAuth())?.token;
                if (!authToken) {
                    sendResponse({ error: 'Not authenticated' });
                    return;
                }
                const stats = await api.getStats(authToken);
                sendResponse({ success: true, data: stats });
                break;
                
            default:
                sendResponse({ error: 'Unknown action' });
        }
    } catch (error) {
        console.error('Message handler error:', error);
        sendResponse({ error: error.message });
    }
}

/**
 * Handle external messages (from web app for OAuth callback)
 */
chrome.runtime.onMessageExternal.addListener((message, sender, sendResponse) => {
    if (message.action === 'setAuthToken' && message.token && message.user) {
        storage.setAuth({ token: message.token, user: message.user })
            .then(() => {
                chrome.runtime.sendMessage({ action: 'authUpdated' });
                sendResponse({ success: true });
            })
            .catch(error => {
                sendResponse({ error: error.message });
            });
        return true;
    }
});

/**
 * Watch for tab updates to re-inject content scripts if needed
 */
chrome.tabs.onUpdated.addListener((tabId, changeInfo, tab) => {
    if (changeInfo.status === 'complete' && tab.url) {
        // Check if this is a supported job board
        const jobBoards = [
            { pattern: /linkedin\.com\/jobs/, script: 'content/linkedin.js' },
            { pattern: /indeed\.(com|ca)/, script: 'content/indeed.js' },
            { pattern: /glassdoor\.(com|ca)/, script: 'content/glassdoor.js' },
            { pattern: /jobbank\.gc\.ca/, script: 'content/jobbank.js' }
        ];
        
        const matchedBoard = jobBoards.find(board => board.pattern.test(tab.url));
        
        if (matchedBoard) {
            // Inject content script if not already present
            chrome.tabs.sendMessage(tabId, { action: 'ping' }, response => {
                if (chrome.runtime.lastError) {
                    // Script not loaded, inject it
                    chrome.scripting.executeScript({
                        target: { tabId },
                        files: [matchedBoard.script]
                    }).catch(console.error);
                }
            });
        }
    }
});

console.log('VwaTek Apply background service worker started');
