package com.github.berserkr2k.coreplugin.infrastructure.anvil

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class AnvilConfig(
    val blacklistWords: List<String> = listOf("exploit", "crash", "hack", "bypass"),
    val minRepairCost: Int = 1,
    val permissionBypass: String = "core.anvil.bypass",
    val permissionColor: String = "core.anvil.color",
    val permissionStyle: String = "core.anvil.style"
)
