package org.neo4j.spec.graph.javagen

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.WildcardTypeName
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import javax.lang.model.element.Modifier

/** equals, hashCode and toString for data classes, matching Kotlin's generated implementations. */
class DataClass(
    private val ft: FileTranslator,
    private val builder: TypeSpec.Builder,
    private val name: ClassName,
    private val properties: List<KtParameter>,
    private val k: KtClassOrObject,
) {
    private fun declares(function: String) = k.declarations.any { it is KtNamedFunction && it.name == function }

    fun generate() {
        val fields = properties.map { javaIdentifier(it.name!!) to ft.types.map(with(ft) { (it.symbol as KaValueParameterSymbol).returnType }) }
        if (!declares("equals")) {
            val m = MethodSpec.methodBuilder("equals").addAnnotation(Override::class.java)
                .addModifiers(Modifier.PUBLIC).returns(TypeName.BOOLEAN).addParameter(OBJECT, "other")
                .addStatement("if (this == other) return true")
                .addStatement("if (!(other instanceof \$T)) return false", name)
            if (fields.isEmpty()) {
                m.addStatement("return true")
            } else {
                m.addStatement("\$T that = (\$T) other", name, name)
                val comparisons = fields.map { (field, type) ->
                    when {
                        type == TypeName.DOUBLE -> CodeBlock.of("\$T.compare(\$N, that.\$N) == 0", java.lang.Double::class.java, field, field)
                        type == TypeName.FLOAT -> CodeBlock.of("\$T.compare(\$N, that.\$N) == 0", java.lang.Float::class.java, field, field)
                        type.isPrimitive -> CodeBlock.of("\$N == that.\$N", field, field)
                        else -> CodeBlock.of("\$T.equals(\$N, that.\$N)", OBJECTS, field, field)
                    }
                }
                m.addStatement("return \$L", CodeBlock.join(comparisons, "\n        && "))
            }
            builder.addMethod(m.build())
        }
        if (!declares("hashCode")) {
            val m = MethodSpec.methodBuilder("hashCode").addAnnotation(Override::class.java)
                .addModifiers(Modifier.PUBLIC).returns(TypeName.INT)
            if (fields.isEmpty()) {
                m.addStatement("return 0")
            } else {
                fields.forEachIndexed { i, (field, type) ->
                    val hash = if (type.isPrimitive) {
                        CodeBlock.of("\$T.hashCode(\$N)", type.box(), field)
                    } else {
                        CodeBlock.of("\$T.hashCode(\$N)", OBJECTS, field)
                    }
                    if (i == 0) m.addStatement("int result = \$L", hash) else m.addStatement("result = 31 * result + \$L", hash)
                }
                m.addStatement("return result")
            }
            builder.addMethod(m.build())
        }
        if (!declares("toString")) {
            val parts = fields.mapIndexed { i, (field, _) ->
                CodeBlock.of("\$S + \$N", (if (i == 0) "${name.simpleName()}(" else ", ") + "$field=", field)
            }
            val code = if (parts.isEmpty()) CodeBlock.of("\$S", "${name.simpleName()}()") else CodeBlock.of("\$L + \$S", CodeBlock.join(parts, " + "), ")")
            builder.addMethod(
                MethodSpec.methodBuilder("toString").addAnnotation(Override::class.java)
                    .addModifiers(Modifier.PUBLIC).returns(STRING).addStatement("return \$L", code).build()
            )
        }
    }
}

/**
 * `class X : Map<K, V> by content` becomes `class X extends AbstractMap<K, V>` delegating to the field, likewise
 * lists extend AbstractList.
 */
