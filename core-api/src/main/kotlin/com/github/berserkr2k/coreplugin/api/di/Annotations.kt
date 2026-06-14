package com.github.berserkr2k.coreplugin.api.di

/**
 * Marks a service interface or class as part of the public API container.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PublicAPI

/**
 * Marks a class or interface as an internal service that must NOT be registered
 * in the public ServiceRegistry.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class InternalService
