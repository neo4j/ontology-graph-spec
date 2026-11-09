pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

rootProject.name = "ontology-graph-spec"

includeBuild("build-logic")

// Reference spec-tests runner, built against this project.
include("spec-tests-kotlin")
project(":spec-tests-kotlin").projectDir = file("spec-tests/runners/kotlin")
