package ru.cubeshield.cubecore.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponseMessage(
    val detail: String
)
