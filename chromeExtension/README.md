# VwaTek Apply Chrome Extension

Quick save job postings to your VwaTek Apply job tracker directly from popular Canadian and international job boards.

## Features

- **One-Click Save**: Save job postings directly from job boards to your tracker
- **Auto-Detection**: Automatically detects job postings and extracts key information
- **Canadian Focus**: Special support for Canadian provinces, NOC codes, and LMIA tracking
- **Offline Support**: Cache jobs locally and sync when connected
- **Notifications**: Get reminders for interviews and follow-ups

## Supported Job Boards

- LinkedIn Jobs
- Indeed (Canada & US)
- Glassdoor
- Job Bank Canada (Government of Canada)

## Installation

### From Chrome Web Store (Coming Soon)
1. Visit the Chrome Web Store
2. Search for "VwaTek Apply"
3. Click "Add to Chrome"

### Development Build
1. Clone the repository
2. Open Chrome and navigate to `chrome://extensions/`
3. Enable "Developer mode"
4. Click "Load unpacked"
5. Select the `chromeExtension` folder

## Directory Structure

```
chromeExtension/
├── manifest.json          # Extension configuration
├── popup/                 # Popup UI
│   ├── popup.html
│   ├── popup.css
│   └── popup.js
├── background/            # Background service worker
│   └── background.js
├── content/               # Content scripts for job boards
│   ├── content.css
│   ├── linkedin.js
│   ├── indeed.js
│   ├── glassdoor.js
│   └── jobbank.js
├── options/               # Settings page
│   ├── options.html
│   └── options.js
├── lib/                   # Shared utilities
│   ├── api.js
│   └── storage.js
├── icons/                 # Extension icons
└── welcome/               # First-run welcome page
```

## Development

### Prerequisites
- Chrome browser (version 88+)
- VwaTek Apply backend running locally or deployed

### Setup
1. Update the API URL in `lib/api.js` if using a local backend
2. Load the extension as unpacked in Chrome
3. Test on supported job boards

### Adding New Job Boards
1. Create a new content script in `content/` (e.g., `newsite.js`)
2. Implement the `extractJobData()` function to parse job details
3. Add the site pattern to `manifest.json` under `content_scripts`
4. Add the site to `host_permissions` in `manifest.json`

## API Endpoints Used

The extension communicates with these backend endpoints:

- `POST /tracker/applications/quick-save` - Quick save job
- `POST /tracker/applications` - Create full application
- `GET /tracker/stats` - Get application statistics
- `GET /tracker/reminders/upcoming` - Get upcoming reminders
- `GET /notifications/preferences` - Get notification preferences
- `PUT /notifications/preferences` - Update notification preferences

## Permissions

- `activeTab` - Access the current tab to extract job data
- `storage` - Store authentication and preferences
- `alarms` - Schedule notification reminders
- `notifications` - Show desktop notifications

## Privacy

- Job data is only extracted from supported job sites when the user explicitly saves
- Authentication tokens are stored securely in Chrome's local storage
- No browsing data is collected or transmitted beyond saved job postings

## License

Copyright © 2024 VwaTek. All rights reserved.
