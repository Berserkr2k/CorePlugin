dependencies {
    implementation(project(":core-api"))
    implementation(project(":core-platform-api"))
    implementation(project(":core-infra"))
    compileOnly(libs.paper.modern)
    compileOnly(libs.protocollib.modern)
    compileOnly(libs.configurate.hocon)
    compileOnly(libs.placeholderapi)
    compileOnly("net.milkbowl.vault:VaultUnlockedAPI:2.16")
}
