package ru.cubeshield.cubecore.modules

import kotlinx.coroutines.*
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.api.ApiResponse
import ru.cubeshield.cubecore.api.dto.DiscoveryCreateDto
import ru.cubeshield.cubecore.api.dto.ItemCreateDto
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.event.*
import ru.cubeshield.cubecore.player.PlayerCache
import java.util.concurrent.ConcurrentHashMap

class DiscoveryModule : ICubeModule {
    override val id = "discovery_module"
    override val name = "Discovery Module"
    override val description = "Модуль, отвечающий за открытия новых предметов"

    private lateinit var apiClient: ApiClient
    private lateinit var modScope: CoroutineScope

    @Volatile
    private var ready = false

    private var cachedDiscoveries = ConcurrentHashMap<String, MutableSet<String>>()

    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        this.apiClient = apiClient
        this.modScope = modScope

        eventBus.subscribe<ServerStartedEvent> { (server) ->
            modScope.launch {
                val discoveries = when (val result = apiClient.getDiscoveries()) {
                    is ApiResponse.Success -> result.data
                    is ApiResponse.Error -> {
                        logger.error("failed to fetch all discoveries", result.exception)
                        return@launch
                    }
                }

                for (discovery in discoveries.discoveries) {
                    val key = "${discovery.item.namespace}:${discovery.item.item}"
                    cachedDiscoveries
                        .computeIfAbsent(key) { mutableSetOf() }
                        .add(discovery.profile.playername)
                }
                logger.info("Discovery cached $cachedDiscoveries")
            }

        }

        var tickCounter = 0
        ServerTickEvents.END_SERVER_TICK.register { server ->
            tickCounter++
            if (tickCounter < 20) return@register
            tickCounter = 0

            server.playerList.players.forEach { player ->
                checkPlayerInventory(player)
            }
        }
    }

    private fun checkPlayerInventory(player: ServerPlayer) {
        for (itemStack in player.inventoryMenu.items) {
            if (itemStack.isEmpty) continue
            checkItemForDiscovery(itemStack, player)
        }
    }

    private fun checkItemForDiscovery(stack: ItemStack, player: ServerPlayer) {
        val location = BuiltInRegistries.ITEM.getKey(stack.item) // ResourceLocation
        val namespace = location.namespace                       // "minecraft"
        val itemPath = location.path                             // "diamond"
        val key = location.toString()                            // "minecraft:diamond"
        val playername = player.gameProfile.name

        val isNewItem = !cachedDiscoveries.containsKey(key)

        val discoverers = cachedDiscoveries.computeIfAbsent(key) { ConcurrentHashMap.newKeySet() }

        if (!discoverers.add(playername)) return

        modScope.launch {
            try {
                if (isNewItem) {
                    val itemDto = ItemCreateDto(
                        translationKey = stack.item.descriptionId,
                        modName = FabricLoader.getInstance()
                            .getModContainer(namespace)
                            .map { it.metadata.name }
                            .orElse(namespace),
                        namespace = namespace,
                        item = itemPath,
                        rare = stack.rarity.name.lowercase(),
                    )
                    logger.info("Discovery item $itemDto")
                    if (apiClient.createItem(itemDto) is ApiResponse.Error) {
                        logger.warn("createItem failed/skip for $key (возможно уже существует)")
                    }
                }

                val playerId = PlayerCache.getId(playername) ?: run {
                    discoverers.remove(playername)
                    return@launch
                }

                when (val res = apiClient.createDiscovery(playerId, DiscoveryCreateDto(namespace, itemPath))) {
                    is ApiResponse.Success -> logger.info("Discovery: $playername -> $key")
                    is ApiResponse.Error -> {
                        logger.error("failed to create discovery $key for $playername", res.exception)
                        discoverers.remove(playername)
                    }
                }
            } catch (e: Exception) {
                logger.error("discovery flow failed for $key / $playername", e)
                discoverers.remove(playername)
            }
        }
    }

    override fun shutdown() {}
}