package com.github.berserkr2k.coreplugin.api.core.event

/**
 * Marks an event class as a public API event that external features or third-party plugins can subscribe to.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PublicEvent

/**
 * Marks an event class as feature-internal. Other modules are restricted from subscribing to this event.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class InternalEvent
