plugins {
    `java-library`
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.spotless)
}

group = rootProject.group
version = rootProject.version

repositories { mavenCentral() }

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
    options.encoding = "UTF-8"
}

// Sources live directly under java/src/main (no java/ subdirectory).
sourceSets.main { java.setSrcDirs(listOf("src/main")) }

dependencies {
    api(libs.jackson.databind)
    api(libs.jackson.dataformat.yaml)
}

tasks.jar {
    manifest { attributes("Automatic-Module-Name" to "org.neo4j.spec.graph.java") }
}

mavenPublishing {
    publishToMavenCentral()
    val inCi = providers.environmentVariable("CI").map { it.isNotBlank() && it != "false" }.getOrElse(false)
    val signingConfigured =
        listOf("signingInMemoryKey", "signing.keyId").any {
            providers.gradleProperty(it).map(String::isNotBlank).getOrElse(false)
        }
    if (inCi || signingConfigured) {
        signAllPublications()
    }
    coordinates(group.toString(), "ontology-graph-spec-java", version.toString())
    pom {
        name = "ontology-graph-spec-java"
        description = "Uniform Graph Specification Library for Neo4j (Java)"
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

spotless {
    java {
        target("src/**/*.java")
        palantirJavaFormat()
        removeUnusedImports()
        importOrder()
        trimTrailingWhitespace()
        endWithNewline()
        licenseHeaderFile(rootProject.file("build-logic/license-header.txt"), "package ")
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
        endWithNewline()
    }
}
