package com.vwatek.apply.util

import kotlinx.browser.window
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.files.File
import org.w3c.files.FileReader
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.js.Promise

/**
 * Document Parser utility for extracting text from PDF, DOCX, DOC, and TXT files
 * Uses PDF.js for PDFs and Mammoth.js for Word documents
 */
object DocumentParser {
    
    init {
        // Set PDF.js worker source
        try {
            val pdfjsLib = js("window.pdfjsLib")
            if (pdfjsLib != null && pdfjsLib != undefined) {
                pdfjsLib.GlobalWorkerOptions.workerSrc = 
                    "https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js"
            }
        } catch (e: Exception) {
            console.log("PDF.js not loaded yet")
        }
    }
    
    /**
     * Parse a file and extract its text content
     */
    suspend fun parseFile(file: File): Result<String> {
        val fileType = file.name.substringAfterLast(".").lowercase()
        
        return when (fileType) {
            "txt" -> parseTextFile(file)
            "pdf" -> parsePdfFile(file)
            "docx" -> parseDocxFile(file)
            "doc" -> parseDocFile(file)
            else -> Result.failure(IllegalArgumentException("Unsupported file type: $fileType"))
        }
    }
    
    /**
     * Parse a plain text file
     */
    private suspend fun parseTextFile(file: File): Result<String> {
        return try {
            val content = readFileAsText(file)
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Parse a PDF file using PDF.js
     */
    private suspend fun parsePdfFile(file: File): Result<String> {
        return try {
            val arrayBuffer = readFileAsArrayBuffer(file)
            val text = extractTextFromPdf(arrayBuffer)
            Result.success(text)
        } catch (e: Exception) {
            console.error("PDF parsing error: ${e.message}")
            Result.failure(Exception("Failed to parse PDF: ${e.message}"))
        }
    }
    
    /**
     * Parse a DOCX file using Mammoth.js
     */
    private suspend fun parseDocxFile(file: File): Result<String> {
        return try {
            val arrayBuffer = readFileAsArrayBuffer(file)
            val text = extractTextFromDocx(arrayBuffer)
            Result.success(text)
        } catch (e: Exception) {
            console.error("DOCX parsing error: ${e.message}")
            Result.failure(Exception("Failed to parse DOCX: ${e.message}"))
        }
    }
    
    /**
     * Parse a DOC file - older format, try with Mammoth or fallback
     */
    private suspend fun parseDocFile(file: File): Result<String> {
        // DOC files are harder to parse client-side
        // Try with Mammoth first (works for some DOC files)
        return try {
            val arrayBuffer = readFileAsArrayBuffer(file)
            val text = extractTextFromDocx(arrayBuffer)
            if (text.isNotBlank()) {
                Result.success(text)
            } else {
                Result.failure(Exception(
                    "DOC format not fully supported. Please convert to DOCX or paste the content manually."
                ))
            }
        } catch (e: Exception) {
            Result.failure(Exception(
                "DOC format parsing failed. Please convert to DOCX or PDF for best results."
            ))
        }
    }
    
    /**
     * Extract text from PDF using PDF.js
     */
    private suspend fun extractTextFromPdf(arrayBuffer: ArrayBuffer): String {
        val pdfjsLib = js("window.pdfjsLib")
        if (pdfjsLib == null || pdfjsLib == undefined) {
            throw Exception("PDF.js library not loaded")
        }
        
        // Create typed data for PDF.js
        val typedArray = Uint8Array(arrayBuffer)
        
        // Load the PDF document
        val loadingTask = pdfjsLib.getDocument(js("{ data: typedArray }"))
        val pdf = loadingTask.promise.unsafeCast<Promise<dynamic>>().await()
        
        val numPages = pdf.numPages as Int
        val textParts = mutableListOf<String>()
        
        // Extract text from each page
        for (pageNum in 1..numPages) {
            val page = pdf.getPage(pageNum).unsafeCast<Promise<dynamic>>().await()
            val textContent = page.getTextContent().unsafeCast<Promise<dynamic>>().await()
            
            val items = textContent.items.unsafeCast<Array<dynamic>>()
            val pageText = items.map { item -> 
                (item.str as? String) ?: ""
            }.joinToString(" ")
            
            textParts.add(pageText)
        }
        
        return textParts.joinToString("\n\n")
            .replace(Regex("\\s+"), " ")
            .replace(Regex(" ([.,;:!?])"), "$1")
            .trim()
    }
    
    /**
     * Extract text from DOCX using Mammoth.js
     */
    private suspend fun extractTextFromDocx(arrayBuffer: ArrayBuffer): String {
        val mammoth = js("window.mammoth")
        if (mammoth == null || mammoth == undefined) {
            throw Exception("Mammoth.js library not loaded")
        }
        
        val options = js("{}")
        options.arrayBuffer = arrayBuffer
        
        val result = mammoth.extractRawText(options).unsafeCast<Promise<dynamic>>().await()
        return (result.value as? String) ?: ""
    }
    
    /**
     * Read file as text
     */
    private suspend fun readFileAsText(file: File): String = suspendCoroutine { continuation ->
        val reader = FileReader()
        reader.onload = { event ->
            val result = event.target.asDynamic().result as? String
            if (result != null) {
                continuation.resume(result)
            } else {
                continuation.resumeWithException(Exception("Failed to read file as text"))
            }
        }
        reader.onerror = {
            continuation.resumeWithException(Exception("Error reading file"))
        }
        reader.readAsText(file)
    }
    
    /**
     * Read file as ArrayBuffer
     */
    private suspend fun readFileAsArrayBuffer(file: File): ArrayBuffer = suspendCoroutine { continuation ->
        val reader = FileReader()
        reader.onload = { event ->
            val result = event.target.asDynamic().result as? ArrayBuffer
            if (result != null) {
                continuation.resume(result)
            } else {
                continuation.resumeWithException(Exception("Failed to read file as ArrayBuffer"))
            }
        }
        reader.onerror = {
            continuation.resumeWithException(Exception("Error reading file"))
        }
        reader.readAsArrayBuffer(file)
    }
    
    /**
     * Get supported file types
     */
    fun getSupportedTypes(): List<String> = listOf("pdf", "docx", "doc", "txt")
    
    /**
     * Check if a file type is supported
     */
    fun isSupported(fileType: String): Boolean {
        return fileType.lowercase() in getSupportedTypes()
    }
    
    /**
     * Get accept attribute for file input
     */
    fun getAcceptAttribute(): String = ".pdf,.docx,.doc,.txt"
}

// Console logging utilities
private external val console: Console

private external interface Console {
    fun log(message: String)
    fun error(message: String)
}
