package ru.cubeshield.cubecore.modules

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import kotlinx.coroutines.*
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.CommandSource
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.particle.ParticleTypes
import net.minecraft.potion.Potions
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.event.*
import kotlin.random.Random


class SpitModule : ICubeModule {
    override val id = "spit_module"
    override val name = "Spit Module"
    override val description = "Модуль, добавляющий плевок"

    private val PREMIUM_PERMISSION = "cubecore.premium"

    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            registerCommands(dispatcher)
        }
    }

    private fun registerCommands(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            CommandManager.literal("spit")
                // Команда доступна, если у игрока есть право ИЛИ он является оператором (уровень 2+)
                .requires { source -> Permissions.check(source, PREMIUM_PERMISSION, source.hasPermissionLevel(2)) }

                // Вариант 1: /spit (без аргументов)
                .executes { context ->
                    val spitter = context.source.playerOrThrow
                    executeSpit(context.source, spitter, null) // Цель не указана
                    1
                }

                // Вариант 2: /spit <player>
                .then(
                    CommandManager.argument("target", EntityArgumentType.player())
                        .executes { context ->
                            val spitter = context.source.playerOrThrow
                            val target = EntityArgumentType.getPlayer(context, "target")
                            executeSpit(context.source, spitter, target) // Указываем цель
                            1
                        }
                )
        )
    }

    /**
     * Основная логика плевка: создает звук и частицы.
     * @param source Источник команды.
     * @param spitter Игрок, который плюет.
     * @param target Цель плевка (может быть null).
     */
    private fun executeSpit(source: ServerCommandSource, spitter: ServerPlayerEntity, target: ServerPlayerEntity?) {
        val world = spitter.world
        val eyePos = spitter.getEyePos() // Позиция глаз игрока - отсюда летят частицы

        // 1. Воспроизводим звук плевка ламы для всех рядом
        world.playSound(
            null, // null означает, что звук не привязан к конкретному игроку и слышен всем вокруг
            eyePos.x, eyePos.y, eyePos.z,
            SoundEvents.ENTITY_LLAMA_SPIT,
            SoundCategory.PLAYERS,
            1.0f, // Громкость
            1.0f  // Тон
        )

        // 2. Создаем частицы плевка
        val lookVec = spitter.getRotationVector() // Направление взгляда игрока

        // Спавним 15 частиц в цикле, чтобы задать им скорость и направление
        for (i in 0..15) {
            // Добавляем небольшой случайный разброс, чтобы плевок не был идеально ровным
            val randomX = (Random.nextDouble() - 0.5) * 0.3
            val randomY = (Random.nextDouble() - 0.5) * 0.3
            val randomZ = (Random.nextDouble() - 0.5) * 0.3

            // Метод spawnParticles с count = 0 использует следующие три аргумента как вектор скорости
            world.spawnParticles(
                ParticleTypes.SPIT, // Тип частиц
                eyePos.x, eyePos.y, eyePos.z,
                0, // count
                lookVec.x + randomX, // Скорость по X
                lookVec.y + randomY, // Скорость по Y
                lookVec.z + randomZ, // Скорость по Z
                0.7 // Множитель скорости
            )
        }

        // 3. Отправляем сообщение в чат
        val message: Text = if (target != null) {
            Text.literal("§a${spitter.name.string} §fплюнул(а) в §c${target.name.string}!")
        } else {
            Text.literal("§a${spitter.name.string} §fсмачно сплюнул(а) на землю.")
        }

        source.server.playerManager.broadcast(message, false)
    }

    override fun shutdown() {}
}