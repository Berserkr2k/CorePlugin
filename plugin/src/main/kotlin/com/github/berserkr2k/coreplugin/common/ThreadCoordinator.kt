package com.github.berserkr2k.coreplugin.common

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.plugin.Plugin
import java.util.concurrent.TimeUnit

class ThreadCoordinator(private val plugin: Plugin) {

    /**
     * Ejecuta una tarea asíncronamente en el planificador de hilos secundarios.
     */
    fun runAsync(runnable: () -> Unit) {
        Bukkit.getAsyncScheduler().runNow(plugin) { _ -> runnable() }
    }

    /**
     * Ejecuta una tarea en la región espacial correspondiente a la ubicación física dada (Folia-compatible).
     */
    fun runTask(location: Location, runnable: () -> Unit) {
        Bukkit.getRegionScheduler().execute(plugin, location, runnable)
    }

    /**
     * Ejecuta una tarea de forma global en el planificador regional maestro del servidor.
     */
    fun runGlobal(runnable: () -> Unit) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, runnable)
    }

    /**
     * Ejecuta una tarea periódica de forma asíncrona exenta de hilos bloqueantes.
     */
    fun runTimerAsync(delayTicks: Long, periodTicks: Long, runnable: () -> Unit) {
        val delayMs = delayTicks * 50
        val periodMs = periodTicks * 50
        Bukkit.getAsyncScheduler().runAtFixedRate(
            plugin,
            { _ -> runnable() },
            delayMs,
            periodMs,
            TimeUnit.MILLISECONDS
        )
    }
}
