package ru.cubeshield.cubecore.modules

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.server.MinecraftServer
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.event.*
import ru.cubeshield.cubecore.utils.MessageUtil

object ServerHolder {
    var server: MinecraftServer? = null
}

@Serializable
data class WSIntent(
    val text: String,
    @SerialName("intent_id") val intentId: String,
)

@Serializable
data class WSNotification(
    @SerialName("to_player") val toPlayer: String,
    val message: String,
    val intents: List<WSIntent> = emptyList(),
)


class NotificationModule : ICubeModule {
    override val id = "notification_module"
    override val name = "Notification Module"
    override val description = "Модуль, отвечающий за отправление уведомлений с API"

    private lateinit var client: HttpClient
    private lateinit var modScope: CoroutineScope

    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        this.modScope = modScope

        ServerLifecycleEvents.SERVER_STARTED.register { server -> ServerHolder.server = server }
        ServerLifecycleEvents.SERVER_STOPPING.register { server -> ServerHolder.server = null }

        client = HttpClient(CIO) {
            install(WebSockets)
        }

        modScope.launch(Dispatchers.IO) {
            connectAndListen(config.api.wsUrl)
        }
    }

    private suspend fun connectAndListen(webSocketUrl: String) {
        while (modScope.isActive) {
            try {
                logger.info("Trying to establish WebSocket connection")

                client.webSocket(webSocketUrl) {
                    logger.info("WebSocket connection established")

                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val receivedText = frame.readText()
                            logger.info("Received message")

                            handleMessage(receivedText)
                        }
                    }
                }

                logger.info("WebSocket session is over")

            } catch (e: Exception) {
                logger.info("WebSocket Error: ${e.javaClass.simpleName} - ${e.message}")
            }

            if (modScope.isActive) {
                logger.info("Reconnecting to WebSocket in 5 seconds")
                delay(5000)
            }
        }
        logger.info("Loop of listening of WebSocket is over")
    }

    private fun handleMessage(text: String) {
        val server = ServerHolder.server ?: return

        try {
            val notification = Json.decodeFromString<WSNotification>(text)
            server.execute {
                val player = server.playerList.getPlayerByName(notification.toPlayer)
                if (player != null) {
                    MessageUtil.send(player, notification.message, false, true, notification.intents)
                }
                else {
                    logger.info("Player '${notification.toPlayer}' not found, notification not delivered")
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to parse WebSocket message: $text", e)
        }

    }

    override fun shutdown() {
        logger.info("Disconnecting from WebSocket")
        modScope.cancel()

        client.close()
        logger.info("Ktor client closed")
    }
}