package com.vwatek.apply.data.security

import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.*

/**
 * iOS Keychain wrapper for secure token storage.
 * Replaces NSUserDefaults for sensitive data like auth tokens.
 */
@OptIn(ExperimentalForeignApi::class)
object KeychainHelper {
    
    private const val SERVICE_NAME = "com.vwatek.apply"
    
    /**
     * Save a string value to the Keychain.
     * Updates existing item if it already exists.
     */
    fun save(key: String, value: String): Boolean {
        // Delete existing item first (update = delete + add)
        delete(key)
        
        val valueData = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return false
        
        val query = cfDictionaryOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to CFBridgingRetain(SERVICE_NAME),
            kSecAttrAccount to CFBridgingRetain(key),
            kSecValueData to CFBridgingRetain(valueData),
            kSecAttrAccessible to kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        )
        
        val status = SecItemAdd(query, null)
        CFRelease(query)
        return status == errSecSuccess
    }
    
    /**
     * Retrieve a string value from the Keychain.
     */
    fun get(key: String): String? {
        val query = cfDictionaryOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to CFBridgingRetain(SERVICE_NAME),
            kSecAttrAccount to CFBridgingRetain(key),
            kSecReturnData to CFBridgingRetain(true as Any),
            kSecMatchLimit to kSecMatchLimitOne
        )
        
        memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)
            CFRelease(query)
            
            if (status == errSecSuccess) {
                val data = CFBridgingRelease(result.value) as? NSData ?: return null
                return NSString.create(data = data, encoding = NSUTF8StringEncoding) as? String
            }
        }
        return null
    }
    
    /**
     * Delete a value from the Keychain.
     */
    fun delete(key: String): Boolean {
        val query = cfDictionaryOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to CFBridgingRetain(SERVICE_NAME),
            kSecAttrAccount to CFBridgingRetain(key)
        )
        
        val status = SecItemDelete(query)
        CFRelease(query)
        return status == errSecSuccess || status == errSecItemNotFound
    }
    
    /**
     * Helper to create a CFDictionary from key-value pairs.
     */
    private fun cfDictionaryOf(vararg pairs: Pair<CFStringRef?, Any?>): CFDictionaryRef? {
        val keys = pairs.map { it.first as CFTypeRef? }
        val values = pairs.map { it.second as CFTypeRef? }
        
        return memScoped {
            val keysArray = allocArrayOf(*keys.toTypedArray())
            val valuesArray = allocArrayOf(*values.toTypedArray())
            CFDictionaryCreate(
                null,
                keysArray.reinterpret(),
                valuesArray.reinterpret(),
                pairs.size.toLong(),
                null,
                null
            )
        }
    }
}
