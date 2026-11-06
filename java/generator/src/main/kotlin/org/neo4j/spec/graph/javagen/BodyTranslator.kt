package org.neo4j.spec.graph.javagen

import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeVariableName
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.resolution.KaCallableMemberCall
import org.jetbrains.kotlin.analysis.api.resolution.KaExplicitReceiverValue
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.KaImplicitReceiverValue
import org.jetbrains.kotlin.analysis.api.resolution.KaReceiverValue
import org.jetbrains.kotlin.analysis.api.resolution.KaSimpleVariableAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.KaSmartCastedReceiverValue
import org.jetbrains.kotlin.analysis.api.resolution.singleCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaLocalVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaIntersectionType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import javax.lang.model.element.Modifier

/**
 * Translates Kotlin function bodies into Java statements.
 *
 * Kotlin expressions Java can only write as statements (`if`/`when`/`try` as values, `?:`, `?.`, scope functions)
 * are lowered into temporary variables and `if` chains, emitted into the current [Block] before the expression that
 * uses them.
 */
class BodyTranslator(val ft: FileTranslator, val ctx: MemberCtx) : KaSession by ft {
    private val gen = ft.gen
    private val types = ft.types
    private val names = HashMap<KaSymbol, String>()
    private val implicitReceivers = HashMap<KaSymbol, JExpr?>()
    private val overrides = HashMap<KtExpression, JExpr>()
    private val functions = ArrayDeque<FnCtx>()
    private val wildcardParameters = HashSet<KaSymbol>()
    private var fillDefaults = false
    private var root = Scope(null)

    /** A function-like context `return` can target. */
    class FnCtx(val kind: Kind, val labels: Set<String>, val returnsUnit: Boolean, val loopValue: ((CodeBlock, Block) -> Unit)? = null) {
        enum class Kind { FUNCTION, JAVA_LAMBDA, LOOP }
    }

    init {
        val cls = ctx.cls
        if (cls != null) implicitReceivers[cls] = null
    }

    fun problem(element: KtElement?, message: String): JExpr {
        gen.problem(element, message)
        return JExpr.of("\$T.todo(\$S)", KT, message.take(80))
    }

    // ===============================================================================================================
    // Members
    // ===============================================================================================================

    fun function(f: KtNamedFunction): List<MethodSpec> {
        val symbol = f.symbol as KaNamedFunctionSymbol
        val name = methodName(symbol, f.name!!)
        val inInterface = ctx.cls?.classKind == KaClassKind.INTERFACE
        val builder = MethodSpec.methodBuilder(name)
        ft.javadoc(f)?.let { builder.addJavadoc(it) }
        val topLevelPrivate = ctx.cls == null && f.hasModifier(KtTokens.PRIVATE_KEYWORD)
        if (!topLevelPrivate) builder.addModifiers(ft.visibilityOf(f))
        if (ctx.isStatic) builder.addModifiers(Modifier.STATIC)
        if (f.hasModifier(KtTokens.OVERRIDE_KEYWORD)) builder.addAnnotation(Override::class.java)
        symbol.typeParameters.forEach { builder.addTypeVariable(typeVariable(it)) }
        val scope = root.child()
        root = scope
        val params = mutableListOf<ParameterSpec>()
        symbol.receiverParameter?.let { receiver ->
            val receiverName = scope.declare(receiverNameFor(receiver.returnType))
            params.add(ParameterSpec.builder(types.map(receiver.returnType, parameter = true), receiverName).build())
            implicitReceivers[receiver] = JExpr.name(receiverName)
            implicitReceivers[symbol] = JExpr.name(receiverName)
        }
        params.addAll(valueParameters(f.valueParameters, scope))
        builder.addParameters(params)
        if (f.valueParameters.lastOrNull()?.isVarArg == true) builder.varargs(true)
        // Overrides keep the overridden return type: Java return types must match generically
        val declaredReturn = symbol.allOverriddenSymbols.firstOrNull { it.allOverriddenSymbols.none() }?.returnType ?: symbol.returnType
        val returnType = types.map(declaredReturn, returnType = true)
        builder.returns(returnType)
        val returnsUnit = ft.isUnit(symbol.returnType)
        when {
            f.hasBody() -> {
                if (inInterface) builder.addModifiers(Modifier.DEFAULT)
                builder.addCode(body(f, scope, returnsUnit))
            }
            inInterface -> builder.addModifiers(Modifier.ABSTRACT)
            else -> builder.addModifiers(Modifier.ABSTRACT)
        }
        val method = builder.build()
        return listOf(method) + overloads(method, f.valueParameters, params.size - f.valueParameters.size, returnsUnit) { args ->
            val target = if (ctx.isStatic) CodeBlock.of("\$T.\$N", ctx.javaClass, name) else CodeBlock.of("\$N", name)
            CodeBlock.of("\$L(\$L)", target, args)
        }
    }

    private fun receiverNameFor(type: KaType): String {
        val id = type.classIdString() ?: return "receiver"
        val simple = id.substringAfterLast('/').substringAfterLast('.')
        return when (simple) {
            "String", "Any", "Int", "Long", "Boolean", "Double", "List", "Map", "Set", "Collection", "Iterable" -> "receiver"
            else -> simple.replaceFirstChar { it.lowercaseChar() }
        }
    }

    fun typeVariable(tp: org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol): TypeVariableName {
        val bounds = tp.upperBounds.filter { it.classIdString() != "kotlin/Any" }.map { types.map(it, boxed = true) }
        return TypeVariableName.get(tp.name.asString(), *bounds.toTypedArray())
    }

    private fun valueParameters(parameters: List<KtParameter>, scope: Scope): List<ParameterSpec> =
        parameters.map { p ->
            val symbol = p.symbol
            val name = scope.declare(p.name!!)
            names[symbol] = name
            // Properties declared in a constructor keep their exact type, so they can be assigned to the field
            var type = types.map(symbol.returnType, parameter = !p.hasValOrVar())
            if (type != types.map(symbol.returnType)) wildcardParameters.add(symbol)
            if (p.isVarArg) type = ArrayTypeName.of(type.box())
            ParameterSpec.builder(type, name).build()
        }

    /**
     * Kotlin's `@JvmOverloads` behaviour: one overload per defaulted parameter, omitting it and every defaulted
     * parameter after it.
     */
    private fun overloads(
        full: MethodSpec,
        parameters: List<KtParameter>,
        leading: Int,
        returnsUnit: Boolean,
        callTarget: (CodeBlock) -> CodeBlock,
    ): List<MethodSpec> {
        val defaults = parameters.indices.filter { parameters[it].hasDefaultValue() }
        val result = mutableListOf<MethodSpec>()
        for (start in defaults.indices.reversed()) {
            val omitted = defaults.drop(start).toSet()
            val builder = if (full.isConstructor) MethodSpec.constructorBuilder() else MethodSpec.methodBuilder(full.name)
            builder.addModifiers(full.modifiers.filter { it != Modifier.ABSTRACT && it != Modifier.DEFAULT })
            if (ctx.cls?.classKind == KaClassKind.INTERFACE && !full.modifiers.contains(Modifier.STATIC)) {
                builder.addModifiers(Modifier.DEFAULT)
            }
            builder.addTypeVariables(full.typeVariables)
            if (!full.isConstructor) builder.returns(full.returnType)
            val args = mutableListOf<CodeBlock>()
            val scope = root.child()
            val saved = root
            root = scope
            val block = Block(scope)
            for (i in 0 until leading) {
                builder.addParameter(full.parameters[i])
                args.add(CodeBlock.of("\$N", full.parameters[i].name))
                scope.declare(full.parameters[i].name)
            }
            for ((i, p) in parameters.withIndex()) {
                val spec = full.parameters[leading + i]
                if (i in omitted) {
                    args.add(expr(p.defaultValue!!, block).code)
                } else {
                    builder.addParameter(spec)
                    scope.declare(spec.name)
                    args.add(CodeBlock.of("\$N", spec.name))
                }
            }
            root = saved
            if (!block.isEmpty) {
                gen.problem(parameters[defaults[start]], "Default value needs statements, overload skipped")
                continue
            }
            val call = callTarget(CodeBlock.join(args, ", "))
            when {
                full.isConstructor -> builder.addCode("this(\$L);\n", CodeBlock.join(args, ", "))
                returnsUnit -> builder.addCode("\$L;\n", call)
                else -> builder.addCode("return \$L;\n", call)
            }
            if (parameters.lastOrNull()?.isVarArg == true && parameters.size - 1 !in omitted) builder.varargs(true)
            result.add(builder.build().also { ft.overloads.add(it) })
        }
        return result
    }

    private fun body(f: KtDeclarationWithBody, scope: Scope, returnsUnit: Boolean): CodeBlock {
        val block = Block(scope)
        functions.addLast(FnCtx(FnCtx.Kind.FUNCTION, setOfNotNull((f as? KtNamedFunction)?.name), returnsUnit))
        val bodyBlock = f.bodyBlockExpression
        if (bodyBlock != null) {
            statements(bodyBlock.statements, block)
        } else {
            val expression = f.bodyExpression!!
            emitInto(expression, block, if (returnsUnit) Sink.Discard else Sink.Return)
        }
        functions.removeLast()
        return block.build()
    }

    fun accessor(accessor: KtPropertyAccessor, property: KaPropertySymbol): CodeBlock {
        val scope = root.child()
        root = scope
        if (accessor.isSetter) {
            accessor.parameter?.let { names[it.symbol] = scope.declare("value") }
            // The generated setter parameter is always called value
        }
        return body(accessor, scope, accessor.isSetter)
    }

    fun primaryConstructor(
        k: KtClass,
        parameters: List<KtParameter>,
        superCall: KtSuperTypeCallEntry?,
        initializers: List<KtDeclaration>,
    ): List<MethodSpec> {
        val primary = k.primaryConstructor
        val builder = MethodSpec.constructorBuilder()
        val symbol = ctx.cls
        when {
            symbol?.classKind == KaClassKind.ENUM_CLASS -> Unit
            primary?.hasModifier(KtTokens.PRIVATE_KEYWORD) == true -> builder.addModifiers(Modifier.PRIVATE)
            k.hasModifier(KtTokens.SEALED_KEYWORD) || k.hasModifier(KtTokens.ABSTRACT_KEYWORD) ->
                builder.addModifiers(Modifier.PROTECTED)
            else -> builder.addModifiers(Modifier.PUBLIC)
        }
        val scope = root.child()
        root = scope
        val params = valueParameters(parameters, scope)
        builder.addParameters(params)
        if (parameters.lastOrNull()?.isVarArg == true) builder.varargs(true)
        val block = Block(scope)
        functions.addLast(FnCtx(FnCtx.Kind.FUNCTION, emptySet(), true))
        superCall?.let { block.addCode(superCall(it, block)) }
        for ((i, p) in parameters.withIndex()) {
            if (p.hasValOrVar()) block.add("this.\$N = \$N", javaIdentifier(p.name!!), params[i].name)
        }
        initializerStatements(initializers, block)
        functions.removeLast()
        builder.addCode(block.build())
        val full = builder.build()
        return listOf(full) + overloads(full, parameters, 0, true) { it }
    }

