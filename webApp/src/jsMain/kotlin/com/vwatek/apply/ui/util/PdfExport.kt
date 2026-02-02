package com.vwatek.apply.ui.util

import com.vwatek.apply.domain.model.Resume
import kotlinx.browser.document
import kotlinx.browser.window

/**
 * PDF Export utility for generating PDF resumes using browser print functionality.
 * Creates a styled HTML representation and triggers the browser's print dialog.
 */
object PdfExport {
    
    /**
     * Export a resume to PDF by opening a new window with print-optimized content.
     * The user can then use the browser's print dialog to save as PDF.
     */
    fun exportResumeToPdf(resume: Resume, format: ResumeFormat = ResumeFormat.PROFESSIONAL) {
        val htmlContent = generateResumeHtml(resume, format)
        
        // Create a new window for printing
        val printWindow = window.open("", "_blank")
        printWindow?.document?.write(htmlContent)
        printWindow?.document?.close()
        
        // Trigger print after a short delay to ensure content is loaded
        window.setTimeout({
            printWindow?.print()
        }, 500)
    }
    
    /**
     * Preview the resume PDF layout in a new window without triggering print.
     */
    fun previewResume(resume: Resume, format: ResumeFormat = ResumeFormat.PROFESSIONAL): String {
        return generateResumeHtml(resume, format)
    }
    
