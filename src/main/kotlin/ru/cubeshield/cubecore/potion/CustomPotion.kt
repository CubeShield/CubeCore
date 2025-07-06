package ru.cubeshield.cubecore.potion

import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import ru.cubeshield.cubecore.config.accentColor

enum class CustomPotion(
    val displayName: Text,
    val color: Int,
    val effects: List<StatusEffectInstance>,
    val drunkennessLevel: Int = 0,
    val description: Text? = null,
) {
    COLA(
        Text.literal("Бутылочка напитка «Cola»").setStyle(Style.EMPTY.withColor(accentColor)),
        5844518,
        listOf(
            StatusEffectInstance(StatusEffects.REGENERATION, 25 * 20)
        ),
    ),
    MOLECAT(
        Text.literal("Бутылочка кротовухи").setStyle(Style.EMPTY.withColor(accentColor)),
        5488941,
        listOf(
            StatusEffectInstance(StatusEffects.HASTE, 40 * 20, 0),
            StatusEffectInstance(StatusEffects.BLINDNESS, 25 * 20)
        )
    ),
    SPID(
        Text.literal("Коктейль «СПИД»").setStyle(Style.EMPTY.withColor(accentColor)),
        15961002,
        listOf(
            StatusEffectInstance(StatusEffects.SPEED, 40 * 20, 1),
            StatusEffectInstance(StatusEffects.BLINDNESS, 15 * 20),
            StatusEffectInstance(StatusEffects.WEAKNESS, 20 * 20),
            StatusEffectInstance(StatusEffects.MINING_FATIGUE, 20 * 20)
        )
    ),
    KB_SKULL(
        Text.literal("«Черепок из КБ ☠»").setStyle(Style.EMPTY.withColor(accentColor)),
        4673362,
        listOf(
            StatusEffectInstance(StatusEffects.BLINDNESS, 50 * 20),
            StatusEffectInstance(StatusEffects.SLOWNESS, 30 * 20),
        ),
        description = Text.literal(""""Ни одна мразь не доживёт до завтра... Я отправлю в могилу стольких, скольких смогу"""").setStyle(Style.EMPTY.withColor(
            Formatting.DARK_PURPLE).withItalic(true))
    ),
    TOODRY(
        Text.literal("Коктейль «Сушняк»").setStyle(Style.EMPTY.withColor(accentColor)),
        10329495,
        listOf(
            StatusEffectInstance(StatusEffects.RESISTANCE, 80 * 20),
            StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 40 * 20),
            StatusEffectInstance(StatusEffects.WITHER, 15 * 20),
        ),
    ),
    FULL_HOUSE(
        Text.literal("Коктейль «Фулл Хаус»").setStyle(Style.EMPTY.withColor(accentColor)),
        13876595,
        listOf(
            StatusEffectInstance(StatusEffects.HASTE, 50 * 20, 1),
            StatusEffectInstance(StatusEffects.RESISTANCE, 50 * 20),
            StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 40 * 20),
            StatusEffectInstance(StatusEffects.WITHER, 25 * 20),
            StatusEffectInstance(StatusEffects.REGENERATION, 15 * 20),
        ),
    ),
    SAAT(
        Text.literal("Коктейль «СААТ»").setStyle(Style.EMPTY.withColor(accentColor)),
        6192150,
        listOf(
            StatusEffectInstance(StatusEffects.INSTANT_DAMAGE, 10 * 20),
            StatusEffectInstance(StatusEffects.NAUSEA, 50 * 20),
            StatusEffectInstance(StatusEffects.STRENGTH, 40 * 20),
        ),
    ),
    GREEN_TEA(
        Text.literal("Бутылочка Зеленого чая").setStyle(Style.EMPTY.withColor(accentColor)),
        4491542,
        listOf(
            StatusEffectInstance(StatusEffects.SATURATION, 10 * 20),
        ),
    ),
    BLACK_TEA(
        Text.literal("Бутылочка Черного чая").setStyle(Style.EMPTY.withColor(accentColor)),
        4536883,
        listOf(
            StatusEffectInstance(StatusEffects.SATURATION, 10 * 20),
        ),
    ),
    ESPRESSO_MACCHIATO(
        Text.literal("Бутылочка Кофе «Порфаворэ-Эспрессо-Мокиато»").setStyle(Style.EMPTY.withColor(accentColor)),
        8606770,
        listOf(
            StatusEffectInstance(StatusEffects.SATURATION, 10 * 20),
        ),
    )
}
