package org.neo4j.spec.graph.javagen

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.psi.KtTypeAlias
import javax.lang.model.element.Modifier

/**
 * Translates the declarations of one Kotlin file. Function bodies are delegated to [BodyTranslator].
 */
class FileTranslator(session: KaSession, val gen: Generator, val file: KtFile) : KaSession by session {
    val types = TypeMapper(session) { gen.problem(currentElement, it) }
    val overloads: MutableSet<MethodSpec> = java.util.Collections.newSetFromMap(java.util.IdentityHashMap())
    var currentElement: org.jetbrains.kotlin.psi.KtElement? = file
    private val header = "Generated from ${gen.relativePath(file)} by :java:generator. Do not edit."

    fun translate() {
        val pkg = file.packageFqName.asString()
        for (declaration in file.declarations) {
            currentElement = declaration
            if (isExcluded(declaration)) continue
            when (declaration) {
                is KtClassOrObject -> translateClass(declaration, nested = false)?.let {
                    gen.addType(ClassName.get(pkg, declaration.name!!), it, header)
                }
                is KtNamedFunction -> facade().addMethods(translateFunction(declaration, MemberCtx.topLevel(facadeName())))
                is KtProperty -> addProperty(facade(), declaration, MemberCtx.topLevel(facadeName()), PropertyOwner.FACADE)
                is KtTypeAlias -> gen.problem(declaration, "Type aliases aren't supported")
                else -> gen.problem(declaration, "Unsupported top-level declaration ${declaration::class.simpleName}")
            }
        }
    }

    fun facadeName(): ClassName {
        val jvmName = file.fileAnnotationList?.annotationEntries
            ?.firstOrNull { it.shortName?.asString() == "JvmName" }
            ?.valueArguments?.firstOrNull()?.getArgumentExpression() as? KtStringTemplateExpression
        val name = jvmName?.entries?.joinToString("") { it.text } ?: (file.name.removeSuffix(".kt") + "Kt")
        return ClassName.get(file.packageFqName.asString(), name)
    }

    private fun facade() = gen.facade(facadeName(), header)

