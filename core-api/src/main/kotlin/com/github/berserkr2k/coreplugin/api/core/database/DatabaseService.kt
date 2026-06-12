package com.github.berserkr2k.coreplugin.api.core.database

interface DatabaseService {
    fun getDatabase(featureId: String): FeatureDatabase

    @Deprecated("Use getDatabase(featureId) instead", ReplaceWith("getDatabase(feature)"))
    fun database(feature: String): FeatureDatabase = getDatabase(feature)
}
