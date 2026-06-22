package ru.cubeshield.cubecore.modules

import com.mojang.brigadier.arguments.StringArgumentType
import kotlinx.coroutines.*
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.Commands
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.event.*
import ru.cubeshield.cubecore.player.PlayerCache

class IntentHandleModule : ICubeModule {
    override val id = "intent_handle_module"
    override val name = "Intent Handle Module"
    override val description = "Модуль, обрабатывающий интенты игроков"

    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                Commands.literal("intent")
                    .then(Commands.argument("id", StringArgumentType.string())
                        .executes { context ->
                            val player = context.source.playerOrException
                            val id = StringArgumentType.getString(context, "id")
                            val playername = player.gameProfile.name
                            val playerId = PlayerCache.getId(playername) ?: return@executes 0

                            modScope.launch(Dispatchers.IO) {
                                try {
                                    apiClient.activateIntent(playerId, id)
                                } catch (e: Exception) {
                                    logger.error("Failed to activate intent $id for $playername ($playerId)", e)
                                }
                            }
                            1
                        }
                    ))
        }
    }

    override fun shutdown() {}
}
