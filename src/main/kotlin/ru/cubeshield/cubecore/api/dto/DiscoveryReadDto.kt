package ru.cubeshield.cubecore.api.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class DiscoveryReadDto(
    val id: String,
    val profile: PlayerProfileReadDto,
    val item: ItemReadDto,
    @SerialName("is_first") val isFirst: Boolean,
    @SerialName("created_at") val createdAt: kotlin.time.Instant,
)
