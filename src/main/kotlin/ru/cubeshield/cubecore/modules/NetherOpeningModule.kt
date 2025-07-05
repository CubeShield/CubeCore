package ru.cubeshield.cubecore.modules

import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.particle.ParticleTypes
import net.minecraft.registry.Registries
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.config.accentColor
import ru.cubeshield.cubecore.config.baseColor
import ru.cubeshield.cubecore.event.*
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

@Serializable
data class AltarItem(
    val id: Identifier,
    var current: Int = 0,
    val required: Int = 16,
    var discovered: Boolean = false
)



class NetherOpeningModule : ICubeModule {
    override val id = "nether_opening_module"
    override val name = "Nether Opening Module"
    override val description = "Модуль, отвечающий за открытие ада"

    private val requiredItems = setOf(
        "minecraft:ender_pearl",
        "minecraft:rotten_flesh",
        "minecraft:golden_carrot",
        "minecraft:spider_eye",
        "minecraft:trial_key",
        "minecraft:glow_ink_sac",
        "minecraft:gunpowder",
        "minecraft:nether_wart",
        "minecraft:redstone",
        "minecraft:glowstone_dust",
        "minecraft:bone",
        "minecraft:quartz"
    ).associate { idStr ->
        val id = Identifier.of(idStr)
        id to AltarItem(id)
    }.toMutableMap()

    private lateinit var hologramPos: BlockPos
    private lateinit var cauldronPos: BlockPos
    private var isNetherOpenned = false


    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        cauldronPos = BlockPos(config.modules.netherOpeningModule.cauldronX, config.modules.netherOpeningModule.cauldronY, config.modules.netherOpeningModule.cauldronZ)
        hologramPos = cauldronPos

        PlayerBlockBreakEvents.BEFORE.register(PlayerBlockBreakEvents.Before { world, player, pos, state, blockEntity ->
            if (handleNetherOpening(world as ServerWorld)) return@Before true
            if (pos == cauldronPos && player is ServerPlayerEntity) {
                val message = if (player.gameProfile.name == "xSDK") {"Бля легенда имхо - пытаешься сломать этот котел = сосал "} else {"Ваша сила несравнима с могучестю этого котла..."}
                player.sendMessage(Text.literal(message).setStyle(Style.EMPTY.withColor(Formatting.DARK_PURPLE)))
                player.playSoundToPlayer(SoundEvents.ENTITY_ZOMBIE_INFECT, SoundCategory.BLOCKS, 1.0f, 1.0f)
                strikeLightning(world as ServerWorld, player.blockPos)
                return@Before false
            }
            true
        })

        UseBlockCallback.EVENT.register(UseBlockCallback { player, world, hand, hitResult ->
            val pos = hitResult.blockPos
            if (handleNetherOpening(world as ServerWorld)) return@UseBlockCallback ActionResult.PASS
            if (!world.isClient && pos == cauldronPos) {
                val playerServerEntity = player as ServerPlayerEntity
                player.sendMessage(Text.literal("Ваша сила несравнима с могучестю этого котла, а тем более с магическим отваром в ней...").setStyle(Style.EMPTY.withColor(Formatting.DARK_PURPLE)))
                strikeLightning(world as ServerWorld, player.blockPos)
                return@UseBlockCallback ActionResult.FAIL
            }
            ActionResult.PASS
        })

        eventBus.subscribe<ServerStartedEvent> { serverStartedEvent ->
            val server = serverStartedEvent.server
            val blockState: BlockState = Blocks.WATER_CAULDRON.defaultState
            server.overworld.setBlockState(cauldronPos, blockState)
        }

