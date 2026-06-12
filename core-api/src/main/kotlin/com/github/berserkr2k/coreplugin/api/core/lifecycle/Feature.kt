package com.github.berserkr2k.coreplugin.api.core.lifecycle

interface Feature {
    val id: String
    
    // Especifica si esta feature requiere obligatoriamente una base de datos activa para encenderse
    val requiresDatabase: Boolean get() = false
    
    // Especifica si requiere obligatoriamente que la economía global esté cargada
    val requiresEconomy: Boolean get() = false

    fun onLoad(context: FeatureContext) {}
    fun onEnable(context: FeatureContext) {}
    fun onDisable(context: FeatureContext) {}
}
