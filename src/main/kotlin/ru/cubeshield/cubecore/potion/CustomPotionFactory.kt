package ru.cubeshield.cubecore.potion

import net.minecraft.component.Component
import net.minecraft.component.ComponentType
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.LoreComponent
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.potion.Potions
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.text.Text
import java.util.*

object CustomPotionFactory {
    fun createPotion(): ItemStack {
        val potionStack = ItemStack(Items.POTION)
        potionStack.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("Окак"))

        potionStack.set(DataComponentTypes.LORE, LoreComponent(listOf(
            Text.literal("ss"),
            Text.literal("dasd")
        )))

        potionStack.set(DataComponentTypes.POTION_CONTENTS,
            PotionContentsComponent(Optional.of(Potions.STRENGTH), Optional.of(2) , mutableListOf(), Optional.of("s"))
        )

        return potionStack;
    }
}