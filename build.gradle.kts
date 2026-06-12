import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}

val rootLibs = libs

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")

    group = "com.github.berserkr2k.coreplugin" // Usando tu alias de Github
    version = "1.0.1"

    afterEvaluate {
        configure<PublishingExtension> {
            publications {
                create<MavenPublication>("maven") {
                    from(components["java"])
                }
            }
        }
    }

    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://repo.papermc.io/repository/maven-public/")

        maven("https://repo.dmulloy2.net/repository/public/")

        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://jitpack.io")
        maven("https://repo.codemc.io/repository/creatorfromhell/")
    }

    // --- LÓGICA DE COMPILACIÓN MODERNA ---
    // Target Java 21 / JVM 21 para Minecraft 26.1+ (Paper/Folia)
    val kotlinTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    val javaTarget = "21"

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(kotlinTarget)
        }
    }

    tasks.withType<JavaCompile> {
        sourceCompatibility = javaTarget
        targetCompatibility = javaTarget
    }

    dependencies {
        add("compileOnly", rootLibs.paper.modern)
        add("compileOnly", rootLibs.configurate.hocon)
        add("implementation", rootLibs.coroutines.core)
    }
}