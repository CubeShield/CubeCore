package ru.cubeshield.cubecore.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.fabricmc.loader.api.FabricLoader
import ru.cubeshield.cubecore.CubeCore
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

@Serializable
data class ModConfig (
    val api: ApiConfig = ApiConfig(),
    val modules: ModuleConfigs = ModuleConfigs()
) {
    @Serializable
    data class ApiConfig (
        val baseUrl: String = "https://api.cubeshield.ru",
        val apiKey: String = "",
    )

    @Serializable
    data class ModuleConfigs (
        val authModule: AuthModuleConfig = AuthModuleConfig(),
        val playerAfkModule: PlayerAfkModuleConfig = PlayerAfkModuleConfig()
    )

    @Serializable
    data class AuthModuleConfig(
        val enable: Boolean = true
    )

    @Serializable
    data class PlayerAfkModuleConfig(
        val afkTimeoutSeconds: Int = 3 * 60
    )

    companion object {
        private val CONFIG_FILE: Path = FabricLoader.getInstance().configDir.resolve("${CubeCore.MOD_ID}.json")
        private val json = Json {
            encodeDefaults = true
            prettyPrint = true
            ignoreUnknownKeys = true
        }

        fun loadConfig(): ModConfig {
            return try {
                if (!CONFIG_FILE.exists()) {
                    CubeCore.LOGGER.info("Config file not found, creating default: $CONFIG_FILE")
                    val defaultConfig = ModConfig()
                    saveConfig(defaultConfig)
                    defaultConfig
                } else {
                    val configJson = CONFIG_FILE.readText()
                    json.decodeFromString<ModConfig>(configJson).also {
                        CubeCore.LOGGER.info("Config loaded from: $CONFIG_FILE")
                    }
                }
            } catch (e: Exception) {
                CubeCore.LOGGER.error("Failed to load config, using default: ${e.message}", e)
                ModConfig()
            }
        }

        fun saveConfig(config: ModConfig) {
            try {
                val configJson = json.encodeToString(ModConfig.serializer(), config)
                CONFIG_FILE.writeText(configJson)
                CubeCore.LOGGER.info("Config saved to: $CONFIG_FILE")
            } catch (e: Exception) {
                CubeCore.LOGGER.error("Failed to save config: ${e.message}", e)
            }
        }
    }
}