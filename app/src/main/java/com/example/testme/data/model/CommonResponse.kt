package com.example.testme.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BaseResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("message") val message: String? = null
)

@Serializable
data class CountResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("count") val count: Int,
    @SerialName("message") val message: String? = null
)
