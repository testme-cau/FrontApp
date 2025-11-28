package com.example.testme.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExamData(
    @SerialName("exam_id") val examId: String,
    @SerialName("subject_id") val subjectId: String,
    @SerialName("title") val title: String,
    @SerialName("num_questions") val numQuestions: Int,
    @SerialName("difficulty") val difficulty: String,
    @SerialName("ai_provider") val aiProvider: String? = null,
    @SerialName("ai_model") val aiModel: String? = null,
    @SerialName("language") val language: String? = null,
    @SerialName("status") val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("questions") val questions: List<ExamQuestion> = emptyList()
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
)

@Serializable
data class ExamQuestion(
    @SerialName("id") val id: Int,
    @SerialName("question") val question: String,
    @SerialName("options") val options: List<String>? = null,

    @SerialName("correct_answer") val correctAnswer: String? = null,

    @SerialName("type") val type: String? = null,
    @SerialName("points") val points: Double? = null,
    @SerialName("topic") val topic: String? = null,
    @SerialName("model_answer") val modelAnswer: String? = null,
    @SerialName("keywords") val keywords: List<String>? = null,
    @SerialName("scoring_rubric") val scoringRubric: List<ScoringRubricItem>? = null
)

@Serializable
data class ScoringRubricItem(
    @SerialName("points") val points: Double,
    @SerialName("criterion") val criterion: String
)

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
data class ExamSubmitResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("submission_id") val submissionId: String,
    @SerialName("job") val job: JobData
)

@Serializable
data class ExamResultResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("submission") val submission: ExamSubmissionData
)

@Serializable
data class GenerateExamRequest(
    @SerialName("num_questions") val numQuestions: Int,
    @SerialName("difficulty") val difficulty: String,
    @SerialName("ai_provider") val aiProvider: String,
    @SerialName("ai_model") val aiModel: String,
    @SerialName("language") val language: String,
    @SerialName("pdf_ids") val pdfIds: List<String>
)

@Serializable
data class ExamSubmissionData(
    @SerialName("subject_id") val subjectId: String,
    @SerialName("exam_id") val examId: String? = null,
    @SerialName("grading_result") val gradingResult: GradingResult? = null,
    @SerialName("answers") val answers: List<SubmittedAnswer> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class GradingResult(
    @SerialName("question_results")
    val questionResults: List<QuestionGradingResult> = emptyList(),

    @SerialName("max_score") val maxScore: Double,
    @SerialName("total_score") val totalScore: Double,
    @SerialName("percentage") val percentage: Double,

    @SerialName("overall_feedback") val overallFeedback: String? = null,
    @SerialName("weaknesses") val weaknesses: List<String> = emptyList(),
    @SerialName("strengths") val strengths: List<String> = emptyList(),
    @SerialName("study_recommendations") val studyRecommendations: List<String> = emptyList()
)

@Serializable
data class QuestionGradingResult(
    @SerialName("question_id") val questionId: Int,
    @SerialName("is_correct") val isCorrect: Boolean? = null,
    @SerialName("score") val score: Double? = null,
    @SerialName("max_points") val maxPoints: Double? = null,
    @SerialName("feedback") val feedback: String? = null,
    @SerialName("model_answer") val modelAnswer: String? = null
)

@Serializable
data class SubmittedAnswer(
    @SerialName("question_id") val questionId: Int,
    @SerialName("answer") val answer: String
)