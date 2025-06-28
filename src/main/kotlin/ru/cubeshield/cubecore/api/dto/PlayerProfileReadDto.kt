package ru.cubeshield.cubecore.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlayerProfileReadDto(
    val id: String,
    val playername: String,
    val balance: Int,
    @SerialName("government_balance") val governmentBalance: Int?
    )