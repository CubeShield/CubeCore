package ru.cubeshield.cubecore.modules

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.api.dto.SessionCreateDto
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.domain.SessionEntity
import ru.cubeshield.cubecore.event.*
import java.util.concurrent.ConcurrentHashMap

class SessionModule : ICubeModule {
    override val id = "session_module"
    override val name = "Session Module"
    override val description = "Модуль, отвечающий за создание и отслеживание активных сессий игроков"

    private var activeSessions = ConcurrentHashMap<String, SessionEntity>()
    private var afkFromMillis = ConcurrentHashMap<String, Long>()

    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        eventBus.subscribe<PlayerAuthorized> { (player, playerId, _, loginTime) ->
            val playername = player.gameProfile.name
            logger.info("Creating new session for player $playername (${player.ipAddress})")
            activeSessions[playername] = SessionEntity(
                playerId = playerId,
                loginTime = loginTime,
                ipAddress = player.ipAddress,
                afkSeconds = 0
            )
        }

        eventBus.subscribe<PlayerWentAfkEvent> { (player, fromMillis) ->
            afkFromMillis[player.gameProfile.name] = fromMillis
        }

        eventBus.subscribe<PlayerReturnedFromAfkEvent> { (player, fromMillis, untilMillis) ->
            val playername = player.gameProfile.name
            afkFromMillis.remove(playername)
            val session = activeSessions[playername] ?: return@subscribe
            session.afkSeconds += ((untilMillis-fromMillis)/1000).toInt()
        }

        eventBus.subscribe<PlayerUnauthorized> { (player, playerId) ->
            val playername = player.gameProfile.name
            val now = System.currentTimeMillis()
            val playerSession = activeSessions[playername] ?: return@subscribe
            val fromMillis = afkFromMillis[playername]
            if (fromMillis != null) {
                playerSession.afkSeconds += ((now-fromMillis)/1000).toInt()
                afkFromMillis.remove(playername)
            }
            logger.info("Closing session for player $playername (${playerSession.ipAddress})")
            val sessionCreateDto = SessionCreateDto.fromEntity(playerSession)
            modScope.launch {
                apiClient.createSession(playerId, sessionCreateDto)
            }
            activeSessions.remove(playername)
        }
    }

    override fun shutdown() {}
}