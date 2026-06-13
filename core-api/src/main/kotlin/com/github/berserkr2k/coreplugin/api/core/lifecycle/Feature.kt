package com.github.berserkr2k.coreplugin.api.core.lifecycle

interface Feature {
    val id: String
    val dependencies: Set<String> get() = emptySet()

    fun onLoad(context: FeatureContext) {}
    fun onEnable(context: FeatureContext) {}
    fun onDisable(context: FeatureContext) {}
}
