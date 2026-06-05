dependencies {
    implementation(project(":core-api"))
    implementation(project(":core-platform-api"))
    implementation(project(":core-infra"))
    compileOnly(libs.paper.modern)
    implementation(libs.cloud.paper)
    compileOnly(libs.protocollib.modern)
}
