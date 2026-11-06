package org.neo4j.spec.graph.javagen

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import org.jetbrains.kotlin.analysis.api.resolution.KaCallableMemberCall
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtPsiUtil

/**
 * Translates calls into the Kotlin standard library, keyed by the resolved callable id.
 *
 * Members of types Java has equivalents for become Java member calls, scope functions are inlined, and the remaining
 * extension functions call the hand-written `org.neo4j.spec.graph.internal.Kt` helpers of the same name.
 */
class Stdlib(private val t: BodyTranslator) {
    private val types = t.typeMapper

    private inner class Site(
        val element: KtExpression,
        val call: KaCallableMemberCall<*, *>,
        val id: String,
        val b: Block,
        val discard: Boolean,
        val assigned: KtExpression?,
    ) {
        val name = id.substringAfterLast('/').substringAfterLast('.')
        val owner = id.substringBeforeLast('.', "").substringAfterLast('/')
        val applied = call.partiallyAppliedSymbol

        private var receiverCache: JExpr? = null
        private var receiverDone = false

        /** The dispatch or extension receiver, translated once. */
        val receiver: JExpr?
            get() {
                if (!receiverDone) {
                    receiverCache = t.receiver(applied.dispatchReceiver, b) ?: t.receiver(applied.extensionReceiver, b)
                    receiverDone = true
                }
                return receiverCache
            }

        val receiverType: KaType? get() = (applied.dispatchReceiver ?: applied.extensionReceiver)?.type

        val receiverId: String get() = receiverType?.classIdString() ?: ""

        val fn = call as? KaFunctionCall<*>

        /** Argument expressions in parameter order (vararg parameters contribute several). */
        val argExpressions: List<Pair<KaValueParameterSymbol, KtExpression>> by lazy {
            val fc = fn ?: return@lazy emptyList()
            val order = fc.symbol.valueParameters
            fc.argumentMapping.entries.sortedBy { order.indexOf(it.value.symbol) }.map { it.value.symbol to it.key }
        }

        fun arg(i: Int): KtExpression? = argExpressions.getOrNull(i)?.second

        fun lambda(i: Int = argExpressions.lastIndex): KtLambdaExpression? =
            arg(i)?.let { KtPsiUtil.safeDeparenthesize(it) as? KtLambdaExpression }

        fun args(): List<CodeBlock> = argExpressions.map { (p, e) -> t.argument(e, p.returnType, b).code }

        fun paramType(i: Int): String = argExpressions.getOrNull(i)?.first?.returnType?.classIdString() ?: ""

        fun typeArgs(): List<TypeName> = call.typeArgumentsMapping.values.map { types.map(it, boxed = true) }

        val resultType: KaType? get() = with(t) { element.expressionType }
    }

    fun call(
        element: KtExpression,
        call: KaCallableMemberCall<*, *>,
        id: String,
        b: Block,
        discard: Boolean,
        assigned: KtExpression?,
    ): JExpr {
        val symbol = call.symbol
        if (symbol is org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol) {
            val classId = symbol.containingClassId ?: return t.problem(element, "Unresolved constructor")
            val site = Site(element, call, classId.asString() + ".<init>", b, discard, assigned)
            if (classId.asString() == "kotlin/text/Regex") {
                return JExpr.call("\$T.compile(\$L)", ClassName.get("java.util.regex", "Pattern"), CodeBlock.join(site.args(), ", "))
            }
            val type = classId.javaName()
            val generic = with(t) { symbol.typeParameters.isNotEmpty() }
            return JExpr.call(if (generic) "new \$T<>(\$L)" else "new \$T(\$L)", type, CodeBlock.join(site.args(), ", "))
        }
        val site = Site(element, call, id, b, discard, assigned)
        return translate(site) ?: helper(site)
    }

    private fun recv(s: Site): CodeBlock = (s.receiver ?: JExpr.THIS).wrap(Prec.PRIMARY)

    private fun member(s: Site, method: String, vararg args: CodeBlock) =
        JExpr.call("\$L.\$N(\$L)", recv(s), method, CodeBlock.join(args.toList(), ", "))

    private fun memberArgs(s: Site, method: String) =
        JExpr.call("\$L.\$N(\$L)", recv(s), method, CodeBlock.join(s.args(), ", "))

