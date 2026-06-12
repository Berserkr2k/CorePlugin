dependencies {
    implementation(project(":core-api"))
    compileOnly(libs.paper.modern)
    implementation(libs.cloud.paper)
    compileOnly("net.milkbowl.vault:VaultUnlockedAPI:2.16")
    compileOnly(libs.placeholderapi)
}
