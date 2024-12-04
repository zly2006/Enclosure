plugins {
    `maven-publish`
    id("fabric-loom")
     kotlin("jvm")
    kotlin("plugin.serialization")
    id("dev.kikugie.j52j")
    id("me.modmuss50.mod-publish-plugin")
}

class ModData {
    val id = property("mod.id").toString()
    val name = property("mod.name").toString()
    val version = property("mod.version").toString()
    val group = property("mod.group").toString()
}

class ModDependencies {
    operator fun get(name: String) = property("deps.$name").toString()
}

val mod = ModData()
val deps = ModDependencies()
val mcVersion = stonecutter.current.version
val mcDep = property("mod.mc_dep").toString()

version = "${mod.version}+$mcVersion"
group = mod.group
base { archivesName.set(mod.id) }

loom {
    accessWidenerPath = rootProject.file("src/main/resources/enclosure.accesswidener")
}

repositories {
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    mavenCentral()
    strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://maven.shedaniel.me")
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings("net.fabricmc:yarn:$mcVersion+build.${deps["yarn_build"]}:v2")
    modImplementation("net.fabricmc:fabric-loader:${deps["fabric_loader"]}")
//    modRuntimeOnly "maven.modrinth:luckperms:v5.4.113-fabric"
    include(modImplementation("org.yaml:snakeyaml:1.33")!!)

    // Fabric Permissions API. To check the permissions
    include(modImplementation("me.lucko:fabric-permissions-api:0.2-SNAPSHOT")!!)
    // REI. To avoid the REI GUI overlap with the Enclosure GUI
    modCompileOnly("me.shedaniel:RoughlyEnoughItems-api-fabric:${project.property("rei_version")}") {
        exclude(group = "net.fabricmc.fabric-api")
        exclude(group = "me.shedaniel.cloth")
        exclude(group = "medev.architectury")
        exclude(group = "me.shedaniel")
    }
    fun fapi(vararg modules: String) = modules.forEach {
        modImplementation(fabricApi.module(it, deps["fabric_api"]))
    }

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.6.10")

    modImplementation("net.fabricmc:fabric-language-kotlin:${deps["kotlin_loader_version"]}")
    fapi(
        // Add modules from https://github.com/FabricMC/fabric
        "fabric-lifecycle-events-v1",
        "fabric-resource-loader-v0",
        "fabric-screen-handler-api-v1",
        "fabric-command-api-v2",
        "fabric-events-interaction-v0",
        "fabric-key-binding-api-v1",
        "fabric-rendering-v1",
        "fabric-entity-events-v1"
    )
}

loom {
    decompilers {
        get("vineflower").apply { // Adds names to lambdas - useful for mixins
            options.put("mark-corresponding-synthetics", "1")
        }
    }

    runConfigs.all {
        ideConfigGenerated(true)
        vmArgs("-Dmixin.debug.export=true")
        runDir = "../../run"
    }
}

val java =
    if (stonecutter.eval(mcVersion, ">=1.20.6")) 21
    else 17

java {
    withSourcesJar()
    targetCompatibility = JavaVersion.toVersion(java)
    sourceCompatibility = JavaVersion.toVersion(java)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
    }
    jvmToolchain(java)
}

tasks.processResources {
    inputs.property("id", mod.id)
    inputs.property("name", mod.name)
    inputs.property("version", mod.version)
    inputs.property("mcdep", mcDep)

    val map = mapOf(
        "id" to mod.id,
        "name" to mod.name,
        "version" to mod.version,
        "mcdep" to mcDep
    )

    filesMatching("fabric.mod.json") { expand(map) }
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    from(tasks.remapJar.get().archiveFile)
    into(rootProject.layout.buildDirectory.file("libs/${mod.version}"))
    dependsOn("build")
}

publishMods {
    file = tasks.remapJar.get().archiveFile
    displayName = "${mod.name} ${mod.version} for $mcVersion"
    version = "${mod.version}+$mcVersion"
    changelog = rootProject.file("CHANGELOG.md").readText()
    type = STABLE
    modLoaders.add("fabric")

//    dryRun = providers.environmentVariable("MODRINTH_TOKEN")
//        .getOrNull() == null || providers.environmentVariable("CURSEFORGE_TOKEN").getOrNull() == null

//    modrinth {
//        projectId = property("publish.modrinth").toString()
//        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
//        minecraftVersions.add(mcVersion)
//        requires {
//            slug = "fabric-api"
//        }
//    }

//    curseforge {
//        projectId = property("publish.curseforge").toString()
//        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
//        minecraftVersions.add(mcVersion)
//        requires {
//            slug = "fabric-api"
//        }
//    }
}

/*
publishing {
    repositories {
        maven("...") {
            name = "..."
            credentials(PasswordCredentials::class.java)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "${property("mod.group")}.${mod.id}"
            artifactId = mod.version
            version = mcVersion

            from(components["java"])
        }
    }
}
*/
