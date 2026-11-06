/*
 * Build-time tool that translates the Kotlin common sources into Java source for java/library.
 * Uses the Kotlin Analysis API in standalone mode, which JetBrains only publishes to its own repository.
 * Nothing here is published or ends up on the runtime classpath of any distribution.
 */
plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

repositories {
    mavenCentral()
    // Analysis API builds used by :java:generator are only published here
    maven("https://redirector.kotlinlang.org/maven/intellij-dependencies") {
        content { includeVersionByRegex("org\\.jetbrains\\.kotlin", ".*", ".*-ij.*") }
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        listOf("KaExperimentalApi", "KaImplementationDetail", "KaIdeApi", "KaNonPublicApi", "KaPlatformInterface")
            .forEach { optIn.add("org.jetbrains.kotlin.analysis.api.$it") }
    }
}

dependencies {
    implementation(libs.javapoet)
    // Analysis API artifacts are published without usable transitive metadata (KTIJ-19820)
    listOf<Provider<MinimalExternalModuleDependency>>(libs.kotlin.analysis.api.api, libs.kotlin.analysis.api.standalone, libs.kotlin.analysis.compiler.k2).forEach {
        implementation(it) { isTransitive = false }
    }
    listOf<Provider<MinimalExternalModuleDependency>>(
        libs.kotlin.analysis.api.impl,
        libs.kotlin.analysis.api.k2,
        libs.kotlin.analysis.low.level.fir,
        libs.kotlin.analysis.api.platform,
        libs.kotlin.analysis.light.classes,
        libs.kotlin.analysis.common
    ).forEach {
        runtimeOnly(it) { isTransitive = false }
    }
    runtimeOnly("com.github.ben-manes.caffeine:caffeine:2.9.3")
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.2")
    // The IntelliJ platform reads its plugin XML with kotlinx-serialization
    runtimeOnly(libs.kotlinx.serializer.json)
}

application { mainClass.set("org.neo4j.spec.graph.javagen.MainKt") }