    fun isExcluded(declaration: KtDeclaration): Boolean {
        val fq = (declaration as? KtNamedDeclaration)?.fqName?.asString() ?: return false
        return fq in Config.excludedDeclarations
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Classes
    // ---------------------------------------------------------------------------------------------------------------

    fun translateClass(k: KtClassOrObject, nested: Boolean): TypeSpec.Builder? {
        currentElement = k
        val symbol = k.symbol as KaClassSymbol
        val name = k.name!!
        val javaName = javaNameOf(symbol)
        val builder = when (symbol.classKind) {
            KaClassKind.INTERFACE -> TypeSpec.interfaceBuilder(name)
            KaClassKind.ENUM_CLASS -> TypeSpec.enumBuilder(name)
            KaClassKind.ANNOTATION_CLASS -> {
                gen.problem(k, "Annotation classes aren't translated")
                return null
            }
            else -> TypeSpec.classBuilder(name)
        }
        javadoc(k)?.let { builder.addJavadoc(it) }
        val private = k.hasModifier(KtTokens.PRIVATE_KEYWORD)
        if (!private || nested) builder.addModifiers(if (private) Modifier.PRIVATE else Modifier.PUBLIC)
        if (nested && symbol.classKind != KaClassKind.INTERFACE && symbol.classKind != KaClassKind.ENUM_CLASS &&
            !k.hasModifier(KtTokens.INNER_KEYWORD)
        ) {
            builder.addModifiers(Modifier.STATIC)
        }
        val isClass = symbol.classKind == KaClassKind.CLASS || symbol.classKind == KaClassKind.OBJECT
        if (isClass) {
            when {
                k.hasModifier(KtTokens.ABSTRACT_KEYWORD) || k.hasModifier(KtTokens.SEALED_KEYWORD) ->
                    builder.addModifiers(Modifier.ABSTRACT)
                k.hasModifier(KtTokens.OPEN_KEYWORD) -> Unit
                else -> builder.addModifiers(Modifier.FINAL)
            }
        }
        (symbol as? KaNamedClassSymbol)?.typeParameters?.forEach { builder.addTypeVariable(BodyTranslator(this, MemberCtx(symbol, OBJECT, false)).typeVariable(it)) }

        val staticHolder = isStaticHolder(symbol)
        val ctx = MemberCtx(symbol, javaName, isStatic = staticHolder)

        // Supertypes
        var superCall: KtSuperTypeCallEntry? = null
        val delegations = mutableListOf<KtDelegatedSuperTypeEntry>()
        for (entry in k.superTypeListEntries) {
            val type = entry.typeReference?.type ?: continue
            val superSymbol = (type as? KaClassType)?.symbol as? KaClassSymbol
            when {
                entry is KtDelegatedSuperTypeEntry -> delegations.add(entry)
                superSymbol?.classKind == KaClassKind.INTERFACE -> {
                    if (symbol.classKind == KaClassKind.INTERFACE) {
                        builder.addSuperinterface(types.map(type))
                    } else {
                        builder.addSuperinterface(types.map(type))
                    }
                }
                else -> {
                    builder.superclass(types.map(type))
                    superCall = entry as? KtSuperTypeCallEntry
                }
            }
        }
        for (delegation in delegations) {
            Delegation(this, builder, ctx).translate(delegation)
        }

        // Properties declared in the primary constructor and the class body
        val owner = when {
            symbol.classKind == KaClassKind.INTERFACE -> PropertyOwner.INTERFACE
            staticHolder -> PropertyOwner.STATIC
            else -> PropertyOwner.INSTANCE
        }
        val constructorParams = (k as? KtClass)?.primaryConstructorParameters.orEmpty()
        for (param in constructorParams.filter { it.hasValOrVar() }) {
            addConstructorProperty(builder, param, ctx)
        }
        val initializers = mutableListOf<KtDeclaration>()
        for (declaration in k.declarations) {
            currentElement = declaration
            if (isExcluded(declaration)) continue
            when (declaration) {
                is KtProperty -> if (addProperty(builder, declaration, ctx, owner)) initializers.add(declaration)
                is KtNamedFunction -> builder.addMethods(translateFunction(declaration, ctx))
                is org.jetbrains.kotlin.psi.KtAnonymousInitializer -> initializers.add(declaration)
                is KtSecondaryConstructor -> builder.addMethod(BodyTranslator(this, ctx).secondaryConstructor(declaration))
                is KtEnumEntry -> addEnumEntry(builder, declaration, ctx)
                is KtObjectDeclaration -> if (declaration.isCompanion()) {
                    translateCompanion(builder, declaration, javaName)
                } else {
                    translateClass(declaration, nested = true)?.let { builder.addType(it.build()) }
                }
                is KtClassOrObject -> translateClass(declaration, nested = true)?.let { builder.addType(it.build()) }
                else -> gen.problem(declaration, "Unsupported member ${declaration::class.simpleName}")
            }
        }

        // Constructors
        when {
            symbol.classKind == KaClassKind.INTERFACE -> Unit
            symbol.classKind == KaClassKind.OBJECT && staticHolder -> {
                builder.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build())
                if (initializers.isNotEmpty()) {
                    builder.addStaticBlock(BodyTranslator(this, ctx).initializers(initializers))
                }
            }
            symbol.classKind == KaClassKind.OBJECT -> {
                builder.addField(
                    FieldSpec.builder(javaName, "INSTANCE", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new \$T()", javaName)
                        .build()
                )
                val ctor = MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE)
                superCall?.let { ctor.addCode(BodyTranslator(this, ctx).superCall(it)) }
                if (initializers.isNotEmpty()) ctor.addCode(BodyTranslator(this, ctx).initializers(initializers))
                builder.addMethod(ctor.build())
            }
            else -> {
                val primary = (k as KtClass).primaryConstructor
                val hasSecondary = k.secondaryConstructors.isNotEmpty()
                if (primary != null || !hasSecondary) {
                    val body = BodyTranslator(this, ctx)
                    builder.addMethods(body.primaryConstructor(k, constructorParams, superCall, initializers))
                }
            }
        }

        if ((symbol as? KaNamedClassSymbol)?.isData == true) {
            DataClass(this, builder, javaName, constructorParams.filter { it.hasValOrVar() }, k).generate()
        }
        Jackson(this).annotate(k, symbol, builder, javaName, constructorParams)
        removeClashingOverloads(builder)
        return builder
    }

