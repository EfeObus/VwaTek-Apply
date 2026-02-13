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
            val minStr = min?.let { String.format("$%.2f", it) } ?: ""
            val maxStr = max?.let { String.format("$%.2f", it) } ?: ""
            val periodStr = if (period == "HOURLY") "/hr" else "/yr"
            
            return when {
                minStr.isNotEmpty() && maxStr.isNotEmpty() -> "$minStr - $maxStr$periodStr"
                minStr.isNotEmpty() -> "From $minStr$periodStr"
                maxStr.isNotEmpty() -> "Up to $maxStr$periodStr"
                else -> "Negotiable"
            }
        }
}

@Serializable
data class CanadianProvince(
    val code: String,
    val name: String,
    val nameFr: String
) {
    companion object {
        val ALL_PROVINCES = listOf(
            CanadianProvince("AB", "Alberta", "Alberta"),
            CanadianProvince("BC", "British Columbia", "Colombie-Britannique"),
            CanadianProvince("MB", "Manitoba", "Manitoba"),
            CanadianProvince("NB", "New Brunswick", "Nouveau-Brunswick"),
            CanadianProvince("NL", "Newfoundland and Labrador", "Terre-Neuve-et-Labrador"),
            CanadianProvince("NS", "Nova Scotia", "Nouvelle-Écosse"),
            CanadianProvince("NT", "Northwest Territories", "Territoires du Nord-Ouest"),
            CanadianProvince("NU", "Nunavut", "Nunavut"),
            CanadianProvince("ON", "Ontario", "Ontario"),
            CanadianProvince("PE", "Prince Edward Island", "Île-du-Prince-Édouard"),
            CanadianProvince("QC", "Quebec", "Québec"),
            CanadianProvince("SK", "Saskatchewan", "Saskatchewan"),
            CanadianProvince("YT", "Yukon", "Yukon")
        )
        
        fun fromCode(code: String): CanadianProvince? = ALL_PROVINCES.find { it.code == code }
    }
}

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
