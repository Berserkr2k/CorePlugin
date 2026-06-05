package com.github.berserkr2k.coreplugin.api.di

interface ServiceRegistry {
    fun <T : Any> register(type: Class<T>, instance: T)
    fun <T : Any> registerProvider(type: Class<T>, provider: () -> T)
    fun <T : Any> get(type: Class<T>): T
    fun <T : Any> getOptional(type: Class<T>): T?
}
