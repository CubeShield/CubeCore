package ru.cubeshield.cubecore.modules

import kotlinx.coroutines.CoroutineScope
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.cubeshield.cubecore.CubeCore
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.event.EventBus

interface ICubeModule {
    val id: String
    val name: String
    val description: String

    fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope)
    fun shutdown()

    val logger: Logger
        get() = LoggerFactory.getLogger("CubeCore: $name")
}