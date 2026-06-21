package ru.cubeshield.cubecore.api.dto


import kotlinx.serialization.Serializable

@Serializable
data class ProfileBaseDto(
    val id: String,
    val playername: String,
    )