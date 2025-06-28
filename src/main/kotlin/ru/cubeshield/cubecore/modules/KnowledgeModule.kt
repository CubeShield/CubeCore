package ru.cubeshield.cubecore.modules

import kotlinx.coroutines.CoroutineScope
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.event.*
import net.minecraft.registry.Registries
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Style
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
class KnowledgeModule : ICubeModule {
    override val id = "knowledge_module"
    override val name = "Knowledge Module"
    override val description = "Модуль, отвечающий за знания о блоках/предметах"

    private val blockedBlocks = setOf(
        "minecraft:ancient_debris",
    ).map { Identifier.of(it) }

    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        PlayerBlockBreakEvents.BEFORE.register(PlayerBlockBreakEvents.Before { world, player, pos, state, blockEntity ->
            if (player is ServerPlayerEntity) {
                val blockId = Registries.BLOCK.getId(state.block)
                if (blockId in blockedBlocks) {
                    val message = if (player.gameProfile.name == "xSDK") {"Сдк, ты - ЕБУЧИЙ ПТУШНИК, ты НИКОГДА не добудешь этот блок"} else {"Вы недостаточно умны, чтобы понять, как добыть это..."}
                    player.sendMessage(Text.literal(message).setStyle(Style.EMPTY.withColor(Formatting.DARK_PURPLE)), false)
                    player.playSoundToPlayer(SoundEvents.ENTITY_ZOMBIE_INFECT, SoundCategory.BLOCKS, 1.0f, 1.0f)
                    return@Before false
                }
            }
            true
        })
    }

    override fun shutdown() {}
}
