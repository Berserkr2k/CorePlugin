import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = "com.github.berserkr2k.coreplugin" // Usando tu alias de Github
    version = "1.0.0"

    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://repo.papermc.io/repository/maven-public/")

        maven("https://repo.dmulloy2.net/repository/public/")

        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }

    // --- LÓGICA CENTRALIZADA DE VERSIONES ---
    // Si el módulo es el API o el de la 1.8.8, usamos Java 8. Para el resto, Java 21.
    val isLegacyModule = name == "core-api" || name == "core-1_8_R3"
    val kotlinTarget = if (isLegacyModule) org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8 else org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    val javaTarget = if (isLegacyModule) "1.8" else "21"

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(kotlinTarget)
        }
    }

    tasks.withType<JavaCompile> {
        sourceCompatibility = javaTarget
        targetCompatibility = javaTarget
    }
}