    private fun generateResumeHtml(resume: Resume, format: ResumeFormat): String {
        val styles = getFormatStyles(format)
        val content = parseResumeContent(resume.content)
        
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${escapeHtml(resume.name)} - Resume</title>
    <style>
        @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap');
        @import url('https://fonts.googleapis.com/css2?family=Merriweather:wght@300;400;700&display=swap');
        
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        @page {
            size: A4;
            margin: 0.5in;
        }
        
        body {
            font-family: ${styles.fontFamily};
            font-size: ${styles.fontSize};
            line-height: ${styles.lineHeight};
            color: ${styles.textColor};
            background: white;
            padding: 0.5in;
            max-width: 8.5in;
            margin: 0 auto;
        }
        
        @media print {
            body {
                padding: 0;
            }
        }
        
        .resume-header {
            text-align: ${styles.headerAlign};
            margin-bottom: 1.5rem;
            padding-bottom: 1rem;
            border-bottom: ${styles.borderStyle};
        }
        
        .resume-name {
            font-size: ${styles.nameSize};
            font-weight: 700;
            color: ${styles.accentColor};
            margin-bottom: 0.5rem;
        }
        
        .contact-info {
            font-size: 0.9rem;
            color: #666;
        }
        
        .contact-info span {
            margin: 0 0.5rem;
        }
        
        .section {
            margin-bottom: 1.25rem;
        }
        
        .section-title {
            font-size: 1.1rem;
            font-weight: 600;
            color: ${styles.accentColor};
            margin-bottom: 0.75rem;
            padding-bottom: 0.25rem;
            border-bottom: ${styles.sectionBorder};
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }
        
        .entry {
            margin-bottom: 1rem;
        }
        
        .entry-header {
            display: flex;
            justify-content: space-between;
            align-items: baseline;
            margin-bottom: 0.25rem;
        }
        
        .entry-title {
            font-weight: 600;
            font-size: 1rem;
        }
        
        .entry-subtitle {
            font-style: italic;
            color: #555;
        }
        
        .entry-date {
            color: #666;
            font-size: 0.9rem;
        }
        
        .entry-content {
            margin-top: 0.5rem;
        }
        
        ul {
            margin-left: 1.25rem;
            margin-top: 0.5rem;
        }
        
        li {
            margin-bottom: 0.25rem;
        }
        
        .skills-list {
            display: flex;
            flex-wrap: wrap;
            gap: 0.5rem;
            list-style: none;
            margin-left: 0;
        }
        
        .skills-list li {
            background: ${styles.skillBadgeBg};
            padding: 0.25rem 0.75rem;
            border-radius: 4px;
            font-size: 0.9rem;
        }
        
        p {
            margin-bottom: 0.5rem;
        }
        
        .summary {
            margin-bottom: 1rem;
        }
        
        /* Print-specific styles */
        @media print {
            .no-print {
                display: none !important;
            }
            
            .section {
                page-break-inside: avoid;
            }
        }
    </style>
</head>
<body>
    <div class="resume-header">
        <h1 class="resume-name">${escapeHtml(resume.name)}</h1>
        ${if (content.contact.isNotBlank()) "<div class=\"contact-info\">${escapeHtml(content.contact)}</div>" else ""}
    </div>
    
    ${content.sections.joinToString("\n") { section -> generateSectionHtml(section) }}
    
    <div class="no-print" style="margin-top: 2rem; text-align: center; color: #999;">
        <p>Press Ctrl+P (Cmd+P on Mac) to save as PDF</p>
        <button onclick="window.print()" style="padding: 10px 20px; cursor: pointer; margin-top: 10px;">Print / Save as PDF</button>
    </div>
</body>
</html>
        """.trimIndent()
    }
    
    private fun generateSectionHtml(section: ResumeSection): String {
        return """
        <div class="section">
            <h2 class="section-title">${escapeHtml(section.title)}</h2>
            <div class="section-content">
                ${section.content}
            </div>
        </div>
        """.trimIndent()
    }
    
    private fun parseResumeContent(content: String): ParsedResume {
        val lines = content.lines()
        var contact = ""
        val sections = mutableListOf<ResumeSection>()
        var currentSection: ResumeSection? = null
        val contentBuilder = StringBuilder()
        
        for (line in lines) {
            val trimmedLine = line.trim()
            
            // Detect section headers (common patterns)
            if (isSectionHeader(trimmedLine)) {
                // Save previous section
                currentSection?.let { 
                    sections.add(it.copy(content = contentBuilder.toString().trim()))
                }
                contentBuilder.clear()
                currentSection = ResumeSection(title = cleanSectionTitle(trimmedLine), content = "")
            } else if (currentSection != null) {
                // Add content to current section
                if (trimmedLine.isNotEmpty()) {
                    if (trimmedLine.startsWith("•") || trimmedLine.startsWith("-") || trimmedLine.startsWith("*")) {
                        // Bullet point
                        val bulletContent = trimmedLine.removePrefix("•").removePrefix("-").removePrefix("*").trim()
                        contentBuilder.append("<li>${escapeHtml(bulletContent)}</li>\n")
                    } else if (contentBuilder.isEmpty() || !contentBuilder.toString().contains("<li>")) {
                        contentBuilder.append("<p>${escapeHtml(trimmedLine)}</p>\n")
                    } else {
                        contentBuilder.append("<li>${escapeHtml(trimmedLine)}</li>\n")
                    }
                }
            } else {
                // Content before first section (likely contact info)
                if (trimmedLine.isNotEmpty()) {
                    contact += if (contact.isEmpty()) trimmedLine else " | $trimmedLine"
                }
            }
        }
        
        // Add last section
        currentSection?.let {
            sections.add(it.copy(content = wrapInUlIfNeeded(contentBuilder.toString().trim())))
        }
        
        return ParsedResume(contact, sections)
    }
    
    private fun wrapInUlIfNeeded(content: String): String {
        return if (content.contains("<li>") && !content.contains("<ul>")) {
            "<ul>$content</ul>"
        } else {
            content
        }
    }
    
    private fun isSectionHeader(line: String): Boolean {
        val sectionKeywords = listOf(
            "summary", "objective", "profile", "about",
            "experience", "employment", "work history", "professional experience",
            "education", "academic", "qualifications",
            "skills", "technical skills", "competencies", "abilities",
            "projects", "portfolio",
            "certifications", "certificates", "licenses",
            "awards", "achievements", "honors",
            "publications", "research",
            "volunteer", "community",
            "interests", "activities",
            "references"
        )
        
        val lowerLine = line.lowercase().replace(":", "").trim()
        return sectionKeywords.any { keyword -> 
            lowerLine == keyword || lowerLine.startsWith(keyword)
        }
    }
    
    private fun cleanSectionTitle(line: String): String {
        return line.replace(":", "").trim().replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase() else it.toString() 
        }
    }
    
    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
    }
    
    private fun getFormatStyles(format: ResumeFormat): FormatStyles {
        return when (format) {
            ResumeFormat.PROFESSIONAL -> FormatStyles(
                fontFamily = "'Inter', -apple-system, BlinkMacSystemFont, sans-serif",
                fontSize = "11pt",
                lineHeight = "1.5",
                textColor = "#333",
                accentColor = "#2563EB",
                headerAlign = "center",
                borderStyle = "2px solid #2563EB",
                sectionBorder = "1px solid #e5e7eb",
                nameSize = "2rem",
                skillBadgeBg = "#EFF6FF"
            )
            ResumeFormat.MODERN -> FormatStyles(
                fontFamily = "'Inter', -apple-system, BlinkMacSystemFont, sans-serif",
                fontSize = "11pt",
                lineHeight = "1.5",
                textColor = "#1f2937",
                accentColor = "#7C3AED",
                headerAlign = "left",
                borderStyle = "none",
                sectionBorder = "2px solid #7C3AED",
                nameSize = "2.25rem",
                skillBadgeBg = "#F3E8FF"
            )
            ResumeFormat.CLASSIC -> FormatStyles(
                fontFamily = "'Merriweather', Georgia, serif",
                fontSize = "11pt",
                lineHeight = "1.6",
                textColor = "#1a1a1a",
                accentColor = "#1a1a1a",
                headerAlign = "center",
                borderStyle = "1px solid #1a1a1a",
                sectionBorder = "1px solid #1a1a1a",
                nameSize = "1.75rem",
                skillBadgeBg = "#f5f5f5"
            )
            ResumeFormat.MINIMAL -> FormatStyles(
                fontFamily = "'Inter', -apple-system, BlinkMacSystemFont, sans-serif",
                fontSize = "10.5pt",
                lineHeight = "1.4",
                textColor = "#374151",
                accentColor = "#374151",
                headerAlign = "left",
                borderStyle = "none",
                sectionBorder = "none",
                nameSize = "1.5rem",
                skillBadgeBg = "transparent"
            )
        }
    }
}

enum class ResumeFormat {
    PROFESSIONAL,
    MODERN,
    CLASSIC,
    MINIMAL
}

data class FormatStyles(
    val fontFamily: String,
    val fontSize: String,
    val lineHeight: String,
    val textColor: String,
    val accentColor: String,
    val headerAlign: String,
    val borderStyle: String,
    val sectionBorder: String,
    val nameSize: String,
    val skillBadgeBg: String
)

data class ResumeSection(
    val title: String,
    val content: String
)

data class ParsedResume(
    val contact: String,
    val sections: List<ResumeSection>
)
