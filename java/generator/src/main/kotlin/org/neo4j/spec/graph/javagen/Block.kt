package org.neo4j.spec.graph.javagen

import com.squareup.javapoet.CodeBlock

/** Java operator precedence, higher binds tighter. */
object Prec {
    const val PRIMARY = 100
    const val UNARY = 90
    const val MUL = 80
    const val ADD = 70
    const val REL = 60
    const val EQ = 55
    const val AND = 35
    const val OR = 30
    const val TERNARY = 20
    const val LAMBDA = 10
    const val ASSIGN = 5
}

/**
 * A translated Java expression.
 * [statement] marks expressions that are valid as Java expression statements (calls, assignments, `new`).
 * [simple] marks side effect free references (locals, `this`, literals) which can be evaluated more than once.
 */
class JExpr(
    val code: CodeBlock,
    val prec: Int,
    val statement: Boolean = false,
    val simple: Boolean = false,
) {
    fun wrap(min: Int): CodeBlock = if (prec >= min) code else CodeBlock.of("(\$L)", code)

    override fun toString() = code.toString()

    companion object {
        fun of(format: String, vararg args: Any?, prec: Int = Prec.PRIMARY, statement: Boolean = false, simple: Boolean = false) =
            JExpr(CodeBlock.of(format, *args), prec, statement, simple)

        fun call(format: String, vararg args: Any?) = JExpr(CodeBlock.of(format, *args), Prec.PRIMARY, statement = true)

        fun name(name: String) = JExpr(CodeBlock.of("\$N", name), Prec.PRIMARY, simple = true)

        val NULL = JExpr(CodeBlock.of("null"), Prec.PRIMARY, simple = true)
        val THIS = JExpr(CodeBlock.of("this"), Prec.PRIMARY, simple = true)
    }
}

/** Java local names in scope, so translated Kotlin names that would shadow (illegal in Java) get renamed. */
class Scope(private val parent: Scope?) {
    private val names = mutableSetOf<String>()

    fun has(name: String): Boolean = name in names || parent?.has(name) == true

    fun declare(base: String): String {
        val start = javaIdentifier(base)
        var name = start
        var i = 2
        while (has(name)) name = "$start${i++}"
        names.add(name)
        return name
    }

    fun child() = Scope(this)
}

/** A sequence of Java statements being built, with its own scope for local names. */
class Block(val scope: Scope) {
    val code: CodeBlock.Builder = CodeBlock.builder()

    fun add(format: String, vararg args: Any?) {
        code.add("$format;\n", *args)
    }

    fun addCode(block: CodeBlock) {
        code.add(block)
    }

    fun child() = Block(scope.child())

    val isEmpty get() = code.isEmpty

    fun build(): CodeBlock = code.build()
}

/** Where the value of a translated expression goes. */
sealed class Sink {
    object Discard : Sink()
    object Return : Sink()
    class Assign(val target: CodeBlock) : Sink()
}
