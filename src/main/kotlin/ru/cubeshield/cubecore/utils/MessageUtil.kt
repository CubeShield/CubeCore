package ru.cubeshield.cubecore.utils

import net.minecraft.core.Holder
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
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
import ru.cubeshield.cubecore.modules.WSIntent
import kotlin.text.append

object MessageUtil {
    private fun createMessage(text: String, isError: Boolean = false, intents: List<WSIntent> = emptyList()): Component {
        val color = if (!isError) {
            accentColor
        } else {
            secondaryAccentColor
        }
        val component = Component.literal("")
            .append(Component.literal("♦ ").setStyle(Style.EMPTY.withColor(color)))
            .append(Component.literal(text).setStyle(Style.EMPTY.withColor(baseColor)))

        if (intents.isNotEmpty()) {
            component.append(Component.literal("\n"))
            for (intent in intents) {
                val button = Component.literal("[${intent.text}]")
                    .setStyle(
                        Style.EMPTY
                            .withColor(accentColor)
                            .withClickEvent(
                                ClickEvent.RunCommand("intent ${intent.intentId}")
                            )
                            .withHoverEvent(
                                HoverEvent.ShowText(Component.literal("Нажмите"))
                            )
                    )
                component.append(Component.literal(" ")).append(button)
            }
        }
        return component
    }

    fun send(player: Player, text: String, isError: Boolean = false, withSound: Boolean = true, intents: List<WSIntent> = emptyList()) {
        player.sendSystemMessage(createMessage(text, isError, intents))
        if (!withSound) return
        // no sound(
    }

    fun send(player: ServerPlayer, text: String, isError: Boolean = false, withSound: Boolean = true, intents: List<WSIntent> = emptyList()) {
        player.sendSystemMessage(createMessage(text, isError, intents))
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