class Delegation(private val ft: FileTranslator, private val builder: TypeSpec.Builder, private val ctx: MemberCtx) {
    fun translate(entry: KtDelegatedSuperTypeEntry) {
        val type = entry.typeReference?.let { with(ft) { it.type } } as? KaClassType
        val delegate = (entry.delegateExpression as? KtNameReferenceExpression)?.getReferencedName()
        if (type == null || delegate == null) {
            ft.gen.problem(entry, "Unsupported delegation")
            return
        }
        val args = type.typeArguments.map { ft.types.map(it.type!!, boxed = true) }
        val field = CodeBlock.of("this.\$N", javaIdentifier(delegate))
        when (type.classId.asString()) {
            "kotlin/collections/Map", "kotlin/collections/MutableMap" -> {
                val (k, v) = args
                builder.superclass(ParameterizedTypeName.get(ClassName.get("java.util", "AbstractMap"), k, v))
                val entrySet = ParameterizedTypeName.get(ClassName.get("java.util", "Set"), ParameterizedTypeName.get(MAP_ENTRY, k, v))
                method("entrySet", entrySet, CodeBlock.of("return \$L.entrySet()", field))
                method("size", TypeName.INT, CodeBlock.of("return \$L.size()", field))
                method("get", v, CodeBlock.of("return \$L.get(key)", field), ParameterSpec.builder(OBJECT, "key").build())
                method("containsKey", TypeName.BOOLEAN, CodeBlock.of("return \$L.containsKey(key)", field), ParameterSpec.builder(OBJECT, "key").build())
                method("keySet", ParameterizedTypeName.get(ClassName.get("java.util", "Set"), k), CodeBlock.of("return \$L.keySet()", field))
                method("values", ParameterizedTypeName.get(ClassName.get("java.util", "Collection"), v), CodeBlock.of("return \$L.values()", field))
            }
            "kotlin/collections/List", "kotlin/collections/MutableList" -> {
                val e = args.single()
                builder.superclass(ParameterizedTypeName.get(ClassName.get("java.util", "AbstractList"), e))
                val index = ParameterSpec.builder(TypeName.INT, "index").build()
                val element = ParameterSpec.builder(e, "element").build()
                method("get", e, CodeBlock.of("return \$L.get(index)", field), index)
                method("size", TypeName.INT, CodeBlock.of("return \$L.size()", field))
                if (type.classId.asString() == "kotlin/collections/MutableList") {
                    method("set", e, CodeBlock.of("return \$L.set(index, element)", field), index, element)
                    method("add", TypeName.VOID, CodeBlock.of("\$L.add(index, element)", field), index, element)
                    method("remove", e, CodeBlock.of("return \$L.remove(index)", field), index)
                }
            }
            else -> ft.gen.problem(entry, "Delegation to ${type.classId} isn't supported")
        }
    }

    private fun method(name: String, returns: TypeName, statement: CodeBlock, vararg params: ParameterSpec) {
        val declared = (ft.currentElement?.let { it as? KtClassOrObject } ?: return)
        if (declared.declarations.any { it is KtNamedFunction && it.name == name && it.valueParameters.size == params.size }) return
        builder.addMethod(
            MethodSpec.methodBuilder(name).addAnnotation(Override::class.java).addModifiers(Modifier.PUBLIC)
                .returns(returns).addParameters(params.toList()).addStatement("\$L", statement).build()
        )
    }
}

/**
 * Jackson annotations for `@Serializable` classes, mirroring how kotlinx-serialization encodes them:
 * `@SerialName`, defaults omitted when encoding (encodeDefaults = false) and applied when decoding, sealed hierarchies
 * with a `type` discriminator.
 */
class Jackson(private val ft: FileTranslator) {
    private val pkg = "com.fasterxml.jackson.annotation"
    private val jsonProperty = ClassName.get(pkg, "JsonProperty")
    private val jsonInclude = ClassName.get(pkg, "JsonInclude")
    private val jsonCreator = ClassName.get(pkg, "JsonCreator")
    private val jsonAutoDetect = ClassName.get(pkg, "JsonAutoDetect")
    private val visibility = ClassName.get(pkg, "JsonAutoDetect", "Visibility")
    private val include = ClassName.get(pkg, "JsonInclude", "Include")

