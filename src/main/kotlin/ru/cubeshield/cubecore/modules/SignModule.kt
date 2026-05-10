package ru.cubeshield.cubecore.modules

import com.mojang.brigadier.CommandDispatcher
import kotlinx.coroutines.*
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.ChatFormatting
import net.minecraft.commands.Commands
import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.component.ItemLore
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.event.*
import ru.cubeshield.cubecore.utils.MessageUtil

class SignModule : ICubeModule {
    override val id = "sign_module"
    override val name = "Sign Module"
    override val description = "Модуль, добавляющий подписывание предметов"

    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            registerCommands(dispatcher)
        }
    }

    private fun registerCommands(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("sign")
                .executes { context ->
                    val player = context.source.playerOrException
                    val stack = player.getMainHandItem()

                    if (stack.isEmpty) {
                        MessageUtil.send(player, "У вас нет предмета в основной руке", true, false)
                        return@executes 0
                    }

                    val oldLore = stack.get(DataComponents.LORE) ?: ItemLore(listOf(), listOf())
                    val alreadySigned = oldLore.lines().any { it.string.contains("♦") }

                    if (alreadySigned) {
                        MessageUtil.send(player, "Этот предмет уже подписан", true, false)
                        return@executes 0
                    }

                    val newLines = oldLore.lines().toMutableList()
                    newLines.add(
                        Component.literal("♦ ${player.name.string}")
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
                    )

                    stack.set(DataComponents.LORE, ItemLore(newLines, listOf()))
                    MessageUtil.send(player, "Предмет был успешно подписан", false, false)
                    1
                }
        )

        dispatcher.register(
            Commands.literal("unsign")
                .executes { context ->
                    val player = context.source.playerOrException
                    val stack = player.getMainHandItem()

                    if (stack.isEmpty) {
                        MessageUtil.send(player, "У вас нет предмета в основной руке", true, false)
                        return@executes 0
                    }

                    val oldLore = stack.get(DataComponents.LORE) ?: ItemLore(listOf(), listOf())
                    val newLines = oldLore.lines().toMutableList()

                    val signatureIndex = newLines.indexOfFirst {
                        it.string.startsWith("♦ ") && it.string.removePrefix("♦ ").equals(player.name.string, ignoreCase = true)
                    }

                    if (signatureIndex == -1) {
                        MessageUtil.send(player, "Вы не являетесь владельцем подписи или подпись отсутствует", true, false)
                        return@executes 0
                    }

                    newLines.removeAt(signatureIndex)
                    stack.set(DataComponents.LORE, ItemLore(newLines, listOf()))

                    MessageUtil.send(player, "Подпись успешно удалена", false, false)
                    1
                }
        )
    }

    override fun shutdown() {}
}