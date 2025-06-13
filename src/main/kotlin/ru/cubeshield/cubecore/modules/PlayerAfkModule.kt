package ru.cubeshield.cubecore.modules

import kotlinx.coroutines.*
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.event.*
import java.util.concurrent.ConcurrentHashMap

class PlayerAfkModule : ICubeModule {
    override val id = "player_afk_module"
    override val name = "Player AFK Module"
    override val description = "Модуль, отвечающий за отслеживание AFK состояния у игроков"

    private val lastActivity = ConcurrentHashMap<String, Long>()
    private val lastRotations = ConcurrentHashMap<String, Pair<Float, Float>>()
    private val afkPlayers = ConcurrentHashMap.newKeySet<String>()
    private var afkTimeoutMillis = 3 * 60 * 1000L

    private lateinit var job: Job

    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        afkTimeoutMillis = config.modules.playerAfkModule.afkTimeoutSeconds * 1000L
        eventBus.subscribe<PlayerAuthorized> { (player, _, _, _) ->
            lastActivity[player.gameProfile.name] = System.currentTimeMillis()
        }

        eventBus.subscribe<PlayerUnauthorized> { (player, _) ->
            val playername = player.gameProfile.name
            lastActivity.remove(playername)
            lastRotations.remove(playername)
            afkPlayers.remove(playername)
        }

        eventBus.subscribe<ServerStartedEvent> { (server) ->
            job = modScope.launch {
                while (isActive) {
                    val now = System.currentTimeMillis()
                    for (player in server.playerManager.playerList) {
                        val playername = player.gameProfile.name
                        val currentRot = player.headYaw to player.pitch
                        val lastRot = lastRotations.put(playername, currentRot)
                        if (lastRot != null && lastRot != currentRot) {
                            val wasAfk = afkPlayers.remove(playername)
                            if (wasAfk) {
                                val fromMillis = lastActivity[playername] ?: now
                                logger.info("Player $playername returned from AFK")
                                eventBus.publish(PlayerReturnedFromAfkEvent(player, fromMillis, now))
                            }
                            lastActivity[playername] = now
                            continue
                        }

                        val lastActive = lastActivity[playername] ?: now
                        if (!afkPlayers.contains(playername) && (now - lastActive > afkTimeoutMillis)) {
                            afkPlayers.add(playername)
                            logger.info("Player $playername went AFK")
                            eventBus.publish(PlayerWentAfkEvent(player, lastActive))
                        }
                    }
                    delay(2000L)
                }
            }

        }

    }

    override fun shutdown() {
        job.cancel()
        lastActivity.clear()
        lastRotations.clear()
        afkPlayers.clear()
    }
}