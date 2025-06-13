package ru.cubeshield.cubecore.domain

import java.util.*

data class PlayerEntity (
    val id: UUID,
    val playerName: String,
    val telegramId: Int,
    val isTelegramLinked: Boolean,
    val accountName: String,
    val skin: String,
    val authCode: String,
    val role: Int,
    val state: PlayerState,
    val isAdmin: Boolean,
    val lastLoginIp: String,
    val trustNewLoginIp: Boolean
)

enum class PlayerState {
    OFFLINE, ONLINE, AFK
}