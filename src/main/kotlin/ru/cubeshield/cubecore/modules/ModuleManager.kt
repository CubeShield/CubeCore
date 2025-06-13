package ru.cubeshield.cubecore.modules

import kotlinx.coroutines.CoroutineScope
import ru.cubeshield.cubecore.CubeCore
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.event.EventBus
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

class ModuleManager (
    private val eventBus: EventBus,
    private val apiClient: ApiClient,
    private val config: ModConfig,
    private val modScope: CoroutineScope
) {
    private val registeredModules = mutableMapOf<String, ICubeModule>()

    fun registerModule(moduleClass: KClass<out ICubeModule>) {
        try {
            val module = moduleClass.createInstance()
            if (registeredModules.contains(module.id)) {
                CubeCore.LOGGER.warn("Module ${module.id} already registered. Skipping.")
                return
            }
            registeredModules[module.id] = module
            CubeCore.LOGGER.info("Registred module: ${module.name} (${module.id})")
        } catch (e: Exception) {
            CubeCore.LOGGER.error("Failed to register module ${moduleClass.simpleName}, ${e.message}", e)
        }
    }

    fun initializeAll() {
        CubeCore.LOGGER.info("Initializing all modules...")
        registeredModules.values.forEach {module ->
            try {
                module.initialize(eventBus, apiClient, config, modScope)
                CubeCore.LOGGER.info("Initialize module: ${module.name} (${module.id})")
            } catch (e: Exception) {
                CubeCore.LOGGER.error("Failed to initialize module ${module.name} (${module.id}): ${e.message}", e)
            }
        }
    }

    fun shutdownAll() {
        CubeCore.LOGGER.info("Shutting all modules...")
        registeredModules.values.reversed().forEach {module ->
            try {
                module.shutdown()
                CubeCore.LOGGER.info("Shutdown module: ${module.name} (${module.id})")
            } catch (e: Exception) {
                CubeCore.LOGGER.error("Failed to shutdown module ${module.name} (${module.id}): ${e.message}", e)
            }
        }
    }

    fun getModule(id: String): ICubeModule? = registeredModules[id]
}