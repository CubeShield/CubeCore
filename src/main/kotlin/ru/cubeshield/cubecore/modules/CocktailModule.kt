package ru.cubeshield.cubecore.modules


import kotlinx.coroutines.*
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder
import net.minecraft.item.Items
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.event.*
import ru.cubeshield.cubecore.potion.CustomPotionFactory


class CocktailModule : ICubeModule {
    override val id = "cocktail_module"
    override val name = "Cocktail Module"
    override val description = "Модуль, добавляющий коктейли"


    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        FabricBrewingRecipeRegistryBuilder.BuildCallback { builder ->
            builder.registerItemRecipe(Items.DIAMOND, Items.IRON_INGOT, CustomPotionFactory.createPotion().item)
        }
    }


    override fun shutdown() {}
}