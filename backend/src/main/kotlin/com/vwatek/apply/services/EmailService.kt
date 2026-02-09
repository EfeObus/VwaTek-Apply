package com.vwatek.apply.services

import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.Properties

/**
 * Email service using SMTP (SendGrid)
 */
object EmailService {
    
    private val smtpHost = System.getenv("SMTP_HOST") ?: "smtp.sendgrid.net"
    private val smtpPort = System.getenv("SMTP_PORT") ?: "587"
    private val smtpUsername = System.getenv("SMTP_USERNAME") ?: "apikey"
    private val smtpPassword = System.getenv("SMTP_PASSWORD") ?: ""
    private val fromEmail = System.getenv("SMTP_FROM_EMAIL") ?: "noreply@vwatekapply.com"
    private val fromName = System.getenv("SMTP_FROM_NAME") ?: "VwaTek Apply"
    
    private val session: Session by lazy {
        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", smtpHost)
            put("mail.smtp.port", smtpPort)
            put("mail.smtp.ssl.protocols", "TLSv1.2")
        }
        
        Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(smtpUsername, smtpPassword)
            }
        })
    }
    
    /**
     * Check if email service is configured
     */
    fun isConfigured(): Boolean {
        return smtpPassword.isNotBlank()
    }
    
    /**
     * Send a password reset email
     */
    fun sendPasswordResetEmail(
        toEmail: String,
        userName: String,
        resetToken: String,
        resetLink: String
    ): Result<Unit> {
        return try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(fromEmail, fromName))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                subject = "Reset Your VwaTek Apply Password"
                
                val htmlContent = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <style>
                            body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; color: #333; }
                            .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                            .header { background: linear-gradient(135deg, #6366f1, #8b5cf6); color: white; padding: 30px; text-align: center; border-radius: 8px 8px 0 0; }
                            .content { background: #f9fafb; padding: 30px; border: 1px solid #e5e7eb; }
                            .button { display: inline-block; background: #6366f1; color: white; padding: 14px 28px; text-decoration: none; border-radius: 8px; font-weight: 600; margin: 20px 0; }
                            .button:hover { background: #4f46e5; }
                            .footer { text-align: center; padding: 20px; color: #6b7280; font-size: 14px; }
                            .code { background: #e5e7eb; padding: 10px 15px; border-radius: 4px; font-family: monospace; font-size: 16px; }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            <div class="header">
                                <h1>VwaTek Apply</h1>
                                <p>AI-Powered Job Application Assistant</p>
                            </div>
                            <div class="content">
                                <h2>Password Reset Request</h2>
                                <p>Hi ${userName},</p>
                                <p>We received a request to reset your password. Click the button below to create a new password:</p>
                                <p style="text-align: center;">
                                    <a href="${resetLink}" class="button">Reset Password</a>
                                </p>
                                <p>Or copy and paste this link into your browser:</p>
                                <p class="code">${resetLink}</p>
                                <p><strong>This link will expire in 1 hour.</strong></p>
                                <p>If you didn't request a password reset, you can safely ignore this email. Your password will remain unchanged.</p>
                            </div>
                            <div class="footer">
                                <p>© 2026 VwaTek Apply. All rights reserved.</p>
                                <p>This is an automated message, please do not reply.</p>
                            </div>
                        </div>
                    </body>
                    </html>
                """.trimIndent()
                
                setContent(htmlContent, "text/html; charset=utf-8")
            }
            
            Transport.send(message)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send a welcome email after registration
     */
    fun sendWelcomeEmail(toEmail: String, userName: String): Result<Unit> {
        return try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(fromEmail, fromName))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                subject = "Welcome to VwaTek Apply!"
                
                val htmlContent = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <style>
                            body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; color: #333; }
                            .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                            .header { background: linear-gradient(135deg, #6366f1, #8b5cf6); color: white; padding: 30px; text-align: center; border-radius: 8px 8px 0 0; }
                            .content { background: #f9fafb; padding: 30px; border: 1px solid #e5e7eb; }
                            .feature { margin: 15px 0; padding: 15px; background: white; border-radius: 8px; border-left: 4px solid #6366f1; }
                            .button { display: inline-block; background: #6366f1; color: white; padding: 14px 28px; text-decoration: none; border-radius: 8px; font-weight: 600; margin: 20px 0; }
                            .footer { text-align: center; padding: 20px; color: #6b7280; font-size: 14px; }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            <div class="header">
                                <h1>Welcome to VwaTek Apply! 🎉</h1>
                                <p>Your AI-Powered Job Application Assistant</p>
                            </div>
                            <div class="content">
                                <h2>Hi ${userName}!</h2>
                                <p>Thank you for joining VwaTek Apply. We're excited to help you land your dream job!</p>
                                
                                <h3>Here's what you can do:</h3>
                                
                                <div class="feature">
                                    <strong>📄 Smart Resume Builder</strong>
                                    <p>Create and optimize your resume with AI-powered suggestions.</p>
                                </div>
                                
                                <div class="feature">
                                    <strong>✉️ Cover Letter Generator</strong>
                                    <p>Generate tailored cover letters for each job application.</p>
                                </div>
                                
                                <div class="feature">
                                    <strong>🎯 Interview Prep</strong>
                                    <p>Practice with AI-powered mock interviews and get instant feedback.</p>
                                </div>
                                
                                <p style="text-align: center;">
                                    <a href="https://storage.googleapis.com/vwatek-apply-frontend/index.html" class="button">Get Started</a>
                                </p>
                            </div>
                            <div class="footer">
                                <p>© 2026 VwaTek Apply. All rights reserved.</p>
                            </div>
                        </div>
                    </body>
                    </html>
                """.trimIndent()
                
                setContent(htmlContent, "text/html; charset=utf-8")
            }
            
            Transport.send(message)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
