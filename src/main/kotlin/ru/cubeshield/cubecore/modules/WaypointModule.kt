package ru.cubeshield.cubecore.modules

import com.mojang.authlib.properties.Property
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import kotlinx.coroutines.*
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.CommandExecutionContext
import net.minecraft.server.MinecraftServer
import net.minecraft.server.PlayerManager
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.text.TextColor
import ru.cubeshield.cubecore.CubeCore
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.event.*
import ru.cubeshield.cubecore.utils.MessageUtil
import kotlin.random.Random


class WaypointModule : ICubeModule {
    override val id = "waypoint_module"
    override val name = "Waypoint Module"
    override val description = "Модуль, отвечающий за вэйпоинты игроков"


    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            registerCommands(dispatcher)
        }
    }

    private fun registerCommands(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            CommandManager.literal("waypoints")
                .executes { context ->
                    val sender = context.source.playerOrThrow
                    val server = context.source.server

                    MessageUtil.send(sender, "Список маркеров игроков", false, true)

                    server.playerManager.playerList.forEach { player ->
                        logger.info(player.waypointConfig.color.get().toString())
                        player.sendMessage(Text.literal(player.gameProfile.name))
                    }
                    1
                }

        )
    }


    override fun shutdown() {}
}