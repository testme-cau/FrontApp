package com.example.testme.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// -------------------------------------------------------------
// Exam 기본 정보
// -------------------------------------------------------------
@Serializable
data class ExamData(
    @SerialName("exam_id") val examId: String,
    @SerialName("subject_id") val subjectId: String,
    @SerialName("title") val title: String,               // ← 반드시 필요
    @SerialName("num_questions") val numQuestions: Int,
    @SerialName("difficulty") val difficulty: String,
    @SerialName("ai_provider") val aiProvider: String,
    @SerialName("ai_model") val aiModel: String,
    @SerialName("language") val language: String,
    @SerialName("status") val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String? = null
)

// -------------------------------------------------------------
// Exam 목록
// -------------------------------------------------------------
@Serializable
data class ExamListResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("exams") val exams: List<ExamData>,
    @SerialName("count") val count: Int
)

// -------------------------------------------------------------
// Exam 단건 조회
// -------------------------------------------------------------
@Serializable
data class ExamDetailResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("exam") val exam: ExamData,
    @SerialName("questions") val questions: List<ExamQuestion>
)

@Serializable
data class ExamQuestion(
    @SerialName("index") val index: Int,
    @SerialName("question") val question: String,
    @SerialName("options") val options: List<String>,
    @SerialName("correct_answer") val correctAnswer: Int? = null,
    @SerialName("question_id") val questionId: Int? = null
)


// -------------------------------------------------------------
// Exam 생성
// -------------------------------------------------------------
@Serializable
data class ExamResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("exam") val exam: ExamData
)


@Serializable
data class ExamAnswerPayload(
    @SerialName("question_id") val questionId: Int,
    @SerialName("answer") val answer: String
)
@Serializable
data class ExamSubmitRequest(
    @SerialName("answers") val answers: List<ExamAnswerPayload>
)

@Serializable
data class ExamSubmitResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("submission_id") val submissionId: String,
    @SerialName("job") val job: JobData
)



// -------------------------------------------------------------
// Exam Result (grading)
// -------------------------------------------------------------
@Serializable
data class ExamResultQuestion(
    @SerialName("index") val index: Int,
    @SerialName("question") val question: String,
    @SerialName("user_answer") val userAnswer: Int?,
    @SerialName("correct_answer") val correctAnswer: Int?
)

@Serializable
data class ExamResultResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("exam_id") val examId: String,
    @SerialName("subject_id") val subjectId: String,
    @SerialName("score") val score: Int,
    @SerialName("questions") val questions: List<ExamResultQuestion>,
    @SerialName("percentage") val percentage: Int? = null,
    @SerialName("created_at") val createdAt: String? = null
)