    /** `Kt.name(receiver, args...)` */
    private fun helper(s: Site, name: String = s.name): JExpr {
        val args = mutableListOf<CodeBlock>()
        if (s.applied.extensionReceiver != null || s.applied.dispatchReceiver != null) {
            s.receiver?.let { args.add(it.code) }
        }
        args.addAll(s.args())
        return JExpr.call("\$T.\$N(\$L)", KT, name, CodeBlock.join(args, ", "))
    }

    private val collectionOwners = setOf(
        "List", "MutableList", "Collection", "MutableCollection", "Set", "MutableSet", "Map", "MutableMap",
        "Iterable", "MutableIterable", "Iterator", "MutableIterator", "Entry", "MutableEntry",
    )

    private fun translate(s: Site): JExpr? {
        val id = s.id
        // Properties of Java-mapped types
        when (id) {
            "kotlin/String.length", "kotlin/CharSequence.length" -> return member(s, "length")
            "kotlin/collections/Map.entries", "kotlin/collections/MutableMap.entries" -> return member(s, "entrySet")
            "kotlin/collections/Map.keys", "kotlin/collections/MutableMap.keys" -> return member(s, "keySet")
            "kotlin/collections/Map.values", "kotlin/collections/MutableMap.values" -> return member(s, "values")
            "kotlin/collections/Map.Entry.key", "kotlin/collections/MutableMap.MutableEntry.key" -> return member(s, "getKey")
            "kotlin/collections/Map.Entry.value", "kotlin/collections/MutableMap.MutableEntry.value" -> return member(s, "getValue")
            "kotlin/Pair.first" -> return member(s, "getFirst")
            "kotlin/Pair.second" -> return member(s, "getSecond")
            "kotlin/collections/IndexedValue.index" -> return member(s, "getIndex")
            "kotlin/collections/IndexedValue.value" -> return member(s, "getValue")
            "kotlin/Enum.name" -> return member(s, "name")
            "kotlin/Enum.ordinal" -> return member(s, "ordinal")
            "kotlin/reflect/KClass.simpleName" -> return member(s, "getSimpleName")
            "kotlin/reflect/KClass.equals" -> return JExpr.call("\$L.equals(\$L)", recv(s), s.args().single())
            "kotlin/collections/indices" -> return helper(s)
            "kotlin/collections/List.size", "kotlin/collections/Collection.size", "kotlin/collections/Map.size",
            "kotlin/collections/Set.size", "kotlin/collections/MutableList.size", "kotlin/collections/MutableMap.size",
            "kotlin/collections/MutableSet.size", "kotlin/collections/MutableCollection.size" -> return member(s, "size")
        }
        if (s.call !is KaFunctionCall<*>) {
            if (s.owner in collectionOwners && s.name == "size") return member(s, "size")
            return null
        }
        val fn = s.fn!!
        return when {
            s.owner in collectionOwners -> collectionMember(s)
            s.owner == "String" || s.owner == "CharSequence" -> stringMember(s)
            s.owner in setOf("Int", "Long", "Double", "Float", "Short", "Byte", "Char", "Number", "Boolean") -> numberMember(s)
            s.owner == "Any" -> anyMember(s)
            s.owner == "Regex" && s.name == "matches" ->
                JExpr.call("\$L.matcher(\$L).matches()", recv(s), s.args().single())
            s.owner == "StringBuilder" -> memberArgs(s, s.name)
            s.owner == "Regex" && s.name == "<init>" ->
                JExpr.call("\$T.compile(\$L)", ClassName.get("java.util.regex", "Pattern"), s.args().single())
            s.owner.startsWith("Function") && s.name == "invoke" -> {
                val applied = fn.partiallyAppliedSymbol
                val receiverType = applied.dispatchReceiver?.type
                val method = receiverType?.let { types.invokeName(it) } ?: "apply"
                val function = t.receiver(applied.dispatchReceiver, s.b) ?: JExpr.THIS
                // A lambda with receiver takes the receiver as its first argument
                val args = listOfNotNull(t.receiver(applied.extensionReceiver, s.b)?.code) + s.args()
                JExpr.call("\$L.\$N(\$L)", function.wrap(Prec.PRIMARY), method, CodeBlock.join(args, ", "))
            }
            else -> topLevel(s)
        }
    }

