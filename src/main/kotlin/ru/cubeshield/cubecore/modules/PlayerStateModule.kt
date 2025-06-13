package ru.cubeshield.cubecore.modules

import kotlinx.coroutines.*
import net.minecraft.server.MinecraftServer
import net.minecraft.server.PlayerManager
import net.minecraft.server.network.ServerPlayerEntity
import ru.cubeshield.cubecore.CubeCore
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.event.*
import java.util.concurrent.ConcurrentHashMap

class PlayerStateModule : ICubeModule {
    override val id = "player_state_module"
    override val name = "Player State Module"
    override val description = "Модуль, отвечающий за отслеживания состояния игрока (Offline/Online/Afk)"

    private var cachedPlayersId = ConcurrentHashMap<String, String>()

    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        eventBus.subscribe<PlayerAuthorized> { (player, playerId, _, _) ->
            cachedPlayersId[player.gameProfile.name] = playerId
            modScope.launch {
                apiClient.setPlayerStateOnline(playerId)
            }
        }

        eventBus.subscribe<PlayerUnauthorized> { (player, playerId) ->
            cachedPlayersId[player.gameProfile.name] = playerId
            modScope.launch {
                apiClient.setPlayerStateOffline(playerId)
            }
        }

        eventBus.subscribe<PlayerWentAfkEvent> { (player) ->
            val playerId = cachedPlayersId[player.gameProfile.name] ?: return@subscribe
            modScope.launch {
                apiClient.setPlayerStateAfk(playerId)
            }
        }

        eventBus.subscribe<PlayerReturnedFromAfkEvent> { (player) ->
            val playerId = cachedPlayersId[player.gameProfile.name] ?: return@subscribe
            modScope.launch {
                apiClient.setPlayerStateOnline(playerId)
            }
        }

    }

    override fun shutdown() {}
}