    fun secondaryConstructor(c: KtSecondaryConstructor): MethodSpec {
        val builder = MethodSpec.constructorBuilder().addModifiers(ft.visibilityOf(c))
        val scope = root.child()
        root = scope
        builder.addParameters(valueParameters(c.valueParameters, scope))
        val block = Block(scope)
        functions.addLast(FnCtx(FnCtx.Kind.FUNCTION, emptySet(), true))
        val delegation = c.getDelegationCall()
        if (!delegation.isImplicit) {
            val call = delegation.resolveToCall()?.singleCallOrNull<KaFunctionCall<*>>()
            val keyword = if (delegation.isCallToThis) "this" else "super"
            if (call != null) {
                fillDefaults = true
                val args = arguments(call, block, delegation)
                fillDefaults = false
                block.add("$keyword(\$L)", CodeBlock.join(args, ", "))
            }
        }
        c.bodyBlockExpression?.let { statements(it.statements, block) }
        functions.removeLast()
        builder.addCode(block.build())
        return builder.build()
    }

    fun superCall(entry: KtSuperTypeCallEntry, block: Block = Block(root.child())): CodeBlock {
        val call = entry.calleeExpression.resolveToCall()?.singleCallOrNull<KaFunctionCall<*>>()
            ?: entry.resolveToCall()?.singleCallOrNull<KaFunctionCall<*>>()
            ?: return problem(entry, "Unresolved super constructor call").code
        val scratch = block.child()
        val args = arguments(call, scratch, entry)
        if (!scratch.isEmpty) gen.problem(entry, "Super constructor arguments need statements")
        return CodeBlock.builder().add("super(\$L);\n", CodeBlock.join(args, ", ")).build()
    }

    fun enumArguments(entry: KtSuperTypeCallEntry): CodeBlock {
        val call = entry.resolveToCall()?.singleCallOrNull<KaFunctionCall<*>>()
            ?: entry.calleeExpression.resolveToCall()?.singleCallOrNull<KaFunctionCall<*>>()
            ?: return problem(entry, "Unresolved enum entry arguments").code
        val block = Block(root.child())
        val args = arguments(call, block, entry)
        if (!block.isEmpty) gen.problem(entry, "Enum arguments need statements")
        return CodeBlock.join(args, ", ")
    }

    fun initializers(initializers: List<KtDeclaration>): CodeBlock {
        val block = Block(root.child())
        functions.addLast(FnCtx(FnCtx.Kind.FUNCTION, emptySet(), true))
        initializerStatements(initializers, block)
        functions.removeLast()
        return block.build()
    }

    private fun initializerStatements(initializers: List<KtDeclaration>, block: Block) {
        for (declaration in initializers) {
            when (declaration) {
                is KtProperty -> {
                    val name = javaIdentifier(declaration.name!!)
                    val target = if (ctx.isStatic) CodeBlock.of("\$T.\$N", ctx.javaClass, name) else CodeBlock.of("this.\$N", name)
                    emitInto(declaration.initializer!!, block, Sink.Assign(target))
                }
                is KtAnonymousInitializer -> declaration.body?.let { emitInto(it, block, Sink.Discard) }
            }
        }
    }

    /** A static field initializer that needs no statements, or null. */
    fun simpleInitializer(expression: KtExpression): CodeBlock? {
        val block = Block(root.child())
        val value = expr(expression, block)
        return if (block.isEmpty) value.code else null
    }

    fun methodName(symbol: KaCallableSymbol, fallback: String): String {
        val fq = symbol.callableId?.asSingleFqName()?.asString()
        Config.renames[fq]?.let { return it }
        return javaIdentifier(symbol.callableId?.callableName?.asString() ?: fallback)
    }

    // ===============================================================================================================
    // Statements
    // ===============================================================================================================

    fun statements(statements: List<KtExpression>, block: Block) {
        for (statement in statements) emitInto(statement, block, Sink.Discard)
    }

    private fun isNothing(e: KtExpression): Boolean {
        val type = e.expressionType ?: return false
        return type.classIdString() == "kotlin/Nothing" && type.nullability != KaTypeNullability.NULLABLE
    }

    fun emitInto(e: KtExpression, b: Block, sink: Sink) {
        when (e) {
            is KtParenthesizedExpression -> emitInto(e.expression!!, b, sink)
            is KtAnnotatedExpression -> emitInto(e.baseExpression!!, b, sink)
            is KtBlockExpression -> {
                val statements = e.statements
                statements.dropLast(1).forEach { emitInto(it, b, Sink.Discard) }
                statements.lastOrNull()?.let { emitInto(it, b, sink) }
            }
            is KtIfExpression -> ifInto(e, b, sink)
            is KtWhenExpression -> whenInto(e, b, sink)
            is KtTryExpression -> tryInto(e, b, sink)
            is KtProperty -> localProperty(e, b)
            is KtDestructuringDeclaration -> destructuring(e, b)
            is KtReturnExpression -> returnStatement(e, b)
            is KtThrowExpression -> b.add("throw \$L", expr(e.thrownExpression!!, b).code)
            is KtBreakExpression -> b.add(e.getLabelName()?.let { "break $it" } ?: "break")
            is KtContinueExpression -> b.add(e.getLabelName()?.let { "continue $it" } ?: "continue")
            is KtForExpression -> forLoop(e, b, null)
            is KtWhileExpression -> whileLoop(e, b, null)
            is KtDoWhileExpression -> doWhileLoop(e, b, null)
            is KtLabeledExpression -> labeled(e, b, sink)
            is KtNamedFunction -> localFunction(e, b)
            is KtClassOrObject -> problem(e, "Local classes aren't supported").also { b.add("\$L", it.code) }
            else -> {
                val value = expr(e, b, discard = sink is Sink.Discard || isNothing(e))
                if (value === UNREACHABLE) return
                when (sink) {
                    Sink.Discard -> if (value.statement) b.add("\$L", value.code)
                    Sink.Return -> if (isNothing(e)) {
                        if (value.statement) b.add("\$L", value.code)
                    } else {
                        b.add("return \$L", value.code)
                    }
                    is Sink.Assign -> b.add("\$L = \$L", sink.target, value.code)
                }
            }
        }
    }

    private fun labeled(e: KtLabeledExpression, b: Block, sink: Sink) {
        val label = e.getLabelName()
        when (val base = e.baseExpression) {
            is KtForExpression -> forLoop(base, b, label)
            is KtWhileExpression -> whileLoop(base, b, label)
            is KtDoWhileExpression -> doWhileLoop(base, b, label)
            null -> Unit
            else -> emitInto(base, b, sink)
        }
    }

    private fun returnStatement(e: KtReturnExpression, b: Block) {
        val label = e.getLabelName()
        val target = if (label == null) {
            functions.lastOrNull { it.kind != FnCtx.Kind.LOOP } ?: functions.last()
        } else {
            functions.lastOrNull { label in it.labels }
        }
        if (target == null) {
            b.add("\$L", problem(e, "Unresolved return label $label").code)
            return
        }
        if (label == null && functions.any { it.kind == FnCtx.Kind.JAVA_LAMBDA }) {
            val innermost = functions.last { it.kind != FnCtx.Kind.LOOP }
            if (innermost.kind == FnCtx.Kind.JAVA_LAMBDA) {
                b.add("\$L", problem(e, "Non-local return from a lambda").code)
                return
            }
        }
        if (target.kind == FnCtx.Kind.LOOP) {
            val returned = e.returnedExpression
            if (returned != null) {
                val sink = target.loopValue
                if (sink == null) gen.problem(e, "Value returned to an inlined loop") else sink(expr(returned, b).code, b)
            }
            b.add("continue")
            return
        }
        val value = e.returnedExpression
        if (value == null || target.returnsUnit) {
            value?.let { emitInto(it, b, Sink.Discard) }
            b.add("return")
        } else {
            emitInto(value, b, Sink.Return)
        }
    }

    private fun localProperty(p: KtProperty, b: Block) {
        if (p.hasDelegate()) {
            b.add("\$L", problem(p, "Delegated local variables aren't supported").code)
            return
        }
        val symbol = p.symbol as KaVariableSymbol
        val type = types.map(symbol.returnType)
        val initializer = p.initializer
        val name = b.scope.declare(p.name!!)
        if (initializer == null) {
            names[symbol] = name
            b.add("\$T \$N", type, name)
            return
        }
        val unwrapped = KtPsiUtil.safeDeparenthesize(initializer)
        when {
            unwrapped is KtIfExpression || unwrapped is KtWhenExpression || unwrapped is KtTryExpression -> {
                if (canInline(unwrapped)) {
                    val value = expr(unwrapped, b)
                    names[symbol] = name
                    b.add("\$T \$N = \$L", type, name, value.code)
                } else {
                    b.add("\$T \$N", type, name)
                    emitInto(unwrapped, b, Sink.Assign(CodeBlock.of("\$N", name)))
                    names[symbol] = name
                }
            }
            unwrapped is KtBinaryExpression && unwrapped.operationToken == KtTokens.ELVIS -> {
                // val x = a ?: b  ->  T x = a; if (x == null) { return ... }, or a single assignment so x stays
                // effectively final for lambdas
                val left = expr(unwrapped.left!!, b)
                val rightExpression = unwrapped.right!!
                if (!isNothing(rightExpression)) {
                    val scratch = b.child()
                    val right = expr(rightExpression, scratch)
                    if (scratch.isEmpty) {
                        val subject = if (left.simple) left else {
                            val tmp = b.scope.declare("${p.name}OrNull")
                            b.add("var \$N = \$L", tmp, left.code)
                            JExpr.name(tmp)
                        }
                        names[symbol] = name
                        b.add("\$T \$N = \$L != null ? \$L : \$L", type, name, subject.wrap(Prec.EQ), subject.wrap(Prec.OR), right.wrap(Prec.TERNARY))
                        return
                    }
                }
                b.add("\$T \$N = \$L", type.box().let { if (type.isPrimitive) it else type }, name, left.code)
                val inner = b.child()
                val right = unwrapped.right!!
                if (isNothing(right)) {
                    emitInto(right, inner, Sink.Discard)
                } else {
                    emitInto(right, inner, Sink.Assign(CodeBlock.of("\$N", name)))
                }
                b.code.beginControlFlow("if (\$N == null)", name).add(inner.build()).endControlFlow()
                names[symbol] = name
            }
            else -> {
                val value = expr(initializer, b)
                names[symbol] = name
                b.add("\$T \$N = \$L", type, name, value.code)
            }
        }
    }

