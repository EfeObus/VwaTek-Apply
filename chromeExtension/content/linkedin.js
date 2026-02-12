/**
 * VwaTek Apply Chrome Extension - LinkedIn Content Script
 * Extracts job data from LinkedIn job postings
 */

(function() {
    'use strict';
    
    const SOURCE = 'LINKEDIN';
    
    /**
     * Extract job data from the current LinkedIn job page
     */
    function extractJobData() {
        try {
            const job = {
                title: null,
                company: null,
                city: null,
                province: null,
                salaryMin: null,
                salaryMax: null,
                jobType: null,
                workModel: null,
                description: null,
                url: window.location.href,
                source: SOURCE
            };
            
            // Job title - try multiple selectors
            const titleSelectors = [
                '.job-details-jobs-unified-top-card__job-title h1',
                '.jobs-unified-top-card__job-title',
                '.t-24.job-details-jobs-unified-top-card__job-title',
                'h1.topcard__title',
                '.jobs-details-top-card__job-title'
            ];
            
            for (const selector of titleSelectors) {
                const element = document.querySelector(selector);
                if (element) {
                    job.title = element.textContent.trim();
                    break;
                }
            }
            
            // Company name
            const companySelectors = [
                '.job-details-jobs-unified-top-card__company-name a',
                '.job-details-jobs-unified-top-card__company-name',
                '.jobs-unified-top-card__company-name a',
                '.topcard__org-name-link',
                '.jobs-details-top-card__company-url'
            ];
            
            for (const selector of companySelectors) {
                const element = document.querySelector(selector);
                if (element) {
                    job.company = element.textContent.trim();
                    break;
                }
            }
            
            // Location
            const locationSelectors = [
                '.job-details-jobs-unified-top-card__primary-description-container span',
                '.jobs-unified-top-card__bullet',
                '.topcard__flavor--bullet',
                '.jobs-details-top-card__bullet'
            ];
            
            for (const selector of locationSelectors) {
                const element = document.querySelector(selector);
                if (element) {
                    const locationText = element.textContent.trim();
                    parseLocation(locationText, job);
                    break;
                }
            }
            
            // Work model (Remote, Hybrid, On-site)
            const workModelText = document.body.innerText;
            if (/remote/i.test(workModelText)) {
                job.workModel = 'REMOTE';
            } else if (/hybrid/i.test(workModelText)) {
                job.workModel = 'HYBRID';
            } else {
                job.workModel = 'ON_SITE';
            }
            
            // Job type
            const insightsSection = document.querySelector('.job-details-jobs-unified-top-card__job-insight');
            if (insightsSection) {
                const insightsText = insightsSection.textContent;
                
                if (/full.?time/i.test(insightsText)) {
                    job.jobType = 'FULL_TIME';
                } else if (/part.?time/i.test(insightsText)) {
                    job.jobType = 'PART_TIME';
                } else if (/contract/i.test(insightsText)) {
                    job.jobType = 'CONTRACT';
                } else if (/internship/i.test(insightsText)) {
                    job.jobType = 'INTERNSHIP';
                }
            }
            
            // Salary - LinkedIn often shows salary in insights
            const salaryMatch = document.body.innerText.match(/\$[\d,]+\s*[-–]\s*\$[\d,]+/);
            if (salaryMatch) {
                const salaryText = salaryMatch[0];
                const numbers = salaryText.match(/[\d,]+/g);
                if (numbers && numbers.length >= 2) {
                    job.salaryMin = parseInt(numbers[0].replace(/,/g, ''));
                    job.salaryMax = parseInt(numbers[1].replace(/,/g, ''));
                }
            }
            
            // Job description
            const descriptionSelectors = [
                '.jobs-description__content',
                '.jobs-description-content__text',
                '.jobs-box__html-content',
                '#job-details'
            ];
            
            for (const selector of descriptionSelectors) {
                const element = document.querySelector(selector);
                if (element) {
                    job.description = element.textContent.trim().substring(0, 5000);
                    break;
                }
            }
            
            return job;
        } catch (error) {
            console.error('VwaTek Apply: Error extracting job data', error);
            return null;
        }
    }
    
    /**
     * Parse location string into city and province
     */
    function parseLocation(locationText, job) {
        if (!locationText) return;
        
        // Canadian provinces mapping
        const provinceMap = {
            'ontario': 'ONTARIO',
            'on': 'ONTARIO',
            'quebec': 'QUEBEC',
            'qc': 'QUEBEC',
            'british columbia': 'BRITISH_COLUMBIA',
            'bc': 'BRITISH_COLUMBIA',
            'alberta': 'ALBERTA',
            'ab': 'ALBERTA',
            'manitoba': 'MANITOBA',
            'mb': 'MANITOBA',
            'saskatchewan': 'SASKATCHEWAN',
            'sk': 'SASKATCHEWAN',
            'nova scotia': 'NOVA_SCOTIA',
            'ns': 'NOVA_SCOTIA',
            'new brunswick': 'NEW_BRUNSWICK',
            'nb': 'NEW_BRUNSWICK',
            'newfoundland': 'NEWFOUNDLAND_AND_LABRADOR',
            'nl': 'NEWFOUNDLAND_AND_LABRADOR',
            'pei': 'PRINCE_EDWARD_ISLAND',
            'prince edward island': 'PRINCE_EDWARD_ISLAND',
            'pe': 'PRINCE_EDWARD_ISLAND',
            'northwest territories': 'NORTHWEST_TERRITORIES',
            'nt': 'NORTHWEST_TERRITORIES',
            'yukon': 'YUKON',
            'yt': 'YUKON',
            'nunavut': 'NUNAVUT',
            'nu': 'NUNAVUT'
        };
        
        // Try to parse "City, Province, Country" format
        const parts = locationText.split(',').map(p => p.trim());
        
        if (parts.length >= 1) {
            job.city = parts[0];
        }
        
        if (parts.length >= 2) {
            const provinceText = parts[1].toLowerCase();
            job.province = provinceMap[provinceText] || null;
        }
    }
    
    /**
     * Add save button to job card
     */
    function addSaveButton() {
        // Check if button already exists
        if (document.querySelector('.vwatek-save-btn')) {
            return;
        }
        
        // Find the apply button area
        const applyButtonArea = document.querySelector('.jobs-apply-button--top-card');
        if (!applyButtonArea) return;
        
        // Create save button
        const saveBtn = document.createElement('button');
        saveBtn.className = 'vwatek-save-btn artdeco-button artdeco-button--secondary artdeco-button--2';
        saveBtn.innerHTML = `
            <span class="artdeco-button__text">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right: 4px; vertical-align: middle;">
                    <path d="M19 21l-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z"></path>
                </svg>
                Save to VwaTek
            </span>
        `;
        saveBtn.style.marginLeft = '8px';
        
        saveBtn.addEventListener('click', async (e) => {
            e.preventDefault();
            e.stopPropagation();
            
            const job = extractJobData();
            if (job) {
                saveBtn.disabled = true;
                saveBtn.innerHTML = '<span class="artdeco-button__text">Saving...</span>';
                
                chrome.runtime.sendMessage({ 
                    action: 'quickSave', 
                    job 
                }, (response) => {
                    if (response && response.success) {
                        saveBtn.innerHTML = '<span class="artdeco-button__text">✓ Saved!</span>';
                        setTimeout(() => {
                            saveBtn.innerHTML = `
                                <span class="artdeco-button__text">
                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right: 4px; vertical-align: middle;">
                                        <path d="M19 21l-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z"></path>
                                    </svg>
                                    Save to VwaTek
                                </span>
                            `;
                            saveBtn.disabled = false;
                        }, 2000);
                    } else {
                        saveBtn.innerHTML = '<span class="artdeco-button__text">Error - Try again</span>';
                        saveBtn.disabled = false;
                    }
                });
            }
        });
        
        applyButtonArea.parentNode.insertBefore(saveBtn, applyButtonArea.nextSibling);
    }
    
    /**
     * Listen for messages from popup
     */
    chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
        if (message.action === 'getJobData') {
            const job = extractJobData();
            sendResponse({ job });
        } else if (message.action === 'ping') {
            sendResponse({ pong: true });
        }
        return true;
    });
    
    /**
     * Initialize content script
     */
    function init() {
        // Wait for page to load
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => {
                setTimeout(addSaveButton, 1000);
            });
        } else {
            setTimeout(addSaveButton, 1000);
        }
        
        // Watch for job changes (LinkedIn is a SPA)
        const observer = new MutationObserver((mutations) => {
            for (const mutation of mutations) {
                if (mutation.addedNodes.length > 0) {
                    setTimeout(addSaveButton, 500);
                }
            }
        });
        
        observer.observe(document.body, {
            childList: true,
            subtree: true
        });
    }
    
    init();
})();
