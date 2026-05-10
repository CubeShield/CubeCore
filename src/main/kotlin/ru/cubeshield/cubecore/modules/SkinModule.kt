package ru.cubeshield.cubecore.modules

import kotlinx.coroutines.*
import ru.cubeshield.cubecore.CubeCore
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.event.*
import kotlin.random.Random


class SkinModule : ICubeModule {
    override val id = "skin_module"
    override val name = "Skin Module"
    override val description = "Модуль, отвечающий за скины игроков"


    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        eventBus.subscribe<PlayerAuthorized> { (player, _, apiPlayer, _) ->


            val playername = player.gameProfile.name
            val skinType = if (apiPlayer.isSlim) {"slim"} else {"classic"}
            val randomInt = Random.nextInt()
            val commandLoadString = "skin set web $skinType \"${apiPlayer.skin}?q=$randomInt\" $playername"

            val server = player.level().server ?: return@subscribe
            server.execute {
                try {
                    val source = server.createCommandSourceStack()
                    server.commands.dispatcher.execute(commandLoadString, source)

                    logger.info("Skin for $playername (${apiPlayer.id}) has handled")
                } catch (e: Exception) {
                    CubeCore.LOGGER.error("Error while handling skin for $playername (${apiPlayer.id})", e)
                }
            }
        }
    }


    override fun shutdown() {}
}