    /** Generated default-argument overloads that clash with a declared member (same erasure) are dropped. */
    private fun removeClashingOverloads(builder: TypeSpec.Builder) {
        fun erasure(type: TypeName): String = when (type) {
            is ParameterizedTypeName -> type.rawType.toString()
            is TypeVariableName -> type.bounds.firstOrNull()?.let { erasure(it) } ?: "java.lang.Object"
            is com.squareup.javapoet.ArrayTypeName -> erasure(type.componentType) + "[]"
            else -> type.toString()
        }
        fun key(m: MethodSpec) = m.name + m.parameters.joinToString(",", "(", ")") { erasure(it.type) }
        val declared = builder.methodSpecs.filter { it !in overloads }.map { key(it) }.toSet()
        val seen = mutableSetOf<String>()
        builder.methodSpecs.removeIf { m -> m in overloads && (key(m) in declared || !seen.add(key(m))) }
    }

    private fun translateCompanion(builder: TypeSpec.Builder, companion: KtObjectDeclaration, outer: ClassName) {
        val symbol = companion.symbol as KaClassSymbol
        if (companion.superTypeListEntries.isNotEmpty()) {
            gen.problem(companion, "Companion objects with supertypes aren't supported")
        }
        val ctx = MemberCtx(symbol, outer, isStatic = true)
        val initializers = mutableListOf<KtDeclaration>()
        for (declaration in companion.declarations) {
            currentElement = declaration
            if (isExcluded(declaration)) continue
            when (declaration) {
                is KtProperty -> if (addProperty(builder, declaration, ctx, PropertyOwner.STATIC)) initializers.add(declaration)
                is KtNamedFunction -> builder.addMethods(translateFunction(declaration, ctx))
                is org.jetbrains.kotlin.psi.KtAnonymousInitializer -> initializers.add(declaration)
                is KtClassOrObject -> translateClass(declaration, nested = true)?.let { builder.addType(it.build()) }
                else -> gen.problem(declaration, "Unsupported companion member ${declaration::class.simpleName}")
            }
        }
        if (initializers.isNotEmpty()) builder.addStaticBlock(BodyTranslator(this, ctx).initializers(initializers))
    }

