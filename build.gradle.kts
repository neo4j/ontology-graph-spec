import org.gradle.api.tasks.JavaExec
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

plugins {
    id("tasks.ts.modifier")
    alias(libs.plugins.spotless)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.js.plain.objects)
    alias(libs.plugins.maven.publish)
}

group = "org.neo4j.spec.graph"

repositories { mavenCentral() }

kotlin {
    jvm {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
        val mainCompilation = compilations.getByName("main")
        // Build-only tooling used to generate JSON schema. Should not be part of the runtime dependencies
        compilations.create("schemaTool") {
            associateWith(mainCompilation)
            defaultSourceSet.dependencies { implementation(libs.kotlinx.schema.generator.json) }
        }
        testRuns.named("test") {
            executionTask.configure {
                useJUnitPlatform()
                // Read by SpecVersionConsistencyTest, so that a change to either reruns the tests.
                inputs.file("ontology-graph-spec.schema.json")
                inputs.property(
                    "releaseSpecTags",
                    providers
                        .exec {
                            commandLine("git", "tag", "-l", "release-spec/v*")
                            isIgnoreExitValue = true
                        }.standardOutput.asText
                )
            }
        }
    }
    js(IR) {
        binaries.library()
        compilations.named("main") {
            packageJson {
                name = "@neo4j/ontology-graph-spec"
                customField(
                    "repository",
                    mapOf(
                        "type" to "git",
                        "url" to "https://github.com/neo4j/ontology-graph-spec"
                    )
                )
            }
        }
        nodejs {
            testTask {
                useMocha()
            }
        }
        compilerOptions {
            sourceMap = true
            moduleKind = JsModuleKind.MODULE_ES
            freeCompilerArgs.add("-opt-in=kotlin.js.ExperimentalWasmJsInterop")
            freeCompilerArgs.add("-Xes-long-as-bigint")
        }
        generateTypeScriptDefinitions()
    }
    macosArm64 {
        binaries.sharedLib { baseName = "graphdatamodel" }
    }
    linuxX64 {
        binaries.sharedLib { baseName = "graphdatamodel" }
    }
    linuxArm64 {
        binaries.sharedLib { baseName = "graphdatamodel" }
    }

    applyDefaultHierarchyTemplate()
    // Override target source sets for KMP
    sourceSets {
        val commonMain by getting
        val commonTest by getting
        val bridge by creating {
            dependsOn(commonMain)
        }
        val bridgeTest by creating {
            dependsOn(commonTest)
        }
        targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
            compilations.getByName("main").defaultSourceSet.dependsOn(bridge)
            compilations.getByName("test").defaultSourceSet.dependsOn(bridgeTest)
        }

        commonMain.dependencies {
            implementation(libs.kotlinx.serializer.json)
            implementation(libs.kotlinx.yamlkt)
        }
        jsMain.dependencies {
            implementation(devNpm("typescript", "5.9.3"))
            implementation(libs.kotlin.js.plain.objects)
            implementation(libs.kotlin.wrappers.ts)
        }
        commonTest.dependencies { implementation(libs.kotlin.test) }
    }

    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.js.ExperimentalJsExport")
        freeCompilerArgs.add("-opt-in=kotlin.js.ExperimentalJsStatic")
    }

    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
        freeCompilerArgs.add("-opt-in=kotlin.native.ExperimentalNativeApi")
    }
}

tasks.withType<Kotlin2JsCompile>().configureEach {
    compilerOptions {
        target = "es2015"
    }
}

val copyReadmeToJs by tasks.registering(Copy::class) {
    from(rootProject.file("README.md"))
    into(layout.buildDirectory.dir("dist/js/productionLibrary"))
}

/*
    Kotlin/JS doesn't support TypeScript unions
    https://youtrack.jetbrains.com/issue/KT-55101/
    This script modifies the generated types and generates a string union given a basic enum.
    It's a somewhat brittle hack but the type safety is much preferred on the frontend.
    There's the potential to use a different library for TS generation in the future which does support this natively.
 */
tasks.register("generateTsUnions", TypeScriptModifierTask::class.java) {
    dependsOn(copyReadmeToJs)
    typescriptFile =
        layout.buildDirectory
            .dir("dist/js/productionLibrary/")
            .get()
            .file("ontology-graph-spec.d.mts")
            .asFile
}

tasks.named("jsProductionLibraryCompileSync") {
    finalizedBy(copyReadmeToJs)
}

tasks.named("jsNodeProductionLibraryDistribution") {
    finalizedBy("generateTsUnions")
}

tasks.register<JavaExec>("generateGraphModelJsonSchema") {
    description = "Writes JSON Schema for GraphModel Go type generation"
    val compilation = kotlin.jvm().compilations.getByName("schemaTool")
    dependsOn(compilation.compileTaskProvider)
    classpath = compilation.output.classesDirs + compilation.runtimeDependencyFiles
    mainClass.set("org.neo4j.spec.graph.schema.GenerateGraphModelJsonSchemaKt")
    workingDir = layout.projectDirectory.asFile
    doFirst {
        val outputFile = layout.projectDirectory.file("ontology-graph-spec.schema.json").asFile
        outputFile.parentFile.mkdirs()
        args(outputFile.absolutePath)
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(group.toString(), "ontology-graph-spec", version.toString())
    pom {
        name = "ontology-graph-spec"
        description = "Uniform Graph Specification Library for Neo4j"
        url = "https://github.com/neo4j/ontology-graph-spec"
        inceptionYear = "2026"
        organization {
            name = "Neo4j, Neo4j Sweden AB"
            url = "https://neo4j.com"
        }
        licenses {
            license {
                name = "Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "manual"
            }
        }
        developers {
            developer {
                id = "team-data-importer"
                name = "Data Importer Team"
                organization = "Neo4j"
                organizationUrl = "https://neo4j.com"
            }
            developer {
                id = "team-connectors"
                name = "Connectors Team"
                organization = "Neo4j"
                organizationUrl = "https://neo4j.com"
            }
        }
        scm {
            connection = "scm:git:git://github.com/neo4j/ontology-graph-spec.git"
            developerConnection = "scm:git:git@github.com:neo4j/ontology-graph-spec.git"
            url = "https://github.com/neo4j/ontology-graph-spec"
        }
    }
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlin {
        ktlint().editorConfigOverride(
            mapOf("code_style" to "intellij_idea", "ktlint_standard_no-unused-imports" to "enabled")
        )
        endWithNewline()
        licenseHeaderFile(rootProject.file("build-logic/license-header.txt"))
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
        endWithNewline()
    }
    kotlin {
        target(
            project.fileTree("src/commonMain/kotlin"),
            project.fileTree("src/commonTest/kotlin"),
            project.fileTree("src/jsMain/kotlin"),
            project.fileTree("src/bridge/kotlin"),
            project.fileTree("src/bridgeTest/kotlin")
        )
    }
}