    /** `fun add(x: T) { ... }` inside a function becomes a local `Consumer<T> add = x -> { ... };` */
    private fun localFunction(f: KtNamedFunction, b: Block) {
        val symbol = f.symbol as KaNamedFunctionSymbol
        val params = symbol.valueParameters
        if (params.size > 2 || symbol.receiverParameter != null) {
            b.add("\$L", problem(f, "Local functions need at most two parameters and no receiver").code)
            return
        }
        val unit = ft.isUnit(symbol.returnType)
        val boxed = params.map { types.map(it.returnType, boxed = true) }
        val result = types.map(symbol.returnType, boxed = true)
        val fn = "java.util.function"
        val type: TypeName = when (params.size) {
            0 -> if (unit) ClassName.get(Runnable::class.java) else ParameterizedTypeName.get(ClassName.get(fn, "Supplier"), result)
            1 -> if (unit) ParameterizedTypeName.get(ClassName.get(fn, "Consumer"), boxed[0]) else ParameterizedTypeName.get(ClassName.get(fn, "Function"), boxed[0], result)
            else -> if (unit) ParameterizedTypeName.get(ClassName.get(fn, "BiConsumer"), boxed[0], boxed[1]) else ParameterizedTypeName.get(ClassName.get(fn, "BiFunction"), boxed[0], boxed[1], result)
        }
        val name = b.scope.declare(f.name!!)
        names[symbol] = name
        val saved = root
        val scope = b.scope.child()
        root = scope
        val paramNames = f.valueParameters.map { p -> scope.declare(p.name!!).also { names[p.symbol] = it } }
        val body = body(f, scope, unit)
        root = saved
        val head = if (paramNames.size == 1) paramNames[0] else "(${paramNames.joinToString(", ")})"
        b.code.add("\$T \$N = \$L -> {\n", type, name, head).indent().add(body).unindent().add("};\n")
    }

    private fun destructuring(d: KtDestructuringDeclaration, b: Block) {
        val initializer = d.initializer ?: return
        val value = expr(initializer, b)
        val source = if (value.simple) value.code else {
            val tmp = b.scope.declare("tmp")
            b.add("var \$N = \$L", tmp, value.code)
            CodeBlock.of("\$N", tmp)
        }
        destructureEntries(d.entries, source, initializer.expressionType, b)
    }

    private fun destructureEntries(entries: List<KtDestructuringDeclarationEntry>, source: CodeBlock, sourceType: KaType?, b: Block) {
        for (entry in entries) {
            if (entry.name == "_") continue
            val symbol = entry.symbol
            val name = b.scope.declare(entry.name!!)
            names[symbol] = name
            val component = component(entry, source, sourceType)
            b.add("var \$N = \$L", name, component)
        }
    }

    /** `componentN()` of a destructuring entry, chosen by the type being destructured. */
    private fun component(entry: KtDestructuringDeclarationEntry, source: CodeBlock, sourceType: KaType?): CodeBlock {
        val index = (entry.parent as KtDestructuringDeclaration).entries.indexOf(entry)
        val id = sourceType?.classIdString() ?: ""
        val accessors = when (id) {
            "kotlin/collections/Map.Entry", "kotlin/collections/MutableMap.MutableEntry" -> listOf("getKey", "getValue")
            "kotlin/Pair" -> listOf("getFirst", "getSecond")
            "kotlin/collections/IndexedValue" -> listOf("getIndex", "getValue")
            else -> null
        }
        if (accessors != null) return CodeBlock.of("\$L.\$N()", source, accessors[index])
        if (id.startsWith("kotlin/collections/")) return CodeBlock.of("\$L.get(\$L)", source, index)
        val cls = ((sourceType as? KaClassType)?.symbol as? KaNamedClassSymbol)
        val param = (cls?.psi as? KtClass)?.primaryConstructorParameters?.filter { it.hasValOrVar() }?.getOrNull(index)
        return if (cls?.isData == true && param != null) {
            CodeBlock.of("\$L.\$N()", source, getterName(param.name!!))
        } else {
            problem(entry, "Unsupported destructuring of $id").code
        }
    }

