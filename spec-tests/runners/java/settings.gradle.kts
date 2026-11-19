rootProject.name = "ogs-spec-tests-java"

// The library under test is built from this checkout by default. With -Pogs.version=<version> a published (or
// mavenLocal) version is used instead.
if (!providers.gradleProperty("ogs.version").isPresent) {
    includeBuild("../../..") {
        dependencySubstitution {
            substitute(module("org.neo4j.spec.graph:ontology-graph-spec-java")).using(project(":java"))
        }
    }
}
