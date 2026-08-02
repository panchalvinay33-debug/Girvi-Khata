package com.girvikhata.app

import android.content.Context

/** Small local owner/business identity used for first-run setup and app branding. */
class OwnerBusinessProfileStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun load(): OwnerBusinessProfile = OwnerBusinessProfile(
        businessName = preferences.getString(KEY_BUSINESS_NAME, "").orEmpty(),
        ownerName = preferences.getString(KEY_OWNER_NAME, "").orEmpty(),
        mobile = preferences.getString(KEY_MOBILE, "").orEmpty(),
        address = preferences.getString(KEY_ADDRESS, "").orEmpty(),
    )

    fun isConfigured(): Boolean = load().let { it.businessName.isNotBlank() && it.ownerName.isNotBlank() }

    fun save(profile: OwnerBusinessProfile) {
        val normalized = normalize(profile)
        preferences.edit()
            .putString(KEY_BUSINESS_NAME, normalized.businessName)
            .putString(KEY_OWNER_NAME, normalized.ownerName)
            .putString(KEY_MOBILE, normalized.mobile)
            .putString(KEY_ADDRESS, normalized.address)
            .commit()
    }

    companion object {
        private const val FILE_NAME = "owner_business_profile_v1"
        private const val KEY_BUSINESS_NAME = "business_name"
        private const val KEY_OWNER_NAME = "owner_name"
        private const val KEY_MOBILE = "mobile"
        private const val KEY_ADDRESS = "address"
        private const val MAX_NAME_LENGTH = 80
        private const val MAX_ADDRESS_LENGTH = 180

        internal fun normalize(profile: OwnerBusinessProfile): OwnerBusinessProfile {
            val businessName = singleLine(profile.businessName).take(MAX_NAME_LENGTH)
            val ownerName = singleLine(profile.ownerName).take(MAX_NAME_LENGTH)
            val mobile = normalizeIndianMobile(profile.mobile)
            val address = profile.address.trim().replace(Regex("\\s+"), " ").take(MAX_ADDRESS_LENGTH)
            require(businessName.isNotBlank()) { "Dukaan / business ka naam required hai" }
            require(ownerName.isNotBlank()) { "Owner / user ka naam required hai" }
            require(mobile.isBlank() || mobile.length == 10) { "Mobile number 10 digits ka hona chahiye" }
            return OwnerBusinessProfile(
                businessName = businessName,
                ownerName = ownerName,
                mobile = mobile,
                address = address,
            )
        }

        internal fun normalizeIndianMobile(value: String): String {
            val digits = value.filter(Char::isDigit)
            return when {
                digits.length == 12 && digits.startsWith("91") -> digits.takeLast(10)
                digits.length == 11 && digits.startsWith("0") -> digits.takeLast(10)
                else -> digits.take(10)
            }
        }

        private fun singleLine(value: String): String = value.trim().replace(Regex("\\s+"), " ")
    }
}

data class OwnerBusinessProfile(
    val businessName: String = "",
    val ownerName: String = "",
    val mobile: String = "",
    val address: String = "",
)
