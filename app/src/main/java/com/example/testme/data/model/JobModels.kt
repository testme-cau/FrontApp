package com.example.testme.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JobData(
    @SerialName("job_id") val jobId: String,
    @SerialName("subject_id") val subjectId: String? = null,
    @SerialName("exam_id") val examId: String? = null,
    @SerialName("submission_id") val submissionId: String? = null,

    @SerialName("status") val status: String,
    @SerialName("total_questions") val totalQuestions: Int? = null,
    @SerialName("ai_provider") val aiProvider: String? = null,
    @SerialName("progress_percentage") val progressPercentage: Int? = null,
    @SerialName("estimated_duration_seconds") val estimatedDurationSeconds: Int? = null,

    @SerialName("error_message") val errorMessage: String? = null,

    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("failed_at") val failedAt: String? = null,
    @SerialName("cancelled_at") val cancelledAt: String? = null
)

@Serializable
data class JobResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("submission_id") val submissionId: String? = null,
    @SerialName("job") val job: JobData
)

@Serializable
data class JobListResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("jobs") val jobs: List<JobData>,
    @SerialName("count") val count: Int
)

