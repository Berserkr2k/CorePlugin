package com.github.berserkr2k.coreplugin.api.item

import org.bukkit.Material

/**
 * Fábrica responsable de instanciar el ItemBuilder correcto
 * dependiendo de la versión del servidor.
 */
interface ItemFactory {

    /**
     * Inicia la creación de un nuevo ítem.
     * @param material El material base de Bukkit.
     * @return Una instancia de ItemBuilder lista para ser configurada.
     */
    fun create(material: Material): ItemBuilder
}