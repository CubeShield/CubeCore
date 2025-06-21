package ru.cubeshield.cubecore.modules

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import kotlinx.coroutines.*
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.CommandSource
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.potion.Potions
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Style
import net.minecraft.text.Text
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

    private val agreementMessages = listOf(
        "Да", "да"
    )

    private val sosalMessages = listOf(
        "сосали?", "Сосали?", "сосал?", "Сосал?"
    )

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

    private fun generateChatFormatted(playername: String, message: String): Text {
        return Text.literal(playername).setStyle(Style.EMPTY.withColor(baseColor))
            .append(Text.literal(" » ").setStyle(Style.EMPTY.withColor(accentColor)))
            .append(Text.literal(message).setStyle(Style.EMPTY.withColor(baseColor)))
    }

    private fun registerCommands(dispatcher: CommandDispatcher<ServerCommandSource>, modScope: CoroutineScope) {
        dispatcher.register(
            CommandManager.literal("factcheckstatus")
                .requires { source -> Permissions.check(source, PREMIUM_PERMISSION, source.hasPermissionLevel(2)) }
                .executes { context ->
                    val source = context.source

                    source.sendMessage(Text.literal("Fact Check Status:"))

                    val result = if (Random.nextBoolean()) {
                        Text.literal("§aTrue ✔")
                    } else {
                        Text.literal("§cFalse ✕")
                    }
                    source.sendMessage(result)

                    1
                }
        )

        dispatcher.register(
            CommandManager.literal("sosal?")
                .requires { source -> Permissions.check(source, PREMIUM_PERMISSION, source.hasPermissionLevel(2)) }
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes { context ->
                        modScope.launch {
                            val source = context.source
                            val sender = source.playerOrThrow
                            val targetPlayer = EntityArgumentType.getPlayer(context, "player")

                            val server = source.server

                            val message1 = generateChatFormatted(sender.gameProfile.name, byteMessages.random())
                            val message2 = generateChatFormatted(sender.gameProfile.name, sosalMessages.random())
                            val message3 = generateChatFormatted(targetPlayer.gameProfile.name, agreementMessages.random())

                            server.playerManager.broadcast(message1, false)
                            delay(Random.nextLong(1400L, 2300L))
                            server.playerManager.broadcast(message2, false)
                            delay(Random.nextLong(200L, 400L))
                            server.playerManager.broadcast(message3, false)

                        }
                        1
                    }
                )
        )

        dispatcher.register(
            CommandManager.literal("sex")
                .requires { source -> Permissions.check(source, PREMIUM_PERMISSION, source.hasPermissionLevel(2)) }
                .then(
                    CommandManager.argument("target", StringArgumentType.greedyString())
                        .suggests { context, builder ->
                            CommandSource.suggestMatching(context.source.playerNames, builder)
                        }
                        .executes { context ->
                            val sender = context.source.playerOrThrow
                            val targetText = StringArgumentType.getString(context, "target")

                            val message = Text.literal("§f${sender.name.string} §c❤ §f$targetText")

                            context.source.server.playerManager.broadcast(message, false)

                            context.source.server.playerManager.playerList.forEach { targetPlayer ->
                                targetPlayer.playSoundToPlayer(
                                    SoundEvents.ENTITY_SHULKER_DEATH,
                                    SoundCategory.HOSTILE,
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