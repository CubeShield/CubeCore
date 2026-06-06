package ru.cubeshield.cubecore.modules

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import kotlinx.coroutines.*
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.config.accentColor
import ru.cubeshield.cubecore.config.baseColor
import ru.cubeshield.cubecore.event.*
import kotlin.random.Random

class FunCommandsModule : ICubeModule {
    override val id = "fun_commands_module"
    override val name = "Fun Commands Module"
    override val description = "Модуль, добавляющий рофельные команды"

    private val PREMIUM_PERMISSION = "cubecore.premium"

    private val agreementMessages = listOf("Да", "да")
    private val sosalMessages = listOf("сосали?", "Сосали?", "сосал?", "Сосал?")
    private val byteMessages = listOf(
        "пойдете лутать триал чамберы?",
        "идем в шахту?",
        "пойдем в шахту?",
        "идем на дракона?",
        "это дом сдк?",
        "у всех есть еда?",
        "есть алмазы?",
        "есть ары ребят?",
        "всем нужны ресурсы?",
        "идете в каньон?",
        "пойдем лутать данжы?",
        "всем хватает ресов?",
        "вы готовы искать крепость?",
        "идем лутать альтушек?",
        "через 5 минут идем рейдить базу сдк?",
        "это ваш маяк?",
        "через 10 минут го?",
        "вместе идем искать харам?",
        "это ваша база на горе?",
        "чо идем в шахту?",
        "идем искать деревню?",
        "это база вани?",
        "это же база сани тут?",
    )

    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            registerCommands(dispatcher, modScope)
        }
    }

    private fun generateChatFormatted(playername: String, message: String): Component {
        return Component.literal(playername).setStyle(Style.EMPTY.withColor(baseColor))
            .append(Component.literal(" » ").setStyle(Style.EMPTY.withColor(accentColor)))
            .append(Component.literal(message).setStyle(Style.EMPTY.withColor(baseColor)))
    }

    private fun registerCommands(dispatcher: CommandDispatcher<CommandSourceStack>, modScope: CoroutineScope) {
        dispatcher.register(
            Commands.literal("factcheckstatus")
                .requires(Permissions.require("cubecore.premium"))
                .executes { context ->
                    val source = context.source
                    source.sendSystemMessage(Component.literal("Fact Check Status:"))
                    val result = if (Random.nextBoolean()) {
                        Component.literal("§aTrue ✔")
                    } else {
                        Component.literal("§cFalse ✕")
                    }
                    source.sendSystemMessage(result)
                    1
                }
        )

        dispatcher.register(
            Commands.literal("sosal?")
                .requires(Permissions.require("cubecore.premium"))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes { context ->
                        modScope.launch {
                            val source = context.source
                            val sender = source.playerOrException
                            val targetPlayer = EntityArgument.getPlayer(context, "player")
                            val server = source.server

                            val message1 = generateChatFormatted(sender.gameProfile.name, byteMessages.random())
                            val message2 = generateChatFormatted(sender.gameProfile.name, sosalMessages.random())
                            val message3 = generateChatFormatted(targetPlayer.gameProfile.name, agreementMessages.random())

                            server.playerList.broadcastSystemMessage(message1, false)
                            delay(Random.nextLong(1400L, 2300L))
                            server.playerList.broadcastSystemMessage(message2, false)
                            delay(Random.nextLong(200L, 400L))
                            server.playerList.broadcastSystemMessage(message3, false)
                        }
                        1
                    }
                )
        )

        dispatcher.register(
            Commands.literal("sex")
                .requires(Permissions.require("cubecore.premium"))
                .then(
                    Commands.argument("target", StringArgumentType.greedyString())
                        .suggests { context, builder ->
                            SharedSuggestionProvider.suggest(context.source.onlinePlayerNames, builder)
                        }
                        .executes { context ->
                            val sender = context.source.playerOrException
                            val targetText = StringArgumentType.getString(context, "target")
                            val message = Component.literal("§f${sender.name.string} §c❤ §f$targetText")

                            context.source.server.playerList.broadcastSystemMessage(message, false)

                            context.source.server.playerList.players.forEach { targetPlayer ->
                                targetPlayer.playSound(
                                    SoundEvents.SHULKER_DEATH,
                                    1.0f,
                                    1.0f
                                )
                            }
                            1
                        }
                )
        )
    }

    override fun shutdown() {}
}