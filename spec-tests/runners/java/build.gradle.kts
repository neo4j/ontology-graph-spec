plugins { java }

// The local publication by default, or a published version with -Pogs.version=<version>.
val ogsVersion = providers.gradleProperty("ogs.version").getOrElse("0.0.0-local")

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation("org.neo4j.spec.graph:ontology-graph-spec:$ogsVersion")
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.20.0")
}

tasks.test {
    useJUnitPlatform()
    val cases = layout.projectDirectory.dir("../../cases")
    inputs.dir(cases)
    systemProperty("specTests.cases", cases.asFile.path)
}
