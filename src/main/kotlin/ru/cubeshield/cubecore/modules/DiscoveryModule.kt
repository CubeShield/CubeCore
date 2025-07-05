package ru.cubeshield.cubecore.modules

import com.mojang.brigadier.CommandDispatcher
import kotlinx.coroutines.*
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.InventoryChangedListener
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.particle.ParticleTypes
import net.minecraft.recipe.RecipeMatcher.ItemCallback
import net.minecraft.registry.Registries
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.Rarity
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.config.accentColor
import ru.cubeshield.cubecore.config.baseColor
import ru.cubeshield.cubecore.event.*
import ru.cubeshield.cubecore.util.ModuleDataStore


@kotlinx.serialization.Serializable
data class DiscoveryData(
    val discoveredItems: MutableSet<String> = mutableSetOf()
)

class DiscoveryModule : ICubeModule {
    override val id = "discovery_module"
    override val name = "Discovery Module"
    override val description = "Модуль, отслеживающий первооткрывателей предметов"

    private lateinit var dataStore: ModuleDataStore<DiscoveryData>
    private val discoveredItems = mutableSetOf<Identifier>()
    private val includeItems = setOf(
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
        Items.NETHERITE_HOE,
        Items.NAME_TAG,
        )

    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        dataStore = ModuleDataStore(this, DiscoveryData.serializer()) { DiscoveryData() }
        val data = dataStore.load()
        data.discoveredItems.forEach {id ->
            discoveredItems.add(Identifier.of(id))
        }

        var tickCounter = 0

        ServerTickEvents.END_WORLD_TICK.register {server ->
            tickCounter++
            if (tickCounter < 20) return@register
            tickCounter = 0

            server.players.forEach {player ->
                checkPlayerInventory(player)
            }
        }

    }

    private fun checkPlayerInventory(player: ServerPlayerEntity) {
        for (itemStack in player.inventory.mainStacks) {
            if (itemStack.isEmpty) continue
            checkItemForDiscovery(itemStack, player)
        }
    }

    private fun checkItemForDiscovery(stack: ItemStack, player: ServerPlayerEntity) {


        if (stack.rarity == Rarity.COMMON && !includeItems.contains(stack.item)) return

        val itemId = Registries.ITEM.getId(stack.item)

        synchronized(dataStore) {
            if (discoveredItems.contains(itemId)) return

            recordDiscovery(itemId, stack, player)
        }
    }

    private fun recordDiscovery(itemId: Identifier, itemStack: ItemStack, player: ServerPlayerEntity) {
        discoveredItems.add(itemId)


        val server = player.server ?: return


        val playerNameText = Text.literal(player.gameProfile.name).setStyle(Style.EMPTY.withColor(accentColor))

        val message = playerNameText
            .append(Text.literal(" первый нашел ").setStyle(Style.EMPTY.withColor(baseColor)))
            .append(itemStack.toHoverableText())

        server.playerManager.broadcast(message, false)
        logger.info("${player.gameProfile.name} discovered ${itemId.path} for the first time.")

        server.playerManager.playerList.forEach { p ->
            p.playSoundToPlayer(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.AMBIENT, 1.0f, 1.2f)
        }
    }


    override fun shutdown() {
        val discoveredItemsString = mutableSetOf<String>()
        discoveredItems.forEach {id -> discoveredItemsString.add(id.toString())}
        dataStore.save(DiscoveryData(discoveredItems=discoveredItemsString))
    }
}