    fun annotate(
        k: KtClassOrObject,
        symbol: KaClassSymbol,
        builder: TypeSpec.Builder,
        javaName: ClassName,
        params: List<KtParameter>,
    ) {
        if (!ft.hasAnnotation(k, "Serializable")) return
        ft.annotationString(k, "Description")?.let {
            builder.addAnnotation(AnnotationSpec.builder(ClassName.get(pkg, "JsonClassDescription")).addMember("value", "\$S", it).build())
        }
        val named = symbol as? KaNamedClassSymbol
        if (k.hasModifier(org.jetbrains.kotlin.lexer.KtTokens.SEALED_KEYWORD) && named != null) {
            sealed(named, builder)
            return
        }
        // Subtypes of a sealed serializable type are identified by their serial name
        val serialName = ft.annotationString(k, "SerialName")
        val sealedParent = symbol.superTypes.any { superType ->
            val s = (superType as? KaClassType)?.symbol as? KaClassSymbol
            (s?.psi as? KtClassOrObject)?.let { it.hasModifier(org.jetbrains.kotlin.lexer.KtTokens.SEALED_KEYWORD) && ft.hasAnnotation(it, "Serializable") } == true
        }
        if (sealedParent && serialName != null) {
            builder.addAnnotation(AnnotationSpec.builder(ClassName.get(pkg, "JsonTypeName")).addMember("value", "\$S", serialName).build())
        }
        if (symbol.classKind == KaClassKind.ENUM_CLASS || symbol.classKind == KaClassKind.OBJECT) return
        val properties = params.filter { it.hasValOrVar() }
        if (properties.isEmpty()) return
        builder.addAnnotation(
            AnnotationSpec.builder(jsonAutoDetect)
                .addMember("fieldVisibility", "\$T.NONE", visibility)
                .addMember("getterVisibility", "\$T.NONE", visibility)
                .addMember("isGetterVisibility", "\$T.NONE", visibility)
                .addMember("setterVisibility", "\$T.NONE", visibility)
                .addMember("creatorVisibility", "\$T.NONE", visibility)
                .build()
        )
        val body = BodyTranslator(ft, MemberCtx(symbol, javaName, isStatic = false))
        val creator = MethodSpec.methodBuilder("fromJson").addAnnotation(jsonCreator)
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC).returns(javaName)
        val creatorArgs = mutableListOf<CodeBlock>()
        for (param in properties) {
            val name = param.name!!
            val field = javaIdentifier(name)
            val jsonName = ft.annotationString(param, "SerialName") ?: name
            val kaType = with(ft) { (param.symbol as KaValueParameterSymbol).returnType }
            val type = ft.types.map(kaType)
            val boxed = type.box()
            val default = param.defaultValue
            val required = ft.hasAnnotation(param, "Required") || default == null
            // Encoding: omit values equal to their default
            val accessor = MethodSpec.methodBuilder("json${capitalize(name)}")
                .addAnnotation(AnnotationSpec.builder(jsonProperty).addMember("value", "\$S", jsonName).build())
                .addModifiers(Modifier.PRIVATE).returns(boxed)
            ft.annotationString(param, "Description")?.let {
                accessor.addAnnotation(AnnotationSpec.builder(ClassName.get(pkg, "JsonPropertyDescription")).addMember("value", "\$S", it).build())
            }
            if (required) {
                accessor.addAnnotation(AnnotationSpec.builder(jsonInclude).addMember("value", "\$T.ALWAYS", include).build())
                accessor.addStatement("return this.\$N", field)
            } else {
                val block = Block(Scope(null))
                val defaultValue = body.expr(default!!, block)
                accessor.addAnnotation(AnnotationSpec.builder(jsonInclude).addMember("value", "\$T.NON_NULL", include).build())
                if (!block.isEmpty) ft.gen.problem(param, "Default value needs statements")
                val equal = if (type.isPrimitive) CodeBlock.of("this.\$N == \$L", field, defaultValue.code)
                else CodeBlock.of("\$T.equals(this.\$N, \$L)", OBJECTS, field, defaultValue.code)
                accessor.addStatement("return \$L ? null : this.\$N", equal, field)
            }
            ft.javaNameOf(symbol)
            // Decoding: missing values take their default
            creator.addParameter(
                ParameterSpec.builder(boxed, field)
                    .addAnnotation(AnnotationSpec.builder(jsonProperty).addMember("value", "\$S", jsonName).build())
                    .build()
            )
            if (default != null) {
                val block = Block(Scope(null))
                val defaultValue = body.expr(default, block)
                creatorArgs.add(CodeBlock.of("\$N != null ? \$N : \$L", field, field, defaultValue.code))
            } else if (kaType.nullability == KaTypeNullability.NULLABLE) {
                creatorArgs.add(CodeBlock.of("\$N", field))
            } else {
                creatorArgs.add(CodeBlock.of("\$T.requireNonNull(\$N, \$S)", OBJECTS, field, "Field '$jsonName' is required"))
            }
            builder.addMethod(accessor.build())
        }
        creator.addStatement("return new \$T(\$L)", javaName, CodeBlock.join(creatorArgs, ",\n    "))
        builder.addMethod(creator.build())
    }

    private fun sealed(symbol: KaNamedClassSymbol, builder: TypeSpec.Builder) {
        val subtypes = with(ft) { symbol.sealedClassInheritors }
        builder.addAnnotation(
            AnnotationSpec.builder(ClassName.get(pkg, "JsonTypeInfo"))
                .addMember("use", "\$T.NAME", ClassName.get(pkg, "JsonTypeInfo", "Id"))
                .addMember("include", "\$T.PROPERTY", ClassName.get(pkg, "JsonTypeInfo", "As"))
                .addMember("property", "\$S", "type")
                .build()
        )
        val list = AnnotationSpec.builder(ClassName.get(pkg, "JsonSubTypes"))
        for (subtype in subtypes) {
            val psi = subtype.psi as? KtClassOrObject ?: continue
            val serialName = ft.annotationString(psi, "SerialName") ?: subtype.classId!!.asFqNameString()
            list.addMember(
                "value", "\$L",
                AnnotationSpec.builder(ClassName.get(pkg, "JsonSubTypes", "Type"))
                    .addMember("value", "\$T.class", ft.javaNameOf(subtype))
                    .addMember("name", "\$S", serialName)
                    .build()
            )
        }
        builder.addAnnotation(list.build())
    }

    fun enumEntry(entry: KtEnumEntry, constant: TypeSpec.Builder) {
        ft.annotationString(entry, "SerialName")?.let {
            constant.addAnnotation(AnnotationSpec.builder(jsonProperty).addMember("value", "\$S", it).build())
        }
    }

    @Suppress("unused")
    private val unusedWildcard = WildcardTypeName.subtypeOf(OBJECT)
}
