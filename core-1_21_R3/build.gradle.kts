dependencies {
    // 1. La API de Spigot 1.21.1 (compileOnly para no pesar en el .jar)
    compileOnly(libs.paper.modern)

    // 2. Nuestro módulo de interfaces
    implementation(project(":core-api"))

    // 3. Librerías de Adventure
    implementation(libs.adventure.api)
    implementation(libs.adventure.bukkit)
    implementation(libs.adventure.minimessage)

    // 4.0 Añadimos ProtocolLib 5.4.0 (Exclusiva de Java 17+)
    compileOnly(libs.protocollib.modern)
}

// ¡OJO! Aquí no ponemos el compilerOptions para Java porque
// automáticamente hereda el Java 21 que configuramos en la raíz.