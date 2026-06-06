package ru.cubeshield.cubecore.modules

import eu.pb4.placeholders.api.PlaceholderResult
import eu.pb4.placeholders.api.Placeholders
import kotlinx.coroutines.*
import net.minecraft.resources.Identifier
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.config.accentColor
import ru.cubeshield.cubecore.config.afkColor
import ru.cubeshield.cubecore.event.*
import java.util.concurrent.ConcurrentHashMap

class PlayerStateModule : ICubeModule {
    override val id = "player_state_module"
    override val name = "Player State Module"
    override val description = "Модуль, отвечающий за отслеживания состояния игрока (Offline/Online/Afk)"

    private var cachedPlayersId = ConcurrentHashMap<String, String>()
    private var cachedPlayersPremium = ConcurrentHashMap<String, Boolean>()
    private var afkPlayers = ConcurrentHashMap.newKeySet<String>()

    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        eventBus.subscribe<PlayerAuthorized> { (player, playerId, apiPlayer, _) ->
            val playername = player.gameProfile.name
            afkPlayers.remove(playername)
            cachedPlayersId[playername] = playerId
            cachedPlayersPremium[playername] = apiPlayer.isPremium
            modScope.launch {
                apiClient.setPlayerStateOnline(playerId)
            }
        }

        eventBus.subscribe<PlayerUnauthorized> { (player, playerId) ->
            afkPlayers.remove(player.gameProfile.name)
            modScope.launch {
                apiClient.setPlayerStateOffline(playerId)
            }
        }

        eventBus.subscribe<PlayerWentAfkEvent> { (player) ->
            val playerId = cachedPlayersId[player.gameProfile.name] ?: return@subscribe
            afkPlayers.add(player.gameProfile.name)
            modScope.launch {
                apiClient.setPlayerStateAfk(playerId)
            }
        }

        eventBus.subscribe<PlayerReturnedFromAfkEvent> { (player) ->
            val playerId = cachedPlayersId[player.gameProfile.name] ?: return@subscribe
            afkPlayers.remove(player.gameProfile.name)
            modScope.launch {
                apiClient.setPlayerStateOnline(playerId)
            }
        }

        Placeholders.registerCommon<String>(Identifier.fromNamespaceAndPath("cubecore", "prefix-color")) { ctx, args ->
            if (!ctx.hasPlayer()) {
                return@registerCommon PlaceholderResult.invalid("No player!")
            }
            val playername = ctx.player()!!.gameProfile.name
            val prefixColor = if (afkPlayers.contains(playername)) afkColor else accentColor
            PlaceholderResult.value(prefixColor.toString())
        }

        Placeholders.registerCommon<String>(Identifier.fromNamespaceAndPath("cubecore", "prefix")) { ctx, args ->
            if (!ctx.hasPlayer()) {
                return@registerCommon PlaceholderResult.invalid("No player!")
            }
            val playername = ctx.player()!!.gameProfile.name
            val isPremium = cachedPlayersPremium[playername] ?: return@registerCommon PlaceholderResult.invalid("No player!")
            val prefixIcon = if (isPremium) "★ " else "♦ "
            PlaceholderResult.value(prefixIcon)
        }

    }

    override fun shutdown() {}
}