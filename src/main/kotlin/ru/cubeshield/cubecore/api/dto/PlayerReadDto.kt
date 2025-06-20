package ru.cubeshield.cubecore.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlayerReadDto(
    val id: String,
    val playername: String,
    @SerialName("telegram_id") val telegramId: Long,
    @SerialName("telegram_linked") val telegramLinked: Boolean,
    @SerialName("account_name") val accountName: String,
    val skin: String,
    @SerialName("is_slim") val isSlim: Boolean,
    @SerialName("is_premium") val isPremium: Boolean,
    @SerialName("auth_code") val authCode: String,
    val role: Int,
    val state: String,
    @SerialName("is_admin") val isAdmin: Boolean,
    @SerialName("last_login_ip") val lastLoginIp: String,
    @SerialName("trust_new_login_ip") val trustNewLoginIp: Boolean,

)
