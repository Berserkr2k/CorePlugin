package com.github.berserkr2k.coreplugin.api.core.filesystem

import java.nio.file.Path

interface FeatureFolderProvider {
    /**
     * Devuelve la ruta (Path) del directorio de datos de una feature específica.
     * Si el directorio no existe, se crea automáticamente.
     */
    fun getFeatureFolder(featureId: String): Path

    /**
     * Devuelve la ruta (Path) del subdirectorio de configuración de una feature específica.
     */
    fun getFeatureConfigFolder(featureId: String): Path

    /**
     * Devuelve la ruta (Path) del subdirectorio de datos (contenido generado) de una feature específica.
     */
    fun getFeatureDataFolder(featureId: String): Path
}
