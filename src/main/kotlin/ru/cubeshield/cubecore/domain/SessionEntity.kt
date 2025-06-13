package ru.cubeshield.cubecore.domain

import kotlinx.datetime.Instant

data class SessionEntity(
    val playerId: String,
    val ipAddress: String,
    val loginTime: Instant,
    var afkSeconds: Int = 0
)
