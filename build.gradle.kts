plugins {
    id("java")
}

subprojects {
    apply(plugin = "java")

    group = "top.worldme"
    version = "1.0.0"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.momirealms.net/releases/")
    }

    dependencies{
        compileOnly("io.papermc.paper:paper-api:26.2.build.+")
    }

    tasks.withType<Jar> {
        // 用子项目名作为 jar 名，去掉默认版本号
        archiveFileName.set("Worldme-${project.name}-${project.version}.jar")
        destinationDirectory.set(rootProject.layout.projectDirectory.dir("out"))
    }
}


tasks.jar {
    enabled = false
}

tasks.withType<Jar> {
    enabled = false
}

tasks.clean {
    delete(layout.projectDirectory.dir("out"))
}



