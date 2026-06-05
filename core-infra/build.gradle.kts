dependencies {
    implementation(project(":core-api"))
    compileOnly(libs.paper.modern)
    compileOnly(libs.placeholderapi)
    implementation(libs.configurate.hocon)
    implementation(libs.hikaricp)
    implementation(libs.postgresql.jdbc)
    compileOnly(libs.sqlite.jdbc)
}
