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
    
    compileOnly(libs.protocollib.legacy)

    compileOnly("me.clip:placeholderapi:2.12.2")

    // Aquí está la magia: importamos nuestros propios submódulos
    implementation(project(":core-api"))
    implementation(project(":core-1_8_R3"))
    implementation(project(":core-1_21_R3"))

    // Librerías de Adventure para inicializar el BukkitAudiences
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