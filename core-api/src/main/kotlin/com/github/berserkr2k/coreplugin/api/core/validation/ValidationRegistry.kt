package com.github.berserkr2k.coreplugin.api.core.validation

interface ConfigValidator<T> {
    fun validate(config: T): List<String>
}

interface ValidationRegistry {
    fun <T> register(validator: ConfigValidator<T>)
}
