/**
 * VwaTek Apply Chrome Extension - Indeed Content Script
 * Extracts job data from Indeed job postings
 */

(function() {
    'use strict';
    
    const SOURCE = 'INDEED';
    
    /**
     * Extract job data from the current Indeed job page
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
            
            // Job title
            const titleSelectors = [
                'h1.jobsearch-JobInfoHeader-title',
                '.jobsearch-JobInfoHeader-title',
                'h1[data-testid="jobsearch-JobInfoHeader-title"]',
                '.icl-u-xs-mb--xs h1'
            ];
            
            for (const selector of titleSelectors) {
                const element = document.querySelector(selector);
                if (element) {
                    job.title = element.textContent.trim().replace(/\s*-\s*job post$/i, '');
                    break;
                }
            }
            
            // Company name
            const companySelectors = [
                '[data-testid="inlineHeader-companyName"] a',
                '.jobsearch-CompanyInfoWithoutHeaderImage a',
                '.icl-u-lg-mr--sm a',
                '.jobsearch-InlineCompanyRating-companyHeader a'
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
                '[data-testid="job-location"]',
                '[data-testid="inlineHeader-companyLocation"]',
                '.jobsearch-JobInfoHeader-subtitle > div:last-child',
                '.icl-u-xs-mt--xs .icl-IconFunctional--location + span'
            ];
            
            for (const selector of locationSelectors) {
                const element = document.querySelector(selector);
                if (element) {
                    parseLocation(element.textContent.trim(), job);
                    break;
                }
            }
            
            // Salary
            const salarySelectors = [
                '#salaryInfoAndJobType span.icl-u-xs-mr--xs',
                '[data-testid="attribute_snippet_testid"]',
                '.jobsearch-JobMetadataHeader-item'
            ];
            
            for (const selector of salarySelectors) {
                const elements = document.querySelectorAll(selector);
                for (const element of elements) {
                    const text = element.textContent;
                    if (text.includes('$')) {
                        parseSalary(text, job);
                        break;
                    }
                }
            }
            
            // Job type
            const jobTypeElement = document.querySelector('#salaryInfoAndJobType');
            if (jobTypeElement) {
                const text = jobTypeElement.textContent.toLowerCase();
                if (text.includes('full-time') || text.includes('full time')) {
                    job.jobType = 'FULL_TIME';
                } else if (text.includes('part-time') || text.includes('part time')) {
                    job.jobType = 'PART_TIME';
                } else if (text.includes('contract')) {
                    job.jobType = 'CONTRACT';
                } else if (text.includes('temporary')) {
                    job.jobType = 'TEMPORARY';
                } else if (text.includes('internship')) {
                    job.jobType = 'INTERNSHIP';
                }
            }
            
            // Work model
            const pageText = document.body.innerText.toLowerCase();
            if (pageText.includes('remote') || pageText.includes('work from home')) {
                job.workModel = 'REMOTE';
            } else if (pageText.includes('hybrid')) {
                job.workModel = 'HYBRID';
            } else {
                job.workModel = 'ON_SITE';
            }
            
            // Job description
            const descriptionSelectors = [
                '#jobDescriptionText',
                '.jobsearch-JobComponent-description',
                '.jobsearch-jobDescriptionText'
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
        
        // Try to parse "City, Province" format
        const parts = locationText.split(',').map(p => p.trim());
        
        if (parts.length >= 1) {
            // Remove remote/hybrid indicators
            job.city = parts[0].replace(/\s*(remote|hybrid|•)/gi, '').trim();
        }
        
        if (parts.length >= 2) {
            const provinceText = parts[1].toLowerCase().trim();
            job.province = provinceMap[provinceText] || null;
        }
    }
    
    /**
     * Parse salary from text
     */
    function parseSalary(text, job) {
        // Match patterns like "$50,000 - $70,000" or "$25 - $35 an hour"
        const rangeMatch = text.match(/\$[\d,]+(?:\.\d{2})?\s*[-–to]\s*\$[\d,]+(?:\.\d{2})?/);
        if (rangeMatch) {
            const numbers = rangeMatch[0].match(/[\d,]+/g);
            if (numbers && numbers.length >= 2) {
                let min = parseInt(numbers[0].replace(/,/g, ''));
                let max = parseInt(numbers[1].replace(/,/g, ''));
                
                // If hourly, convert to annual estimate
                if (text.toLowerCase().includes('hour') || text.toLowerCase().includes('hr')) {
                    min = min * 2080; // 40 hours * 52 weeks
                    max = max * 2080;
                }
                
                job.salaryMin = min;
                job.salaryMax = max;
            }
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
        const applyButtonArea = document.querySelector('.jobsearch-ViewJobOtherActions-container') ||
                               document.querySelector('[data-testid="applyButton-indeedApply"]')?.parentElement ||
                               document.querySelector('.icl-u-lg-block');
        
        if (!applyButtonArea) return;
        
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
        
        applyButtonArea.appendChild(saveBtn);
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
        
        // Watch for job changes (Indeed uses dynamic loading)
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
