package org.neo4j.spec.graph.javagen

import com.intellij.psi.PsiElement
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.psi.KtFile
import java.nio.file.Files
import java.nio.file.Path
import javax.lang.model.element.Modifier

/**
 * Collects the Java types produced from every Kotlin file and writes them out.
 */
class Generator(private val sourceRoots: List<Path>) {
    private val types = linkedMapOf<ClassName, TypeSpec.Builder>()
    private val headers = mutableMapOf<ClassName, String>()
    val problems = mutableListOf<String>()

    fun translate(files: List<KtFile>) {
        for (file in files) {
            val relative = relativePath(file)
            if (relative in Config.excludedFiles) continue
            analyze(file) {
                FileTranslator(this, this@Generator, file).translate()
            }
        }
    }

    fun relativePath(file: KtFile): String {
        val path = Path.of(file.virtualFilePath)
        val root = sourceRoots.firstOrNull { path.startsWith(it) } ?: return path.toString()
        return root.relativize(path).toString().replace('\\', '/')
    }

    fun problem(element: PsiElement?, message: String) {
        val file = element?.containingFile as? KtFile
        val location = if (file != null && element != null) {
            val line = file.viewProvider.document?.getLineNumber(element.textOffset)?.plus(1)
            "${relativePath(file)}:$line"
        } else {
            "?"
        }
        problems.add("$location: $message")
    }

    fun addType(name: ClassName, type: TypeSpec.Builder, header: String) {
        check(name !in types) { "Duplicate Java type $name" }
        types[name] = type
        headers[name] = header
    }

    /** Facade classes holding top-level declarations, shared between files with the same `@file:JvmName`. */
    fun facade(name: ClassName, header: String): TypeSpec.Builder = types.getOrPut(name) {
        headers[name] = header
        TypeSpec.classBuilder(name)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build())
    }

    fun write(out: Path) {
        if (Files.exists(out)) out.toFile().deleteRecursively()
        for ((name, builder) in types) {
            JavaFile.builder(name.packageName(), builder.build())
                .addFileComment(headers.getValue(name))
                .skipJavaLangImports(true)
                .indent("    ")
                .build()
                .writeTo(out)
        }
    }

    val typeCount get() = types.size
}
