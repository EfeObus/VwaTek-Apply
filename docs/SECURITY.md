# Security Policy

## Overview

VwaTek Apply takes the security and privacy of user data seriously. This document outlines our security practices, vulnerability reporting procedures, and data protection measures.

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.x.x   | Yes                |
| < 1.0   | No                 |

## Security Architecture

### Data Storage

#### On-Device Encryption

All user data is stored locally on the device with encryption:

```
+------------------------------------------------------------------+
|                    Data Protection Layers                         |
+------------------------------------------------------------------+
|                                                                   |
|  +------------------------------------------------------------+  |
|  |              SQLDelight + SQLCipher                        |  |
|  |         AES-256 Encrypted Database                         |  |
|  |                                                            |  |
|  |  * Resume content                                          |  |
|  |  * Cover letters                                           |  |
|  |  * Interview transcripts                                   |  |
|  |  * Job descriptions                                        |  |
|  +------------------------------------------------------------+  |
|                           |                                       |
|                           v                                       |
|  +------------------------------------------------------------+  |
|  |         iOS Keychain / Android Keystore / Web Crypto      |  |
|  |         Secure Credential Storage                          |  |
|  |                                                            |  |
|  |  * API keys                                                |  |
|  |  * Database encryption key                                 |  |
|  |  * User preferences (sensitive)                            |  |
|  +------------------------------------------------------------+  |
|                                                                   |
+------------------------------------------------------------------+
```

#### Database Encryption Implementation

**Common Interface:**
```kotlin
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
```

**iOS Implementation:**
```kotlin
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val passphrase = getOrCreatePassphrase()
        return NativeSqliteDriver(
            schema = VwaTekDatabase.Schema,
            name = "vwatek.db",
            onConfiguration = { config ->
                config.copy(extendedConfig = DatabaseConfiguration.Extended(
                    foreignKeys = true,
                    encryptionKey = passphrase
                ))
            }
        )
    }
    
    private fun getOrCreatePassphrase(): String {
        // Retrieve from Keychain or generate new
        return KeychainManager.getOrCreate("db_passphrase") {
            generateSecureRandom(32)
        }
    }
}
```

**Android Implementation:**
```kotlin
actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        val passphrase = getOrCreatePassphrase()
        return AndroidSqliteDriver(
            schema = VwaTekDatabase.Schema,
            context = context,
            name = "vwatek.db",
            factory = SupportFactory(passphrase)
        )
    }
    
    private fun getOrCreatePassphrase(): ByteArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        // Retrieve from Android Keystore or generate new
        return KeystoreManager.getOrCreate(masterKey, "db_passphrase") {
            generateSecureRandom(32)
        }
    }
}
```

### Network Security

#### TLS Configuration

All network communications use TLS 1.3:

```kotlin
val httpClient = HttpClient {
    install(HttpsRedirect)
    
    engine {
        https {
            // Enforce TLS 1.3
            minVersion = TlsVersion.TLS_1_3
            
            // Certificate pinning for Gemini API
            addTrustManager(CertificatePinnerTrustManager(
                "generativelanguage.googleapis.com" to listOf(
                    "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
                )
            ))
        }
    }
}
```

#### API Key Protection

- API keys are never stored in source code
- Keys are loaded from `secrets.properties` at build time
- Runtime keys stored in iOS Keychain / Android Keystore
- Keys are never logged or transmitted unnecessarily

### Data Handling

#### What We Store Locally

| Data Type | Storage Location | Encryption |
|-----------|-----------------|------------|
| Resumes | SQLDelight DB | AES-256 |
| Cover Letters | SQLDelight DB | AES-256 |
| Job Descriptions | SQLDelight DB | AES-256 |
| Interview Sessions | SQLDelight DB | AES-256 |
| API Key | iOS Keychain / Android Keystore / Web Crypto | System |
| User Preferences | iOS Keychain / Android Keystore / Web Crypto | System |

#### What We DON'T Store

- We do NOT store data on any server
- We do NOT collect analytics or telemetry
- We do NOT track user behavior
- We do NOT share data with third parties

### Data Transmission

#### Gemini API Communication

When communicating with the Gemini API:

1. **Minimal Data**: Only necessary content is sent
2. **No PII Logging**: Personal information is never logged
3. **Secure Transport**: TLS 1.3 encryption
4. **No Storage**: Google does not store prompts or responses (per API terms)

