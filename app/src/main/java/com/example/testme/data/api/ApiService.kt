package com.example.testme.data.api

import com.example.testme.data.model.ExamDetailResponse
import com.example.testme.data.model.ExamListResponse
import com.example.testme.data.model.ExamResultResponse
import com.example.testme.data.model.ExamSubmitRequest
import com.example.testme.data.model.ExamSubmitResponse
import com.example.testme.data.model.JobListResponse
import com.example.testme.data.model.JobResponse
import com.example.testme.data.model.LanguageListResponse
import com.example.testme.data.model.PdfDeleteResponse
import com.example.testme.data.model.PdfDownloadResponse
import com.example.testme.data.model.PdfListResponse
import com.example.testme.data.model.PdfResponse
import com.example.testme.data.model.SubjectCreateRequest
import com.example.testme.data.model.SubjectDeleteResponse
import com.example.testme.data.model.SubjectListResponse
import com.example.testme.data.model.SubjectResponse
import com.example.testme.data.model.SubjectUpdateRequest
import com.example.testme.data.model.UserProfileResponse
import com.example.testme.data.model.UserProfileUpdateRequest
import com.example.testme.data.model.UserProfileUpdateResponse
import com.example.testme.data.model.group.GroupCreateRequest
import com.example.testme.data.model.group.GroupDeleteResponse
import com.example.testme.data.model.group.GroupListResponse
import com.example.testme.data.model.group.GroupResponse
import com.example.testme.data.model.group.GroupUpdateRequest
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.*

interface ApiService {

