package ru.cubeshield.cubecore.modules

import com.mojang.brigadier.CommandDispatcher
import kotlinx.coroutines.*
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.core.Holder
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.Items
import net.minecraft.world.item.Rarity
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

    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            registerCommands(dispatcher, modScope)
        }
    }

    private fun registerCommands(dispatcher: CommandDispatcher<CommandSourceStack>, modScope: CoroutineScope) {
        dispatcher.register(
            Commands.literal("flex")
                .requires(Permissions.require("cubecore.premium"))
                .executes { context ->
                    val source = context.source
                    val sender = source.playerOrException

                    val mainHandStack = sender.getMainHandItem()

                    if (mainHandStack.isEmpty) {
                        MessageUtil.send(source.player!!, "Ты должен держать предмет в руке, чтобы флексить!", true, false)
                        return@executes 0
                    }

                    val server = source.server

                    val playerNameText = Component.literal(sender.gameProfile.name)
                        .setStyle(Style.EMPTY.withColor(accentColor))

                    var flexItemAmount = 0
                    for (i in 0 until sender.inventory.getContainerSize()) {
                        val stack = sender.inventory.getItem(i)
                        if (!stack.`is`(mainHandStack.item)) continue
                        flexItemAmount += stack.count
                    }
                    val amountMessage = if (flexItemAmount > 1 && mainHandStack.item != Items.ENCHANTED_BOOK) "x$flexItemAmount " else ""

                    var itemStyle = mainHandStack.getDisplayName().copy().style
                    if (mainHandStack.rarity == Rarity.COMMON) {
                        itemStyle = itemStyle.withColor(accentColor)
                    }

                    val message = playerNameText
                        .append(Component.literal(" флексит своим предметом ").setStyle(Style.EMPTY.withColor(baseColor)))
                        .append(Component.literal(amountMessage).setStyle(itemStyle))
                        .append(mainHandStack.getDisplayName().copy().setStyle(itemStyle))

                    server.playerList.broadcastSystemMessage(message, false)
                    server.playerList.players.forEach { player ->
                        player.connection.send(
                            ClientboundSoundPacket(
                                Holder.direct(SoundEvents.VILLAGER_CELEBRATE),
                                SoundSource.MASTER,
                                player.x, player.y, player.z,
                                1.0f, 1.0f,
                                player.level().random.nextLong()
                            )
                        )
                    }

                    1
                }
        )
    }

    override fun shutdown() {}
}
