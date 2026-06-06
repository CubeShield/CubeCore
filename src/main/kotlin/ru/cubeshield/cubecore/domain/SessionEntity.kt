package ru.cubeshield.cubecore.domain


data class SessionEntity(
    val playerId: String,
    val ipAddress: String,
    val loginTime: kotlin.time.Instant,
    var afkSeconds: Int = 0
)