    companion object {

        //private const val BASE_URL = "http://10.0.2.2:5000/"
        private const val BASE_URL="https://testmeapi.jdn.kr/"

        fun create(): ApiService {

            val json = Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }

            val contentType = "application/json".toMediaType()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()

            return retrofit.create(ApiService::class.java)
        }
    }

    // -------------------------------------------------------------
    // GROUPS
    // -------------------------------------------------------------
    @GET("/api/groups")
    suspend fun getGroups(
        @Header("Authorization") token: String
    ): GroupListResponse

    @POST("/api/groups")
    suspend fun createGroup(
        @Header("Authorization") token: String,
        @Body request: GroupCreateRequest
    ): GroupResponse

    @GET("/api/groups/{group_id}")
    suspend fun getGroupDetail(
        @Header("Authorization") token: String,
        @Path("group_id") groupId: String
    ): GroupResponse

    @PUT("/api/groups/{group_id}")
    suspend fun updateGroup(
        @Header("Authorization") token: String,
        @Path("group_id") groupId: String,
        @Body request: GroupUpdateRequest
    ): GroupResponse

    @DELETE("/api/groups/{group_id}")
    suspend fun deleteGroup(
        @Header("Authorization") token: String,
        @Path("group_id") groupId: String
    ): GroupDeleteResponse


    // -------------------------------------------------------------
    // SUBJECTS
    // -------------------------------------------------------------
    @GET("/api/subjects")
    suspend fun getAllSubjects(
        @Header("Authorization") token: String
    ): SubjectListResponse

    @POST("/api/subjects")
    suspend fun createSubject(
        @Header("Authorization") token: String,
        @Body request: SubjectCreateRequest
    ): SubjectResponse

    @GET("/api/subjects/{subject_id}")
    suspend fun getSubjectDetail(
        @Header("Authorization") token: String,
        @Path("subject_id") subjectId: String
    ): SubjectResponse

    @PUT("/api/subjects/{subject_id}")
    suspend fun updateSubject(
        @Header("Authorization") token: String,
        @Path("subject_id") subjectId: String,
        @Body request: SubjectUpdateRequest
    ): SubjectResponse

    @DELETE("/api/subjects/{subject_id}")
    suspend fun deleteSubject(
        @Header("Authorization") token: String,
        @Path("subject_id") subjectId: String
    ): SubjectDeleteResponse


    // -------------------------------------------------------------
    // PDF
    // -------------------------------------------------------------
    @GET("/api/subjects/{subject_id}/pdfs")
    suspend fun getPdfsBySubject(
        @Header("Authorization") token: String,
        @Path("subject_id") subjectId: String
    ): PdfListResponse

    @Multipart
    @POST("/api/subjects/{subject_id}/pdfs/upload")
    suspend fun uploadPdf(
        @Header("Authorization") token: String,
        @Path("subject_id") subjectId: String,
        @Part file: MultipartBody.Part
    ): PdfResponse

    @GET("/api/subjects/{subject_id}/pdfs/{file_id}")
    suspend fun getPdfDetail(
        @Header("Authorization") token: String,
        @Path("subject_id") subjectId: String,
        @Path("file_id") fileId: String
    ): PdfResponse

    @DELETE("/api/subjects/{subject_id}/pdfs/{file_id}")
    suspend fun deletePdf(
        @Header("Authorization") token: String,
        @Path("subject_id") subjectId: String,
        @Path("file_id") fileId: String
    ): PdfDeleteResponse

    @GET("/api/subjects/{subject_id}/pdfs/{file_id}/download")
    suspend fun getPdfDownloadUrl(
        @Header("Authorization") token: String,
        @Path("subject_id") subjectId: String,
        @Path("file_id") fileId: String
    ): PdfDownloadResponse

    @GET("/api/pdf/list")
    suspend fun getAllPdfs(
        @Header("Authorization") token: String
    ): PdfListResponse

    @DELETE("/api/pdf/{file_id}")
    suspend fun deletePdfByFileId(
        @Header("Authorization") token: String,
        @Path("file_id") fileId: String
    ): PdfDeleteResponse


    // -------------------------------------------------------------
    // EXAMS
    // -------------------------------------------------------------
    @POST("/api/subjects/{subject_id}/exams/generate")
    suspend fun generateExam(
        @Header("Authorization") token: String,
        @Path("subject_id") subjectId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): JobResponse

    @DELETE("/api/subjects/{subject_id}/exams/{exam_id}")
    suspend fun deleteExam(
        @Header("Authorization") token: String,
        @Path("subject_id") subjectId: String,
        @Path("exam_id") examId: String
    ): JobResponse

    @GET("/api/subjects/{subject_id}/exams")
    suspend fun getExamsBySubject(
        @Header("Authorization") token: String,
        @Path("subject_id") subjectId: String
    ): ExamListResponse

    @GET("/api/subjects/{subject_id}/exams/{exam_id}")
    suspend fun getExamDetail(
        @Header("Authorization") token: String,
        @Path("subject_id") subjectId: String,
        @Path("exam_id") examId: String
    ): ExamDetailResponse

    @POST("/api/subjects/{subject_id}/exams/{exam_id}/submit")
    suspend fun submitExam(
        @Header("Authorization") token: String,
        @Path("subject_id") subjectId: String,
        @Path("exam_id") examId: String,
        @Body body: ExamSubmitRequest
    ): ExamSubmitResponse


    @GET("/api/subjects/{subject_id}/exams/{exam_id}/submission")
    suspend fun getExamResult(
        @Header("Authorization") token: String,
        @Path("subject_id") subjectId: String,
        @Path("exam_id") examId: String
    ): ExamResultResponse

    @GET("/api/subjects/{subject_id}/grading-jobs")
    suspend fun getGradingJobs(
        @Header("Authorization") token: String,
        @Path("subject_id") subjectId: String
    ): JobListResponse

    @GET("/api/subjects/{subject_id}/grading-jobs/{job_id}")
    suspend fun getGradingJobStatus(
        @Header("Authorization") token: String,
        @Path("subject_id") subjectId: String,
        @Path("job_id") jobId: String
    ): JobResponse

    @GET("/api/subjects/{subject_id}/exam-jobs")
    suspend fun getExamJobs(
        @Header("Authorization") token: String,
        @Path("subject_id") subjectId: String
    ): JobListResponse

    @GET("/api/subjects/{subject_id}/exam-jobs/{job_id}")
    suspend fun getExamJob(
        @Header("Authorization") token: String,
        @Path("subject_id") subjectId: String,
        @Path("job_id") jobId: String
    ): JobResponse

    @DELETE("/api/subjects/{subject_id}/exam-jobs/{job_id}")
    suspend fun cancelExamJob(
        @Header("Authorization") token: String,
        @Path("subject_id") subjectId: String,
        @Path("job_id") jobId: String
    ): JobResponse

    @DELETE("/api/subjects/{subject_id}/grading-jobs/{job_id}")
    suspend fun cancelGradingJob(
        @Header("Authorization") token: String,
        @Path("subject_id") subjectId: String,
        @Path("job_id") jobId: String
    ): String


    // -------------------------------------------------------------
    // USER PROFILE
    // -------------------------------------------------------------
    @GET("/api/user/profile")
    suspend fun getUserProfile(
        @Header("Authorization") token: String
    ): UserProfileResponse

    @PUT("/api/user/profile")
    suspend fun updateUserProfile(
        @Header("Authorization") token: String,
        @Body request: UserProfileUpdateRequest
    ): UserProfileUpdateResponse

    @GET("/api/user/languages")
    suspend fun getSupportedLanguages(): LanguageListResponse

}
