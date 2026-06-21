package ru.cubeshield.cubecore.modules

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.*
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.Rarity
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.api.ApiResponse
import ru.cubeshield.cubecore.api.dto.DiscoveryCreateDto
import ru.cubeshield.cubecore.api.dto.ItemCreateDto
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.config.accentColor
import ru.cubeshield.cubecore.config.baseColor
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

    private val extraNotifyItems = setOf(
        Items.IRON_INGOT,
        Items.COPPER_INGOT,
        Items.GOLD_INGOT,
        Items.NETHERITE_SCRAP,
        Items.ANCIENT_DEBRIS,
        Items.NETHERITE_INGOT,
        Items.GOLDEN_APPLE,
        Items.DIAMOND,
        Items.LAPIS_LAZULI,
        Items.EMERALD,
        Items.RAW_GOLD,
        Items.RAW_COPPER,
        Items.RAW_IRON,
        Items.CHARCOAL,
        Items.COAL,
        Items.QUARTZ,
        Items.AMETHYST_SHARD,
        Items.TURTLE_SCUTE,
        Items.PHANTOM_MEMBRANE,
        Items.TURTLE_HELMET,
        Items.GHAST_TEAR,
        Items.TRIAL_KEY,
        Items.OMINOUS_TRIAL_KEY,
        Items.SHULKER_SHELL,
        Items.ENDER_PEARL,
        Items.ENDER_EYE,
        Items.SPYGLASS,
        Items.AXOLOTL_BUCKET,
        Items.END_CRYSTAL,
        Items.TNT,
        Items.DIAMOND_HELMET,
        Items.DIAMOND_CHESTPLATE,
        Items.DIAMOND_LEGGINGS,
        Items.DIAMOND_BOOTS,
        Items.DIAMOND_SWORD,
        Items.DIAMOND_AXE,
        Items.DIAMOND_PICKAXE,
        Items.DIAMOND_SHOVEL,
        Items.DIAMOND_HOE,
        Items.NETHERITE_HELMET,
        Items.NETHERITE_CHESTPLATE,
        Items.NETHERITE_LEGGINGS,
        Items.NETHERITE_BOOTS,
        Items.NETHERITE_SWORD,
        Items.NETHERITE_AXE,
        Items.NETHERITE_PICKAXE,
        Items.NETHERITE_SHOVEL,
        Items.NETHERITE_HOE,)

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

    private fun checkItemNotification(stack: ItemStack): Boolean {
        if (stack.rarity != Rarity.COMMON) return true
        if (extraNotifyItems.contains(stack.item)) return true
        return false
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
                    if (apiClient.createItem(itemDto) is ApiResponse.Error) {
                        logger.warn("createItem failed/skip for $key (возможно уже существует)")
                    }
                }

                val playerId = PlayerCache.getId(playername) ?: run {
                    discoverers.remove(playername)
                    return@launch
                }

                when (val res = apiClient.createDiscovery(playerId, DiscoveryCreateDto(namespace, itemPath))) {
                    is ApiResponse.Success -> {
                        logger.info("Discovery: $playername -> $key")
                        val server = player.level().server
                        server.execute {
                            if (checkItemNotification(stack) && isNewItem) {
                                val playerNameText = Component.literal(player.gameProfile.name)
                                    .setStyle(Style.EMPTY.withColor(accentColor))

                                val message = playerNameText
                                    .append(Component.literal(" первый нашел ").setStyle(Style.EMPTY.withColor(baseColor)))
                                    .append(stack.getDisplayName())




                                server.playerList.broadcastSystemMessage(message, false)
                                server.playerList.players.forEach { p ->
                                    p.connection.send(
                                        ClientboundSoundPacket(
                                            Holder.direct(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE),
                                            SoundSource.MASTER,
                                            p.x, p.y, p.z,
                                            1.0f, 1.2f,
                                            p.level().random.nextLong()
                                        )
                                    )
                                }
                            } else {
                                val itemName = stack.displayName  // уже "[Слеза гаста]" со скобками и цветом

                                val text = if (isNewItem) {
                                    Component.literal("Ты — первый, кто открыл ")
                                        .setStyle(Style.EMPTY.withColor(baseColor))
                                        .append(itemName)
                                } else {
                                    Component.literal("Ты открыл ")
                                        .setStyle(Style.EMPTY.withColor(baseColor))
                                        .append(itemName)
                                }

                                player.connection.send(ClientboundSetActionBarTextPacket(text))
                                player.connection.send(
                                    ClientboundSoundPacket(
                                        Holder.direct(SoundEvents.UI_TOAST_IN),
                                        SoundSource.MASTER,
                                        player.x, player.y, player.z,
                                        1.0f, 1.2f,
                                        player.level().random.nextLong()
                                    )
                                )
                            }
                        }

                    }
                    is ApiResponse.Error -> {
                        if (res.statusCode == HttpStatusCode.Conflict) {
                            logger.debug("discovery already exists $key for $playername (ok)")
                        } else {
                            logger.error("failed to create discovery $key for $playername", res.exception)
                            discoverers.remove(playername)
                        }
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