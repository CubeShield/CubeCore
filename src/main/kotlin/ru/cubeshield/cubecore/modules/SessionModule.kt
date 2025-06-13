package ru.cubeshield.cubecore.modules

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.api.dto.SessionCreateDto
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.domain.SessionEntity
import ru.cubeshield.cubecore.event.EventBus
import ru.cubeshield.cubecore.event.PlayerAuthorized
import ru.cubeshield.cubecore.event.PlayerUnauthorized
import java.util.concurrent.ConcurrentHashMap

class SessionModule : ICubeModule {
    override val id = "session_module"
    override val name = "Session Module"
    override val description = "Модуль, отвечающий за создание и отслеживание активных сессий игроков"

    private var activeSessions = ConcurrentHashMap<String, SessionEntity>()

    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        eventBus.subscribe<PlayerAuthorized> { (player, playerId, _, loginTime) ->
            val playername = player.gameProfile.name
            logger.info("Creating new session for player $playername")
            activeSessions[playername] = SessionEntity(
                playerId = playerId,
                loginTime = loginTime,
                ipAddress = player.ip,
                afkSeconds = 0
            )
        }

        eventBus.subscribe<PlayerUnauthorized> { (player, playerId) ->
            val playername = player.gameProfile.name
            val playerSession = activeSessions[playername] ?: return@subscribe
            logger.info("Closing session for player $playername")
            val sessionCreateDto = SessionCreateDto.fromEntity(playerSession)
            modScope.launch {
                apiClient.createSession(playerId, sessionCreateDto)
            }
            activeSessions.remove(playername)
        }
    }

    override fun shutdown() {}
}