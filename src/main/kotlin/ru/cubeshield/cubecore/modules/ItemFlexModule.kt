package ru.cubeshield.cubecore.modules

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import kotlinx.coroutines.*
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.CommandSource
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.item.ItemStack
import net.minecraft.potion.Potions
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.config.accentColor
import ru.cubeshield.cubecore.config.baseColor
import ru.cubeshield.cubecore.event.*
import ru.cubeshield.cubecore.utils.MessageUtil
import kotlin.random.Random


class ItemFlexModule : ICubeModule {
    override val id = "item_flex_module"
    override val name = "Item Flex Module"
    override val description = "Модуль, добавляющий возможность флексить предметами"

    private val PREMIUM_PERMISSION = "cubecore.premium"

    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            registerCommands(dispatcher, modScope)
        }
    }

    private fun registerCommands(dispatcher: CommandDispatcher<ServerCommandSource>, modScope: CoroutineScope) {
        dispatcher.register(
            CommandManager.literal("flex")
                .requires { source -> Permissions.check(source, PREMIUM_PERMISSION, source.hasPermissionLevel(2)) }
                .executes { context ->
                    val source = context.source
                    val sender = source.playerOrThrow

                    val mainHandStack: ItemStack = sender.mainHandStack

                    if (mainHandStack.isEmpty) {
                        MessageUtil.send(source.player!!,"Ты должен держать предмет в руке, чтобы флексить!", true)
                        return@executes 0
                    }

                    val server = source.server

                    val playerNameText = Text.literal(sender.gameProfile.name).setStyle(Style.EMPTY.withColor(
                        accentColor
                    ))

                    val message = playerNameText
                        .append(Text.literal(" флексит своим предметом ").setStyle(Style.EMPTY.withColor(baseColor)))
                        .append(mainHandStack.toHoverableText())

                    server.playerManager.broadcast(message, false)

                    source.player?.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_CELEBRATE, SoundCategory.PLAYERS, 1.0f, 1.0f)

                    1
                }
        )
    }

    override fun shutdown() {}
}