package ru.cubeshield.cubecore.modules

import io.ktor.util.logging.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.network.ServerPlayerInteractionManager
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.api.ApiResponse
import ru.cubeshield.cubecore.api.dto.PlayerReadDto
import ru.cubeshield.cubecore.api.dto.SessionCreateDto
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.domain.SessionEntity
import ru.cubeshield.cubecore.event.*
import java.util.concurrent.ConcurrentHashMap

class AuthModule: ICubeModule {
    override val id = "auth_module"
    override val name = "Auth Module"
    override val description = "Модуль отвечающий за авторизацию игроков на сервере, а также обработку игровых сессий"

    private var cachedPlayersId = ConcurrentHashMap<String, String>()

    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        eventBus.subscribe<PlayerJoinedEvent> { (player) ->
            val loginTime = Clock.System.now()
            val playername = player.gameProfile.name
            logger.info("Handling authorization for joined player $playername")
            modScope.launch {
                var apiPlayer: PlayerReadDto? = null
                when (val result = apiClient.getPlayer(playername)) {
                    is ApiResponse.Success -> {
                        apiPlayer = result.data
                    }
                    is ApiResponse.Error -> {
                        when (val newResult = apiClient.createPlayer(playername)) {
                            is ApiResponse.Success -> {
                                apiPlayer = newResult.data
                                logger.info("Created new joined player $playername in API")
                            }
                            is ApiResponse.Error -> {
                            }
                        }
                    }
                }
                if (apiPlayer == null) {
                    player.server.execute {
                        player.networkHandler.disconnect(Text.literal("Не удалось вас авторизировать"))
                    }
                    logger.warn("Not succeeded in authorization player $playername")
                    return@launch
                }
                if (!apiPlayer.telegramLinked) {
                    player.server.execute {
                        player.networkHandler.disconnect(Text.literal("Продолжите авторизацию в Телеграм Боте @cubeshieldbot\nВаш код аунтефикации: ${apiPlayer.authCode}").styled { it.withBold(true) })
                    }
                    logger.info("Player $playername has not linked Telegram")
                    return@launch
                }
                if (!apiPlayer.trustNewLoginIp && apiPlayer.lastLoginIp != player.ip) {
                    player.server.execute {
                        player.networkHandler.disconnect(Text.literal("Подтвердите вход с нового IP-Адреса в Телеграм Боте @cubeshieldbot").styled { it.withBold(true) })
                    }
                    apiClient.warnPlayerNewIp(apiPlayer.id, player.ip)
                    return@launch
                }
                if (apiPlayer.trustNewLoginIp) {
                    apiClient.successPlayerNewIp(apiPlayer.id)
                    player.sendMessage(Text.literal("Вы успешно вошли с нового IP-Адреса").styled { it.withColor(
                        Formatting.GRAY) })
                }
                cachedPlayersId[playername] = apiPlayer.id
                eventBus.publish(PlayerAuthorized(player, apiPlayer.id, apiPlayer, loginTime))
                logger.info("Player $playername (${apiPlayer.id}) has been authorized")
            }
        }

        eventBus.subscribe<PlayerLeftEvent> { (player) ->
            val playername = player.gameProfile.name
            val playerId = cachedPlayersId[playername] ?: return@subscribe
            eventBus.publish(PlayerUnauthorized(player, playerId))
            logger.info("Player $playername ($playerId) has been unauthorized")
        }
    }

    override fun shutdown() {}
}