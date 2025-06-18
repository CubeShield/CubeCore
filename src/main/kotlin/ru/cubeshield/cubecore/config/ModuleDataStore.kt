package ru.cubeshield.cubecore.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import net.fabricmc.loader.api.FabricLoader
import ru.cubeshield.cubecore.CubeCore
import ru.cubeshield.cubecore.modules.ICubeModule
import java.nio.file.Path
import kotlin.io.path.*

class ModuleDataStore<T : Any>(
    private val module: ICubeModule,
    private val serializer: KSerializer<T>,
    private val default: () -> T
) {
    private val json = Json {
        encodeDefaults = true
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val configDir: Path =
        FabricLoader.getInstance().configDir.resolve(CubeCore.MOD_ID).also { it.createDirectories() }

    private val file: Path = configDir.resolve("${module.id}.json")

    @Volatile
    private var cached: T? = null

    fun load(): T {
        if (cached != null) return cached!!

        return try {
            if (!file.exists()) {
                val def = default()
                save(def)
                cached = def
                def
            } else {
                val text = file.readText()
                val obj = json.decodeFromString(serializer, text)
                cached = obj
                obj
            }
        } catch (e: Exception) {
            CubeCore.LOGGER.error("Failed to load config for ${module.id}: ${e.message}", e)
            val def = default()
            save(def)
            def
        }
    }

    fun save(data: T) {
        try {
            val text = json.encodeToString(serializer, data)
            file.writeText(text)
            cached = data
        } catch (e: Exception) {
            CubeCore.LOGGER.error("Failed to save config for ${module.id}: ${e.message}", e)
        }
    }
}