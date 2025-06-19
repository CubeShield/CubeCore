package ru.cubeshield.cubecore.utils

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Style
import net.minecraft.text.Text
import ru.cubeshield.cubecore.config.accentColor
import ru.cubeshield.cubecore.config.baseColor
import ru.cubeshield.cubecore.config.secondaryAccentColor

object MessageUtil {
    private fun createMessage(text: String, isError: Boolean = false): Text {
        val color = if (!isError) {accentColor} else {secondaryAccentColor}
        return Text.literal("")
            .append(Text.literal("♦ ").setStyle(Style.EMPTY.withColor(color)))
            .append(Text.literal(text).setStyle(Style.EMPTY.withColor(baseColor)))
    }
    fun send(player: PlayerEntity, text: String, isError: Boolean = false, withSound: Boolean = true) {
        player.sendMessage(createMessage(text, isError), false)
        if (!withSound) return
        player.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.0f)
    }
    fun send(player: ServerPlayerEntity, text: String, isError: Boolean = false, withSound: Boolean = true) {
        player.sendMessage(createMessage(text, isError), false)
        if (!withSound) return
        player.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.0f)
    }
}