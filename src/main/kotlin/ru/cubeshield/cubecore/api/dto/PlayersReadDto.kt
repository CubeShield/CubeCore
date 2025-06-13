package ru.cubeshield.cubecore.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlayersReadDto(
    val players: List<PlayerReadDto>
)
