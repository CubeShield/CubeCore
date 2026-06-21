package ru.cubeshield.cubecore.utils

import net.minecraft.core.Holder
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
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
        // no sound(
    }

    fun send(player: ServerPlayer, text: String, isError: Boolean = false, withSound: Boolean = true) {
        player.sendSystemMessage(createMessage(text, isError))
        if (!withSound) return
        player.connection.send(
            ClientboundSoundPacket(
                Holder.direct(SoundEvents.EXPERIENCE_ORB_PICKUP),
                SoundSource.MASTER,
                player.x, player.y, player.z,
                1.0f, 1.0f,
                player.level().random.nextLong()
            )
        )
    }
}
