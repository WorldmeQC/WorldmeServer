repositories {
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("me.clip:placeholderapi:2.11.6")
}

tasks.jar {
    // 手动 fat jar：将 sqlite-jdbc 打进插件包，保证自包含
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().files.map { file: File -> if (file.isFile) zipTree(file) else file })
}
