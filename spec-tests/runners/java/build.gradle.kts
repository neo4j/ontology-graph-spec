plugins { java }

repositories { mavenCentral() }

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

// The library under test: the Kotlin JVM target by default, or the native Java port with -Pogs.artifact=java.
val ogsArtifact = providers.gradleProperty("ogs.artifact").getOrElse("kotlin")

dependencies {
    when (ogsArtifact) {
        "kotlin" -> testImplementation(project(":"))
        "java" -> testImplementation(project(":java"))
        else -> error("Unknown -Pogs.artifact=$ogsArtifact, expected 'kotlin' or 'java'")
    }
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("tools.jackson.core:jackson-databind:3.3.0")
}

// Api.java adapts the differing entry points (GraphSpec.getJson() vs json(), Validations.getCore() vs core()).
sourceSets.test { java.srcDir("src/$ogsArtifact") }

tasks.test {
    useJUnitPlatform()
    val cases = layout.projectDirectory.dir("../../cases")
    inputs.dir(cases)
    inputs.property("ogs.artifact", ogsArtifact)
    systemProperty("specTests.cases", cases.asFile.path)
}