    private fun isMapEntry(type: KaType?): Boolean {
        val id = type?.classIdString() ?: return false
        return id == "kotlin/collections/Map.Entry" || id == "kotlin/collections/MutableMap.MutableEntry"
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Control flow
    // ---------------------------------------------------------------------------------------------------------------

    /** Whether a branching expression can be a ternary: every branch and condition translates without statements. */
    private fun canInline(e: KtExpression): Boolean {
        val probe = BodyTranslator(ft, ctx).also {
            it.names.putAll(names)
            it.implicitReceivers.putAll(implicitReceivers)
            it.overrides.putAll(overrides)
            it.functions.addAll(functions)
            it.root = root
            it.silent = true
        }
        val block = Block(root.child())
        return try {
            when (e) {
                is KtIfExpression -> {
                    val els = e.`else` ?: return false
                    val then = e.then ?: return false
                    if (probe.isNothing(then) || probe.isNothing(els)) return false
                    probe.expr(e.condition!!, block)
                    probe.branchValue(then, block)
                    probe.branchValue(els, block)
                    block.isEmpty
                }
                is KtWhenExpression -> {
                    if (e.elseExpression == null) return false
                    if (e.subjectVariable != null) return false
                    val subject = e.subjectExpression
                    if (subject != null && !probe.expr(subject, block).simple) return false
                    for (entry in e.entries) {
                        val value = entry.expression ?: return false
                        if (probe.isNothing(value)) return false
                        for (c in entry.conditions) probe.condition(c, subject?.let { probe.expr(it, block) }, block)
                        probe.branchValue(value, block)
                    }
                    block.isEmpty
                }
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }

    private var silent = false

    /** Value of a branch body: a block's last expression, or the expression itself. */
    private fun branchValue(e: KtExpression, b: Block): JExpr {
        if (e is KtBlockExpression) {
            val statements = e.statements
            statements.dropLast(1).forEach { emitInto(it, b, Sink.Discard) }
            val last = statements.lastOrNull() ?: return JExpr.NULL
            return expr(last, b)
        }
        return expr(e, b)
    }

    private fun ifInto(e: KtIfExpression, b: Block, sink: Sink) {
        val condition = expr(e.condition!!, b)
        val then = b.child()
        e.then?.let { emitInto(it, then, sink) }
        b.code.beginControlFlow("if (\$L)", condition.code).add(then.build())
        var els = e.`else`
        while (els != null) {
            val unwrapped = KtPsiUtil.safeDeparenthesize(els)
            if (unwrapped is KtIfExpression) {
                val probe = b.child()
                val nextCondition = expr(unwrapped.condition!!, probe)
                if (probe.isEmpty) {
                    val branch = b.child()
                    unwrapped.then?.let { emitInto(it, branch, sink) }
                    b.code.nextControlFlow("else if (\$L)", nextCondition.code).add(branch.build())
                    els = unwrapped.`else`
                    continue
                }
            }
            val branch = b.child()
            emitInto(els, branch, sink)
            b.code.nextControlFlow("else").add(branch.build())
            break
        }
        if (els == null && sink is Sink.Assign) {
            b.code.nextControlFlow("else").add("\$L = null;\n", sink.target)
        }
        b.code.endControlFlow()
    }

    private fun whenSubject(e: KtWhenExpression, b: Block): JExpr? {
        e.subjectVariable?.let { variable ->
            localProperty(variable, b)
            return JExpr.name(names.getValue(variable.symbol))
        }
        val subject = e.subjectExpression ?: return null
        val value = expr(subject, b)
        if (value.simple) return value
        val tmp = b.scope.declare("subject")
        b.add("\$T \$N = \$L", types.map(subject.expressionType!!), tmp, value.code)
        return JExpr.name(tmp)
    }

    private fun condition(c: KtWhenCondition, subject: JExpr?, b: Block): CodeBlock = when (c) {
        is KtWhenConditionWithExpression -> {
            val value = expr(c.expression!!, b)
            if (subject == null) {
                value.code
            } else {
                equality(subject, value, c.expression!!.expressionType, c.expression!!.expressionType, negate = false).code
            }
        }
        is KtWhenConditionIsPattern -> {
            val type = c.typeReference!!.type
            val test = CodeBlock.of("\$L instanceof \$T", subject!!.wrap(Prec.REL), erasure(type))
            if (c.isNegated) CodeBlock.of("!(\$L)", test) else test
        }
        is KtWhenConditionInRange -> {
            val range = c.rangeExpression!!
            val contains = containsCode(range, subject!!, b)
            if (c.isNegated) CodeBlock.of("!\$L", contains.wrap(Prec.UNARY)) else contains.code
        }
        else -> problem(c, "Unsupported when condition").code
    }

    private fun containsCode(range: KtExpression, value: JExpr, b: Block): JExpr {
        val unwrapped = KtPsiUtil.safeDeparenthesize(range)
        if (unwrapped is KtBinaryExpression && unwrapped.operationReference.getReferencedName() == "..") {
            val low = expr(unwrapped.left!!, b)
            val high = expr(unwrapped.right!!, b)
            return JExpr.of("\$L >= \$L && \$L <= \$L", value.wrap(Prec.REL), low.wrap(Prec.REL), value.wrap(Prec.REL), high.wrap(Prec.REL), prec = Prec.AND)
        }
        val collection = expr(range, b)
        val method = if (range.expressionType?.classIdString()?.endsWith("Map") == true) "containsKey" else "contains"
        return JExpr.call("\$L.\$N(\$L)", collection.wrap(Prec.PRIMARY), method, value.code)
    }

    private fun whenInto(e: KtWhenExpression, b: Block, sink: Sink) {
        val subject = whenSubject(e, b)
        var current = b
        var opened = false
        val entries = e.entries
        val closers = mutableListOf<Block>()
        for ((index, entry) in entries.withIndex()) {
            val body = current.child()
            entry.expression?.let { emitInto(it, body, sink) }
            if (entry.isElse) {
                if (opened) current.code.nextControlFlow("else").add(body.build()) else current.code.add(body.build())
                break
            }
            val probe = current.child()
            val conditions = entry.conditions.map { condition(it, subject, probe) }
            val test = if (conditions.size == 1) conditions[0] else CodeBlock.join(conditions.map { CodeBlock.of("(\$L)", it) }, " || ")
            if (!probe.isEmpty) {
                // The condition needs statements, so it can't be an `else if`: nest the rest inside an else block.
                if (opened) {
                    current.code.nextControlFlow("else")
                    val nested = current.child()
                    closers.add(current)
                    current = nested
                }
                current.code.add(probe.build())
                current.code.beginControlFlow("if (\$L)", test).add(body.build())
                opened = true
            } else if (opened) {
                current.code.nextControlFlow("else if (\$L)", test).add(body.build())
            } else {
                current.code.beginControlFlow("if (\$L)", test).add(body.build())
                opened = true
            }
            if (index == entries.lastIndex && sink !is Sink.Discard) {
                current.code.nextControlFlow("else")
                    .add("throw new \$T(\$S);\n", ISE, "Unexpected when branch")
            }
        }
        if (opened) current.code.endControlFlow()
        for (outer in closers.reversed()) {
            outer.code.add(current.build())
            outer.code.endControlFlow()
            current = outer
        }
    }

    private fun tryInto(e: KtTryExpression, b: Block, sink: Sink) {
        val body = b.child()
        emitInto(e.tryBlock, body, sink)
        b.code.beginControlFlow("try").add(body.build())
        for (clause in e.catchClauses) {
            val param = clause.catchParameter!!
            val catchBlock = b.child()
            val name = catchBlock.scope.declare(param.name!!)
            names[param.symbol] = name
            clause.catchBody?.let { emitInto(it, catchBlock, sink) }
            b.code.nextControlFlow("catch (\$T \$N)", types.map(param.symbol.returnType), name).add(catchBlock.build())
        }
        e.finallyBlock?.let { fin ->
            val finBlock = b.child()
            emitInto(fin.finalExpression, finBlock, Sink.Discard)
            b.code.nextControlFlow("finally").add(finBlock.build())
        }
        b.code.endControlFlow()
    }

    private fun forLoop(e: KtForExpression, b: Block, label: String?) {
        val prefix = label?.let { "$it: " } ?: ""
        val range = KtPsiUtil.safeDeparenthesize(e.loopRange!!)
        val body = b.child()
        val param = e.loopParameter!!
        functions.addLast(FnCtx(FnCtx.Kind.FUNCTION, emptySet(), functions.lastOrNull()?.returnsUnit ?: true).also { })
        functions.removeLast()

        // Numeric ranges become classic for loops
        if (range is KtBinaryExpression && param.destructuringDeclaration == null) {
            val op = range.operationReference.getReferencedName()
            val bound = when (op) {
                ".." -> "<="
                "until", "..<" -> "<"
                "downTo" -> ">="
                else -> null
            }
            if (bound != null) {
                val start = expr(range.left!!, b)
                val end = expr(range.right!!, b)
                val name = body.scope.declare(param.name!!)
                names[param.symbol] = name
                e.body?.let { emitInto(it, body, Sink.Discard) }
                val step = if (op == "downTo") "--" else "++"
                b.code.beginControlFlow(
                    "${prefix}for (\$T \$N = \$L; \$N $bound \$L; \$N$step)",
                    types.map(param.symbol.returnType), name, start.code, name, end.code, name
                ).add(body.build()).endControlFlow()
                return
            }
        }
        val rangeType = range.expressionType
        val rangeId = rangeType?.classIdString() ?: ""
        val source = expr(range, b)
        val iterable = when {
            rangeId.endsWith("Map") -> CodeBlock.of("\$L.entrySet()", source.wrap(Prec.PRIMARY))
            rangeId == "kotlin/String" || rangeId == "kotlin/CharSequence" -> CodeBlock.of("\$L.toCharArray()", source.wrap(Prec.PRIMARY))
            else -> source.code
        }
        val destructuring = param.destructuringDeclaration
        if (destructuring != null) {
            val element = body.scope.declare(if (rangeId.endsWith("Map")) "entry" else "item")
            destructureEntries(destructuring.entries, CodeBlock.of("\$N", element), param.symbol.returnType, body)
            e.body?.let { emitInto(it, body, Sink.Discard) }
            b.code.beginControlFlow("${prefix}for (var \$N : \$L)", element, iterable).add(body.build()).endControlFlow()
        } else {
            val name = body.scope.declare(param.name!!)
            names[param.symbol] = name
            e.body?.let { emitInto(it, body, Sink.Discard) }
            b.code.beginControlFlow("${prefix}for (var \$N : \$L)", name, iterable)
                .add(body.build()).endControlFlow()
        }
    }

    private fun whileLoop(e: KtWhileExpression, b: Block, label: String?) {
        val prefix = label?.let { "$it: " } ?: ""
        val probe = b.child()
        val condition = expr(e.condition!!, probe)
        val body = b.child()
        if (probe.isEmpty) {
            e.body?.let { emitInto(it, body, Sink.Discard) }
            b.code.beginControlFlow("${prefix}while (\$L)", condition.code).add(body.build()).endControlFlow()
        } else {
            body.addCode(probe.build())
            body.code.beginControlFlow("if (!\$L)", condition.wrap(Prec.UNARY)).add("break;\n").endControlFlow()
            e.body?.let { emitInto(it, body, Sink.Discard) }
            b.code.beginControlFlow("${prefix}while (true)").add(body.build()).endControlFlow()
        }
    }

    private fun doWhileLoop(e: KtDoWhileExpression, b: Block, label: String?) {
        val prefix = label?.let { "$it: " } ?: ""
        val body = b.child()
        e.body?.let { emitInto(it, body, Sink.Discard) }
        val condition = expr(e.condition!!, body)
        b.code.add("${prefix}do {\n").indent().add(body.build()).unindent().add("} while (\$L);\n", condition.code)
    }

    // ===============================================================================================================
    // Expressions
    // ===============================================================================================================

    fun expr(e: KtExpression, b: Block, discard: Boolean = false): JExpr {
        overrides[e]?.let { return it }
        return when (e) {
            is KtConstantExpression -> constant(e)
            is KtStringTemplateExpression -> template(e, b)
            is KtParenthesizedExpression -> expr(e.expression!!, b, discard)
            is KtAnnotatedExpression -> expr(e.baseExpression!!, b, discard)
            is KtLabeledExpression -> expr(e.baseExpression!!, b, discard)
            is KtThisExpression -> thisExpression(e)
            is KtSimpleNameExpression -> smartCast(e, nameReference(e, b, discard))
            is KtDotQualifiedExpression -> smartCast(e, member(e.selectorExpression!!, b, discard))
            is KtSafeQualifiedExpression -> safeCall(e, b, discard)
            is KtCallExpression -> smartCast(e, member(e, b, discard))
            is KtBinaryExpression -> binary(e, b, discard)
            is KtPrefixExpression -> prefix(e, b)
            is KtPostfixExpression -> postfix(e, b)
            is KtIsExpression -> {
                val value = expr(e.leftHandSide, b)
                val test = JExpr.of("\$L instanceof \$T", value.wrap(Prec.REL), erasure(e.typeReference!!.type), prec = Prec.REL)
                if (e.isNegated) JExpr.of("!\$L", test.wrap(Prec.UNARY), prec = Prec.UNARY) else test
            }
            is KtBinaryExpressionWithTypeRHS -> cast(e, b)
            is KtIfExpression, is KtWhenExpression, is KtTryExpression -> branching(e, b)
            is KtLambdaExpression -> javaLambda(e, b)
            is KtCallableReferenceExpression -> callableReference(e)
            is KtClassLiteralExpression -> classLiteral(e, b)
            is KtArrayAccessExpression -> member(e, b, discard)
            is KtReturnExpression, is KtThrowExpression, is KtBreakExpression, is KtContinueExpression -> {
                emitInto(e, b, Sink.Discard)
                UNREACHABLE
            }
            is KtBlockExpression -> {
                emitInto(e, b, Sink.Discard)
                UNREACHABLE
            }
            else -> problem(e, "Unsupported expression ${e::class.simpleName}")
        }
    }

    private fun constant(e: KtConstantExpression): JExpr {
        val text = e.text.replace("_", "")
        val id = e.expressionType?.classIdString()
        return when (e.node.elementType) {
            KtNodeTypes.NULL -> JExpr.NULL
            KtNodeTypes.INTEGER_CONSTANT -> when (id) {
                "kotlin/Long" -> JExpr.of(if (text.endsWith("L")) text else "${text}L", simple = true)
                "kotlin/Double" -> JExpr.of("$text.0", simple = true)
                "kotlin/Float" -> JExpr.of("${text}f", simple = true)
                else -> JExpr.of(text.removeSuffix("L"), simple = true)
            }
            else -> JExpr.of(text, simple = true)
        }
    }

    private fun template(e: KtStringTemplateExpression, b: Block): JExpr {
        val parts = mutableListOf<CodeBlock>()
        val literal = StringBuilder()
        var hasExpression = false
        fun flush() {
            if (literal.isNotEmpty()) {
                parts.add(CodeBlock.of("\$S", literal.toString()))
                literal.clear()
            }
        }
        for (entry in e.entries) {
            when (entry) {
                is KtLiteralStringTemplateEntry -> literal.append(entry.text)
                is KtEscapeStringTemplateEntry -> literal.append(entry.unescapedValue)
                is KtStringTemplateEntryWithExpression -> {
                    flush()
                    hasExpression = true
                    parts.add(expr(entry.expression!!, b).wrap(Prec.MUL))
                }
            }
        }
        flush()
        if (parts.isEmpty()) return JExpr.of("\$S", "", simple = true)
        if (!hasExpression) return JExpr.of("\$L", parts.single(), simple = true)
        if (parts.size == 1) return JExpr.of("\$T.valueOf(\$L)", STRING, parts.single())
        // Make sure the concatenation starts with a String
        val first = e.entries.first()
        if (first is KtStringTemplateEntryWithExpression && parts.size >= 2 &&
            e.entries.getOrNull(1) is KtStringTemplateEntryWithExpression
        ) {
            parts.add(0, CodeBlock.of("\"\""))
        }
        return JExpr(CodeBlock.join(parts, " + "), Prec.ADD)
    }

    private fun thisExpression(e: KtThisExpression): JExpr {
        val target = e.instanceReference.mainReference.resolveToSymbol()
        if (target != null && implicitReceivers.containsKey(target)) return implicitReceivers[target] ?: JExpr.THIS
        val label = e.getLabelName()
        if (label != null && target is KaClassSymbol && target != ctx.cls) {
            return JExpr.of("\$T.this", ft.javaNameOf(target))
        }
        // Receivers of lambdas and extension functions are looked up by their owner
        if (target is KaCallableSymbol) {
            target.receiverParameter?.let { implicitReceivers[it]?.let { r -> return r } }
            implicitReceivers[target]?.let { return it }
        }
        return JExpr.THIS
    }

    /** Wraps [value] in a cast when Kotlin smart casts [e] to a different class than it's declared as. */
    private fun smartCast(e: KtExpression, value: JExpr): JExpr {
        if (value === UNREACHABLE) return value
        val info = e.smartCastInfo ?: return value
        if (!info.isStable) return value
        val declared = declaredType(e) ?: return value
        val cast = castTarget(info.smartCastType, declared) ?: return value
        if (erasure(cast) == erasure(declared)) return value
        return JExpr.of("((\$T) \$L)", types.map(cast, boxed = true), value.wrap(Prec.UNARY))
    }

    private fun castTarget(type: KaType, declared: KaType): KaType? = when (type) {
        is KaIntersectionType -> type.conjuncts.firstOrNull { erasure(it) != erasure(declared) }
        else -> type
    }

    /** The type Java sees for [e] before any smart cast: the declared type of the referenced variable or call. */
    private fun declaredType(e: KtExpression): KaType? {
        val selector = when (e) {
            is KtDotQualifiedExpression -> e.selectorExpression
            is KtSafeQualifiedExpression -> e.selectorExpression
            else -> e
        } ?: return null
        val call = selector.resolveToCall()?.singleCallOrNull<KaCallableMemberCall<*, *>>() ?: return null
        val symbol = call.symbol
        if (symbol is KaFunctionSymbol && symbol !is KaConstructorSymbol) {
            return types.let { symbol.returnType }
        }
        return symbol.returnType
    }

    fun erasure(type: KaType): TypeName {
        val mapped = types.map(type, boxed = true)
        return when (mapped) {
            is ParameterizedTypeName -> mapped.rawType
            is TypeVariableName -> OBJECT
            else -> mapped
        }
    }

    private fun cast(e: KtBinaryExpressionWithTypeRHS, b: Block): JExpr {
        val value = expr(e.left, b)
        val type = e.right!!.type
        val target = types.map(type, boxed = true)
        return when (e.operationReference.getReferencedNameElementType()) {
            KtTokens.AS_KEYWORD -> JExpr.of("(\$T) \$L", target, value.wrap(Prec.UNARY), prec = Prec.UNARY)
            KtTokens.AS_SAFE -> {
                val subject = simpleOrTemp(value, e.left.expressionType, b, "value")
                JExpr.of(
                    "\$L instanceof \$T ? (\$T) \$L : null",
                    subject.wrap(Prec.REL), erasure(type), target, subject.wrap(Prec.UNARY), prec = Prec.TERNARY
                )
            }
            else -> problem(e, "Unsupported cast")
        }
    }

    private fun simpleOrTemp(value: JExpr, type: KaType?, b: Block, base: String): JExpr {
        if (value.simple) return value
        val tmp = b.scope.declare(base)
        val javaType = type?.let { types.map(it, boxed = true) }
        if (javaType == null || javaType == OBJECT && type.classIdString() != "kotlin/Any") {
            b.add("var \$N = \$L", tmp, value.code)
        } else {
            b.add("\$T \$N = \$L", javaType, tmp, value.code)
        }
        return JExpr.name(tmp)
    }

    private fun branching(e: KtExpression, b: Block): JExpr {
        if (canInline(e)) return ternary(e, b)
        val type = e.expressionType
        val tmp = b.scope.declare("result")
        val javaType = type?.let { types.map(it, boxed = true) } ?: OBJECT
        b.add("\$T \$N", javaType, tmp)
        emitInto(e, b, Sink.Assign(CodeBlock.of("\$N", tmp)))
        return JExpr.name(tmp)
    }

    private fun ternary(e: KtExpression, b: Block): JExpr = when (e) {
        is KtIfExpression -> {
            val condition = expr(e.condition!!, b)
            val then = branchValue(e.then!!, b)
            val els = branchValue(e.`else`!!, b)
            JExpr.of("\$L ? \$L : \$L", condition.wrap(Prec.OR), then.wrap(Prec.OR), els.wrap(Prec.TERNARY), prec = Prec.TERNARY)
        }
        is KtWhenExpression -> {
            val subject = e.subjectExpression?.let { expr(it, b) }
            var result: CodeBlock? = null
            for (entry in e.entries.reversed()) {
                val value = branchValue(entry.expression!!, b)
                result = if (entry.isElse) {
                    value.wrap(Prec.TERNARY)
                } else {
                    val conditions = entry.conditions.map { condition(it, subject, b) }
                    val test = CodeBlock.join(conditions.map { CodeBlock.of("(\$L)", it) }, " || ")
                    CodeBlock.of("\$L ? \$L : \$L", test, value.wrap(Prec.OR), result!!)
                }
            }
            JExpr(result!!, Prec.TERNARY)
        }
        else -> problem(e, "Unsupported ternary")
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Null safety
    // ---------------------------------------------------------------------------------------------------------------

    private fun safeCall(e: KtSafeQualifiedExpression, b: Block, discard: Boolean): JExpr {
        val receiver = expr(e.receiverExpression, b)
        val subject = simpleOrTemp(receiver, e.receiverExpression.expressionType, b, "receiver")
        overrides[e.receiverExpression] = subject
        val inner = b.child()
        val selector = e.selectorExpression!!
        val value = smartCast(e, member(selector, inner, discard))
        overrides.remove(e.receiverExpression)
        if (discard) {
            b.code.beginControlFlow("if (\$L != null)", subject.wrap(Prec.EQ)).add(inner.build())
            if (value.statement) b.code.add("\$L;\n", value.code)
            b.code.endControlFlow()
            return UNREACHABLE
        }
        if (inner.isEmpty) {
            return JExpr.of("\$L == null ? null : \$L", subject.wrap(Prec.EQ), value.wrap(Prec.TERNARY), prec = Prec.TERNARY)
        }
        val tmp = b.scope.declare("result")
        val type = e.expressionType?.let { types.map(it, boxed = true) } ?: OBJECT
        b.add("\$T \$N = null", type, tmp)
        b.code.beginControlFlow("if (\$L != null)", subject.wrap(Prec.EQ)).add(inner.build())
            .add("\$N = \$L;\n", tmp, value.code).endControlFlow()
        return JExpr.name(tmp)
    }

    private fun elvis(e: KtBinaryExpression, b: Block): JExpr {
        val left = expr(e.left!!, b)
        val right = e.right!!
        if (isNothing(right)) {
            val subject = simpleOrTemp(left, e.left!!.expressionType, b, "value")
            val inner = b.child()
            emitInto(right, inner, Sink.Discard)
            b.code.beginControlFlow("if (\$L == null)", subject.wrap(Prec.EQ)).add(inner.build()).endControlFlow()
            return subject
        }
        val inner = b.child()
        val rightValue = expr(right, inner)
        if (inner.isEmpty) {
            val subject = simpleOrTemp(left, e.left!!.expressionType, b, "value")
            return JExpr.of("\$L != null ? \$L : \$L", subject.wrap(Prec.EQ), subject.wrap(Prec.OR), rightValue.wrap(Prec.TERNARY), prec = Prec.TERNARY)
        }
        val tmp = b.scope.declare("value")
        val type = e.expressionType?.let { types.map(it, boxed = true) } ?: OBJECT
        b.add("\$T \$N = \$L", type, tmp, left.code)
        b.code.beginControlFlow("if (\$N == null)", tmp).add(inner.build()).add("\$N = \$L;\n", tmp, rightValue.code).endControlFlow()
        return JExpr.name(tmp)
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Operators
    // ---------------------------------------------------------------------------------------------------------------

    private fun binary(e: KtBinaryExpression, b: Block, discard: Boolean): JExpr {
        val token = e.operationToken
        return when (token) {
            KtTokens.ELVIS -> elvis(e, b)
            KtTokens.EQ -> assignment(e, b)
            KtTokens.ANDAND, KtTokens.OROR -> logical(e, b)
            KtTokens.EQEQ, KtTokens.EXCLEQ -> {
                val left = expr(e.left!!, b)
                val right = expr(e.right!!, b)
                equality(left, right, e.left!!.expressionType, e.right!!.expressionType, token == KtTokens.EXCLEQ)
            }
            KtTokens.EQEQEQ, KtTokens.EXCLEQEQEQ -> {
                val left = expr(e.left!!, b)
                val right = expr(e.right!!, b)
                JExpr.of("\$L ${if (token == KtTokens.EQEQEQ) "==" else "!="} \$L", left.wrap(Prec.EQ), right.wrap(Prec.REL), prec = Prec.EQ)
            }
            KtTokens.IN_KEYWORD, KtTokens.NOT_IN -> {
                val contains = member(e, b, false)
                if (token == KtTokens.NOT_IN) JExpr.of("!\$L", contains.wrap(Prec.UNARY), prec = Prec.UNARY) else contains
            }
            KtTokens.PLUSEQ, KtTokens.MINUSEQ, KtTokens.MULTEQ, KtTokens.DIVEQ, KtTokens.PERCEQ -> compoundAssignment(e, b)
            KtTokens.LT, KtTokens.GT, KtTokens.LTEQ, KtTokens.GTEQ -> comparison(e, b)
            KtTokens.PLUS, KtTokens.MINUS, KtTokens.MUL, KtTokens.DIV, KtTokens.PERC -> arithmetic(e, b)
            else -> member(e, b, discard) // infix calls: to, until, and, or, ...
        }
    }

    private fun logical(e: KtBinaryExpression, b: Block): JExpr {
        val and = e.operationToken == KtTokens.ANDAND
        val left = expr(e.left!!, b)
        val inner = b.child()
        val right = expr(e.right!!, inner)
        val op = if (and) "&&" else "||"
        val prec = if (and) Prec.AND else Prec.OR
        if (inner.isEmpty) return JExpr.of("\$L $op \$L", left.wrap(prec), right.wrap(prec + 1), prec = prec)
        val tmp = b.scope.declare("condition")
        b.add("boolean \$N = \$L", tmp, left.code)
        b.code.beginControlFlow(if (and) "if (\$N)" else "if (!\$N)", tmp).add(inner.build())
            .add("\$N = \$L;\n", tmp, right.code).endControlFlow()
        return JExpr.name(tmp)
    }

    fun isPrimitiveType(type: KaType?): Boolean {
        type ?: return false
        if (type.nullability == KaTypeNullability.NULLABLE) return false
        return types.map(type).isPrimitive
    }

    private fun isEnum(type: KaType?): Boolean =
        ((type as? KaClassType)?.symbol as? KaClassSymbol)?.classKind == KaClassKind.ENUM_CLASS

    fun equality(left: JExpr, right: JExpr, leftType: KaType?, rightType: KaType?, negate: Boolean): JExpr {
        val nullCheck = left.code.toString() == "null" || right.code.toString() == "null"
        val identity = nullCheck || (isPrimitiveType(leftType) && isPrimitiveType(rightType)) ||
            (isEnum(leftType) && isEnum(rightType))
        return if (identity) {
            JExpr.of("\$L ${if (negate) "!=" else "=="} \$L", left.wrap(Prec.EQ), right.wrap(Prec.REL), prec = Prec.EQ)
        } else {
            val call = JExpr.of("\$T.equals(\$L, \$L)", OBJECTS, left.code, right.code)
            if (negate) JExpr.of("!\$L", call.code, prec = Prec.UNARY) else call
        }
    }

    private fun operatorCall(e: KtExpression): KaFunctionCall<*>? = e.resolveToCall()?.singleCallOrNull<KaFunctionCall<*>>()

    private fun isBuiltinOperator(call: KaFunctionCall<*>?): Boolean {
        val id = call?.symbol?.callableId?.toString() ?: return true
        val owner = id.substringBeforeLast('.')
        return owner in setOf("kotlin/Int", "kotlin/Long", "kotlin/Double", "kotlin/Float", "kotlin/Short", "kotlin/Byte", "kotlin/Char", "kotlin/String", "kotlin/Boolean") ||
            id == "kotlin/plus"
    }

    private fun comparison(e: KtBinaryExpression, b: Block): JExpr {
        val op = e.operationReference.text
        val call = operatorCall(e)
        val left = expr(e.left!!, b)
        val right = expr(e.right!!, b)
        val primitive = call == null || (isBuiltinOperator(call) && call.symbol.callableId?.toString()?.startsWith("kotlin/String") != true)
        return if (primitive) {
            JExpr.of("\$L $op \$L", left.wrap(Prec.REL), right.wrap(Prec.REL + 1), prec = Prec.REL)
        } else {
            JExpr.of("\$L.compareTo(\$L) $op 0", left.wrap(Prec.PRIMARY), right.code, prec = Prec.REL)
        }
    }

    private fun arithmetic(e: KtBinaryExpression, b: Block): JExpr {
        val call = operatorCall(e)
        if (!isBuiltinOperator(call)) return member(e, b, false)
        val op = e.operationReference.text
        val prec = if (op == "+" || op == "-") Prec.ADD else Prec.MUL
        val left = expr(e.left!!, b)
        val right = expr(e.right!!, b)
        return JExpr.of("\$L $op \$L", left.wrap(prec), right.wrap(prec + 1), prec = prec)
    }

    private fun prefix(e: KtPrefixExpression, b: Block): JExpr {
        val base = e.baseExpression!!
        return when (e.operationToken) {
            KtTokens.EXCL -> JExpr.of("!\$L", expr(base, b).wrap(Prec.UNARY), prec = Prec.UNARY)
            KtTokens.MINUS -> JExpr.of("-\$L", expr(base, b).wrap(Prec.UNARY), prec = Prec.UNARY)
            KtTokens.PLUS -> expr(base, b)
            KtTokens.PLUSPLUS -> JExpr.of("++\$L", expr(base, b).code, prec = Prec.UNARY, statement = true)
            KtTokens.MINUSMINUS -> JExpr.of("--\$L", expr(base, b).code, prec = Prec.UNARY, statement = true)
            else -> problem(e, "Unsupported prefix operator ${e.operationReference.text}")
        }
    }

    private fun postfix(e: KtPostfixExpression, b: Block): JExpr {
        val base = e.baseExpression!!
        return when (e.operationToken) {
            KtTokens.EXCLEXCL -> {
                val value = expr(base, b)
                if (base.expressionType?.nullability != KaTypeNullability.NULLABLE) value
                else JExpr.call("\$T.requireNonNull(\$L)", OBJECTS, value.code)
            }
            KtTokens.PLUSPLUS -> JExpr.of("\$L++", expr(base, b).code, prec = Prec.UNARY, statement = true)
            KtTokens.MINUSMINUS -> JExpr.of("\$L--", expr(base, b).code, prec = Prec.UNARY, statement = true)
            else -> problem(e, "Unsupported postfix operator ${e.operationReference.text}")
        }
    }

    private fun assignment(e: KtBinaryExpression, b: Block): JExpr {
        val left = KtPsiUtil.safeDeparenthesize(e.left!!)
        val right = e.right!!
        if (left is KtArrayAccessExpression) return member(left, b, true, assigned = right)
        val selector = (left as? KtQualifiedExpression)?.selectorExpression ?: left
        val call = selector.resolveToCall()?.singleCallOrNull<KaCallableMemberCall<*, *>>()
        val symbol = call?.symbol
        return when (symbol) {
            is KaLocalVariableSymbol, is KaValueParameterSymbol -> {
                val value = expr(right, b)
                JExpr.of("\$N = \$L", localName(symbol), value.code, prec = Prec.ASSIGN, statement = true)
            }
            is KaBackingFieldSymbol -> {
                val value = expr(right, b)
                JExpr.of("\$L = \$L", backingField(symbol), value.code, prec = Prec.ASSIGN, statement = true)
            }
            is KaPropertySymbol -> {
                val receiver = receiverOf(call, b)
                val value = expr(right, b)
                writeProperty(symbol, receiver, value, left)
            }
            else -> problem(e, "Unsupported assignment target")
        }
    }

    private fun compoundAssignment(e: KtBinaryExpression, b: Block): JExpr {
        val resolved = e.resolveToCall()?.singleCallOrNull<KaCallableMemberCall<*, *>>()
        if (resolved is KaFunctionCall<*>) {
            // e.g. list += element  ->  plusAssign
            return member(e, b, true)
        }
        val left = KtPsiUtil.safeDeparenthesize(e.left!!)
        val op = e.operationReference.text
        val selector = (left as? KtQualifiedExpression)?.selectorExpression ?: left
        val symbol = selector.resolveToCall()?.singleCallOrNull<KaCallableMemberCall<*, *>>()?.symbol
        val value = expr(e.right!!, b)
        return when (symbol) {
            is KaLocalVariableSymbol, is KaValueParameterSymbol ->
                JExpr.of("\$N $op \$L", localName(symbol), value.code, prec = Prec.ASSIGN, statement = true)
            is KaPropertySymbol -> {
                val call = selector.resolveToCall()?.singleCallOrNull<KaCallableMemberCall<*, *>>()
                val receiver = call?.let { receiverOf(it, b) }
                val current = readProperty(symbol, receiver, left)
                val binaryOp = op.removeSuffix("=")
                writeProperty(symbol, receiver, JExpr.of("\$L $binaryOp \$L", current.wrap(Prec.ADD), value.wrap(Prec.MUL), prec = Prec.ADD), left)
            }
            else -> problem(e, "Unsupported compound assignment")
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // References and calls
    // ---------------------------------------------------------------------------------------------------------------

    fun localName(symbol: KaSymbol): String {
        names[symbol]?.let { return it }
        val name = (symbol as? KaVariableSymbol)?.name?.asString() ?: "unknown"
        return javaIdentifier(name)
    }

    private fun backingField(symbol: KaBackingFieldSymbol): CodeBlock {
        val property = symbol.owningProperty
        val name = javaIdentifier(property.name.asString())
        return if (ctx.isStatic) CodeBlock.of("\$T.\$N", ctx.javaClass, name) else CodeBlock.of("this.\$N", name)
    }

    private fun nameReference(e: KtSimpleNameExpression, b: Block, discard: Boolean): JExpr {
        val call = e.resolveToCall()?.singleCallOrNull<KaCallableMemberCall<*, *>>()
        if (call != null) return member(e, b, discard)
        // Not a call: a class or object reference
        val symbol = e.mainReference.resolveToSymbol()
        if (symbol is KaClassSymbol) return objectReference(symbol, e)
        return problem(e, "Unresolved reference ${e.getReferencedName()}")
    }

    private fun objectReference(symbol: KaClassSymbol, e: KtElement): JExpr = when {
        symbol.classKind == KaClassKind.OBJECT && !ft.isStaticHolder(symbol) ->
            JExpr.of("\$T.INSTANCE", ft.javaNameOf(symbol), simple = true)
        symbol.classKind == KaClassKind.OBJECT && symbol.classId?.asString() == "kotlin/Unit" -> JExpr.NULL
        else -> problem(e, "Class ${symbol.classId} used as a value")
    }

    private fun receiverOf(call: KaCallableMemberCall<*, *>, b: Block): JExpr? {
        val applied = call.partiallyAppliedSymbol
        return receiver(applied.dispatchReceiver, b) ?: receiver(applied.extensionReceiver, b)
    }

    fun receiver(value: KaReceiverValue?, b: Block): JExpr? = when (value) {
        null -> null
        is KaExplicitReceiverValue -> expr(value.expression, b)
        is KaImplicitReceiverValue -> implicitReceiver(value.symbol)
        is KaSmartCastedReceiverValue -> {
            val original = receiver(value.original, b)
            val declared = value.original.type
            if (original != null && erasure(value.type) != erasure(declared)) {
                JExpr.of("((\$T) \$L)", types.map(value.type, boxed = true), original.wrap(Prec.UNARY))
            } else {
                original
            }
        }
        else -> null
    }

    private fun implicitReceiver(symbol: KaSymbol): JExpr? {
        if (implicitReceivers.containsKey(symbol)) return implicitReceivers[symbol]
        if (symbol is KaClassSymbol) {
            if (ft.isStaticHolder(symbol)) return null
            if (symbol.classKind == KaClassKind.OBJECT) return JExpr.of("\$T.INSTANCE", ft.javaNameOf(symbol), simple = true)
            // Receiver of an enclosing class
            if (symbol != ctx.cls) return JExpr.of("\$T.this", ft.javaNameOf(symbol))
        }
        return null
    }

    /**
     * A call, property access, index access or operator call. [e] is the element the call resolves on, its receivers
     * come from the resolved call.
     */
    fun member(e: KtExpression, b: Block, discard: Boolean, assigned: KtExpression? = null): JExpr {
        val info = e.resolveToCall()
        val call = info?.singleCallOrNull<KaCallableMemberCall<*, *>>()
        if (call == null) {
            if (e is KtSimpleNameExpression) {
                val symbol = e.mainReference.resolveToSymbol()
                if (symbol is KaClassSymbol) return objectReference(symbol, e)
            }
            if (e is KtCallExpression) {
                val callee = e.calleeExpression
                if (callee is KtSimpleNameExpression && callee.mainReference.resolveToSymbol() is KaClassSymbol) {
                    return problem(e, "Unresolved constructor call ${e.text.take(40)}")
                }
            }
            return problem(e, "Unresolved call ${e.text.take(60)}")
        }
        val symbol = call.symbol
        val origin = symbol.origin
        val id = symbol.callableId?.toString()
        val ownCode = origin == KaSymbolOrigin.SOURCE || origin == KaSymbolOrigin.SOURCE_MEMBER_GENERATED ||
            symbol is KaLocalVariableSymbol || symbol is KaValueParameterSymbol || symbol is KaBackingFieldSymbol
        if (!ownCode) {
            return Stdlib(this).call(e, call, id ?: "", b, discard, assigned)
        }
        return when (symbol) {
            is KaValueParameterSymbol -> {
                val name = JExpr.name(localName(symbol))
                if (symbol in wildcardParameters && isValueUse(e)) exactType(symbol, name) else name
            }
            is KaLocalVariableSymbol -> JExpr.name(localName(symbol))
            is KaBackingFieldSymbol -> JExpr(backingField(symbol), Prec.PRIMARY, simple = true)
            is KaEnumEntrySymbol -> JExpr.of("\$T.\$N", (symbol.containingDeclaration as KaClassSymbol).let { ft.javaNameOf(it) }, symbol.name.asString(), simple = true)
            is KaPropertySymbol -> {
                if (symbol.name.asString() == "entries" && isEnumOwner(symbol)) {
                    return JExpr.of("\$T.asList(\$T.values())", ClassName.get("java.util", "Arrays"), ft.javaNameOf(symbol.containingDeclaration as KaClassSymbol))
                }
                val receiver = if (isStaticMember(symbol)) null else receiverOf(call, b)
                readProperty(symbol, receiver, e)
            }
            is KaConstructorSymbol -> {
                val fc = call as KaFunctionCall<*>
                val args = arguments(fc, b, e)
                val cls = symbol.containingDeclaration as? KaClassSymbol
                val type = e.expressionType?.let { types.map(it) } ?: cls?.let { ft.javaNameOf(it) } ?: OBJECT
                val raw = if (type is ParameterizedTypeName) CodeBlock.of("\$T<>", type.rawType) else CodeBlock.of("\$T", type)
                JExpr.call("new \$L(\$L)", raw, CodeBlock.join(args, ", "))
            }
            is KaNamedFunctionSymbol -> if (names.containsKey(symbol)) {
                val unit = ft.isUnit(symbol.returnType)
                val method = when {
                    symbol.valueParameters.isEmpty() -> if (unit) "run" else "get"
                    unit -> "accept"
                    else -> "apply"
                }
                JExpr.call("\$N.\$N(\$L)", names.getValue(symbol), method, CodeBlock.join(arguments(call as KaFunctionCall<*>, b, e), ", "))
            } else {
                functionCall(symbol, call as KaFunctionCall<*>, b, e)
            }
            else -> problem(e, "Unsupported call target ${symbol::class.simpleName}")
        }
    }

    /**
     * Parameters get wildcard types (`List<? extends T>`) for Kotlin's covariance; where one is used as a plain value
     * it's cast back to its exact Kotlin type, which Kotlin allows implicitly for read-only collections.
     */
    private fun exactType(symbol: KaVariableSymbol, value: JExpr): JExpr =
        JExpr.of("((\$T) \$L)", types.map(symbol.returnType, boxed = true), value.code)

    private fun isValueUse(e: KtExpression): Boolean {
        val parent = e.parent
        return when {
            parent is KtQualifiedExpression && parent.receiverExpression == e -> false
            parent is KtContainerNode && parent.parent is KtForExpression -> false
            parent is KtBinaryExpression && parent.operationToken in setOf(KtTokens.EQEQ, KtTokens.EXCLEQ, KtTokens.IN_KEYWORD, KtTokens.NOT_IN) -> false
            parent is KtIsExpression -> false
            parent is KtStringTemplateEntry -> false
            else -> true
        }
    }

    /** For scope functions returning their receiver: casts a wildcard parameter back to its exact type. */
    fun exactSubject(receiver: KtExpression?, value: JExpr): JExpr {
        val name = KtPsiUtil.safeDeparenthesize(receiver ?: return value) as? KtSimpleNameExpression ?: return value
        val symbol = name.resolveToCall()?.singleCallOrNull<KaCallableMemberCall<*, *>>()?.symbol as? KaValueParameterSymbol ?: return value
        return if (symbol in wildcardParameters) exactType(symbol, value) else value
    }

    private fun isEnumOwner(symbol: KaCallableSymbol) =
        (symbol.containingDeclaration as? KaClassSymbol)?.classKind == KaClassKind.ENUM_CLASS

    private fun isStaticMember(symbol: KaCallableSymbol): Boolean {
        val owner = symbol.containingDeclaration as? KaClassSymbol ?: return true
        return ft.isStaticHolder(owner)
    }

    private fun ownerClass(symbol: KaCallableSymbol): ClassName {
        val owner = symbol.containingDeclaration as? KaClassSymbol
        if (owner != null) return ft.javaNameOf(owner)
        val file = symbol.psi?.containingFile as? KtFile ?: return ctx.javaClass
        return facadeOf(file)
    }

    private fun facadeOf(file: KtFile): ClassName {
        val jvmName = file.fileAnnotationList?.annotationEntries
            ?.firstOrNull { it.shortName?.asString() == "JvmName" }
            ?.valueArguments?.firstOrNull()?.getArgumentExpression() as? KtStringTemplateExpression
        val name = jvmName?.entries?.joinToString("") { it.text } ?: (file.name.removeSuffix(".kt") + "Kt")
        return ClassName.get(file.packageFqName.asString(), name)
    }

    private fun isPrivate(symbol: KaCallableSymbol): Boolean =
        (symbol.psi as? KtModifierListOwner)?.hasModifier(KtTokens.PRIVATE_KEYWORD) == true

    private fun isConst(symbol: KaPropertySymbol): Boolean =
        (symbol as? KaKotlinPropertySymbol)?.isConst == true ||
            (symbol.psi as? KtAnnotated)?.let { ft.hasAnnotation(it, "JvmField") } == true

    fun readProperty(symbol: KaPropertySymbol, receiver: JExpr?, e: KtElement): JExpr {
        val name = symbol.name.asString()
        if (symbol.isExtension) {
            return JExpr.call("\$T.\$N(\$L)", ownerClass(symbol), getterName(name), receiver?.code ?: CodeBlock.of("this"))
        }
        val inInterface = (symbol.containingDeclaration as? KaClassSymbol)?.classKind == KaClassKind.INTERFACE
        val field = !inInterface && (isConst(symbol) || (isPrivate(symbol) && symbol.hasBackingField &&
            (symbol.psi as? KtProperty)?.getter?.hasBody() != true))
        if (isStaticMember(symbol)) {
            val owner = ownerClass(symbol)
            return if (field) {
                JExpr.of("\$T.\$N", owner, if (isConst(symbol)) name else javaIdentifier(name), simple = true)
            } else {
                JExpr.call("\$T.\$N()", owner, getterName(name))
            }
        }
        return if (field) {
            JExpr.of("\$L.\$N", (receiver ?: JExpr.THIS).wrap(Prec.PRIMARY), javaIdentifier(name), simple = receiver == null || receiver.simple)
        } else if (receiver == null) {
            JExpr.call("\$N()", getterName(name))
        } else {
            JExpr.call("\$L.\$N()", receiver.wrap(Prec.PRIMARY), getterName(name))
        }
    }

    private fun writeProperty(symbol: KaPropertySymbol, receiver: JExpr?, value: JExpr, e: KtElement): JExpr {
        val name = symbol.name.asString()
        val field = isPrivate(symbol) && symbol.hasBackingField && (symbol.psi as? KtProperty)?.setter?.hasBody() != true
        if (isStaticMember(symbol)) {
            val owner = ownerClass(symbol)
            return if (field) {
                JExpr.of("\$T.\$N = \$L", owner, javaIdentifier(name), value.code, prec = Prec.ASSIGN, statement = true)
            } else {
                JExpr.call("\$T.\$N(\$L)", owner, setterName(name), value.code)
            }
        }
        return if (field) {
            JExpr.of("\$L.\$N = \$L", (receiver ?: JExpr.THIS).wrap(Prec.PRIMARY), javaIdentifier(name), value.code, prec = Prec.ASSIGN, statement = true)
        } else if (receiver == null) {
            JExpr.call("\$N(\$L)", setterName(name), value.code)
        } else {
            JExpr.call("\$L.\$N(\$L)", receiver.wrap(Prec.PRIMARY), setterName(name), value.code)
        }
    }

    private fun functionCall(symbol: KaNamedFunctionSymbol, call: KaFunctionCall<*>, b: Block, e: KtElement): JExpr {
        val name = methodName(symbol, symbol.name.asString())
        val applied = call.partiallyAppliedSymbol
        val owner = symbol.containingDeclaration as? KaClassSymbol
        val static = owner == null || ft.isStaticHolder(owner)
        val dispatch = if (static) null else receiver(applied.dispatchReceiver, b)
        val extension = receiver(applied.extensionReceiver, b)
        val args = mutableListOf<CodeBlock>()
        if (symbol.isExtension) args.add(extension?.code ?: CodeBlock.of("this"))
        args.addAll(arguments(call, b, e))
        val joined = CodeBlock.join(args, ", ")
        return when {
            static -> JExpr.call("\$T.\$N(\$L)", ownerClass(symbol), name, joined)
            dispatch == null -> JExpr.call("\$N(\$L)", name, joined)
            else -> JExpr.call("\$L.\$N(\$L)", dispatch.wrap(Prec.PRIMARY), name, joined)
        }
    }

    /** Arguments in parameter order. Missing trailing defaults rely on the generated overloads. */
    fun arguments(call: KaFunctionCall<*>, b: Block, element: KtElement): List<CodeBlock> {
        val symbol = call.symbol
        val parameters = symbol.valueParameters
        val byParameter = LinkedHashMap<KaValueParameterSymbol, MutableList<KtExpression>>()
        for ((expression, signature) in call.argumentMapping) {
            byParameter.getOrPut(signature.symbol) { mutableListOf() }.add(expression)
        }
        val missing = parameters.indices.filter { parameters[it] !in byParameter }
        val defaulted = parameters.indices.filter { parameters[it].hasDefaultValue }
        // Overloads exist for "every defaulted parameter from some point onwards"
        val ownCode = symbol.origin == KaSymbolOrigin.SOURCE
        val useOverload = ownCode && !fillDefaults && missing.isNotEmpty() && missing.all { it in defaulted } &&
            defaulted.indices.any { start -> defaulted.drop(start) == missing }
        val result = mutableListOf<CodeBlock>()
        for ((index, parameter) in parameters.withIndex()) {
            val expressions = byParameter[parameter]
            if (expressions == null) {
                if (useOverload || parameter.isVararg) continue
                val default = (parameter.psi as? KtParameter)?.defaultValue
                if (default == null) {
                    result.add(problem(element, "Missing argument ${parameter.name}").code)
                } else {
                    result.add(expr(default, b).code)
                }
                continue
            }
            for (expression in expressions) {
                val value = argument(expression, parameter.returnType, b)
                result.add(if (needsCovariantCast(expression, parameter)) CodeBlock.of("\$T.uncheckedCast(\$L)", KT, value.code) else value.code)
            }
            if (index < 0) break
        }
        return result
    }

    /** Constructor properties keep exact types, so covariant arguments (`List<SchemaMap>` for `List<Map<..>>`) need a cast. */
    private fun needsCovariantCast(expression: KtExpression, parameter: KaValueParameterSymbol): Boolean {
        if ((parameter.psi as? KtParameter)?.hasValOrVar() != true) return false
        val expected = types.map(parameter.returnType) as? ParameterizedTypeName ?: return false
        val actual = expression.expressionType?.let { types.map(it) } as? ParameterizedTypeName ?: return false
        return expected.rawType == actual.rawType && expected.typeArguments != actual.typeArguments
    }

    fun argument(expression: KtExpression, parameterType: KaType?, b: Block): JExpr {
        val unwrapped = KtPsiUtil.safeDeparenthesize(expression)
        if (unwrapped is KtLambdaExpression) return javaLambda(unwrapped, b)
        return expr(expression, b)
    }

    private fun callableReference(e: KtCallableReferenceExpression): JExpr {
        val symbol = e.callableReference.mainReference.resolveToSymbol() as? KaCallableSymbol
            ?: return problem(e, "Unresolved callable reference")
        if (symbol is KaConstructorSymbol) {
            return JExpr.of("\$T::new", ft.javaNameOf(symbol.containingDeclaration as KaClassSymbol))
        }
        val name = methodName(symbol, symbol.callableId?.callableName?.asString() ?: "")
        val receiver = e.receiverExpression
        return when {
            receiver != null -> JExpr.of("\$L::\$N", expr(receiver, Block(root.child())).code, name)
            isStaticMember(symbol) -> JExpr.of("\$T::\$N", ownerClass(symbol), name)
            else -> JExpr.of("this::\$N", name)
        }
    }

    private fun classLiteral(e: KtClassLiteralExpression, b: Block): JExpr {
        val receiver = e.receiverExpression ?: return problem(e, "Unsupported class literal")
        val symbol = (receiver as? KtSimpleNameExpression)?.mainReference?.resolveToSymbol()
            ?: (receiver as? KtDotQualifiedExpression)?.selectorExpression?.let { (it as? KtSimpleNameExpression)?.mainReference?.resolveToSymbol() }
        if (symbol is KaClassSymbol) return JExpr.of("\$T.class", erasure(e.expressionType?.let { (it as? KaClassType)?.typeArguments?.firstOrNull()?.type } ?: return problem(e, "?")))
        return JExpr.call("\$L.getClass()", expr(receiver, b).wrap(Prec.PRIMARY))
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Lambdas
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * Whether a lambda can't be a Java lambda: it assigns a local variable declared outside it, or returns from the
     * enclosing function. Kotlin allows both as the stdlib functions are inline.
     */
    fun needsInlining(l: KtLambdaExpression): Boolean {
        var found = false
        l.accept(object : KtTreeVisitorVoid() {
            override fun visitBinaryExpression(expression: KtBinaryExpression) {
                if (expression.operationToken in KtTokens.ALL_ASSIGNMENTS && isOuterLocal(expression.left, l)) found = true
                super.visitBinaryExpression(expression)
            }

            override fun visitUnaryExpression(expression: KtUnaryExpression) {
                if ((expression.operationToken == KtTokens.PLUSPLUS || expression.operationToken == KtTokens.MINUSMINUS) &&
                    isOuterLocal(expression.baseExpression, l)
                ) {
                    found = true
                }
                super.visitUnaryExpression(expression)
            }

            override fun visitReturnExpression(expression: KtReturnExpression) {
                if (expression.getLabelName() == null) found = true
                super.visitReturnExpression(expression)
            }
        })
        return found
    }

    private fun isOuterLocal(target: KtExpression?, lambda: KtLambdaExpression): Boolean {
        val name = KtPsiUtil.safeDeparenthesize(target ?: return false) as? KtSimpleNameExpression ?: return false
        val symbol = name.mainReference.resolveToSymbol() as? KaLocalVariableSymbol ?: return false
        val declaration = symbol.psi ?: return false
        return !com.intellij.psi.util.PsiTreeUtil.isAncestor(lambda, declaration, false)
    }

    fun javaLambda(l: KtLambdaExpression, b: Block, label: String? = null): JExpr {
        if (needsInlining(l)) {
            return problem(l, "Lambda assigns a captured variable or returns non-locally, which Java lambdas can't")
        }
        val literal = l.functionLiteral
        val symbol = literal.symbol
        val saved = root
        val scope = b.scope.child()
        root = scope
        val body = Block(scope)
        val params = mutableListOf<String>()
        symbol.receiverParameter?.let { receiver ->
            val name = scope.declare(receiverNameFor(receiver.returnType))
            params.add(name)
            implicitReceivers[receiver] = JExpr.name(name)
            implicitReceivers[symbol] = JExpr.name(name)
        }
        val declared = literal.valueParameters
        if (declared.isEmpty()) {
            symbol.valueParameters.forEach { p ->
                val name = scope.declare(p.name.asString())
                names[p] = name
                params.add(name)
            }
        } else {
            for (p in declared) {
                val destructuring = p.destructuringDeclaration
                if (destructuring != null) {
                    val name = scope.declare("entry")
                    params.add(name)
                    destructureEntries(destructuring.entries, CodeBlock.of("\$N", name), p.symbol.returnType, body)
                } else {
                    val name = scope.declare(if (p.name == "_") "unused" else p.name!!)
                    names[p.symbol] = name
                    params.add(name)
                }
            }
        }
        val returnsUnit = ft.isUnit(symbol.returnType)
        val labels = setOfNotNull(label, lambdaLabel(l))
        functions.addLast(FnCtx(FnCtx.Kind.JAVA_LAMBDA, labels, returnsUnit))
        val statements = l.bodyExpression?.statements.orEmpty()
        var expression: CodeBlock? = null
        if (body.isEmpty && statements.size == 1 && statements[0] !is KtReturnExpression) {
            val probe = body.child()
            val single = statements[0]
            if (!isNothing(single) && single !is KtProperty && single !is KtIfExpression && single !is KtWhenExpression &&
                single !is KtForExpression && single !is KtWhileExpression
            ) {
                val value = expr(single, probe, discard = returnsUnit)
                if (probe.isEmpty && value !== UNREACHABLE && (!returnsUnit || value.statement)) expression = value.code
                else {
                    body.addCode(probe.build())
                    if (value !== UNREACHABLE) {
                        if (returnsUnit) { if (value.statement) body.add("\$L", value.code) } else body.add("return \$L", value.code)
                    }
                    expression = null
                    functions.removeLast()
                    root = saved
                    return lambdaCode(params, null, body)
                }
            }
        }
        if (expression == null) {
            if (returnsUnit) {
                statements(statements, body)
            } else {
                statements.dropLast(1).forEach { emitInto(it, body, Sink.Discard) }
                statements.lastOrNull()?.let { emitInto(it, body, Sink.Return) }
            }
        }
        functions.removeLast()
        root = saved
        return lambdaCode(params, expression, body)
    }

    private fun lambdaCode(params: List<String>, expression: CodeBlock?, body: Block): JExpr {
        val head = when (params.size) {
            1 -> CodeBlock.of("\$N", params[0])
            else -> CodeBlock.of("(\$L)", params.joinToString(", "))
        }
        if (expression != null) return JExpr.of("\$L -> \$L", head, expression, prec = Prec.LAMBDA)
        val code = CodeBlock.builder().add("\$L -> {\n", head).indent().add(body.build()).unindent().add("}").build()
        return JExpr(code, Prec.LAMBDA)
    }

    private fun lambdaLabel(l: KtLambdaExpression): String? {
        (l.parent as? KtLabeledExpression)?.getLabelName()?.let { return it }
        val call = l.parent?.parent as? KtCallExpression ?: (l.parent as? KtLambdaArgument)?.parent as? KtCallExpression
        return call?.calleeExpression?.text
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Inline scope functions (let, also, apply, run, ...), used by [Stdlib]
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * Inlines the body of [lambda] into [b], binding its parameter (or receiver) to [subject].
     * Returns the value of the last expression unless [discard].
     */
    fun inlineLambda(lambda: KtLambdaExpression, subject: JExpr?, b: Block, discard: Boolean, asReceiver: Boolean): JExpr {
        val literal = lambda.functionLiteral
        val symbol = literal.symbol
        if (subject != null) {
            if (asReceiver) {
                symbol.receiverParameter?.let { implicitReceivers[it] = subject }
                implicitReceivers[symbol] = subject
            } else {
                val param = literal.valueParameters.firstOrNull()
                val destructuring = param?.destructuringDeclaration
                if (destructuring != null) {
                    destructureEntries(destructuring.entries, subject.code, param.symbol.returnType, b)
                } else {
                    val paramSymbol = param?.symbol ?: symbol.valueParameters.firstOrNull()
                    if (paramSymbol != null) {
                        if (subject.simple) {
                            names[paramSymbol] = subject.code.toString()
                        } else {
                            val name = b.scope.declare(paramSymbol.name.asString())
                            b.add("var \$N = \$L", name, subject.code)
                            names[paramSymbol] = name
                        }
                    }
                }
            }
        }
        val statements = lambda.bodyExpression?.statements.orEmpty()
        if (discard) {
            statements(statements, b)
            return UNREACHABLE
        }
        statements.dropLast(1).forEach { emitInto(it, b, Sink.Discard) }
        val last = statements.lastOrNull() ?: return JExpr.NULL
        return expr(last, b)
    }

    /** Registers [name] as the Java name of the lambda's single parameter (for loops over forEach). */
    fun bindLambdaParameters(lambda: KtLambdaExpression, javaNames: List<String>, b: Block) {
        val literal = lambda.functionLiteral
        val declared = literal.valueParameters
        if (declared.isEmpty()) {
            literal.symbol.valueParameters.forEachIndexed { i, p -> names[p] = javaNames[i] }
            return
        }
        declared.forEachIndexed { i, p ->
            val destructuring = p.destructuringDeclaration
            if (destructuring != null) destructureEntries(destructuring.entries, CodeBlock.of("\$N", javaNames[i]), p.symbol.returnType, b)
            else names[p.symbol] = javaNames[i]
        }
    }

    fun lambdaParameterNames(lambda: KtLambdaExpression, scope: Scope, defaults: List<String>): List<String> {
        val declared = lambda.functionLiteral.valueParameters
        return defaults.indices.map { i ->
            val p = declared.getOrNull(i)
            when {
                p == null -> scope.declare(if (defaults[i] == "it" && declared.isEmpty()) "it" else defaults[i])
                p.destructuringDeclaration != null -> scope.declare(defaults[i])
                p.name == "_" -> scope.declare("unused")
                else -> scope.declare(p.name!!)
            }
        }
    }

    fun withLoop(label: String?, loopValue: ((CodeBlock, Block) -> Unit)? = null, action: () -> Unit) {
        functions.addLast(FnCtx(FnCtx.Kind.LOOP, setOfNotNull(label), true, loopValue))
        try {
            action()
        } finally {
            functions.removeLast()
        }
    }

    val typeMapper get() = types
    val generator get() = gen
    val currentScope get() = root

    companion object {
        /** Marker for expressions fully emitted as statements, with no value. */
        val UNREACHABLE = JExpr(CodeBlock.of("/* unreachable */"), Prec.PRIMARY)
    }
}
