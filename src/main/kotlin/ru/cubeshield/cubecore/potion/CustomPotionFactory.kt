package ru.cubeshield.cubecore.potion

import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.LoreComponent
import net.minecraft.component.type.NbtComponent
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import java.util.*

object CustomPotionFactory {
    fun createPotion(customPotion: CustomPotion): ItemStack {
        val potionStack = ItemStack(Items.POTION)
        potionStack.set(DataComponentTypes.CUSTOM_NAME, customPotion.displayName)
        if (customPotion.description != null) {
            potionStack.set(DataComponentTypes.LORE, LoreComponent(listOf(customPotion.description)))
        }
        potionStack.set(DataComponentTypes.POTION_CONTENTS,
            PotionContentsComponent(Optional.empty(), Optional.of(customPotion.color), customPotion.effects, Optional.empty()),
        )
        val nbt = NbtCompound()
        nbt.putString("custom_potion_id", customPotion.name)
        potionStack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt))
        return potionStack;
    }
}