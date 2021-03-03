import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.file.Files.createSymbolicLink
import java.nio.file.Files.deleteIfExists

import java.nio.file.Path.of as withPath

plugins {
    kotlin("jvm") version "1.4.31"
    application
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    languageVersion = "1.4"
}

group = "me.dmytro"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.json:json:20201115")
}

application {
    mainClass.set("MainKt")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }
    withType<Jar> {
        manifest {
            attributes["Main-Class"] = application.mainClass
        }
        // here zip stuff found in runtimeClasspath:
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        //Convert jar file to the directly executable, without the need of 'java -jar' prefix
        doLast {
            File("$buildDir/libs/${project.name}")
                .let {
                    it.writeText("#!/usr/bin/java -jar\n")
                    it.appendBytes(archiveFile.get().asFile.readBytes())
                    it.setExecutable(true)
                }
        }
    }

    register("Link") {
        dependsOn(withType<Jar>())
        doLast {
            val homeDir = System.getenv("HOME");
            createSymbolicLink(
                withPath("${homeDir}/.local/bin/${project.name}"),
                withPath("$buildDir/libs/${project.name}")
            )
        }
    }
    register("Unlink") {
        doLast {
            val homeDir = System.getenv("HOME");
            deleteIfExists(withPath("${homeDir}/.local/bin/${project.name}"))
        }
    }
}
