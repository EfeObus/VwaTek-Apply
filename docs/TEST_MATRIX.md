# VwaTek Apply - Cross-Platform Test Matrix

**Last Updated:** February 8, 2026  
**Purpose:** Track feature parity verification across Android, iOS, and Web platforms

---

## Legend

| Symbol | Meaning |
|--------|---------|
|  | Implemented and verified |
|  | Partially implemented |
|  | Not implemented |
|  | Needs testing |

---

## 1. Authentication Features

| Feature | Android | iOS | Web |
|---------|---------|-----|-----|
| Email/Password Login |  |  |  |
| Email/Password Registration |  |  |  |
| Google Sign-In |  |  |  |
| LinkedIn Sign-In |  |  |  |
| Apple Sign-In |  |  |  |
| Forgot Password |  |  |  |
| Change Password |  |  |  |
| Remember Me |  |  |  |
| Logout |  |  |  |
| Profile View |  |  |  |
| Profile Edit |  |  |  |

---

## 2. Resume Features

| Feature | Android | iOS | Web |
|---------|---------|-----|-----|
| Create Resume |  |  |  |
| Edit Resume |  |  |  |
| Delete Resume |  |  |  |
| View Resume List |  |  |  |
| LinkedIn Import |  |  |  |
| PDF Export |  |  |  |
| Version History |  |  |  |
| Restore Version |  |  |  |
| Template Selection |  |  |  |

---

## 3. Optimizer Features

| Feature | Android | iOS | Web |
|---------|---------|-----|-----|
| ATS Score Analysis |  |  |  |
| Format Issues Detection |  |  |  |
| Keyword Analysis |  |  |  |
| Structure Issues |  |  |  |
| Recommendations |  |  |  |
| Section Rewriter |  |  |  |
| Writing Style Selection |  |  |  |
| Target Keywords Input |  |  |  |

---

## 4. Cover Letter Features

| Feature | Android | iOS | Web |
|---------|---------|-----|-----|
| Generate Cover Letter |  |  |  |
| Tone Selection |  |  |  |
| Saved Letters List |  |  |  |
| View Saved Letter |  |  |  |
| Delete Cover Letter |  |  |  |
| Copy to Clipboard |  |  |  |
| Share Cover Letter |  |  |  |
| Resume Selection |  |  |  |

---

## 5. Interview Prep Features

| Feature | Android | iOS | Web |
|---------|---------|-----|-----|
| Start Interview Session |  |  |  |
| Resume Selection |  |  |  |
| Question Generation |  |  |  |
| Answer Recording |  |  |  |
| Feedback Display |  |  |  |
| Session History |  |  |  |
| STAR Method Coaching |  |  |  |
| Copy STAR Response |  |  |  |

---

## 6. Settings Features

| Feature | Android | iOS | Web |
|---------|---------|-----|-----|
| API Key Configuration |  |  |  |
| Notification Preferences |  |  |  |
| Dark Mode |  |  |  |
| Appearance Settings |  |  |  |
| Data Export |  |  |  |
| Clear All Data |  |  |  |
| About/Version Info |  |  |  |

---

## 7. Dashboard/Home Features

| Feature | Android | iOS | Web |
|---------|---------|-----|-----|
| Welcome Card |  |  |  |
| Quick Stats |  |  |  |
| Getting Started Wizard |  |  |  |
| Step Completion Detection |  |  |  |
| Quick Actions |  |  |  |
| Pro Tips |  |  |  |

---

## 8. Navigation

| Feature | Android | iOS | Web |
|---------|---------|-----|-----|
| Bottom Navigation |  |  | N/A |
| Sidebar Navigation | N/A | N/A |  |
| Tab Navigation |  |  |  |
| Back Navigation |  |  |  |
| Deep Linking |  |  |  |

---

## 9. UI/UX Consistency

| Aspect | Android | iOS | Web |
|--------|---------|-----|-----|
| Loading States |  |  |  |
| Error Messages |  |  |  |
| Empty States |  |  |  |
| Confirmation Dialogs |  |  |  |
| Snackbar/Toast |  |  |  |
| Pull to Refresh |  |  |  |
| Skeleton Loaders |  |  |  |

---

## Build Verification Status

| Platform | Build Status | Last Verified |
|----------|--------------|---------------|
| Android (Debug) |  Passing | Feb 8, 2026 |
| iOS (Shared Module) |  Passing | Feb 8, 2026 |
| Web (Production) |  Passing | Feb 8, 2026 |

---

## Known Issues & Discrepancies

1. **Apple Sign-In:** Only available on iOS (platform limitation)
2. **Share functionality:** Web uses ShareLink API, Android needs share intent implementation
3. **Web Profile Edit:** Limited compared to mobile (missing some fields)
4. **LinkedIn Import on Web:** OAuth popup flow may have CORS issues
5. **Deep Linking:** Needs further implementation on mobile

---

## Test Environment

| Platform | Test Device/Browser | OS Version |
|----------|---------------------|------------|
| Android | Pixel 7 (Emulator) | Android 14 |
| iOS | iPhone 15 (Simulator) | iOS 17 |
| Web | Chrome/Safari | Latest |

---

## Phase Completion Summary

| Phase | Description | Status |
|-------|-------------|--------|
| Phase 1 | Critical Navigation |  Complete |
| Phase 2 | Authentication Feature Parity |  Complete |
| Phase 3 | Resume Feature Parity |  Complete |
| Phase 4 | Optimizer Feature Parity |  Complete |
| Phase 5 | Interview Prep Feature Parity |  Complete |
| Phase 6 | Cover Letter Feature Parity |  Complete |
| Phase 7 | Profile and Settings Parity |  Complete |
| Phase 8 | Dashboard/Home Feature Parity |  Complete |
| Phase 9 | Testing and Quality Assurance |  Complete |

---

## Sign-Off

- [ ] Android Lead Review
- [ ] iOS Lead Review  
- [ ] Web Lead Review
- [ ] QA Sign-Off
- [ ] Product Owner Approval
