plugins {
    // Aplicamos el plugin de Shadow que acabamos de registrar
    alias(libs.plugins.shadow)
}

base {
    archivesName.set("CorePlugin")
}

dependencies {
    // Usamos la API moderna como base para compilar
    compileOnly(libs.paper.modern)
    
    // ProtocolLib moderna y PlaceholderAPI
    compileOnly(libs.protocollib.modern)
    compileOnly("me.clip:placeholderapi:2.12.2")

    // Sponge Configurate HOCON asíncrono e Incendo Cloud v2 para comandos nativos
    implementation(libs.configurate.hocon)
    implementation(libs.cloud.paper)

    // Librerías de Adventure para inicializar el BukkitAudiences (si es necesario) y Kyori
    implementation(libs.adventure.api)
    implementation(libs.adventure.bukkit)
    implementation(libs.adventure.minimessage)

    // Bases de datos asíncronas
    implementation(libs.hikaricp)
    implementation(libs.sqlite.jdbc)
}

tasks {
    shadowJar {
        archiveClassifier.set("")

        val prefix = "com.github.berserkr2k.coreplugin.libs"
        relocate("net.kyori", "$prefix.kyori")
        relocate("com.zaxxer.hikari", "$prefix.hikari")
    }

    build {
        dependsOn(shadowJar)
    }
}