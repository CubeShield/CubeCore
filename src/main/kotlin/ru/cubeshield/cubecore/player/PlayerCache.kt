package ru.cubeshield.cubecore.player

import java.util.concurrent.ConcurrentHashMap

object PlayerCache {
    private val ids = ConcurrentHashMap<String, String>()

    fun put(name: String, id: String) {
        ids[name] = id
    }

    fun getId(name: String): String? = ids[name]
}