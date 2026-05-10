package ru.cubeshield.cubecore.modules

import com.mojang.brigadier.CommandDispatcher
import kotlinx.coroutines.*
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Rarity
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.config.accentColor
import ru.cubeshield.cubecore.config.baseColor
import ru.cubeshield.cubecore.event.*
import ru.cubeshield.cubecore.utils.MessageUtil


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
                        MessageUtil.send(source.player!!,"Ты должен держать предмет в руке, чтобы флексить!", true, false)
                        return@executes 0
                    }

                    val server = source.server

                    val playerNameText = Text.literal(sender.gameProfile.name).setStyle(Style.EMPTY.withColor(
                        accentColor
                    ))

                    var flexItemAmount = 0
                    for (i in 0 until sender.inventory.size()) {
                        val stack = sender.inventory.getStack(i)
                        if (!stack.isOf(mainHandStack.item)) continue
                        flexItemAmount += stack.count
                    }
                    val amountMessage = if (flexItemAmount > 1 && mainHandStack.item != Items.ENCHANTED_BOOK) {"x$flexItemAmount "} else {""}

                    var itemStyle = mainHandStack.toHoverableText().copy().style
                    if (mainHandStack.rarity == Rarity.COMMON) {
                        itemStyle = itemStyle.withColor(accentColor.rgb)
                    }

                    val message = playerNameText
                        .append(Text.literal(" флексит своим предметом ").setStyle(Style.EMPTY.withColor(baseColor)))
                        .append(Text.literal(amountMessage).setStyle(itemStyle))
                        .append(mainHandStack.toHoverableText().copy().setStyle(itemStyle))

                    server.playerManager.broadcast(message, false)
                    server.playerManager.playerList.forEach {player ->
                        player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_CELEBRATE, SoundCategory.PLAYERS, 1.0f, 1.0f)
                    }


                    1
                }
        )
    }

    override fun shutdown() {}
}