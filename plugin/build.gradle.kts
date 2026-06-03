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
    compileOnly("net.milkbowl.vault:VaultUnlockedAPI:2.16")

    // Sponge Configurate HOCON asíncrono e Incendo Cloud v2 para comandos nativos
    implementation(libs.configurate.hocon)
    implementation(libs.cloud.paper)

    // Bases de datos asíncronas
    implementation(libs.hikaricp)
    implementation(libs.postgresql.jdbc)
    compileOnly(libs.sqlite.jdbc)
}

tasks {
    shadowJar {
        archiveClassifier.set("")

        val prefix = "com.github.berserkr2k.coreplugin.libs"
        relocate("com.zaxxer.hikari", "$prefix.hikari")
        relocate("org.postgresql", "$prefix.postgresql")
    }

    build {
        dependsOn(shadowJar)
    }
}