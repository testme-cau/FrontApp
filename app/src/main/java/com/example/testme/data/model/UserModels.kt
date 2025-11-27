package com.example.testme.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    @SerialName("email") val email: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("language_preference") val language: String? = null
)

@Serializable
data class UserProfileResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("user") val user: UserProfile
)

@Serializable
data class UserProfileUpdateRequest(
    @SerialName("display_name") val displayName: String?,
    @SerialName("language_preference") val language: String?
)

@Serializable
data class UserProfileUpdateResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("message") val message: String
)

@Serializable
data class LanguageOption(
    @SerialName("code") val code: String,
    @SerialName("name") val name: String,
    @SerialName("native_name") val nativeName: String,
    @SerialName("flag") val flag: String
)

@Serializable
data class LanguageListResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("languages") val languages: List<LanguageOption>,
    @SerialName("count") val count: Int
)