/*
 * Uses the pure-Java library the way a customer would: a plain Java project depending on the built artifact, with no
 * Kotlin anywhere on its classpath. Not published.
 */
plugins { java }

repositories { mavenCentral() }

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

tasks.withType<JavaCompile>().configureEach {
    // The library targets Java 11, so its consumers can too
    options.release.set(11)
}

dependencies {
    testImplementation(project(":java:library"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

tasks.test {
    useJUnitPlatform()
    val library = project(":java:library").tasks.named<Jar>("jar")
    dependsOn(library)
    systemProperty("libraryJar", library.get().archiveFile.get().asFile.absolutePath)
    val fixture = rootProject.file("src/jvmTest/resources/org/neo4j/spec/graph/migrate/migration/dataModel/graph-data-model-3.0.0.json")
    inputs.file(fixture)
    systemProperty("dataModelFixture", fixture.absolutePath)
}
