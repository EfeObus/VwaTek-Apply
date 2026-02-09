package com.vwatek.apply.android.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.vwatek.apply.domain.model.Resume
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for exporting resumes to PDF
 */
class PdfExportUtil(private val context: Context) {
    
    enum class Template {
        PROFESSIONAL,
        MODERN,
        CLASSIC,
        MINIMAL
    }
    
    companion object {
        private const val PAGE_WIDTH = 595 // A4 width in points
        private const val PAGE_HEIGHT = 842 // A4 height in points
        private const val MARGIN = 50
        private const val LINE_HEIGHT = 16
    }
    
    /**
     * Exports a resume to PDF with the specified template
     * @return URI of the generated PDF file
     */
    fun exportToPdf(resume: Resume, template: Template): Uri? {
        val document = PdfDocument()
        
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = document.startPage(pageInfo)
            
            when (template) {
                Template.PROFESSIONAL -> drawProfessionalTemplate(page.canvas, resume)
                Template.MODERN -> drawModernTemplate(page.canvas, resume)
                Template.CLASSIC -> drawClassicTemplate(page.canvas, resume)
                Template.MINIMAL -> drawMinimalTemplate(page.canvas, resume)
            }
            
            document.finishPage(page)
            
            // Save to file
            val fileName = "${resume.name.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
            val directory = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "resumes")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            
            val file = File(directory, fileName)
            FileOutputStream(file).use { outputStream ->
                document.writeTo(outputStream)
            }
            
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            document.close()
        }
    }
    
    /**
     * Share a PDF file
     */
    fun sharePdf(uri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Resume PDF"))
    }
    
    // MARK: - Template Drawing Methods
    
    private fun drawProfessionalTemplate(canvas: Canvas, resume: Resume) {
        val titlePaint = Paint().apply {
            color = Color.parseColor("#1a365d")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val sectionPaint = Paint().apply {
            color = Color.parseColor("#2563eb")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            isAntiAlias = true
        }
        
        val linePaint = Paint().apply {
            color = Color.parseColor("#2563eb")
            strokeWidth = 2f
        }
        
        var y = MARGIN.toFloat()
        
        // Header with blue line
        canvas.drawText(resume.name, MARGIN.toFloat(), y + 28, titlePaint)
        y += 40
        canvas.drawLine(MARGIN.toFloat(), y, (PAGE_WIDTH - MARGIN).toFloat(), y, linePaint)
        y += 30
        
        // Parse and draw content sections
        val sections = parseContent(resume.content)
        sections.forEach { (section, content) ->
            // Section header
            canvas.drawText(section.uppercase(), MARGIN.toFloat(), y, sectionPaint)
            y += 20
            
            // Section content
            val lines = wrapText(content, bodyPaint, (PAGE_WIDTH - 2 * MARGIN).toFloat())
            lines.forEach { line ->
                canvas.drawText(line, MARGIN.toFloat(), y, bodyPaint)
                y += LINE_HEIGHT
                if (y > PAGE_HEIGHT - MARGIN) return@forEach
            }
            y += 15
        }
    }
    
    private fun drawModernTemplate(canvas: Canvas, resume: Resume) {
        // Modern: Uses accent colors and clean sans-serif fonts
        val accentColor = Color.parseColor("#0ea5e9")
        
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 32f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            isAntiAlias = true
        }
        
        val sectionPaint = Paint().apply {
            color = accentColor
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.1f
            isAntiAlias = true
        }
        
        val bodyPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 11f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            isAntiAlias = true
        }
        
        val accentRect = Paint().apply {
            color = accentColor
        }
        
        var y = MARGIN.toFloat()
        
        // Accent bar at top
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 8f, accentRect)
        
        // Name
        canvas.drawText(resume.name, MARGIN.toFloat(), y + 32, titlePaint)
        y += 50
        
        // Draw sections
        val sections = parseContent(resume.content)
        sections.forEach { (section, content) ->
            canvas.drawText(section.uppercase(), MARGIN.toFloat(), y, sectionPaint)
            y += 18
            
            val lines = wrapText(content, bodyPaint, (PAGE_WIDTH - 2 * MARGIN).toFloat())
            lines.forEach { line ->
                canvas.drawText(line, MARGIN.toFloat(), y, bodyPaint)
                y += LINE_HEIGHT
                if (y > PAGE_HEIGHT - MARGIN) return@forEach
            }
            y += 20
        }
    }
    
    private fun drawClassicTemplate(canvas: Canvas, resume: Resume) {
        // Classic: Traditional serif fonts, formal layout
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val sectionPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        
        val linePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 1f
        }
        
        var y = MARGIN.toFloat() + 10
        
        // Centered name
        val nameWidth = titlePaint.measureText(resume.name)
        canvas.drawText(resume.name, (PAGE_WIDTH - nameWidth) / 2, y + 24, titlePaint)
        y += 35
        
        // Underline
        canvas.drawLine(MARGIN.toFloat(), y, (PAGE_WIDTH - MARGIN).toFloat(), y, linePaint)
        y += 25
        
        // Sections
        val sections = parseContent(resume.content)
        sections.forEach { (section, content) ->
            canvas.drawText(section, MARGIN.toFloat(), y, sectionPaint)
            y += 5
            canvas.drawLine(MARGIN.toFloat(), y, (MARGIN + 100).toFloat(), y, linePaint)
            y += 15
            
            val lines = wrapText(content, bodyPaint, (PAGE_WIDTH - 2 * MARGIN).toFloat())
            lines.forEach { line ->
                canvas.drawText(line, MARGIN.toFloat(), y, bodyPaint)
                y += LINE_HEIGHT
                if (y > PAGE_HEIGHT - MARGIN) return@forEach
            }
            y += 15
        }
    }
    
    private fun drawMinimalTemplate(canvas: Canvas, resume: Resume) {
        // Minimal: Simple, lots of whitespace, subtle design
        val titlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 22f
            typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL)
            isAntiAlias = true
        }
        
        val sectionPaint = Paint().apply {
            color = Color.GRAY
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            letterSpacing = 0.15f
            isAntiAlias = true
        }
        
        val bodyPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
            isAntiAlias = true
        }
        
        var y = MARGIN.toFloat() + 20
        
        // Simple name
        canvas.drawText(resume.name, MARGIN.toFloat(), y + 22, titlePaint)
        y += 50
        
        // Sections with lots of spacing
        val sections = parseContent(resume.content)
        sections.forEach { (section, content) ->
            canvas.drawText(section.uppercase(), MARGIN.toFloat(), y, sectionPaint)
            y += 20
            
            val lines = wrapText(content, bodyPaint, (PAGE_WIDTH - 2 * MARGIN).toFloat())
            lines.forEach { line ->
                canvas.drawText(line, MARGIN.toFloat(), y, bodyPaint)
                y += 14
                if (y > PAGE_HEIGHT - MARGIN) return@forEach
            }
            y += 25
        }
    }
    
    // MARK: - Helper Methods
    
    private fun parseContent(content: String): List<Pair<String, String>> {
        val sections = mutableListOf<Pair<String, String>>()
        val lines = content.split("\n")
        
        var currentSection = "Profile"
        var currentContent = StringBuilder()
        
        // Common section headers
        val sectionHeaders = listOf(
            "summary", "objective", "experience", "education", 
            "skills", "certifications", "projects", "awards"
        )
        
        for (line in lines) {
            val trimmed = line.trim()
            val lowerLine = trimmed.lowercase()
            
            if (sectionHeaders.any { lowerLine.startsWith(it) || lowerLine == it }) {
                // Save previous section
                if (currentContent.isNotEmpty()) {
                    sections.add(currentSection to currentContent.toString().trim())
                }
                currentSection = trimmed.replaceFirstChar { it.uppercase() }
                currentContent = StringBuilder()
            } else if (trimmed.isNotEmpty()) {
                currentContent.append(trimmed).append(" ")
            }
        }
        
        // Add last section
        if (currentContent.isNotEmpty()) {
            sections.add(currentSection to currentContent.toString().trim())
        }
        
        // If no sections found, create a single "Content" section
        if (sections.isEmpty() && content.isNotBlank()) {
            sections.add("Content" to content.trim())
        }
        
        return sections
    }
    
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)
            
            if (width > maxWidth) {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                    currentLine = StringBuilder(word)
                } else {
                    lines.add(word)
                }
            } else {
                currentLine = StringBuilder(testLine)
            }
        }
        
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        
        return lines
    }
}
