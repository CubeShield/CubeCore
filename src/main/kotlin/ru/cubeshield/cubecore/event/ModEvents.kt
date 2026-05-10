package ru.cubeshield.cubecore.event

import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import ru.cubeshield.cubecore.api.dto.PlayerReadDto

data class ServerStartingEvent(val server: MinecraftServer) : Event
data class ServerStartedEvent(val server: MinecraftServer) : Event
data class ServerStoppingEvent(val server: MinecraftServer) : Event
data class ServerStoppedEvent(val server: MinecraftServer) : Event

data class PlayerJoinedEvent(val player: ServerPlayer) : Event
data class PlayerLeftEvent(val player: ServerPlayer) : Event

data class PlayerAuthorized(val player: ServerPlayer, val playerId: String, val apiPlayerData: PlayerReadDto, val loginTime: kotlin.time.Instant): Event
data class PlayerUnauthorized(val player: ServerPlayer, val playerId: String): Event

data class PlayerActivityEvent(val player: ServerPlayer): Event

data class PlayerWentAfkEvent(val player: ServerPlayer, val fromMillis: Long): Event
data class PlayerReturnedFromAfkEvent(val player: ServerPlayer, val fromMillis: Long, val untilMillis: Long): Event