import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version: String by project

plugins {
    kotlin("jvm") version "2.4.0"
    kotlin("plugin.serialization") version "2.4.0"
    id("net.fabricmc.fabric-loom") version "1.16-SNAPSHOT"
    id("maven-publish")
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

val targetJavaVersion = 25
java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
    maven("https://api.modrinth.com/maven")
    maven("https://maven.nucleoid.xyz/") { name = "Nucleoid" }
}

val shade: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")

    implementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    implementation("net.fabricmc:fabric-language-kotlin:${project.property("kotlin_loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_api_version")}")

    implementation("eu.pb4:placeholder-api:3.1.0-beta.1+26.2")
    implementation("me.lucko:fabric-permissions-api:0.7.0")

    shade("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
    shade("io.ktor:ktor-client-core:$ktor_version")
    shade("io.ktor:ktor-client-cio:$ktor_version")
    shade("io.ktor:ktor-client-content-negotiation:$ktor_version")
    shade("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    shade("io.ktor:ktor-client-logging:$ktor_version")
    shade("io.ktor:ktor-client-resources:$ktor_version")
    shade("io.ktor:ktor-client-json:$ktor_version")
}

configurations.implementation.get().extendsFrom(shade)

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", project.property("minecraft_version"))
    inputs.property("loader_version", project.property("loader_version"))
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to project.property("minecraft_version")!!,
            "loader_version" to project.property("loader_version")!!,
            "kotlin_loader_version" to project.property("kotlin_loader_version")!!
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName}" }
    }

    from(shade.map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/**")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.property("archives_base_name") as String
            from(components["java"])
        }
    }
    repositories {}
}

tasks.register("listConfigs") {
    doLast {
        configurations.forEach { println(it.name) }
    }
}