        ServerTickEvents.END_WORLD_TICK.register(ServerTickEvents.EndWorldTick { world ->
            if (world.isClient) return@EndWorldTick
            if (handleNetherOpening(world)) return@EndWorldTick
            val entities = world.iterateEntities()
            entities.filterIsInstance<ServerPlayerEntity>().filter { it.blockPos == cauldronPos }.forEach {serverPlayerEntity ->
                val commandSound = "audioplayer play ${config.modules.netherOpeningModule.damageAudioPlayerSoundId} ${cauldronPos.x} ${cauldronPos.y+10} ${cauldronPos.z} 40"
                world.server.execute {
                    try {
                        world.server.commandManager.dispatcher.execute(commandSound, world.server.commandSource)
                    } catch (_: Exception) {
                    }
                }
                world.spawnParticles(ParticleTypes.CLOUD, serverPlayerEntity.x, serverPlayerEntity.y, serverPlayerEntity.z, 20, 0.5, 0.5, 0.5, 0.01)
                launchPlayerUp(serverPlayerEntity, 10.0)
            }
            entities.filterIsInstance<ItemEntity>().filter { it.blockPos == cauldronPos }.forEach {itemEntity ->
                val id = Registries.ITEM.getId(itemEntity.stack.item)
                val altarItem = requiredItems[id]
                logger.info(id.toString())
                if (altarItem != null) {
                    if (!altarItem.discovered) {
                        val message = Text.literal("Могучая сила котла принимает в дар ").setStyle(Style.EMPTY.withColor(Formatting.DARK_PURPLE))
                            .append(itemEntity.stack.toHoverableText().copy().formatted(Formatting.LIGHT_PURPLE))
                        world.server.playerManager.broadcast(message, false)
                        world.server.playerManager.playerList.forEach { p ->
                            p.playSoundToPlayer(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.AMBIENT, 1.0f, 1.2f)
                        }
                        altarItem.discovered = true
                    }
                    altarItem.current += itemEntity.stack.count
                    world.spawnParticles(
                        ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                        cauldronPos.x + 0.5,
                        cauldronPos.y + 1.0,
                        cauldronPos.z + 0.5,
                        20,
                        0.3, 0.0, 0.3,
                        0.01
                    )
                    world.playSound(null, cauldronPos, SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS, 1.0f, 1.0f)
                    itemEntity.discard()
                } else {
                    world.server.playerManager.broadcast(Text.literal("Могучая сила котла отвергает подношение!").setStyle(Style.EMPTY.withColor(Formatting.DARK_PURPLE)), false)
                    itemEntity.discard()
                    strikeLightning(world, cauldronPos)
                    getMobPoses().forEach {mobPos ->
                        spawnRandomMob(world, mobPos)
                    }
                }
            }
            val particlePos = Vec3d(cauldronPos.x + 0.5, cauldronPos.y + 1.2, cauldronPos.z + 0.5)
            (world as ServerWorld).spawnParticles(
                ParticleTypes.PORTAL,
                particlePos.x,
                particlePos.y,
                particlePos.z,
                5,
                0.3, 0.1, 0.3,
                0.01
            )
        })
    }

    private fun handleNetherOpening(world: ServerWorld): Boolean {
        var isOpen = true
        requiredItems.forEach {id, altarItem ->
            isOpen = isOpen && altarItem.current >= altarItem.required
        }
        if (isOpen && !isNetherOpenned) {
            val message = Text.literal("Вы чувствуете, как могучая сила котла приоткрывает дверцу в другой мир...").setStyle(Style.EMPTY.withColor(Formatting.DARK_PURPLE))
            world.server.playerManager.broadcast(message, false)
            world.server.playerManager.playerList.forEach { p ->
                p.playSoundToPlayer(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.AMBIENT, 1.0f, 1.2f)
            }
            isNetherOpenned = true
            world.breakBlock(cauldronPos, false)
        }
        val commandSound = "noportals disableNetherPortal false"
        world.server.execute {
            try {
                world.server.commandManager.dispatcher.execute(commandSound, world.server.commandSource)
            } catch (_: Exception) {
            }
        }
        return isOpen
    }

    private fun launchPlayerUp(player: ServerPlayerEntity, power: Double = 2.0) {
        player.addVelocity(0.0, power, 0.0)
        player.velocityModified = true
    }

    private fun strikeLightning(world: ServerWorld, pos: BlockPos) {
        val lightning = EntityType.LIGHTNING_BOLT.create(world, SpawnReason.COMMAND)

        if (lightning != null) {
            lightning.refreshPositionAfterTeleport(
                pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5
            )
            world.spawnEntity(lightning)
        }
    }

    private fun spawnRandomMob(world: ServerWorld, pos: BlockPos) {
        val mobTypes = listOf(
            EntityType.ZOMBIE,
            EntityType.SKELETON,
            EntityType.SPIDER,
            EntityType.CAVE_SPIDER,
            EntityType.WITCH
        )

        val type = mobTypes.random()
        val entity = type.create(world, SpawnReason.COMMAND)

        if (entity != null) {
            entity.refreshPositionAndAngles(
                pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5,
                world.random.nextFloat() * 360f, 0f
            )
            world.spawnEntity(entity)
        }
    }

    private fun getMobPoses(): Set<BlockPos> {
        val positions = mutableSetOf<BlockPos>()

        val count = if (Random.nextBoolean()) 3 else 4
        val radius = Random.nextInt(2, 5)

        val angleStep = (2 * Math.PI) / count
        val yOffset = 0

        for (i in 0 until count) {
            val angle = angleStep * i + Random.nextDouble(0.0, 0.5)
            val dx = (cos(angle) * radius).roundToInt()
            val dz = (sin(angle) * radius).roundToInt()

            val pos = cauldronPos.add(dx, yOffset, dz)
            positions.add(pos)
        }

        return positions
    }

    override fun shutdown() {}
}