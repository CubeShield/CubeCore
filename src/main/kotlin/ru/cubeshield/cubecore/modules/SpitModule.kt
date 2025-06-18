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
                .requires { source -> Permissions.check(source, PREMIUM_PERMISSION, source.hasPermissionLevel(2)) }
                .executes { context ->
                    val spitter = context.source.playerOrThrow
                    executeSpit(context.source, spitter)
                    1
                }

        )
    }


    private fun executeSpit(source: ServerCommandSource, spitter: ServerPlayerEntity) {
        val world = spitter.world
        val eyePos = spitter.eyePos
        world.playSound(
            null,
            eyePos.x, eyePos.y, eyePos.z,
            SoundEvents.ENTITY_LLAMA_SPIT,
            SoundCategory.PLAYERS,
            1.0f,
            1.0f
        )

        val lookVec = spitter.rotationVector

        for (i in 0..15) {
            val randomX = (Random.nextDouble() - 0.5) * 0.3
            val randomY = (Random.nextDouble() - 0.5) * 0.3
            val randomZ = (Random.nextDouble() - 0.5) * 0.3

            world.spawnParticles(
                ParticleTypes.SPIT,
                eyePos.x, eyePos.y, eyePos.z,
                0,
                lookVec.x + randomX,
                lookVec.y + randomY,
                lookVec.z + randomZ,
                0.7
            )
        }
    }

    override fun shutdown() {}
}