    private fun collectionMember(s: Site): JExpr? = when (s.name) {
        "get" -> if (s.owner == "Map" || s.owner == "MutableMap") memberArgs(s, "get") else memberArgs(s, "get")
        "set" -> memberArgs(s, "set")
        "contains" -> memberArgs(s, "contains")
        "addAll", "putAll" -> covariantArgument(s)
        "containsKey", "containsValue", "isEmpty", "add", "remove", "removeAll", "retainAll", "clear",
        "indexOf", "lastIndexOf", "subList", "put", "iterator", "hasNext", "next", "listIterator",
        "containsAll" -> memberArgs(s, s.name)
        "removeAt" -> JExpr.call("\$L.remove((int) \$L)", recv(s), s.args().single())
        else -> null
    }

    /**
     * `addAll`/`putAll` accept covariant arguments in Kotlin (a `List<SchemaMap>` into a `MutableList<Map<String, Any>>`)
     * which Java's generics reject, so arguments with different type arguments go through an unchecked cast.
     */
    private fun covariantArgument(s: Site): JExpr {
        val expression = s.arg(0) ?: return memberArgs(s, s.name)
        val argType = with(t) { expression.expressionType }
        val receiverArgs = (s.receiverType as? org.jetbrains.kotlin.analysis.api.types.KaClassType)?.typeArguments?.mapNotNull { it.type?.let { a -> types.map(a, boxed = true) } }
        val argArgs = (argType as? org.jetbrains.kotlin.analysis.api.types.KaClassType)?.typeArguments?.mapNotNull { it.type?.let { a -> types.map(a, boxed = true) } }
        val value = t.expr(expression, s.b)
        return if (receiverArgs != null && argArgs != null && receiverArgs != argArgs) {
            JExpr.call("\$L.\$N(\$T.uncheckedCast(\$L))", recv(s), s.name, KT, value.code)
        } else {
            JExpr.call("\$L.\$N(\$L)", recv(s), s.name, value.code)
        }
    }

    private fun stringMember(s: Site): JExpr? = when (s.name) {
        "get" -> memberArgs(s, "charAt")
        "plus" -> JExpr.of("\$L + \$L", s.receiver!!.wrap(Prec.ADD), t.expr(s.arg(0)!!, s.b).wrap(Prec.MUL), prec = Prec.ADD)
        "compareTo" -> memberArgs(s, "compareTo")
        "equals" -> memberArgs(s, "equals")
        "toString" -> s.receiver
        "subSequence", "substring" -> memberArgs(s, s.name)
        else -> null
    }

    private fun numberMember(s: Site): JExpr? {
        val r = s.receiver!!
        val castTo = mapOf("toInt" to "int", "toLong" to "long", "toDouble" to "double", "toFloat" to "float", "toShort" to "short", "toByte" to "byte", "toChar" to "char")
        castTo[s.name]?.let { return JExpr.of("(\$L) \$L", it, r.wrap(Prec.UNARY), prec = Prec.UNARY) }
        val ops = mapOf("plus" to "+", "minus" to "-", "times" to "*", "div" to "/", "rem" to "%")
        ops[s.name]?.let { op -> return JExpr.of("\$L $op \$L", r.wrap(Prec.ADD), s.args().single(), prec = Prec.ADD) }
        return when (s.name) {
            "toString" -> JExpr.call("\$T.valueOf(\$L)", STRING, r.code)
            "compareTo" -> JExpr.call("\$T.compare(\$L, \$L)", boxedOf(s.owner), r.code, s.args().single())
            "equals" -> JExpr.call("\$T.equals(\$L, \$L)", OBJECTS, r.code, s.args().single())
            "not" -> JExpr.of("!\$L", r.wrap(Prec.UNARY), prec = Prec.UNARY)
            else -> null
        }
    }

    private fun boxedOf(owner: String): ClassName = when (owner) {
        "Int" -> ClassName.get(Integer::class.java)
        "Char" -> ClassName.get(Character::class.java)
        else -> ClassName.get("java.lang", owner)
    }

    private fun anyMember(s: Site): JExpr? = when (s.name) {
        "equals" -> JExpr.call("\$T.equals(\$L, \$L)", OBJECTS, s.receiver?.code ?: CodeBlock.of("this"), s.args().single())
        "hashCode" -> JExpr.call("\$T.hashCode(\$L)", OBJECTS, s.receiver?.code ?: CodeBlock.of("this"))
        "toString" -> JExpr.call("\$T.valueOf(\$L)", STRING, s.receiver?.code ?: CodeBlock.of("this"))
        else -> null
    }

