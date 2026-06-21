package ru.cubeshield.cubecore.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class DiscoveriesReadDto(
    val discoveries: List<DiscoveryReadDto>
)
