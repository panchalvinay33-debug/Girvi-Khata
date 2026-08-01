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
        val normalized = profile.copy(
            businessName = profile.businessName.trim(),
            ownerName = profile.ownerName.trim(),
            mobile = profile.mobile.filter(Char::isDigit).take(10),
            address = profile.address.trim(),
        )
        require(normalized.businessName.isNotBlank()) { "Dukaan / business ka naam required hai" }
        require(normalized.ownerName.isNotBlank()) { "Owner / user ka naam required hai" }
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
    }
}

data class OwnerBusinessProfile(
    val businessName: String = "",
    val ownerName: String = "",
    val mobile: String = "",
    val address: String = "",
)
