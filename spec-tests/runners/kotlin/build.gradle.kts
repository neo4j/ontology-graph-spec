plugins { kotlin("jvm") }

repositories { mavenCentral() }

dependencies {
    testImplementation(project(":"))
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.serializer.json)
    testImplementation(libs.json.schema.validator)
}

tasks.test {
    useJUnitPlatform()
    val cases = layout.projectDirectory.dir("../../cases")
    inputs.dir(cases)
    systemProperty("specTests.cases", cases.asFile.path)
    systemProperty("specTests.schema", rootProject.file("ontology-graph-spec.schema.json").path)
    // With -PspecTests.update=true the expected files are rewritten instead of asserted.
    val update = providers.gradleProperty("specTests.update").getOrElse("false")
    inputs.property("specTests.update", update)
    systemProperty("specTests.update", update)
}
