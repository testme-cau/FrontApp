package com.example.testme.data.model.group

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// -------------------------------------------------------------
// Group 기본 데이터
// -------------------------------------------------------------
@Serializable
data class GroupData(
    @SerialName("group_id") val groupId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("color") val color: String? = null,
    @SerialName("icon") val icon: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String? = null
)

// -------------------------------------------------------------
// 응답들
// -------------------------------------------------------------
@Serializable
data class GroupListResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("groups") val groups: List<GroupData>,
    @SerialName("count") val count: Int
)

@Serializable
data class GroupResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("group") val group: GroupData
)

@Serializable
data class GroupDeleteResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("message") val message: String
)

@Serializable
data class GroupCreateRequest(
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("color") val color: String? = null,
    @SerialName("icon") val icon: String? = null
)


@Serializable
data class GroupUpdateRequest(
    @SerialName("name") val name: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("color") val color: String? = null,
    @SerialName("icon") val icon: String? = null
)

