package ru.cubeshield.cubecore.utils

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import ru.cubeshield.cubecore.config.accentColor
import ru.cubeshield.cubecore.config.baseColor
import ru.cubeshield.cubecore.config.secondaryAccentColor

object MessageUtil {
    private fun createMessage(text: String, isError: Boolean = false): Component {
        val color = if (!isError) {
            accentColor
        } else {
            secondaryAccentColor
        }
        return Component.literal("")
            .append(Component.literal("♦ ").setStyle(Style.EMPTY.withColor(color)))
            .append(Component.literal(text).setStyle(Style.EMPTY.withColor(baseColor)))
    }

    fun send(player: Player, text: String, isError: Boolean = false, withSound: Boolean = true) {
        player.sendSystemMessage(createMessage(text, isError))
        if (!withSound) return
        player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)
    }

    fun send(player: ServerPlayer, text: String, isError: Boolean = false, withSound: Boolean = true) {
        player.sendSystemMessage(createMessage(text, isError))
        if (!withSound) return
        player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)
    }
}
