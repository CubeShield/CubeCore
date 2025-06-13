package ru.cubeshield.cubecore.event


import ru.cubeshield.cubecore.CubeCore
import java.util.ArrayList
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class EventBus {
    val handlers = ConcurrentHashMap<KClass<out Event>, MutableList<Any>>()

    inline fun <reified T: Event> subscribe(noinline handler: (T) -> Unit) {
        handlers.computeIfAbsent(T::class) { Collections.synchronizedList(mutableListOf()) }.add(handler)
        CubeCore.LOGGER.debug("Registered handler for event type: ${T::class.simpleName}")
    }

    fun publish(event: Event) {
        val eventType = event::class
        val listeners = handlers[eventType] ?: return
        CubeCore.LOGGER.debug("Publishing event: ${eventType.simpleName}")

        ArrayList(listeners).forEach {listener ->
            try {
                @Suppress("UNCHECKED_CAST")
                (listener as (Event) -> Unit)(event)
            } catch (e: Exception) {
                CubeCore.LOGGER.error("Error processing event ${eventType.simpleName} by handler: ${e.message}", e)
            }
        }
    }
}