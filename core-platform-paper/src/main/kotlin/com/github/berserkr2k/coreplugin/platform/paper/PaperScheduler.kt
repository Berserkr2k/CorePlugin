package com.github.berserkr2k.coreplugin.platform.paper

import com.github.berserkr2k.coreplugin.api.core.scheduler.Task
import com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler
import com.github.berserkr2k.coreplugin.api.core.scheduler.RegionTaskScheduler
import com.github.berserkr2k.coreplugin.api.core.scheduler.ThreadAssertion
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.plugin.Plugin
import java.util.concurrent.TimeUnit

class FoliaTask(private val scheduledTask: io.papermc.paper.threadedregions.scheduler.ScheduledTask?) : Task {
    override val isCancelled: Boolean
        get() = scheduledTask == null || scheduledTask.isCancelled
    override fun cancel() {
        scheduledTask?.cancel()
    }
}

class PaperTaskScheduler(private val plugin: Plugin) : TaskScheduler {
    override fun runSync(runnable: Runnable): Task {
        if (!plugin.isEnabled) {
            runnable.run()
            return FoliaTask(null)
        }
        val task = Bukkit.getGlobalRegionScheduler().run(plugin) { _ -> runnable.run() }
        return FoliaTask(task)
    }

    override fun runAsync(runnable: Runnable): Task {
        if (!plugin.isEnabled) {
            java.util.concurrent.ForkJoinPool.commonPool().execute { runnable.run() }
            return FoliaTask(null)
        }
        val task = Bukkit.getAsyncScheduler().runNow(plugin) { _ -> runnable.run() }
        return FoliaTask(task)
    }

    override fun runSyncLater(runnable: Runnable, delayTicks: Long): Task {
        if (!plugin.isEnabled) {
            runnable.run()
            return FoliaTask(null)
        }
        val task = Bukkit.getGlobalRegionScheduler().runDelayed(plugin, { _ -> runnable.run() }, maxOf(1L, delayTicks))
        return FoliaTask(task)
    }

    override fun runAsyncLater(runnable: Runnable, delayTicks: Long): Task {
        if (!plugin.isEnabled) {
            java.util.concurrent.ForkJoinPool.commonPool().execute { runnable.run() }
            return FoliaTask(null)
        }
        val task = Bukkit.getAsyncScheduler().runDelayed(plugin, { _ -> runnable.run() }, delayTicks * 50L, TimeUnit.MILLISECONDS)
        return FoliaTask(task)
    }

    override fun runSyncTimer(runnable: Runnable, delayTicks: Long, periodTicks: Long): Task {
        if (!plugin.isEnabled) {
            return FoliaTask(null)
        }
        val task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { _ -> runnable.run() }, maxOf(1L, delayTicks), maxOf(1L, periodTicks))
        return FoliaTask(task)
    }

    override fun runAsyncTimer(runnable: Runnable, delayTicks: Long, periodTicks: Long): Task {
        if (!plugin.isEnabled) {
            return FoliaTask(null)
        }
        val task = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { _ -> runnable.run() }, delayTicks * 50L, periodTicks * 50L, TimeUnit.MILLISECONDS)
        return FoliaTask(task)
    }
}

class PaperRegionTaskScheduler(private val plugin: Plugin) : RegionTaskScheduler {
    override fun runAtLocation(location: Location, runnable: Runnable): Task {
        if (!plugin.isEnabled) {
            runnable.run()
            return FoliaTask(null)
        }
        val task = Bukkit.getRegionScheduler().run(plugin, location) { _ -> runnable.run() }
        return FoliaTask(task)
    }

    override fun runAtEntity(entity: Entity, runnable: Runnable): Task {
        if (!plugin.isEnabled) {
            runnable.run()
            return FoliaTask(null)
        }
        val task = entity.scheduler.run(plugin, { _ -> runnable.run() }, null)
        return FoliaTask(task)
    }
}

class PaperThreadAssertion : ThreadAssertion {
    override fun ensureMainThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw IllegalStateException("This operation must run on the primary server thread!")
        }
    }

    override fun ensureRegionThread(location: Location) {
        if (!Bukkit.isOwnedByCurrentRegion(location)) {
            throw IllegalStateException("This operation must run on the thread owning region at location: $location")
        }
    }

    override fun ensureAsyncThread() {
        if (Bukkit.isPrimaryThread()) {
            throw IllegalStateException("This operation must run asynchronously!")
        }
    }
}
