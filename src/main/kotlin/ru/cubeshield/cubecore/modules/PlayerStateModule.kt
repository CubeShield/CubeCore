package ru.cubeshield.cubecore.modules

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.event.EventBus
import ru.cubeshield.cubecore.event.PlayerAuthorized
import ru.cubeshield.cubecore.event.PlayerUnauthorized
import java.util.concurrent.ConcurrentHashMap

class PlayerStateModule : ICubeModule {
    override val id = "player_state_module"
    override val name = "Player State Module"
    override val description = "Модуль, отвечающий за состояние игрока (Offline/Online/Afk)"

    private val lastActivity = ConcurrentHashMap<String, Long>()
    private val afkPlayers = ConcurrentHashMap.newKeySet<String>()
    private val afkTimeoutMillis = 5 * 60 * 1000L

    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        //eventBus.subscribe<PlayerAuthorized> { (player, _, _, _) ->
        //    lastActivity[player.gameProfile.name] = System.currentTimeMillis()
        //}

        //eventBus.subscribe<PlayerUnauthorized> { (player, _) ->
        //    val playername = player.gameProfile.name
        //    lastActivity.remove(playername)
        //    afkPlayers.remove(playername)
        //}

        eventBus.subscribe<PlayerAuthorized> { (_, playerId, _, _) ->
            modScope.launch {
                apiClient.setPlayerStateOnline(playerId)
            }
        }

        eventBus.subscribe<PlayerUnauthorized> { (_, playerId) ->
            modScope.launch {
                apiClient.setPlayerStateOffline(playerId)
            }
        }
    }

    override fun shutdown() {}
}