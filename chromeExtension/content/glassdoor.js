/**
 * VwaTek Apply Chrome Extension - Glassdoor Content Script
 * Extracts job data from Glassdoor job postings
 */

(function() {
    'use strict';
    
    const SOURCE = 'GLASSDOOR';
    
    /**
     * Extract job data from the current Glassdoor job page
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
                salaryEstimate: null,
                companyRating: null,
                jobType: null,
                workModel: null,
                description: null,
                url: window.location.href,
                source: SOURCE
            };
            
            // Job title
            const titleSelectors = [
                '[data-test="job-title"]',
                '.JobDetailHeader_jobTitle__',
                'h1.heading-6',
                '.job-title'
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
                '[data-test="employer-name"]',
                '.EmployerProfile_employerName__',
                '.employer-name',
                '.JobDetailHeader_companyNameLink__'
            ];
            
            for (const selector of companySelectors) {
                const element = document.querySelector(selector);
                if (element) {
                    job.company = element.textContent.trim();
                    break;
                }
            }
            
            // Company rating
            const ratingElement = document.querySelector('[data-test="rating"]') ||
                                 document.querySelector('.ratingNumber');
            if (ratingElement) {
                const rating = parseFloat(ratingElement.textContent);
                if (!isNaN(rating)) {
                    job.companyRating = rating;
                }
            }
            
            // Location
            const locationSelectors = [
                '[data-test="location"]',
                '.JobDetailHeader_location__',
                '.location'
            ];
            
            for (const selector of locationSelectors) {
                const element = document.querySelector(selector);
                if (element) {
                    parseLocation(element.textContent.trim(), job);
                    break;
                }
            }
            
            // Salary estimate (Glassdoor often shows estimates)
            const salarySelectors = [
                '[data-test="salary-estimate"]',
                '.JobDetailHeader_salaryEstimate__',
                '.salary-estimate',
                '[data-test="detailSalary"]'
            ];
            
            for (const selector of salarySelectors) {
                const element = document.querySelector(selector);
                if (element) {
                    parseSalary(element.textContent, job);
                    break;
                }
            }
            
            // Job type and work model from job details
            const detailItems = document.querySelectorAll('.JobDetails_jobDetailsItem__') ||
                               document.querySelectorAll('[data-test="job-detail-item"]');
            
            detailItems.forEach(item => {
                const text = item.textContent.toLowerCase();
                
                // Job type
                if (text.includes('full-time') || text.includes('full time')) {
                    job.jobType = 'FULL_TIME';
                } else if (text.includes('part-time') || text.includes('part time')) {
                    job.jobType = 'PART_TIME';
                } else if (text.includes('contract')) {
                    job.jobType = 'CONTRACT';
                } else if (text.includes('internship') || text.includes('intern')) {
                    job.jobType = 'INTERNSHIP';
                }
                
                // Work model
                if (text.includes('remote')) {
                    job.workModel = 'REMOTE';
                } else if (text.includes('hybrid')) {
                    job.workModel = 'HYBRID';
                }
            });
            
            if (!job.workModel) {
                job.workModel = 'ON_SITE';
            }
            
            // Job description
            const descriptionSelectors = [
                '[data-test="description"]',
                '.JobDetails_jobDescription__',
                '.jobDescription',
                '.desc'
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
        
        // Parse "City, Province" or "City, Province (Remote)" format
        const cleanLocation = locationText.replace(/\(.*\)/g, '').trim();
        const parts = cleanLocation.split(',').map(p => p.trim());
        
        if (parts.length >= 1) {
            job.city = parts[0];
        }
        
        if (parts.length >= 2) {
            const provinceText = parts[1].toLowerCase()
                .replace(/canada/gi, '')
                .trim();
            
            job.province = provinceMap[provinceText] || null;
        }
        
        // Check for remote indicator
        if (locationText.toLowerCase().includes('remote')) {
            job.workModel = 'REMOTE';
        }
    }
    
    /**
     * Parse salary from text
     */
    function parseSalary(text, job) {
        if (!text) return;
        
        // Glassdoor shows estimates like "$50K - $80K (Glassdoor est.)"
        const kMatch = text.match(/\$?([\d.]+)K?\s*[-–to]\s*\$?([\d.]+)K/i);
        if (kMatch) {
            const multiplier = text.toLowerCase().includes('k') ? 1000 : 1;
            job.salaryMin = Math.round(parseFloat(kMatch[1]) * multiplier);
            job.salaryMax = Math.round(parseFloat(kMatch[2]) * multiplier);
            
            // Mark as estimate if from Glassdoor
            if (text.toLowerCase().includes('est')) {
                job.salaryEstimate = true;
            }
            return;
        }
        
        // Full number format "$50,000 - $80,000"
        const fullMatch = text.match(/\$?([\d,]+)\s*[-–to]\s*\$?([\d,]+)/);
        if (fullMatch) {
            job.salaryMin = parseInt(fullMatch[1].replace(/,/g, ''));
            job.salaryMax = parseInt(fullMatch[2].replace(/,/g, ''));
        }
    }
    
    /**
     * Add save button to job page
     */
    function addSaveButton() {
        // Check if button already exists
        if (document.querySelector('.vwatek-save-btn')) {
            return;
        }
        
        // Find apply button area
        const applyBtnArea = document.querySelector('[data-test="apply-now-cta"]')?.parentElement ||
                           document.querySelector('.ApplyButton_container__') ||
                           document.querySelector('.apply-button-wrapper');
        
        if (!applyBtnArea) return;
        
        // Create save button
        const saveBtn = document.createElement('button');
        saveBtn.className = 'vwatek-save-btn';
        saveBtn.innerHTML = `
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right: 6px; vertical-align: middle;">
                <path d="M19 21l-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z"></path>
            </svg>
            Save to VwaTek
        `;
        
        saveBtn.addEventListener('click', async (e) => {
            e.preventDefault();
            e.stopPropagation();
            
            const job = extractJobData();
            if (job) {
                saveBtn.disabled = true;
                saveBtn.textContent = 'Saving...';
                
                chrome.runtime.sendMessage({ 
                    action: 'quickSave', 
                    job 
                }, (response) => {
                    if (response && response.success) {
                        saveBtn.innerHTML = '✓ Saved!';
                        setTimeout(() => {
                            saveBtn.innerHTML = `
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right: 6px; vertical-align: middle;">
                                    <path d="M19 21l-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z"></path>
                                </svg>
                                Save to VwaTek
                            `;
                            saveBtn.disabled = false;
                        }, 2000);
                    } else {
                        saveBtn.textContent = 'Error - Try again';
                        saveBtn.disabled = false;
                    }
                });
            }
        });
        
        applyBtnArea.appendChild(saveBtn);
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
                setTimeout(addSaveButton, 1500);
            });
        } else {
            setTimeout(addSaveButton, 1500);
        }
        
        // Watch for job changes (Glassdoor uses dynamic loading)
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
