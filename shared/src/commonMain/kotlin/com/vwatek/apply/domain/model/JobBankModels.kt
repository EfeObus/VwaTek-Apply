package com.vwatek.apply.domain.model

import kotlinx.serialization.Serializable

/**
 * Job Bank Canada Domain Models
 * 
 * Models for Job Bank Canada job search and labour market data
 */

@Serializable
data class JobBankJob(
    val id: String,
    val title: String,
    val employer: String,
    val location: JobBankLocation,
    val salary: JobBankSalary? = null,
    val nocCode: String? = null,
    val postingDate: String,
    val expiryDate: String? = null,
    val description: String,
    val requirements: List<String> = emptyList(),
    val benefits: List<String> = emptyList(),
    val hours: String? = null,
    val jobType: String? = null,
    val vacancies: Int = 1,
    val url: String
)

@Serializable
data class JobBankLocation(
    val city: String,
    val province: String,
    val postalCode: String? = null,
    val isRemote: Boolean = false
) {
    val displayName: String get() = if (isRemote) "$city, $province (Remote)" else "$city, $province"
}

@Serializable
data class JobBankSalary(
    val min: Double? = null,
    val max: Double? = null,
    val period: String, // "HOURLY" or "ANNUALLY"
    val currency: String = "CAD"
) {
    val displayRange: String
        get() {
            val minStr = min?.let { "$${it.formatCurrency()}" } ?: ""
            val maxStr = max?.let { "$${it.formatCurrency()}" } ?: ""
            val periodStr = if (period == "HOURLY") "/hr" else "/yr"
            
            return when {
                minStr.isNotEmpty() && maxStr.isNotEmpty() -> "$minStr - $maxStr$periodStr"
                minStr.isNotEmpty() -> "From $minStr$periodStr"
                maxStr.isNotEmpty() -> "Up to $maxStr$periodStr"
                else -> "Negotiable"
            }
        }
    
    private fun Double.formatCurrency(): String {
        val intPart = this.toLong()
        val decPart = ((this - intPart) * 100).toLong()
        return if (decPart == 0L) "$intPart.00" else "$intPart.${decPart.toString().padStart(2, '0')}"
    }
}

// CanadianProvince is defined in JobApplication.kt as an enum

@Serializable
data class JobBankSearchFilters(
    val query: String? = null,
    val location: String? = null,
    val provinceCode: String? = null,
    val nocCode: String? = null,
    val salaryMin: Double? = null,
    val remote: Boolean? = null
)

@Serializable
data class JobOutlook(
    val nocCode: String,
    val provinceCode: String? = null,
    val rating: OutlookRating,
    val description: String,
    val employmentGrowth: Double? = null,
    val retirementReplacements: Int? = null,
    val projectedOpenings: Int? = null,
    val medianWage: Double? = null
)

@Serializable
enum class OutlookRating(val displayName: String, val color: String) {
    VERY_GOOD("Very Good", "#22c55e"),
    GOOD("Good", "#84cc16"),
    FAIR("Fair", "#eab308"),
    LIMITED("Limited", "#f97316"),
    UNDETERMINED("Undetermined", "#6b7280")
}
