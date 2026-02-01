# Frequently Asked Questions (FAQ)

## General Questions

### What is VwaTek Apply?

VwaTek Apply is a professional AI-powered career suite that helps job seekers optimize their resumes, generate tailored cover letters, and prepare for interviews. It's built with Kotlin Multiplatform and available on iOS, Android, and Web.

### What platforms are supported?

VwaTek Apply is available on:
- **iOS** (iPhone and iPad)
- **Android** (phones and tablets)
- **Web** (modern browsers)

### Is my data safe?

Yes. All your data is stored locally on your device with AES-256 encryption. We do not store any user data on our servers. See our [Security Policy](SECURITY.md) for more details.

### Do I need an internet connection?

An internet connection is required for AI-powered features (resume analysis, cover letter generation, mock interviews) as these use the Gemini API. However, you can view and manage your saved documents offline.

---

## Account & Setup

### Do I need to create an account?

No. VwaTek Apply does not require account creation. All data is stored locally on your device.

### How do I get a Gemini API key?

1. Visit [Google AI Studio](https://aistudio.google.com/)
2. Sign in with your Google account
3. Navigate to "Get API key"
4. Create a new API key
5. Copy the key and add it to your VwaTek Apply settings

### Is the Gemini API free?

Google offers a free tier for the Gemini API with usage limits. Check Google's pricing page for current limits and pricing for higher usage.

### How do I switch between devices?

Currently, data is stored locally on each device. Cross-device sync is planned for a future release. You can export your resumes as PDF and import them on another device.

---

## Features

### Resume Review & ATS Optimization

#### How accurate is the match scoring?

The match scoring analyzes keyword overlap, skills alignment, and experience relevance between your resume and the job description. While highly accurate, it's a tool to guide improvements, not an absolute measure.

#### What does "ATS Optimization" mean?

ATS (Applicant Tracking System) optimization ensures your resume contains the right keywords and formatting to pass automated screening systems used by employers.

#### Will the AI change my actual experience?

No. The AI suggests improvements to how you present your experience but will never fabricate or misrepresent your qualifications.

### Cover Letters

#### How personalized are the generated cover letters?

Each cover letter is generated based on:
- Your specific resume content
- The job description requirements
- Company information (if provided)
- Your selected tone preference

#### Can I edit the generated cover letters?

Yes. Generated cover letters are fully editable. We recommend reviewing and personalizing them before use.

### Interview Preparation

#### How does the mock interview work?

The AI acts as a realistic recruiter, asking questions based on:
- The job requirements
- Your resume content
- Common interview patterns for the role
- Follow-up questions based on your responses

#### What is the STAR method?

STAR stands for:
- **S**ituation - The context or background
- **T**ask - Your responsibility or goal
- **A**ction - The specific steps you took
- **R**esult - The outcome or impact

Our coaching feature helps you structure responses using this proven framework.

---

## Technical Questions

### Why Kotlin Multiplatform?

Kotlin Multiplatform allows us to share business logic across iOS, Android, and Web while maintaining native UI experiences on each platform. This ensures consistent behavior and reduces development time.

### What AI model powers VwaTek Apply?

VwaTek Apply uses **Gemini 3 Flash** from Google, which provides fast, high-quality responses optimized for our use cases.

### How is my data encrypted?

- **Database**: SQLDelight with SQLCipher (AES-256)
- **iOS**: Keychain Services for sensitive data
- **Android**: Android Keystore for sensitive data
- **Web**: Web Crypto API for encryption

### Can I use my own AI model?

Currently, only Gemini 3 Flash is supported. Support for additional models may be added in future releases.

---

## Troubleshooting

### The app is not analyzing my resume

1. Check your internet connection
2. Verify your Gemini API key is valid
3. Ensure your resume text is not empty
4. Try restarting the app

### AI responses are slow

Response time depends on:
- Your internet connection speed
- Gemini API server load
- Complexity of the request

Enable real-time streaming in settings for a more responsive experience.

### The app crashes on startup

**iOS/Android:**
1. Ensure you have the latest app version
2. Restart your device
3. Reinstall the app if the issue persists

**Web:**
1. Clear browser cache
2. Try a different browser
3. Disable browser extensions

### PDF export is not working

**iOS:** Ensure the app has permission to save files
**Android:** Grant storage permissions in device settings
**Web:** Check that pop-ups are not blocked

---

## Privacy & Data

### What data does VwaTek Apply collect?

VwaTek Apply does NOT collect any user data. All information stays on your device.

### Can I delete my data?

Yes. You can:
1. Delete individual resumes, cover letters, and interview sessions within the app
2. Delete the app entirely, which removes all data

### Does VwaTek Apply share my data with third parties?

No. Your data is never shared with third parties. When using AI features, only the necessary content is sent to the Gemini API for processing (and is not stored by Google per their API terms).

---

## Billing & Pricing

### Is VwaTek Apply free?

The app is free to download. AI features require a Gemini API key, which has free tier usage limits from Google.

### Are there premium features?

Currently, all features are available to all users. Premium tiers may be introduced in the future.

---

## Contact & Support

### How do I report a bug?

1. Open an issue on our [GitHub repository](https://github.com/vwatek/vwatek-apply/issues)
2. Use the bug report template
3. Include steps to reproduce, expected behavior, and screenshots

### How do I request a feature?

1. Check existing feature requests on GitHub
2. Open a new issue with the feature request template
3. Describe the use case and proposed solution

### How do I contact support?

- **Email:** support@vwatek.com
- **GitHub:** Open an issue or discussion
- **Response time:** Within 48 hours

---

## Future Plans

### What features are planned?

See our [Roadmap](../README.md#roadmap) for upcoming features:
- Voice Mock Interviews
- LinkedIn Integration
- Application Tracker

### How can I contribute?

See our [Contributing Guide](CONTRIBUTING.md) for details on how to contribute code, report bugs, or suggest features.
