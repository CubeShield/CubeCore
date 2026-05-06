package ru.cubeshield.cubecore.modules


import kotlinx.coroutines.*
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder
import net.fabricmc.fabric.mixin.content.registry.BrewingRecipeRegistryBuilderMixin
import net.minecraft.block.Blocks
import net.minecraft.component.DataComponentTypes
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.recipe.BrewingRecipeRegistry
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.math.BlockPos
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.event.*
import ru.cubeshield.cubecore.potion.CustomPotion
import ru.cubeshield.cubecore.potion.CustomPotionFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min
import kotlin.random.Random

data class PlayerDrunkenness (
    var amplifier: Int,
    var until: Long,
)

class CocktailModule : ICubeModule {
    override val id = "cocktail_module"
    override val name = "Cocktail Module"
    override val description = "Модуль, добавляющий коктейли"

    private val drunkennessLevels = ConcurrentHashMap<String, PlayerDrunkenness>()

    private val CHANCE_PER_AMPLIFIER_PER_TICK = 0.015
    private val BASE_INTENSITY = 0.08
    private val MAX_INTENSITY = 0.4

    private val brewingStands = mutableListOf<BlockPos>()

    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        eventBus.subscribe<PlayerAuthorized> { (player) ->
            CustomPotion.entries.map { potion ->
                player.giveItemStack(CustomPotionFactory.createPotion(potion))
            }
        }


        ServerTickEvents.END_WORLD_TICK.register(ServerTickEvents.EndWorldTick { world ->
            if (world.isClient) return@EndWorldTick

            world.server.playerManager.playerList.forEach { player ->
                val playername = player.gameProfile.name
                val drunkenness = drunkennessLevels[playername] ?: return@forEach

                if (System.currentTimeMillis() > drunkenness.until) {
                    drunkennessLevels.remove(playername)
                    return@forEach
                }

                val amplifier = drunkenness.amplifier
                val chance = CHANCE_PER_AMPLIFIER_PER_TICK * amplifier

                if (Random.nextDouble() < chance) {
                    val intensity = min(MAX_INTENSITY, BASE_INTENSITY * amplifier)
                    player.addVelocity(
                        Random.nextDouble(-intensity, intensity),
                        0.0,
                        Random.nextDouble(-intensity, intensity)
                    )
                    player.velocityModified = true
                }
            }
        })

        ServerTickEvents.END_WORLD_TICK.register(ServerTickEvents.EndWorldTick { world ->
            if (world.isClient) return@EndWorldTick
        })

        UseItemCallback.EVENT.register(UseItemCallback { player, world, hand ->
            val stack = player.getStackInHand(hand)
            val nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA)
            val item = stack.item

            if (world.isClient) return@UseItemCallback ActionResult.PASS
            if (stack.item != Items.POTION) return@UseItemCallback ActionResult.PASS

            val count = player.inventory.count(item)
            modScope.launch {
                delay(2100L)
                if (player.inventory.count(item) == count) return@launch
                val id = nbtComponent?.copyNbt()?.get("custom_potion_id") ?: return@launch
                val potion = CustomPotion.valueOf(id.toString().trim('"'))
                potion.onApply.invoke(player as ServerPlayerEntity, potion)
                if (potion.drunkennessAmplifier == 0) return@launch
                val playername = player.gameProfile.name
                val playerDrunkenness = drunkennessLevels[playername]
                if (playerDrunkenness == null) {
                    drunkennessLevels[playername] = PlayerDrunkenness(potion.drunkennessAmplifier, System.currentTimeMillis()+180000)
                }
                else {
                    drunkennessLevels[playername]?.until = playerDrunkenness.until + 180000
                    if (potion.drunkennessAmplifier > playerDrunkenness.amplifier && Random.nextBoolean()) {
                        drunkennessLevels[playername]?.amplifier = playerDrunkenness.amplifier + 1
                    }
                }
            }
            return@UseItemCallback ActionResult.PASS
        })
    }

    override fun shutdown() {}
}