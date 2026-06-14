package com.github.berserkr2k.coreplugin.infrastructure.validation

import com.github.berserkr2k.coreplugin.api.core.validation.ConfigValidator
import com.github.berserkr2k.coreplugin.api.core.validation.ValidationRegistry
import java.util.concurrent.CopyOnWriteArrayList

class ValidationEngine : ValidationRegistry {
    private val validators = CopyOnWriteArrayList<ConfigValidator<*>>()
    private val warnings = CopyOnWriteArrayList<ConfigWarning>()

    data class ConfigWarning(val fileName: String, val message: String)

    fun recordWarning(fileName: String, message: String) {
        warnings.add(ConfigWarning(fileName, message))
    }

    fun getWarnings(): List<ConfigWarning> {
        return warnings.toList()
    }

    fun clearWarnings() {
        warnings.clear()
    }

    override fun <T> register(validator: ConfigValidator<T>) {
        validators.add(validator)
    }

    private fun getValidatedType(validator: ConfigValidator<*>): Class<*>? {
        for (iface in validator.javaClass.genericInterfaces) {
            if (iface is java.lang.reflect.ParameterizedType) {
                if (iface.rawType == ConfigValidator::class.java) {
                    val actualType = iface.actualTypeArguments.firstOrNull()
                    if (actualType is Class<*>) {
                        return actualType
                    }
                }
            }
        }
        val superclass = validator.javaClass.genericSuperclass
        if (superclass is java.lang.reflect.ParameterizedType) {
            if (superclass.rawType == ConfigValidator::class.java) {
                val actualType = superclass.actualTypeArguments.firstOrNull()
                if (actualType is Class<*>) {
                    return actualType
                }
            }
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> validate(config: T): List<String> {
        val errors = mutableListOf<String>()
        val configClass = config.javaClass
        for (validator in validators) {
            val validatedType = getValidatedType(validator)
            if (validatedType == null || validatedType.isAssignableFrom(configClass)) {
                try {
                    val v = validator as ConfigValidator<T>
                    errors.addAll(v.validate(config))
                } catch (e: Exception) {
                    // Ignore cast exceptions or mismatch errors
                }
            }
        }
        return errors
    }
}
