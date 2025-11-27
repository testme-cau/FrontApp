package com.example.testme.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubjectData(
    @SerialName("subject_id") val subjectId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("group_id") val groupId: String?= null,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("language_preference") val language: String? = null,
    @SerialName("pdf_count") val pdfCount: Int,
    @SerialName("exam_count") val examCount: Int,
    @SerialName("color") val color: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String? = null  
)

@Serializable
data class SubjectListResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("subjects") val subjects: List<SubjectData>,
    @SerialName("count") val count: Int
)

@Serializable
data class SubjectResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("subject") val subject: SubjectData
)

@Serializable
data class SubjectDeleteResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("message") val message: String
)

@Serializable
data class SubjectCreateRequest(
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("color") val color: String? = null,
    @SerialName("language_preference") val language: String = "ko"
)

@Serializable
data class SubjectUpdateRequest(
    @SerialName("name") val name: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("color") val color: String? = null,
    @SerialName("language_preference") val language: String? = null
)
