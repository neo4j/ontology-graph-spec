/*
 * Pure-Java distribution of the library: no Kotlin bytecode and Jackson as the only dependency.
 * Most sources are generated from src/commonMain/kotlin by :java:generator, the rest is hand-written in src/main/java.
 */
plugins {
    `java-library`
}

repositories {
    mavenCentral()
    // Analysis API builds used by :java:generator are only published here
    maven("https://redirector.kotlinlang.org/maven/intellij-dependencies") {
        content { includeVersionByRegex("org\\.jetbrains\\.kotlin", ".*", ".*-ij.*") }
    }
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
    options.encoding = "UTF-8"
}

val javagen: Configuration by configurations.creating {
    isCanBeConsumed = false
}

// Libraries the Kotlin sources compile against, so the generator can resolve their calls and types
val kotlinSourceClasspath: Configuration by configurations.creating {
    isCanBeConsumed = false
    isTransitive = false
}

dependencies {
    javagen(project(":java:generator"))
    kotlinSourceClasspath("org.jetbrains.kotlin:kotlin-stdlib:${libs.versions.kotlin.asProvider().get()}")
    kotlinSourceClasspath("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:${libs.versions.kotlinx.serialization.get()}")
    kotlinSourceClasspath("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:${libs.versions.kotlinx.serialization.get()}")
    kotlinSourceClasspath("net.mamoe.yamlkt:yamlkt-jvm:0.13.0")

    api(libs.jackson.databind)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

val generatedJava = layout.buildDirectory.dir("generated/java")

val generateJava by tasks.registering(JavaExec::class) {
    description = "Translates the Kotlin common sources into Java"
    group = "build"
    val sources = rootProject.layout.projectDirectory.dir("src/commonMain/kotlin")
    inputs.dir(sources)
    inputs.files(kotlinSourceClasspath)
    outputs.dir(generatedJava)
    classpath = javagen
    mainClass.set("org.neo4j.spec.graph.javagen.MainKt")
    jvmArgs("-Xmx2g")
    argumentProviders.add(
        CommandLineArgumentProvider {
            listOf(
                "--sources", sources.asFile.absolutePath,
                "--classpath", kotlinSourceClasspath.asPath,
                "--out", generatedJava.get().asFile.absolutePath,
                // Kotlin the generator can't translate fails the build rather than generating a placeholder
                "--strict", "true"
            )
        }
    )
}

sourceSets.main { java.srcDir(generateJava) }

tasks.test {
    useJUnitPlatform()
    val root = rootProject.layout.projectDirectory
    inputs.dir(root.dir("conformance"))
    systemProperty("conformanceRoot", root.asFile.absolutePath)
}
