package org.neo4j.spec.graph.javagen

import java.io.File
import java.nio.file.Path
import kotlin.system.exitProcess

/**
 * Usage: --sources <dirs> --classpath <jars> --out <dir> [--strict true]
 *
 * Translates the Kotlin sources into Java under --out. Constructs that can't be translated are listed and, with
 * --strict, fail the run; otherwise they become `Kt.todo(...)` calls so the rest can still be inspected.
 */
fun main(args: Array<String>) {
    val opts = args.toList().chunked(2).associate { (k, v) -> k.removePrefix("--") to v }
    val sources = opts.getValue("sources").split(File.pathSeparator).map { Path.of(it).toAbsolutePath() }
    val classpath = opts.getValue("classpath").split(File.pathSeparator).filter { it.isNotBlank() }.map { Path.of(it) }
    val out = Path.of(opts.getValue("out"))
    val strict = opts["strict"]?.toBoolean() ?: false
    val code = try {
        Session(sources, classpath).use { session ->
            val generator = Generator(sources)
            generator.translate(session.files)
            generator.write(out)
            println("Generated ${generator.typeCount} Java types into $out")
            if (generator.problems.isNotEmpty()) {
                System.err.println("${generator.problems.size} constructs couldn't be translated:")
                generator.problems.forEach { System.err.println("  $it") }
            }
            if (strict && generator.problems.isNotEmpty()) 1 else 0
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        1
    }
    // The IntelliJ platform leaves non-daemon threads behind
    exitProcess(code)
}
