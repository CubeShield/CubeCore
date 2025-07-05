package ru.cubeshield.cubecore.modules

import com.mojang.brigadier.CommandDispatcher
import kotlinx.coroutines.*
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.LoreComponent
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Formatting
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

    private fun registerCommands(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            CommandManager.literal("sign")
                .executes { context ->
                    val player = context.source.playerOrThrow
                    val stack = player.mainHandStack

                    if (stack.isEmpty) {
                        MessageUtil.send(player, "У вас нет предмета в основной руке", true, false)
                        return@executes 0
                    }

                    val oldLore = stack.get(DataComponentTypes.LORE) ?: LoreComponent(listOf())
                    val alreadySigned = oldLore.lines.any { it.string.contains("♦") }

                    if (alreadySigned) {
                        MessageUtil.send(player, "Этот предмет уже подписан", true, false)
                        return@executes 0
                    }

                    val newLore = oldLore.lines.toMutableList()
                    newLore.add(Text.literal("♦ ${player.name.string}").formatted(Formatting.DARK_GRAY, Formatting.ITALIC))

                    stack.set(DataComponentTypes.LORE, LoreComponent(newLore))
                    MessageUtil.send(player, "Предмет был успешно подписан", false, false)

                    1
                }

        )
        dispatcher.register(
            CommandManager.literal("unsign")
                .executes { context ->
                    val player = context.source.playerOrThrow
                    val stack = player.mainHandStack

                    if (stack.isEmpty) {
                        MessageUtil.send(player, "У вас нет предмета в основной руке", true, false)
                        return@executes 0
                    }

                    val oldLore = stack.get(DataComponentTypes.LORE) ?: LoreComponent(listOf())
                    val newLore = oldLore.lines.toMutableList()

                    val signatureIndex = newLore.indexOfFirst {
                        it.string.startsWith("♦ ") && it.string.removePrefix("♦ ").equals(player.name.string, ignoreCase = true)
                    }

                    if (signatureIndex == -1) {
                        MessageUtil.send(player, "Вы не являетесь владельцем подписи или подпись отсутствует", true, false)
                        return@executes 0
                    }

                    newLore.removeAt(signatureIndex)
                    stack.set(DataComponentTypes.LORE, LoreComponent(newLore))

                    MessageUtil.send(player, "Подпись успешно удалена", false, false)
                    1
                }
        )
    }

    override fun shutdown() {}
}