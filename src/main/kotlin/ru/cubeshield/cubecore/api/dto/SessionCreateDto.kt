package ru.cubeshield.cubecore.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.cubeshield.cubecore.domain.SessionEntity

@Serializable
data class SessionCreateDto (
    @SerialName("login_time") val loginTime: kotlin.time.Instant,
    @SerialName("logout_time") val logoutTime: kotlin.time.Instant,
    @SerialName("duration_seconds") val durationSeconds: Int,
    @SerialName("total_afk_seconds") val totalAfkSeconds: Int,
    @SerialName("total_active_seconds") val totalActiveSeconds: Int,
    @SerialName("ip_address") val ipAddress: String,
) {
    companion object {
        fun fromEntity (sessionEntity: SessionEntity): SessionCreateDto {
            val logoutTime = kotlin.time.Clock.System.now()
            val durationSeconds = (logoutTime - sessionEntity.loginTime).inWholeSeconds.toInt()
            val totalAfkSeconds = sessionEntity.afkSeconds
            val totalActiveSeconds = durationSeconds - totalAfkSeconds

            return SessionCreateDto(
                loginTime = sessionEntity.loginTime,
                logoutTime = logoutTime,
                durationSeconds = durationSeconds,
                totalAfkSeconds = totalAfkSeconds,
                totalActiveSeconds = totalActiveSeconds,
                ipAddress = sessionEntity.ipAddress
            )
        }
    }

}