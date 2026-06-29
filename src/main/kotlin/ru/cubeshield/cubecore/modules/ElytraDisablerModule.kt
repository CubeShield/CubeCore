package ru.cubeshield.cubecore.modules

import kotlinx.coroutines.CoroutineScope
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.level.ServerPlayer
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.event.EventBus
import kotlin.math.max
import kotlin.math.min

class ElytraDisablerModule : ICubeModule {
    override val id = "elytra_disabler_module"
    override val name = "Elytra Disabler Module"
    override val description = "Модуль, отключающий полёт на элитре в заданных зонах"

    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        val moduleConfig = config.modules.elytraDisablerModule
        if (!moduleConfig.enable) return

        ServerTickEvents.END_SERVER_TICK.register { server ->
            if (moduleConfig.zones.isEmpty()) return@register
            for (player in server.playerList.players) {
                if (player.isFallFlying && isPlayerInDisabledZone(player, config)) {
                    player.stopFallFlying()
                }
            }
        }
    }

    private fun isPlayerInDisabledZone(player: ServerPlayer, config: ModConfig): Boolean {
        val pos = player.blockPosition()
        for (zone in config.modules.elytraDisablerModule.zones) {
            if (pos.x >= min(zone.fromX, zone.toX) && pos.x <= max(zone.fromX, zone.toX) &&
                pos.y >= min(zone.fromY, zone.toY) && pos.y <= max(zone.fromY, zone.toY) &&
                pos.z >= min(zone.fromZ, zone.toZ) && pos.z <= max(zone.fromZ, zone.toZ)
            ) {
                return true
            }
        }
        return false
    }

    override fun shutdown() {}
}
