package ru.cubeshield.cubecore.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlayerCreateDto(
    val playername: String
)