    private fun addEnumEntry(builder: TypeSpec.Builder, entry: KtEnumEntry, ctx: MemberCtx) {
        if (entry.body != null) gen.problem(entry, "Enum entries with bodies aren't supported")
        val call = entry.initializerList?.initializers?.firstOrNull() as? KtSuperTypeCallEntry
        val args = call?.let { BodyTranslator(this, ctx.copy(isStatic = true)).enumArguments(it) }
        val constant = if (args == null) {
            TypeSpec.anonymousClassBuilder("")
        } else {
            TypeSpec.anonymousClassBuilder("\$L", args)
        }
        Jackson(this).enumEntry(entry, constant)
        javadoc(entry)?.let { constant.addJavadoc(it) }
        builder.addEnumConstant(entry.name!!, constant.build())
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Properties
    // ---------------------------------------------------------------------------------------------------------------

    private fun addConstructorProperty(builder: TypeSpec.Builder, param: KtParameter, ctx: MemberCtx) {
        val symbol = param.symbol
        val property = (symbol as? org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol)
            ?.generatedPrimaryConstructorProperty ?: return
        val name = param.name!!
        val type = types.map(property.returnType)
        val mutable = param.isMutable
        val field = FieldSpec.builder(type, javaIdentifier(name), Modifier.PRIVATE)
        if (!mutable) field.addModifiers(Modifier.FINAL)
        builder.addField(field.build())
        if (param.hasModifier(KtTokens.PRIVATE_KEYWORD)) return
        val visibility = visibilityOf(param)
        val getter = MethodSpec.methodBuilder(getterName(name))
            .addModifiers(visibility)
            .returns(type)
            .addStatement("return this.\$N", javaIdentifier(name))
        if (param.hasModifier(KtTokens.OVERRIDE_KEYWORD)) getter.addAnnotation(Override::class.java)
        javadoc(param)?.let { getter.addJavadoc(it) }
        builder.addMethod(getter.build())
        if (mutable) {
            val setter = MethodSpec.methodBuilder(setterName(name))
                .addModifiers(visibility)
                .addParameter(type, "value")
                .addStatement("this.\$N = value", javaIdentifier(name))
            builder.addMethod(setter.build())
        }
    }

    /**
     * Adds a field and accessors for [property]. Returns true when the property has an initializer which must run in
     * the constructor (or static initializer).
     */
    fun addProperty(builder: TypeSpec.Builder, property: KtProperty, ctx: MemberCtx, owner: PropertyOwner): Boolean {
        val symbol = property.symbol as KaPropertySymbol
        if (property.hasDelegate()) {
            gen.problem(property, "Delegated properties aren't supported")
            return false
        }
        val name = property.name!!
        val type = types.map(symbol.returnType)
        val static = owner == PropertyOwner.STATIC || owner == PropertyOwner.FACADE
        val isConst = property.hasModifier(KtTokens.CONST_KEYWORD) || property.annotationEntries.any {
            it.shortName?.asString() == "JvmField"
        }
        val private = property.hasModifier(KtTokens.PRIVATE_KEYWORD)
        val getterBody = property.getter?.takeIf { it.hasBody() }
        val setterBody = property.setter?.takeIf { it.hasBody() }
        val docs = javadoc(property)
        val override = property.hasModifier(KtTokens.OVERRIDE_KEYWORD)

        if (owner == PropertyOwner.INTERFACE) {
            val getter = MethodSpec.methodBuilder(getterName(name)).addModifiers(Modifier.PUBLIC).returns(type)
            docs?.let { getter.addJavadoc(it) }
            if (override) getter.addAnnotation(Override::class.java)
            if (getterBody != null) {
                getter.addModifiers(Modifier.DEFAULT).addCode(BodyTranslator(this, ctx).accessor(getterBody, symbol))
            } else {
                getter.addModifiers(Modifier.ABSTRACT)
            }
            builder.addMethod(getter.build())
            if (property.isVar) {
                builder.addMethod(
                    MethodSpec.methodBuilder(setterName(name)).addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(type, "value").build()
                )
            }
            return false
        }

        val modifiers = mutableListOf<Modifier>()
        var initialized = false
        if (symbol.hasBackingField) {
            val field = FieldSpec.builder(type, if (isConst) name else javaIdentifier(name))
            modifiers.add(if (isConst && !private) Modifier.PUBLIC else Modifier.PRIVATE)
            if (static) modifiers.add(Modifier.STATIC)
            if (!property.isVar) modifiers.add(Modifier.FINAL)
            field.addModifiers(*modifiers.toTypedArray())
            if (isConst) docs?.let { field.addJavadoc(it) }
            val initializer = property.initializer
            if (initializer != null) {
                // Simple static initializers stay on the field, others run in the static block / constructor
                val inline = if (static) BodyTranslator(this, ctx).simpleInitializer(initializer) else null
                if (inline != null) field.initializer(inline) else initialized = true
            }
            builder.addField(field.build())
        }
        if (isConst || (private && getterBody == null && setterBody == null)) return initialized

        val visibility = visibilityOf(property)
        val getter = MethodSpec.methodBuilder(getterName(name)).addModifiers(visibility).returns(type)
        if (static) getter.addModifiers(Modifier.STATIC)
        if (override) getter.addAnnotation(Override::class.java)
        docs?.let { getter.addJavadoc(it) }
        if (getterBody != null) {
            getter.addCode(BodyTranslator(this, ctx).accessor(getterBody, symbol))
        } else if (symbol.hasBackingField) {
            getter.addStatement(if (static) "return \$T.\$N" else "return this.\$N", *fieldRef(static, ctx, name))
        } else {
            getter.addModifiers(Modifier.ABSTRACT)
        }
        builder.addMethod(getter.build())
        if (property.isVar && !property.setter.let { it != null && it.hasModifier(KtTokens.PRIVATE_KEYWORD) }) {
            val setter = MethodSpec.methodBuilder(setterName(name)).addModifiers(visibility).addParameter(type, "value")
            if (static) setter.addModifiers(Modifier.STATIC)
            if (setterBody != null) {
                setter.addCode(BodyTranslator(this, ctx).accessor(setterBody, symbol))
            } else {
                setter.addStatement(if (static) "\$T.\$N = value" else "this.\$N = value", *fieldRef(static, ctx, name))
            }
            builder.addMethod(setter.build())
        }
        return initialized
    }

    private fun fieldRef(static: Boolean, ctx: MemberCtx, name: String): Array<Any> =
        if (static) arrayOf(ctx.javaClass, javaIdentifier(name)) else arrayOf(javaIdentifier(name))

    // ---------------------------------------------------------------------------------------------------------------
    // Functions
    // ---------------------------------------------------------------------------------------------------------------

    fun translateFunction(function: KtNamedFunction, ctx: MemberCtx): List<MethodSpec> {
        currentElement = function
        if (function.hasModifier(KtTokens.INLINE_KEYWORD) &&
            function.typeParameters.any { it.hasModifier(KtTokens.REIFIED_KEYWORD) }
        ) {
            gen.problem(function, "Reified inline functions aren't supported")
            return emptyList()
        }
        return BodyTranslator(this, ctx).function(function)
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------------------------

    fun visibilityOf(declaration: KtModifierListOwner): Modifier = when {
        declaration.hasModifier(KtTokens.PRIVATE_KEYWORD) -> Modifier.PRIVATE
        declaration.hasModifier(KtTokens.PROTECTED_KEYWORD) -> Modifier.PROTECTED
        else -> Modifier.PUBLIC
    }

    /** Companion objects and objects without supertypes become static members rather than singletons. */
    fun isStaticHolder(symbol: KaClassSymbol): Boolean = when (symbol.classKind) {
        KaClassKind.COMPANION_OBJECT -> true
        KaClassKind.OBJECT -> symbol.superTypes.all { it.classIdString() == "kotlin/Any" }
        else -> false
    }

    /** Java class holding members of [symbol]; companion members live on the outer class. */
    fun javaNameOf(symbol: KaClassSymbol): ClassName {
        val id = symbol.classId ?: return OBJECT
        if (symbol.classKind == KaClassKind.COMPANION_OBJECT) return id.outerClassId!!.javaName()
        return id.javaName()
    }

    fun javadoc(declaration: KtDeclaration): CodeBlock? {
        val doc = declaration.docComment?.getDefaultSection()?.getContent()?.trim()
        if (doc.isNullOrEmpty()) return null
        val text = doc.replace(Regex("\\[([A-Za-z0-9_.]+)]")) { "{@code ${it.groupValues[1]}}" }
            .replace("*/", "*&#47;")
        return CodeBlock.of("\$L\n", text.replace("$", "$$"))
    }

    fun annotationString(annotated: KtAnnotated, shortName: String): String? {
        val entry = annotated.annotationEntries.firstOrNull { it.shortName?.asString() == shortName } ?: return null
        val arg = entry.valueArguments.firstOrNull()?.getArgumentExpression() ?: return null
        return constantString(arg)
    }

    fun constantString(expression: org.jetbrains.kotlin.psi.KtExpression): String? {
        (expression as? KtStringTemplateExpression)?.let { template ->
            if (template.entries.all { it is org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry }) {
                return template.entries.joinToString("") { it.text }
            }
        }
        val value = expression.evaluate()
        return (value as? org.jetbrains.kotlin.analysis.api.base.KaConstantValue.StringValue)?.value
    }

    fun hasAnnotation(annotated: KtAnnotated, shortName: String) =
        annotated.annotationEntries.any { it.shortName?.asString() == shortName }

    fun isUnit(type: KaType) = types.isUnit(type)

    fun parameterized(raw: ClassName, vararg args: TypeName): TypeName = ParameterizedTypeName.get(raw, *args)

    fun param(type: TypeName, name: String) = ParameterSpec.builder(type, name).build()

    fun annotation(type: ClassName) = AnnotationSpec.builder(type)
}

enum class PropertyOwner { INSTANCE, STATIC, INTERFACE, FACADE }

/** Where a member body is being translated. */
data class MemberCtx(
    val cls: KaClassSymbol?,
    val javaClass: ClassName,
    val isStatic: Boolean,
) {
    companion object {
        fun topLevel(facade: ClassName) = MemberCtx(null, facade, isStatic = true)
    }
}
