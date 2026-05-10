package ru.cubeshield.cubecore.modules

import com.mojang.brigadier.CommandDispatcher
import kotlinx.coroutines.*
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.Permissions
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
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

    private fun registerCommands(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("spit")
                .requires { source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR) } // FIX IMPLEMENT LP
                .executes { context ->
                    val spitter = context.source.playerOrException
                    executeSpit(context.source, spitter)
                    1
                }
        )
    }

    private fun executeSpit(source: CommandSourceStack, spitter: ServerPlayer) {
        val level = spitter.level()
        val eyePos = spitter.getEyePosition()

        level.playSound(
            null,
            eyePos.x, eyePos.y, eyePos.z,
            SoundEvents.LLAMA_SPIT,
            SoundSource.PLAYERS,
            1.0f,
            1.0f
        )

        val lookVec = spitter.getLookAngle()

        for (i in 0..15) {
            val randomX = (Random.nextDouble() - 0.5) * 0.3
            val randomY = (Random.nextDouble() - 0.5) * 0.3
            val randomZ = (Random.nextDouble() - 0.5) * 0.3

            level.addParticle(
                ParticleTypes.SPIT,
                eyePos.x, eyePos.y, eyePos.z,
                lookVec.x + randomX,
                lookVec.y + randomY,
                lookVec.z + randomZ
            )
        }
    }

    override fun shutdown() {}
}