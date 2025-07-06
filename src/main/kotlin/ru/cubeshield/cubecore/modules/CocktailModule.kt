package ru.cubeshield.cubecore.modules


import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.MapDecoder
import kotlinx.coroutines.*
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder
import net.minecraft.component.ComponentType
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.NbtComponent
import net.minecraft.item.Items
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtOps
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.event.*
import ru.cubeshield.cubecore.potion.CustomPotion
import ru.cubeshield.cubecore.potion.CustomPotionFactory
import kotlin.math.log


class CocktailModule : ICubeModule {
    override val id = "cocktail_module"
    override val name = "Cocktail Module"
    override val description = "Модуль, добавляющий коктейли"


    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        eventBus.subscribe<PlayerAuthorized> { (player) ->
            CustomPotion.entries.map { potion ->
                player.giveItemStack(CustomPotionFactory.createPotion(potion))
            }
        }

        UseItemCallback.EVENT.register(UseItemCallback { player, world, hand ->
            val stack = player.getStackInHand(hand)

            if (world.isClient) return@UseItemCallback ActionResult.PASS

            if (stack.item == Items.POTION) {
                val nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA)
                val id = nbtComponent?.copyNbt()?.get("custom_potion_id") ?: return@UseItemCallback ActionResult.PASS
                val potion = CustomPotion.valueOf(id.toString().trim('"'))
            }

            return@UseItemCallback ActionResult.PASS
        })
    }

    override fun shutdown() {}
}