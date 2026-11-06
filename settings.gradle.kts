pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

rootProject.name = "ontology-graph-spec"

includeBuild("build-logic")

// Pure-Java artifact generated from the Kotlin sources, see java/README.md
include(":java:generator", ":java:library", ":java:consumer-test")
