import org.jetbrains.kotlin.gradle.dsl.JvmTarget

dependencies {
    // 1. La API de Spigot 1.8.8 (compileOnly para no meterla dentro del .jar final)
    compileOnly(libs.spigot.legacy)

    // 2. Dependemos de nuestro propio módulo core-api para poder leer la interfaz
    implementation(project(":core-api"))

    // 3. Librerías de Adventure para la 1.8
    implementation(libs.adventure.api)
    implementation(libs.adventure.bukkit)
    implementation(libs.adventure.minimessage)

    // 4. Añadimos ProtocolLib 5.3.0 (Compatible con Java 8)
    compileOnly(libs.protocollib.legacy)
}

// Forzamos que este módulo específico se compile para Java 8
// Esto asegura que no explote si el servidor 1.8.8 usa un Java antiguo
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}
