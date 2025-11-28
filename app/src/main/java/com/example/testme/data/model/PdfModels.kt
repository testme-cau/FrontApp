package com.example.testme.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PdfData(
    @SerialName("file_id") val fileId: String,
    @SerialName("subject_id") val subjectId: String? = null,
    @SerialName("subject_name") val subjectName: String? = null,
    @SerialName("original_filename") val originalFilename: String,
    @SerialName("file_url") val fileUrl: String,
    @SerialName("size") val size: Long,
    @SerialName("uploaded_at") val uploadedAt: String,
    @SerialName("status") val status: String? = null
)

@Serializable
data class PdfListResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("pdfs") val pdfs: List<PdfData>,
    @SerialName("count") val count: Int
)

@Serializable
data class PdfResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("file_id") val file_id: String,
    @SerialName("original_filename") val original_filename: String,
    @SerialName("file_url") val file_url: String,
    @SerialName("uploaded_at") val uploaded_at: String,
    @SerialName("size") val size: Int
)

@Serializable
data class PdfDeleteResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("message") val message: String
)

@Serializable
data class PdfDownloadResponse(
    @SerialName("download_url") val downloadUrl: String
)
