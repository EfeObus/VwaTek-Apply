/**
 * VwaTek Apply Chrome Extension - Job Bank Canada Content Script
 * Extracts job data from the Government of Canada Job Bank
 */

(function() {
    'use strict';
    
    const SOURCE = 'JOB_BANK_CANADA';
    
    /**
     * Extract job data from the current Job Bank page
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
                nocCode: null,
                lmiaRequired: null,
                url: window.location.href,
                source: SOURCE
            };
            
            // Job title
            const titleElement = document.querySelector('h1.title') ||
                                document.querySelector('.job-posting-title') ||
                                document.querySelector('h1.wb-inv + span');
            
            if (titleElement) {
                job.title = titleElement.textContent.trim();
            }
            
            // Company name
            const companyElement = document.querySelector('.business-name') ||
                                  document.querySelector('[property="hiringOrganization"] [property="name"]') ||
                                  document.querySelector('.details .name');
            
            if (companyElement) {
                job.company = companyElement.textContent.trim();
            }
            
            // Location
            const locationElement = document.querySelector('.location') ||
                                   document.querySelector('[property="jobLocation"] [property="address"]') ||
                                   document.querySelector('.details .location');
            
            if (locationElement) {
                parseLocation(locationElement.textContent.trim(), job);
            }
            
            // Salary - Job Bank often shows detailed salary info
            const salaryElement = document.querySelector('.salary') ||
                                 document.querySelector('[property="baseSalary"]') ||
                                 document.querySelector('.details .wage');
            
            if (salaryElement) {
                parseSalary(salaryElement.textContent, job);
            }
            
            // Job type
            const employmentTypeElement = document.querySelector('.employment-type') ||
                                         document.querySelector('[property="employmentType"]');
            
            if (employmentTypeElement) {
                const text = employmentTypeElement.textContent.toLowerCase();
                if (text.includes('full-time') || text.includes('full time') || text.includes('permanent')) {
                    job.jobType = 'FULL_TIME';
                } else if (text.includes('part-time') || text.includes('part time')) {
                    job.jobType = 'PART_TIME';
                } else if (text.includes('contract') || text.includes('term')) {
                    job.jobType = 'CONTRACT';
                } else if (text.includes('temporary') || text.includes('casual')) {
                    job.jobType = 'TEMPORARY';
                }
            }
            
            // Work model
            const pageText = document.body.innerText.toLowerCase();
            if (pageText.includes('remote') || pageText.includes('telework') || pageText.includes('work from home')) {
                job.workModel = 'REMOTE';
            } else if (pageText.includes('hybrid')) {
                job.workModel = 'HYBRID';
            } else {
                job.workModel = 'ON_SITE';
            }
            
            // NOC Code (important for Canadian immigration)
            const nocElement = document.querySelector('.noc-code') ||
                              document.querySelector('[data-noc]') ||
                              document.body.innerText.match(/NOC[:\s]*(\d{4,5})/i);
            
            if (nocElement) {
                if (typeof nocElement === 'object' && nocElement.textContent) {
                    const nocMatch = nocElement.textContent.match(/(\d{4,5})/);
                    if (nocMatch) {
                        job.nocCode = nocMatch[1];
                    }
                } else if (Array.isArray(nocElement) && nocElement[1]) {
                    job.nocCode = nocElement[1];
                }
            }
            
            // LMIA info - important for work permits
            const lmiaText = document.body.innerText.toLowerCase();
            if (lmiaText.includes('lmia') || lmiaText.includes('labour market impact assessment')) {
                if (lmiaText.includes('lmia approved') || lmiaText.includes('positive lmia')) {
                    job.lmiaRequired = true;
                }
            }
            
            // Check for immigration-friendly indicators
            if (lmiaText.includes('willing to sponsor') || 
                lmiaText.includes('work permit') ||
                lmiaText.includes('foreign worker')) {
                job.lmiaRequired = true;
            }
            
            // Job description
            const descriptionElement = document.querySelector('.job-posting-details') ||
                                      document.querySelector('.job-description') ||
                                      document.querySelector('[property="description"]');
            
            if (descriptionElement) {
                job.description = descriptionElement.textContent.trim().substring(0, 5000);
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
            'québec': 'QUEBEC',
            'british columbia': 'BRITISH_COLUMBIA',
            'bc': 'BRITISH_COLUMBIA',
            'colombie-britannique': 'BRITISH_COLUMBIA',
            'alberta': 'ALBERTA',
            'ab': 'ALBERTA',
            'manitoba': 'MANITOBA',
            'mb': 'MANITOBA',
            'saskatchewan': 'SASKATCHEWAN',
            'sk': 'SASKATCHEWAN',
            'nova scotia': 'NOVA_SCOTIA',
            'ns': 'NOVA_SCOTIA',
            'nouvelle-écosse': 'NOVA_SCOTIA',
            'new brunswick': 'NEW_BRUNSWICK',
            'nb': 'NEW_BRUNSWICK',
            'nouveau-brunswick': 'NEW_BRUNSWICK',
            'newfoundland': 'NEWFOUNDLAND_AND_LABRADOR',
            'newfoundland and labrador': 'NEWFOUNDLAND_AND_LABRADOR',
            'nl': 'NEWFOUNDLAND_AND_LABRADOR',
            'terre-neuve-et-labrador': 'NEWFOUNDLAND_AND_LABRADOR',
            'pei': 'PRINCE_EDWARD_ISLAND',
            'prince edward island': 'PRINCE_EDWARD_ISLAND',
            'pe': 'PRINCE_EDWARD_ISLAND',
            'île-du-prince-édouard': 'PRINCE_EDWARD_ISLAND',
            'northwest territories': 'NORTHWEST_TERRITORIES',
            'nt': 'NORTHWEST_TERRITORIES',
            'territoires du nord-ouest': 'NORTHWEST_TERRITORIES',
            'yukon': 'YUKON',
            'yt': 'YUKON',
            'nunavut': 'NUNAVUT',
            'nu': 'NUNAVUT'
        };
        
        // Job Bank format is usually "City, Province"
        const parts = locationText.split(',').map(p => p.trim());
        
        if (parts.length >= 1) {
            job.city = parts[0];
        }
        
        if (parts.length >= 2) {
            // Clean up province text
            const provinceText = parts[1].toLowerCase()
                .replace(/\(.*\)/, '') // Remove parentheses
                .trim();
            
            job.province = provinceMap[provinceText] || null;
        }
    }
    
    /**
     * Parse salary from text
     */
    function parseSalary(text, job) {
        if (!text) return;
        
        // Job Bank often shows "HOUR: $X.XX" or "$X.XX to $X.XX hourly"
        const hourlyMatch = text.match(/\$?([\d,.]+)\s*(?:to|-)\s*\$?([\d,.]+)?\s*(?:\/?\s*hour|hourly|HOUR)/i);
        if (hourlyMatch) {
            const min = parseFloat(hourlyMatch[1].replace(/,/g, ''));
            const max = hourlyMatch[2] ? parseFloat(hourlyMatch[2].replace(/,/g, '')) : min;
            
            // Convert to annual (40 hours * 52 weeks)
            job.salaryMin = Math.round(min * 2080);
            job.salaryMax = Math.round(max * 2080);
            return;
        }
        
        // Annual salary
        const annualMatch = text.match(/\$?([\d,]+)\s*(?:to|-)\s*\$?([\d,]+)?\s*(?:\/?\s*year|annually|YEAR)/i);
        if (annualMatch) {
            job.salaryMin = parseInt(annualMatch[1].replace(/,/g, ''));
            job.salaryMax = annualMatch[2] ? parseInt(annualMatch[2].replace(/,/g, '')) : job.salaryMin;
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
        
        // Find a good place for the button
        const actionsArea = document.querySelector('.job-posting-actions') ||
                          document.querySelector('.btn-toolbar') ||
                          document.querySelector('.details');
        
        if (!actionsArea) return;
        
        // Create save button matching Job Bank style
        const saveBtn = document.createElement('button');
        saveBtn.className = 'vwatek-save-btn btn btn-primary';
        saveBtn.innerHTML = `
            <span class="glyphicon glyphicon-bookmark" style="margin-right: 6px;"></span>
            Save to VwaTek Apply
        `;
        saveBtn.style.marginTop = '10px';
        saveBtn.style.marginRight = '10px';
        
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
                        saveBtn.innerHTML = '✓ Saved to VwaTek!';
                        setTimeout(() => {
                            saveBtn.innerHTML = `
                                <span class="glyphicon glyphicon-bookmark" style="margin-right: 6px;"></span>
                                Save to VwaTek Apply
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
        
        actionsArea.insertBefore(saveBtn, actionsArea.firstChild);
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
        
        // Watch for page changes
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
