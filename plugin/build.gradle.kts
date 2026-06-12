plugins {
    alias(libs.plugins.shadow)
}

base {
    archivesName.set("CorePlugin")
}

dependencies {
    implementation(project(":core-api"))
    implementation(project(":core-platform-api"))
    implementation(project(":core-platform-paper"))
    implementation(project(":core-infra"))
    implementation(project(":core-feature-economy"))
    implementation(project(":core-feature-warps"))
    implementation(project(":core-feature-holograms"))
    implementation(project(":core-feature-leaderboard"))
    implementation(project(":core-feature-kits"))
    implementation(project(":core-feature-trails"))
    implementation(project(":core-feature-shop"))
    implementation(project(":core-feature-utility"))
    implementation(project(":core-feature-chat"))
    implementation(project(":core-feature-scoreboard"))
    implementation(project(":core-feature-regions"))
    implementation(project(":core-feature-spawn"))

    compileOnly(libs.paper.modern)
    implementation(libs.cloud.paper)
    compileOnly(libs.protocollib.modern)
    compileOnly("me.clip:placeholderapi:2.12.2")
    compileOnly("net.milkbowl.vault:VaultUnlockedAPI:2.16")

    // ArchUnit for testing modular boundaries
    testImplementation(libs.paper.modern)
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

tasks {
    jar {
        enabled = false
    }

    shadowJar {
        archiveClassifier.set("")

        val prefix = "com.github.berserkr2k.coreplugin.libs"
        relocate("com.zaxxer.hikari", "$prefix.hikari")
        relocate("org.postgresql", "$prefix.postgresql")
        relocate("org.spongepowered.configurate", "$prefix.configurate")
        relocate("org.incendo.cloud", "$prefix.cloud")
        relocate("kotlinx.coroutines", "$prefix.coroutines")
    }

    test {
        useJUnitPlatform()
    }

    build {
        dependsOn(shadowJar)
    }
}