package ru.cubeshield.cubecore.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ItemCreateDto(
    @SerialName("translation_key") val translationKey: String,
    @SerialName("mod_name") val modName: String,
    val namespace: String,
    val item: String,
    val rare: String,
)
