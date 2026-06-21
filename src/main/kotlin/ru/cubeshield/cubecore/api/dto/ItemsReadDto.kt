package ru.cubeshield.cubecore.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class ItemsReadDto(
    val items: List<ItemReadDto>
)
