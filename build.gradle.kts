plugins {
    java
    id("net.neoforged.moddev") version "2.0.80"
}

version = property("mod_version")!!
group = "ua.ivan.sableccticksync"

base {
    archivesName.set("sable-cc-tick-sync")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
}

neoForge {
    version = property("neo_version") as String
    mods {
        create("sable_cc_tick_sync") {
            sourceSet(sourceSets.main.get())
        }
    }
    runs {
        create("server") {
            server()
            gameDirectory = file("run")
            programArgument("--nogui")
        }
    }
}

dependencies {
    compileOnly(files("../_deps_sable_2_0_3/sable-neoforge-1.21.1-2.0.3.jar"))
    compileOnly(files("../_deps_sable/META-INF/jarjar/sable-companion-common-1.21.1-1.6.0.jar"))
    compileOnly(files("libs/cc-tweaked-1.21.1-forge-1.120.0.jar"))

    runtimeOnly(files("../_deps_sable_2_0_3/sable-neoforge-1.21.1-2.0.3.jar"))
    runtimeOnly(files("libs/cc-tweaked-1.21.1-forge-1.120.0.jar"))
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to project.version)
    }
}