    private fun topLevel(s: Site): JExpr? {
        val pkg = s.id.substringBeforeLast('/')
        return when (pkg) {
            "kotlin" -> kotlinFunction(s)
            "kotlin/collections" -> collectionFunction(s)
            "kotlin/text" -> textFunction(s)
            "kotlin/ranges" -> when (s.name) {
                "coerceAtLeast" -> JExpr.call("\$T.max(\$L, \$L)", ClassName.get(Math::class.java), s.receiver!!.code, s.args().single())
                "coerceAtMost" -> JExpr.call("\$T.min(\$L, \$L)", ClassName.get(Math::class.java), s.receiver!!.code, s.args().single())
                else -> null
            }
            else -> null
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // kotlin.*
    // ---------------------------------------------------------------------------------------------------------------

    private fun kotlinFunction(s: Site): JExpr? = when (s.name) {
        "to" -> JExpr.call("new \$T<>(\$L, \$L)", PAIR, s.receiver!!.code, s.args().single())
        "Pair" -> JExpr.call("new \$T<>(\$L)", PAIR, CodeBlock.join(s.args(), ", "))
        "error" -> throwing(s, ISE, s.args().single(), s.arg(0)?.let { with(t) { it.expressionType } }?.classIdString() == "kotlin/String")
        "TODO" -> throwing(s, ClassName.get(UnsupportedOperationException::class.java), CodeBlock.of("\$S", "Not implemented"))
        "require", "check" -> {
            val exception = if (s.name == "require") IAE else ISE
            val condition = t.expr(s.arg(0)!!, s.b)
            val message = s.lambda(1)?.let { inlineValue(it, null, s.b) }
                ?: JExpr.of("\$S", if (s.name == "require") "Failed requirement." else "Check failed.")
            s.b.code.beginControlFlow("if (!\$L)", condition.wrap(Prec.UNARY))
                .add("throw new \$T(\$T.valueOf(\$L));\n", exception, STRING, message.code)
                .endControlFlow()
            BodyTranslator.UNREACHABLE
        }
        "requireNotNull", "checkNotNull" -> {
            val value = t.expr(s.arg(0)!!, s.b)
            JExpr.call("\$T.requireNonNull(\$L)", OBJECTS, value.code)
        }
        "let" -> scope(s, asReceiver = false, returnsSubject = false)
        "run" -> if (s.receiver == null && s.applied.extensionReceiver == null) {
            inlineValue(s.lambda(0)!!, null, s.b, s.discard)
        } else {
            scope(s, asReceiver = true, returnsSubject = false)
        }
        "with" -> {
            val subject = simple(t.expr(s.arg(0)!!, s.b), s, "receiver")
            t.inlineLambda(s.lambda(1)!!, subject, s.b, s.discard, asReceiver = true)
        }
        "also" -> scope(s, asReceiver = false, returnsSubject = true)
        "apply" -> scope(s, asReceiver = true, returnsSubject = true)
        "takeIf", "takeUnless" -> {
            val subject = simple(t.exactSubject((s.applied.extensionReceiver as? org.jetbrains.kotlin.analysis.api.resolution.KaExplicitReceiverValue)?.expression, s.receiver!!), s, "value")
            val inner = s.b.child()
            val condition = t.inlineLambda(s.lambda()!!, subject, inner, false, asReceiver = false)
            if (!inner.isEmpty) {
                s.b.addCode(inner.build())
            }
            val test = if (s.name == "takeIf") condition.wrap(Prec.OR) else CodeBlock.of("!\$L", condition.wrap(Prec.UNARY))
            JExpr.of("\$L ? \$L : null", test, subject.wrap(Prec.OR), prec = Prec.TERNARY)
        }
        "repeat" -> {
            val times = t.expr(s.arg(0)!!, s.b)
            val lambda = s.lambda()!!
            val body = s.b.child()
            val index = t.lambdaParameterNames(lambda, body.scope, listOf("it")).single()
            t.bindLambdaParameters(lambda, listOf(index), body)
            t.withLoop("repeat") { t.inlineLambda(lambda, null, body, true, false) }
            s.b.code.beginControlFlow("for (int \$N = 0; \$N < \$L; \$N++)", index, index, times.code, index)
                .add(body.build()).endControlFlow()
            BodyTranslator.UNREACHABLE
        }
        "lazy" -> null
        "toString" -> JExpr.call("\$T.valueOf(\$L)", STRING, s.receiver!!.code)
        "hashCode" -> JExpr.call("\$T.hashCode(\$L)", OBJECTS, s.receiver!!.code)
        "plus" -> JExpr.of("\$L + \$L", s.receiver!!.wrap(Prec.ADD), CodeBlock.of("\$T.valueOf(\$L)", STRING, s.args().single()), prec = Prec.ADD)
        else -> null
    }

    private fun throwing(s: Site, exception: ClassName, message: CodeBlock, isString: Boolean = false): JExpr {
        if (s.discard || isStatementPosition(s)) {
            if (isString) s.b.add("throw new \$T(\$L)", exception, message)
            else s.b.add("throw new \$T(\$T.valueOf(\$L))", exception, STRING, message)
            return BodyTranslator.UNREACHABLE
        }
        return JExpr.call("\$T.error(\$L)", KT, message)
    }

    /** Nothing-typed calls are emitted as statements by emitInto, which calls through with discard = true. */
    private fun isStatementPosition(s: Site) = s.discard

    private fun simple(value: JExpr, s: Site, base: String): JExpr {
        if (value.simple) return value
        val name = s.b.scope.declare(base)
        val type = s.receiverType?.let { types.map(it, boxed = true) }
        if (type != null && type != OBJECT) s.b.add("\$T \$N = \$L", type, name, value.code) else s.b.add("var \$N = \$L", name, value.code)
        return JExpr.name(name)
    }

    private fun scope(s: Site, asReceiver: Boolean, returnsSubject: Boolean): JExpr {
        val lambda = s.lambda() ?: return helper(s)
        val subject = simple(s.receiver!!, s, if (asReceiver) "receiver" else "it")
        if (returnsSubject) {
            t.inlineLambda(lambda, subject, s.b, true, asReceiver)
            return subject
        }
        return t.inlineLambda(lambda, subject, s.b, s.discard, asReceiver)
    }

    private fun inlineValue(lambda: KtLambdaExpression, subject: JExpr?, b: Block, discard: Boolean = false): JExpr =
        t.inlineLambda(lambda, subject, b, discard, asReceiver = false)

    // ---------------------------------------------------------------------------------------------------------------
    // kotlin.collections.*
    // ---------------------------------------------------------------------------------------------------------------

    private fun newCollection(raw: ClassName, s: Site): JExpr {
        val args = s.typeArgs()
        val concrete = args.isNotEmpty() && args.none { it.toString().contains("?") }
        return if (concrete) {
            JExpr.call("new \$T()", ParameterizedTypeName.get(raw, *args.toTypedArray()))
        } else {
            JExpr.call("new \$T<>()", raw)
        }
    }

    private fun witness(s: Site, name: String): JExpr {
        val args = s.typeArgs()
        val call = CodeBlock.join(s.args(), ", ")
        return if (args.isNotEmpty() && args.none { it.toString().contains("?") }) {
            JExpr.call("\$T.<\$L>\$N(\$L)", KT, CodeBlock.join(args.map { CodeBlock.of("\$T", it) }, ", "), name, call)
        } else {
            JExpr.call("\$T.\$N(\$L)", KT, name, call)
        }
    }

    private fun collectionFunction(s: Site): JExpr? = when (s.name) {
        "mutableListOf", "arrayListOf" -> if (s.argExpressions.isEmpty()) newCollection(ARRAY_LIST, s) else helper(s, "mutableListOf")
        "mutableMapOf", "linkedMapOf" -> if (s.argExpressions.isEmpty()) newCollection(LINKED_HASH_MAP, s) else helper(s, "mutableMapOf")
        "mutableSetOf", "linkedSetOf" -> if (s.argExpressions.isEmpty()) newCollection(LINKED_HASH_SET, s) else helper(s, "mutableSetOf")
        "emptyList", "emptyMap", "emptySet", "listOf", "setOf", "mapOf" ->
            if (s.argExpressions.isEmpty()) witness(s, s.name) else helper(s)
        "set" -> {
            val args = s.args().toMutableList()
            if (args.size < 2) s.assigned?.let { args.add(t.expr(it, s.b).code) }
            JExpr.call(if (s.receiverId.endsWith("Map")) "\$L.put(\$L)" else "\$L.set(\$L)", recv(s), CodeBlock.join(args, ", "))
        }
        "get" -> memberArgs(s, "get")
        "contains" -> if (s.receiverId.endsWith("Map")) memberArgs(s, "containsKey") else helper(s)
        "plusAssign" -> if (isCollectionType(s.paramType(0))) memberArgs(s, "addAll") else memberArgs(s, "add")
        "minusAssign" -> if (isCollectionType(s.paramType(0))) memberArgs(s, "removeAll") else memberArgs(s, "remove")
        "plus" -> when {
            s.receiverId.endsWith("Map") -> helper(s, if (s.paramType(0) == "kotlin/Pair") "plusEntry" else "plusMap")
            isCollectionType(s.paramType(0)) -> helper(s, "plusAll")
            else -> helper(s, "plus")
        }
        "minus" -> if (isCollectionType(s.paramType(0))) helper(s, "minusAll") else helper(s)
        "isNotEmpty" -> JExpr.of("!\$L.isEmpty()", recv(s), prec = Prec.UNARY)
        "isEmpty" -> member(s, "isEmpty")
        "getOrPut" -> helper(s)
        "map", "mapNotNull" -> if (s.lambda()?.let { t.needsInlining(it) } == true) mapLoop(s, s.name == "mapNotNull") else null
        "forEach" -> forEach(s, indexed = false)
        "forEachIndexed" -> forEach(s, indexed = true)
        "filterIsInstance" -> {
            val type = s.typeArgs().single()
            val raw = if (type is ParameterizedTypeName) type.rawType else type
            JExpr.call("\$T.filterIsInstance(\$L, \$T.class)", KT, s.receiver!!.code, raw)
        }
        "toTypedArray" -> null
        else -> null
    }

    /** `map { }` whose lambda Java can't express, expanded into the loop Kotlin's inline function amounts to. */
    private fun mapLoop(s: Site, notNull: Boolean): JExpr {
        val lambda = s.lambda()!!
        val source = s.receiver!!
        val isMap = s.receiverId.endsWith("Map")
        val elementType = s.resultType?.let { (it as? org.jetbrains.kotlin.analysis.api.types.KaClassType)?.typeArguments?.firstOrNull()?.type }
        val result = s.b.scope.declare("mapped")
        val listType = elementType?.let { ParameterizedTypeName.get(ClassName.get("java.util", "List"), types.map(it, boxed = true)) }
        val arrayType = elementType?.let { ParameterizedTypeName.get(ARRAY_LIST, types.map(it, boxed = true)) }
        if (listType != null) s.b.add("\$T \$N = new \$T()", listType, result, arrayType) else s.b.add("var \$N = new \$T<>()", result, ARRAY_LIST)
        val body = s.b.child()
        val names = t.lambdaParameterNames(lambda, body.scope, listOf(if (isMap) "entry" else "it"))
        t.bindLambdaParameters(lambda, names, body)
        val add: (CodeBlock, Block) -> Unit = { value, b ->
            if (notNull) {
                val tmp = b.scope.declare("value")
                b.add("var \$N = \$L", tmp, value)
                b.code.beginControlFlow("if (\$N != null)", tmp).add("\$N.add(\$N);\n", result, tmp).endControlFlow()
            } else {
                b.add("\$N.add(\$L)", result, value)
            }
        }
        t.withLoop(s.name, add) {
            val value = t.inlineLambda(lambda, null, body, false, false)
            if (value !== BodyTranslator.UNREACHABLE) add(value.code, body)
        }
        val iterable = if (isMap) CodeBlock.of("\$L.entrySet()", source.wrap(Prec.PRIMARY)) else source.code
        s.b.code.beginControlFlow("for (var \$N : \$L)", names[0], iterable).add(body.build()).endControlFlow()
        return JExpr.name(result)
    }

    private fun isCollectionType(id: String) = id.startsWith("kotlin/collections/") &&
        (id.endsWith("Iterable") || id.endsWith("Collection") || id.endsWith("List") || id.endsWith("Set")) ||
        id == "kotlin/Array" || id == "kotlin/sequences/Sequence"

    private fun forEach(s: Site, indexed: Boolean): JExpr {
        val lambda = s.lambda() ?: return helper(s)
        val source = s.receiver!!
        val body = s.b.child()
        val isMap = s.receiverId.endsWith("Map")
        if (indexed) {
            val names = t.lambdaParameterNames(lambda, body.scope, listOf("index", "item"))
            t.bindLambdaParameters(lambda, names, body)
            t.withLoop("forEachIndexed") { t.inlineLambda(lambda, null, body, true, false) }
            val counter = names[0]
            s.b.add("int \$N = 0", counter)
            body.add("\$N++", counter)
            s.b.code.beginControlFlow("for (var \$N : \$L)", names[1], source.code).add(body.build()).endControlFlow()
            return BodyTranslator.UNREACHABLE
        }
        val names = t.lambdaParameterNames(lambda, body.scope, listOf(if (isMap) "entry" else "it"))
        t.bindLambdaParameters(lambda, names, body)
        t.withLoop("forEach") { t.inlineLambda(lambda, null, body, true, false) }
        val iterable = if (isMap) CodeBlock.of("\$L.entrySet()", source.wrap(Prec.PRIMARY)) else source.code
        s.b.code.beginControlFlow("for (var \$N : \$L)", names[0], iterable).add(body.build()).endControlFlow()
        return BodyTranslator.UNREACHABLE
    }

    // ---------------------------------------------------------------------------------------------------------------
    // kotlin.text.*
    // ---------------------------------------------------------------------------------------------------------------

    private fun textFunction(s: Site): JExpr? = when (s.name) {
        "buildString" -> {
            val name = s.b.scope.declare("builder")
            s.b.add("\$T \$N = new \$T()", StringBuilder::class.java, name, StringBuilder::class.java)
            t.inlineLambda(s.lambda()!!, JExpr.name(name), s.b, true, asReceiver = true)
            JExpr.call("\$N.toString()", name)
        }
        "append" -> memberArgs(s, "append")
        "lowercase" -> JExpr.call("\$L.toLowerCase(\$T.ROOT)", recv(s), ClassName.get("java.util", "Locale"))
        "uppercase" -> JExpr.call("\$L.toUpperCase(\$T.ROOT)", recv(s), ClassName.get("java.util", "Locale"))
        "repeat" -> memberArgs(s, s.name)
        "startsWith", "endsWith" -> if (s.argExpressions.size == 1 && s.paramType(0) == "kotlin/String") memberArgs(s, s.name) else helper(s)
        "substring" -> memberArgs(s, "substring")
        "indexOf" -> if (s.argExpressions.size <= 2) memberArgs(s, "indexOf") else helper(s)
        "isEmpty" -> member(s, "isEmpty")
        "isNotEmpty" -> JExpr.of("!\$L.isEmpty()", recv(s), prec = Prec.UNARY)
        "toInt" -> JExpr.call("\$T.parseInt(\$L)", ClassName.get(Integer::class.java), s.receiver!!.code)
        "toLong" -> JExpr.call("\$T.parseLong(\$L)", ClassName.get(java.lang.Long::class.java), s.receiver!!.code)
        "toDouble" -> JExpr.call("\$T.parseDouble(\$L)", ClassName.get(java.lang.Double::class.java), s.receiver!!.code)
        "toRegex" -> JExpr.call("\$T.compile(\$L)", ClassName.get("java.util.regex", "Pattern"), s.receiver!!.code)
        "isDigit" -> JExpr.call("\$T.isDigit(\$L)", ClassName.get(Character::class.java), s.receiver!!.code)
        "isLetter" -> JExpr.call("\$T.isLetter(\$L)", ClassName.get(Character::class.java), s.receiver!!.code)
        "isLetterOrDigit" -> JExpr.call("\$T.isLetterOrDigit(\$L)", ClassName.get(Character::class.java), s.receiver!!.code)
        "replace" -> if (s.paramType(0) == "kotlin/text/Regex" || s.argExpressions.size > 2) helper(s) else memberArgs(s, "replace")
        "contains" -> if (s.paramType(0) == "kotlin/CharSequence" && s.argExpressions.size == 1) memberArgs(s, "contains") else helper(s)
        else -> null
    }

    @Suppress("unused")
    private fun isCall(e: KtExpression) = e is KtCallExpression
}