```kotlin
// Data sanitization before API calls
fun sanitizeForApi(content: String): String {
    return content
        .removePhoneNumbers()
        .removeEmailAddresses()
        .removeSocialSecurityNumbers()
        .removeAddresses()
}
```

## Vulnerability Reporting

### Reporting a Vulnerability

If you discover a security vulnerability, please report it responsibly:

1. **DO NOT** create a public GitHub issue
2. Email security details to: **security@vwatek.com**
3. Include:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if any)

### What to Expect

| Timeline | Action |
|----------|--------|
| 24 hours | Acknowledgment of report |
| 72 hours | Initial assessment |
| 7 days | Detailed response with action plan |
| 30-90 days | Fix deployed (depending on severity) |

### Severity Classification

| Severity | Description | Response Time |
|----------|-------------|---------------|
| **Critical** | Remote code execution, data breach | 24-48 hours |
| **High** | Authentication bypass, data exposure | 1 week |
| **Medium** | Limited data access, DoS | 2 weeks |
| **Low** | Minor issues, theoretical attacks | 1 month |

### Bug Bounty

We appreciate security researchers who help keep VwaTek Apply secure. While we don't have a formal bug bounty program, we will:

- Credit researchers in our security acknowledgments
- Provide reference letters upon request
- Consider monetary rewards for critical findings

## Security Best Practices for Users

### Protecting Your Data

1. **Keep Your Device Updated**
   - Always use the latest iOS/Android version
   - Keep your browser updated for Web version
   - Enable automatic updates

2. **Use Device Security**
   - Enable Face ID, Touch ID, or fingerprint authentication
   - Use a strong device passcode
   - Enable Find My Device features

3. **API Key Security**
   - Never share your Gemini API key
   - Rotate keys periodically
   - Use separate keys for development and production

4. **Export Security**
   - Be cautious when sharing exported PDFs
   - Delete temporary files after use
   - Use secure sharing methods

### What to Do If Compromised

If you suspect your data has been compromised:

1. Delete the app and reinstall
2. Generate a new Gemini API key
3. Review exported documents for sensitive info
4. Contact us at security@vwatek.com

## Compliance

### Privacy Regulations

VwaTek Apply is designed with privacy by default:

- **GDPR Compliant**: No personal data leaves your device without consent
- **CCPA Compliant**: No sale of personal information
- **SOC 2 Principles**: Security and privacy controls in place

### Data Retention

All data is stored locally on your device. We do not retain any user data on our servers because we don't have servers storing user data.

To delete all data:
1. Delete the app from your device
2. Data is automatically removed with app deletion

## Security Updates

### Notification Process

Security updates are communicated through:

1. App Store / Play Store release notes
2. Web application banner notifications
3. In-app notifications (for critical updates)
4. GitHub security advisories
5. Email to registered users (optional)

### Update Recommendations

| Update Type | Recommendation |
|-------------|----------------|
| Security Patch | Install immediately |
| Minor Update | Install within 1 week |
| Major Update | Install within 2 weeks |

## Third-Party Dependencies

### Dependency Security

We regularly audit our dependencies:

```
+------------------------------------------------------------------+
|                  Dependency Security                              |
+------------------------------------------------------------------+
|  * Automated vulnerability scanning (Dependabot)                 |
|  * Regular dependency updates                                    |
|  * Security-focused code reviews                                 |
|  * License compliance checks                                     |
+------------------------------------------------------------------+
```

### Key Dependencies

| Dependency | Purpose | Security Notes |
|------------|---------|----------------|
| Ktor | Networking | TLS support, regular updates |
| SQLDelight | Database | SQLCipher integration |
| Koin | DI | No security concerns |
| Compose | UI | Google-maintained |

## Incident Response

### Response Plan

In case of a security incident:

1. **Identify**: Determine scope and impact
2. **Contain**: Prevent further damage
3. **Eradicate**: Remove the threat
4. **Recover**: Restore normal operations
5. **Learn**: Post-incident analysis

### Communication

During incidents, we will:

- Notify affected users within 72 hours
- Provide clear remediation steps
- Publish post-incident reports
- Update security measures as needed

## Contact

For security-related inquiries:

- **Email**: security@vwatek.com
- **PGP Key**: [Available upon request]
- **Response Time**: Within 24 hours

---

Last Updated: February 2026

This security policy is reviewed and updated quarterly.
