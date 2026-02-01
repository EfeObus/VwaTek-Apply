@file:JsModule("BUILTIN")
@file:JsNonModule

package com.vwatek.apply.util

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.files.File
import kotlin.js.Promise

// PDF.js external declarations
external object pdfjsLib {
    fun getDocument(data: dynamic): PDFDocumentLoadingTask
    var GlobalWorkerOptions: GlobalWorkerOptionsType
}

external interface GlobalWorkerOptionsType {
    var workerSrc: String
}

external interface PDFDocumentLoadingTask {
    val promise: Promise<PDFDocumentProxy>
}

external interface PDFDocumentProxy {
    val numPages: Int
    fun getPage(pageNumber: Int): Promise<PDFPageProxy>
}

external interface PDFPageProxy {
    fun getTextContent(): Promise<TextContent>
}

external interface TextContent {
    val items: Array<TextItem>
}

external interface TextItem {
    val str: String
}

// Mammoth.js external declarations
external object mammoth {
    fun extractRawText(options: MammothOptions): Promise<MammothResult>
    fun convertToHtml(options: MammothOptions): Promise<MammothResult>
}

external interface MammothOptions {
    var arrayBuffer: ArrayBuffer?
}

external interface MammothResult {
    val value: String
    val messages: Array<dynamic>
}

// Google Identity Services external declarations
external object google {
    val accounts: GoogleAccounts
}

external interface GoogleAccounts {
    val id: GoogleId
    val oauth2: GoogleOAuth2
}

external interface GoogleId {
    fun initialize(config: GoogleIdConfig)
    fun prompt(callback: (PromptMomentNotification) -> Unit)
    fun renderButton(element: dynamic, options: GoogleButtonOptions)
}

external interface GoogleIdConfig {
    var client_id: String
    var callback: (CredentialResponse) -> Unit
    var auto_select: Boolean?
    var cancel_on_tap_outside: Boolean?
}

external interface GoogleButtonOptions {
    var type: String?
    var theme: String?
    var size: String?
    var text: String?
    var shape: String?
    var logo_alignment: String?
    var width: Int?
}

external interface CredentialResponse {
    val credential: String
    val select_by: String
}

external interface PromptMomentNotification {
    fun isDisplayMoment(): Boolean
    fun isDisplayed(): Boolean
    fun isNotDisplayed(): Boolean
    fun getNotDisplayedReason(): String
    fun isSkippedMoment(): Boolean
    fun getSkippedReason(): String
    fun isDismissedMoment(): Boolean
    fun getDismissedReason(): String
}

external interface GoogleOAuth2 {
    fun initTokenClient(config: OAuth2Config): TokenClient
    fun initCodeClient(config: OAuth2Config): CodeClient
}

external interface OAuth2Config {
    var client_id: String
    var scope: String
    var callback: ((TokenResponse) -> Unit)?
    var error_callback: ((ErrorResponse) -> Unit)?
    var redirect_uri: String?
}

external interface TokenClient {
    fun requestAccessToken(overrideConfig: dynamic = definedExternally)
}

external interface CodeClient {
    fun requestCode()
}

external interface TokenResponse {
    val access_token: String
    val token_type: String
    val expires_in: Int
    val scope: String
}

external interface ErrorResponse {
    val type: String
    val message: String
}

// File Reader utilities
external class FileReaderSync : org.w3c.files.FileReader {
    fun readAsArrayBuffer(file: File): ArrayBuffer
}
