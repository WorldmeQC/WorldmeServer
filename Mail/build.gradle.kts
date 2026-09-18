dependencies {
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")
    compileOnly("net.momirealms:craft-engine-core:26.7.4")
    compileOnly("net.momirealms:craft-engine-bukkit:26.7.4")
}

tasks.jar {
    // 手动 fat jar：将 sqlite-jdbc 打进插件包，保证自包含
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().files.map { file: File -> if (file.isFile) zipTree(file) else file })
}