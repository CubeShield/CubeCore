package ru.cubeshield.cubecore.modules

import com.mojang.authlib.properties.Property
import kotlinx.coroutines.*
import net.minecraft.command.CommandExecutionContext
import net.minecraft.server.MinecraftServer
import net.minecraft.server.PlayerManager
import net.minecraft.server.network.ServerPlayerEntity
import ru.cubeshield.cubecore.CubeCore
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.event.*
import kotlin.random.Random


class PremiumPermissionModule : ICubeModule {
    override val id = "premium_permission_module"
    override val name = "Premium Permission Module"
    override val description = "Модуль, отвечающий за выдачу прав игрока"


    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        eventBus.subscribe<PlayerAuthorized> { (player, _, apiPlayer, _) ->
            val playername = player.gameProfile.name

            val commandAddString = "lp user ${playername} parent add premium"
            val commandRemoveString = "lp user ${playername} parent remove premium"

            val server = player.server ?: return@subscribe
            server.execute {
                try {
                    val source = server.commandSource
                    val command = if (apiPlayer.isPremium) {commandAddString} else {commandRemoveString}
                    server.commandManager.dispatcher.execute(command, source)
                    logger.info("Premium permissions for player $playername (${apiPlayer.id}) has handled")
                } catch (_: Exception) {
                }
            }
        }
    }


    override fun shutdown() {}
}