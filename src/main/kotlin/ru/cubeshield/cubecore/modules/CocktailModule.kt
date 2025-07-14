package ru.cubeshield.cubecore.modules


import kotlinx.coroutines.*
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.component.DataComponentTypes
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.event.*
import ru.cubeshield.cubecore.potion.CustomPotion
import ru.cubeshield.cubecore.potion.CustomPotionFactory
import java.util.concurrent.ConcurrentHashMap
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

    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        eventBus.subscribe<PlayerAuthorized> { (player) ->
            CustomPotion.entries.map { potion ->
                player.giveItemStack(CustomPotionFactory.createPotion(potion))
            }
        }
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