package ru.cubeshield.cubecore.potion

import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.LoreComponent
import net.minecraft.component.type.NbtComponent
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Text
import java.util.*

object CustomPotionFactory {
    private val romanNumerals = listOf(
        "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"
    )

    private fun intToRoman(n: Int): String {
        return romanNumerals.getOrNull(n - 1) ?: n.toString()
    }

    fun createPotion(customPotion: CustomPotion): ItemStack {
        val potionStack = ItemStack(Items.POTION)
        potionStack.set(DataComponentTypes.CUSTOM_NAME, customPotion.displayName)
        val description = buildList {
            if (customPotion.drunkennessAmplifier > 0) {
                add(Text.literal("Опьянение ${intToRoman(customPotion.drunkennessAmplifier)}"))
            }
            customPotion.description?.let { add(it) }
        }
        potionStack.set(DataComponentTypes.LORE, LoreComponent(description))
        potionStack.set(DataComponentTypes.POTION_CONTENTS,
            PotionContentsComponent(Optional.empty(), Optional.of(customPotion.color), customPotion.effects, Optional.empty()),
        )
        val nbt = NbtCompound()
        nbt.putString("custom_potion_id", customPotion.name)
        potionStack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt))
        return potionStack;
    }
}