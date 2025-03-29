plugins {
    id("fabric-loom") version "1.10-SNAPSHOT"
    id("maven-publish")
    id("org.ajoberstar.grgit") version "5.2.2"
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
}

version = if (project.property("archives_preview_version")?.toString()?.isEmpty() != false) {
    "${project.property("mod_version")}+${grgit.branch.current().name}" // usually branch name is the mc version
} else {
    "${project.property("mod_version")}-${project.property("archives_preview_version")}+${grgit.branch.current().name}-${
        grgit.head().id.substring(
            0,
            8
        )
    }"
}
group = project.property("maven_group") as String

repositories {
    mavenCentral()
    maven { url = uri("https://maven.fabricmc.net/") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
    }
    maven { url = uri("https://maven.shedaniel.me") }

//        maven {
//            name = "Reden"
//            url = ("https://maven.starlight.cool/artifactory/reden")
//        }
}

base {
    archivesName.set(project.property("archives_base_name") as String)
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    inputs.property("version", project.version)

    from("src/main/resources") {
        include("fabric.mod.json")
        expand("version" to project.version.toString())
    }
}

dependencies {
    // Minecraft
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    // Fabric API
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")
    // Kotlin support
    modImplementation("net.fabricmc:fabric-language-kotlin:1.11.0+kotlin.2.0.0")

    // SnakeYAML. To convert the old config
    include(modImplementation("org.yaml:snakeyaml:1.33")!!)

    // Fabric Permissions API. To check the permissions
    include(modImplementation("me.lucko:fabric-permissions-api:0.2-SNAPSHOT")!!)
    // REI. To avoid the REI GUI overlap with the Enclosure GUI
    modCompileOnly("me.shedaniel:RoughlyEnoughItems-api-fabric:${project.property("rei_version")}")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
    }
    jvmToolchain(21)
}

java {
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

loom {
    accessWidenerPath.set(file("src/main/resources/enclosure.accesswidener"))
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${base.archivesName.get()}" }
    }
}

// configure the maven publication
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}