dependencies {
    implementation(project(":core-api"))
    compileOnly(libs.paper.modern)
    implementation(libs.cloud.paper)
    compileOnly(libs.protocollib.modern)
}
