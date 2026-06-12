package com.github.berserkr2k.coreplugin.api.core.event

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler

enum class EventPriority {
    LOW, NORMAL, HIGH, MONITOR
}

interface CancellableEvent {
    var isCancelled: Boolean
}

class RegisteredHandler(
    val priority: EventPriority,
    val executor: (Any) -> Unit
)

class CoreEventBus {
    private val handlers = ConcurrentHashMap<Class<*>, CopyOnWriteArrayList<RegisteredHandler>>()
    private val hierarchyCache = ConcurrentHashMap<Class<*>, List<Class<*>>>()

    fun <T : Any> register(eventType: Class<T>, priority: EventPriority = EventPriority.NORMAL, handler: (T) -> Unit) {
        handlers.computeIfAbsent(eventType) { CopyOnWriteArrayList() }
            .add(RegisteredHandler(priority) { handler(it as T) })
    }

    fun postSync(event: Any) {
        val classes = resolveHierarchy(event.javaClass)
        for (priority in EventPriority.entries) {
            for (clazz in classes) {
                val list = handlers[clazz] ?: continue
                for (registered in list) {
                    if (registered.priority == priority) {
                        if (event is CancellableEvent && event.isCancelled && priority != EventPriority.MONITOR) {
                            continue
                        }
                        registered.executor(event)
                    }
                }
            }
        }
    }

    fun postAsync(event: Any, scheduler: TaskScheduler) {
        scheduler.runAsync {
            postSync(event)
        }
    }

    private fun resolveHierarchy(clazz: Class<*>): List<Class<*>> {
        return hierarchyCache.computeIfAbsent(clazz) {
            val list = mutableListOf<Class<*>>()
            var current: Class<*>? = clazz
            while (current != null) {
                list.add(current)
                current.interfaces.forEach { list.addAll(resolveHierarchy(it)) }
                current = current.superclass
            }
            list.distinct()
        }
    }
}
