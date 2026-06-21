package ru.cubeshield.cubecore.modules

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.minecraft.network.chat.Component
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.api.ApiResponse
import ru.cubeshield.cubecore.api.dto.PlayerReadDto
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.event.*
import ru.cubeshield.cubecore.player.PlayerCache
import ru.cubeshield.cubecore.utils.MessageUtil
import java.util.concurrent.ConcurrentHashMap

class AuthModule : ICubeModule {
    override val id = "auth_module"
    override val name = "Auth Module"
    override val description = "Модуль отвечающий за авторизацию игроков на сервере, а также обработку игровых сессий"


    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        eventBus.subscribe<PlayerJoinedEvent> { (player) ->
            val loginTime = kotlin.time.Clock.System.now()
            val playername = player.gameProfile.name
            logger.info("Handling authorization for joined player $playername")
            modScope.launch {
                var apiPlayer: PlayerReadDto? = null
                when (val result = apiClient.getPlayer(playername)) {
                    is ApiResponse.Success -> {
                        apiPlayer = result.data
                        logger.info("Found player $playername (${apiPlayer.id})")
                    }
                    is ApiResponse.Error -> {
                        logger.info("Player $playername not found, creating new one...")
                        when (val newResult = apiClient.createPlayer(playername)) {
                            is ApiResponse.Success -> {
                                apiPlayer = newResult.data
                                logger.info("Created new joined player $playername in API")
                            }
                            is ApiResponse.Error -> {}
                        }
                    }
                }
                if (apiPlayer == null) {
                    player.level().server.execute {
                        player.connection.disconnect(Component.literal("Не удалось вас авторизировать"))
                    }
                    logger.warn("Not succeeded in authorization player $playername")
                    return@launch
                }
                if (!apiPlayer.telegramLinked) {
                    player.level().server.execute {
                        player.connection.disconnect(
                            Component.literal("Продолжите авторизацию в Телеграм Боте @cubeshieldbot\nВаш код аунтефикации: ${apiPlayer.authCode}")
                                .withStyle { it.withBold(true) }
                        )
                    }
                    logger.info("Player $playername (${apiPlayer.id}) has not linked Telegram")
                    return@launch
                }
                if (!apiPlayer.trustNewLoginIp && apiPlayer.lastLoginIp != player.ipAddress) {
                    player.level().server.execute {
                        player.connection.disconnect(
                            Component.literal("Подтвердите вход с нового IP-Адреса в Телеграм Боте @cubeshieldbot")
                                .withStyle { it.withBold(true) }
                        )
                    }
                    apiClient.warnPlayerNewIp(apiPlayer.id, player.ipAddress)
                    return@launch
                }
                if (apiPlayer.trustNewLoginIp) {
                    apiClient.successPlayerNewIp(apiPlayer.id)
                    MessageUtil.send(player, "Вы успешно вошли с нового IP-Адреса", false, false)
                }
                PlayerCache.put(playername, apiPlayer.id)
                eventBus.publish(PlayerAuthorized(player, apiPlayer.id, apiPlayer, loginTime))
                logger.info("Player $playername (${apiPlayer.id}) has been authorized")
            }
        }

        eventBus.subscribe<PlayerLeftEvent> { (player) ->
            val playername = player.gameProfile.name
            val playerId = PlayerCache.getId(playername) ?: return@subscribe
            eventBus.publish(PlayerUnauthorized(player, playerId))
            logger.info("Player $playername ($playerId) has been unauthorized")
        }
    }

    override fun shutdown() {}
}