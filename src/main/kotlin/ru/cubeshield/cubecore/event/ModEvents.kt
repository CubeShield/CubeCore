package ru.cubeshield.cubecore.event

import kotlinx.datetime.Instant
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import ru.cubeshield.cubecore.api.dto.PlayerReadDto

data class ServerStartingEvent(val server: MinecraftServer) : Event
data class ServerStartedEvent(val server: MinecraftServer) : Event
data class ServerStoppingEvent(val server: MinecraftServer) : Event
data class ServerStoppedEvent(val server: MinecraftServer) : Event

data class PlayerJoinedEvent(val player: ServerPlayerEntity) : Event
data class PlayerLeftEvent(val player: ServerPlayerEntity) : Event

data class PlayerAuthorized(val player: ServerPlayerEntity, val playerId: String, val apiPlayerData: PlayerReadDto, val loginTime: Instant): Event
data class PlayerUnauthorized(val player: ServerPlayerEntity, val playerId: String): Event

data class PlayerActivityEvent(val player: ServerPlayerEntity): Event

data class PlayerWentAfkEvent(val player: ServerPlayerEntity, val fromMillis: Long): Event
data class PlayerReturnedFromAfkEvent(val player: ServerPlayerEntity, val fromMillis: Long, val untilMillis: Long): Event