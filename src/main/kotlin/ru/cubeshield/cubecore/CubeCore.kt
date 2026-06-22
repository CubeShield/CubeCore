package ru.cubeshield.cubecore

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.Json
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import org.slf4j.LoggerFactory
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.event.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import net.minecraft.server.MinecraftServer
import ru.cubeshield.cubecore.modules.*

class CubeCore : DedicatedServerModInitializer {
    companion object {
        const val MOD_ID = "CubeCore"
        val LOGGER: org.slf4j.Logger = LoggerFactory.getLogger(MOD_ID)
        lateinit var minecraftServer: MinecraftServer
    }

    private lateinit var config: ModConfig
    private lateinit var eventBus: EventBus
    private lateinit var apiClient: ApiClient
    private lateinit var moduleManager: ModuleManager

    private val mainScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onInitializeServer() {
        LOGGER.info("Initializing CubeCore...")

        config = ModConfig.loadConfig()
        LOGGER.info("Config loaded!")

        eventBus = EventBus()

        val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
            prettyPrint = true
            coerceInputValues = true
        }

        val httpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(json)
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.NONE
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 15000L
                connectTimeoutMillis = 5000L
                socketTimeoutMillis = 15000L
            }
        }
        apiClient = ApiClient(httpClient, json, config.api.baseUrl, config.api.apiKey, mainScope)

        moduleManager = ModuleManager(eventBus, apiClient, config, mainScope)

        moduleManager.registerModule(AuthModule::class)
        moduleManager.registerModule(SessionModule::class)
        moduleManager.registerModule(PlayerAfkModule::class)
        moduleManager.registerModule(PlayerStateModule::class)
        moduleManager.registerModule(FunCommandsModule::class)
        moduleManager.registerModule(SkinModule::class)
        moduleManager.registerModule(SpitModule::class)
        moduleManager.registerModule(NotificationModule::class)
        moduleManager.registerModule(ItemFlexModule::class)
        moduleManager.registerModule(BankModule::class)
        moduleManager.registerModule(SignPaymentModule::class)
        moduleManager.registerModule(PremiumPermissionModule::class)
        moduleManager.registerModule(SignModule::class)
        moduleManager.registerModule(DiscoveryModule::class)
        moduleManager.registerModule(IntentHandleModule::class)

        LOGGER.info("Initializing all modules")
        moduleManager.initializeAll()

        ServerLifecycleEvents.SERVER_STARTING.register{server ->
            eventBus.publish(ServerStartingEvent(server))
            minecraftServer = server
        }

        ServerLifecycleEvents.SERVER_STARTED.register{server ->
            eventBus.publish(ServerStartedEvent(server))
        }

        ServerLifecycleEvents.SERVER_STOPPING.register{server ->
            eventBus.publish(ServerStoppingEvent(server))
            moduleManager.shutdownAll()
            mainScope.cancel()
        }

        ServerPlayerEvents.JOIN.register{player ->
            eventBus.publish(PlayerJoinedEvent(player))
        }

        ServerPlayerEvents.LEAVE.register{player ->
            eventBus.publish(PlayerLeftEvent(player))
        }

    }
}
