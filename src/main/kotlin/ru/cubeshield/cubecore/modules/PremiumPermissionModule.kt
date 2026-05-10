package ru.cubeshield.cubecore.modules

import kotlinx.coroutines.*
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.event.*


class PremiumPermissionModule : ICubeModule {
    override val id = "premium_permission_module"
    override val name = "Premium Permission Module"
    override val description = "Модуль, отвечающий за выдачу прав игрока"


    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        eventBus.subscribe<PlayerAuthorized> { (player, _, apiPlayer, _) ->
            val playername = player.gameProfile.name

            val commandAddString = "lp user ${playername} parent add premium"
            val commandRemoveString = "lp user ${playername} parent remove premium"

            val server = player.level().server ?: return@subscribe
            server.execute {
                try {
                    val source = server.createCommandSourceStack()
                    val command = if (apiPlayer.isPremium) {commandAddString} else {commandRemoveString}
                    server.commands.dispatcher.execute(command, source)
                    logger.info("Premium permissions for player $playername (${apiPlayer.id}) has handled")
                } catch (_: Exception) {
                }
            }
        }
    }


    override fun shutdown() {}
}