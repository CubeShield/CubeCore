package ru.cubeshield.cubecore.api.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BillCreateDto(
    @SerialName("to_player_id") val toPlayerId: String,
    val amount: Int,
    val note: String,
    val until: Instant
)
