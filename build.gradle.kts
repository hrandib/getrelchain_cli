import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.31"
    application
}

group = "me.dmytro"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.json:json:20201115")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClass.set("MainKt")
}

tasks {
    withType<Jar> {
        manifest {
            attributes["Main-Class"] = application.mainClass
        }
        // here zip stuff found in runtimeClasspath:
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    }
}