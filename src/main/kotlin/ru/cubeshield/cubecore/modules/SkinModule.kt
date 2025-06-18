package ru.cubeshield.cubecore.modules

import com.mojang.authlib.properties.Property
import kotlinx.coroutines.*
import net.minecraft.server.MinecraftServer
import net.minecraft.server.PlayerManager
import net.minecraft.server.network.ServerPlayerEntity
import ru.cubeshield.cubecore.CubeCore
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.event.*
import java.util.concurrent.ConcurrentHashMap

import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager.*
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import java.util.*
import net.minecraft.util.Pair
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action


class SkinModule : ICubeModule {
    override val id = "skin_module"
    override val name = "Skin Module"
    override val description = "Модуль, отвечающий за скины игроков"

    private var cachedPlayersSkins = ConcurrentHashMap<String, String>()

    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {

        eventBus.subscribe<PlayerAuthorized> { (player, _, apiPlayer, _) ->
            logger.info(apiPlayer.skin)
            val textureJson = """
            {
              "textures": {
                "SKIN": {
                  "url": "http://textures.minecraft.net/texture/9484ef5035334ea57980d9d85e1660c74a074f080a24f01d2d26e520ddbd5eb4"
                }
              }
            }
            """
            val value = Base64.getEncoder().encodeToString(textureJson.toByteArray())
            val profile = player.gameProfile
            profile.properties.removeAll("textures")
            profile.properties.put("textures", Property("textures", value))

        }

    }

    override fun shutdown() {}
}