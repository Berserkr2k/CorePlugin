package com.github.berserkr2k.coreplugin.api.scheduler

import org.bukkit.Location
import org.bukkit.entity.Entity

interface Task {
    val isCancelled: Boolean
    fun cancel()
}

interface TaskScheduler {
    fun runSync(runnable: Runnable): Task
    fun runAsync(runnable: Runnable): Task
    fun runSyncLater(runnable: Runnable, delayTicks: Long): Task
    fun runAsyncLater(runnable: Runnable, delayTicks: Long): Task
    fun runSyncTimer(runnable: Runnable, delayTicks: Long, periodTicks: Long): Task
    fun runAsyncTimer(runnable: Runnable, delayTicks: Long, periodTicks: Long): Task
}

interface RegionTaskScheduler {
    fun runAtLocation(location: Location, runnable: Runnable): Task
    fun runAtEntity(entity: Entity, runnable: Runnable): Task
}

interface ThreadAssertion {
    fun ensureMainThread()
    fun ensureRegionThread(location: Location)
    fun ensureAsyncThread()
}
