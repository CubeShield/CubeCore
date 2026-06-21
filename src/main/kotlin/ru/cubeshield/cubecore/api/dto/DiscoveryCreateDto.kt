package ru.cubeshield.cubecore.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class DiscoveryCreateDto(
    val namespace: String,
    val item: String,
)
