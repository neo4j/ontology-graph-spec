package org.neo4j.spec.graph.javagen

import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeVariableName
import com.squareup.javapoet.WildcardTypeName
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaCapturedType
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaDefinitelyNotNullType
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.analysis.api.types.KaFlexibleType
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaIntersectionType
import org.jetbrains.kotlin.analysis.api.types.KaStarTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.Variance

val OBJECT: ClassName = ClassName.get(Object::class.java)
val STRING: ClassName = ClassName.get(String::class.java)
val OBJECTS: ClassName = ClassName.get("java.util", "Objects")
val KT: ClassName = ClassName.get(Config.RUNTIME_PACKAGE, "Kt")
val PAIR: ClassName = ClassName.get(Config.RUNTIME_PACKAGE, "Pair")
val INDEXED_VALUE: ClassName = ClassName.get(Config.RUNTIME_PACKAGE, "IndexedValue")
val ARRAY_LIST: ClassName = ClassName.get("java.util", "ArrayList")
val LINKED_HASH_MAP: ClassName = ClassName.get("java.util", "LinkedHashMap")
val LINKED_HASH_SET: ClassName = ClassName.get("java.util", "LinkedHashSet")
val MAP: ClassName = ClassName.get("java.util", "Map")
val MAP_ENTRY: ClassName = ClassName.get("java.util", "Map", "Entry")
val ISE: ClassName = ClassName.get(IllegalStateException::class.java)
val IAE: ClassName = ClassName.get(IllegalArgumentException::class.java)

private val primitives = mapOf(
    "kotlin/Int" to TypeName.INT,
    "kotlin/Long" to TypeName.LONG,
    "kotlin/Double" to TypeName.DOUBLE,
    "kotlin/Float" to TypeName.FLOAT,
    "kotlin/Boolean" to TypeName.BOOLEAN,
    "kotlin/Char" to TypeName.CHAR,
    "kotlin/Byte" to TypeName.BYTE,
    "kotlin/Short" to TypeName.SHORT,
)

private val mapped: Map<String, ClassName> = buildMap {
    put("kotlin/Any", OBJECT)
    put("kotlin/String", STRING)
    put("kotlin/CharSequence", ClassName.get(CharSequence::class.java))
    put("kotlin/Comparable", ClassName.get(Comparable::class.java))
    put("kotlin/Number", ClassName.get(Number::class.java))
    put("kotlin/Enum", ClassName.get(Enum::class.java))
    put("kotlin/Throwable", ClassName.get(Throwable::class.java))
    put("kotlin/Exception", ClassName.get(Exception::class.java))
    put("kotlin/RuntimeException", ClassName.get(RuntimeException::class.java))
    put("kotlin/IllegalStateException", ISE)
    put("kotlin/IllegalArgumentException", IAE)
    put("kotlin/UnsupportedOperationException", ClassName.get(UnsupportedOperationException::class.java))
    put("kotlin/NumberFormatException", ClassName.get(NumberFormatException::class.java))
    put("kotlin/NoSuchElementException", ClassName.get("java.util", "NoSuchElementException"))
    put("kotlin/Pair", PAIR)
    put("kotlin/collections/IndexedValue", INDEXED_VALUE)
    put("kotlin/text/Regex", ClassName.get("java.util.regex", "Pattern"))
    put("kotlin/text/StringBuilder", ClassName.get(StringBuilder::class.java))
    put("kotlin/reflect/KClass", ClassName.get(Class::class.java))
    val util = { name: String -> ClassName.get("java.util", name) }
    for (name in listOf("List", "Collection", "Iterable", "Set", "Map", "Iterator", "ListIterator")) {
        val java = if (name == "Iterable") ClassName.get(Iterable::class.java) else util(name)
        put("kotlin/collections/$name", java)
        put("kotlin/collections/Mutable$name", java)
    }
    put("kotlin/collections/Map.Entry", MAP_ENTRY)
    put("kotlin/collections/MutableMap.MutableEntry", MAP_ENTRY)
    put("kotlin/collections/ArrayList", ARRAY_LIST)
    put("kotlin/collections/LinkedHashMap", LINKED_HASH_MAP)
    put("kotlin/collections/HashMap", util("HashMap"))
    put("kotlin/collections/LinkedHashSet", LINKED_HASH_SET)
    put("kotlin/collections/HashSet", util("HashSet"))
}

fun ClassId.javaName(): ClassName {
    mapped[asString()]?.let { return it }
    val segments = relativeClassName.pathSegments().map { it.asString() }
    return ClassName.get(packageFqName.asString(), segments.first(), *segments.drop(1).toTypedArray())
}

fun isPrimitive(type: TypeName) = type.isPrimitive

/**
 * Maps Kotlin types onto Java ones. Nullable or generic positions use boxed types.
 */
class TypeMapper(private val session: KaSession, private val report: (String) -> Unit) {

    /**
     * [parameter] adds the wildcards Kotlin's declaration-site variance implies for value parameters, e.g. a
     * `List<Any>` parameter accepts any list so becomes `List<? extends Object>`, like the Kotlin/JVM compiler emits.
     */
    fun map(type: KaType, boxed: Boolean = false, returnType: Boolean = false, parameter: Boolean = false): TypeName = with(session) {
        when (type) {
            is KaFlexibleType -> map(type.lowerBound, boxed, returnType, parameter)
            is KaDefinitelyNotNullType -> map(type.original, boxed, returnType, parameter)
            is KaIntersectionType -> map(type.conjuncts.first(), boxed, returnType, parameter)
            is KaCapturedType -> OBJECT
            is KaErrorType -> {
                report("Unresolved type: $type")
                OBJECT
            }
            is KaTypeParameterType -> TypeVariableName.get(type.name.asString())
            is KaFunctionType -> functionType(type)
            is KaClassType -> classType(type, boxed || type.nullability == KaTypeNullability.NULLABLE, returnType, parameter)
            else -> {
                report("Unsupported type: $type")
                OBJECT
            }
        }
    }

    private fun classType(type: KaClassType, boxed: Boolean, returnType: Boolean, parameter: Boolean = false): TypeName {
        val id = type.classId.asString()
        if (id == "kotlin/Unit") return if (returnType && !boxed) TypeName.VOID else ClassName.get(Void::class.java)
        if (id == "kotlin/Nothing") return if (returnType && !boxed) TypeName.VOID else ClassName.get(Void::class.java)
        primitives[id]?.let { return if (boxed) it.box() else it }
        if (id == "kotlin/Array") {
            return ArrayTypeName.of(argument(type.typeArguments.single(), forArray = true, parameter = parameter))
        }
        when (id) {
            "kotlin/IntArray" -> return ArrayTypeName.of(TypeName.INT)
            "kotlin/CharArray" -> return ArrayTypeName.of(TypeName.CHAR)
        }
        val raw = type.classId.javaName()
        if (type.typeArguments.isEmpty()) return raw
        val declared = with(session) { (type.symbol as? org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol)?.typeParameters }
        val args = type.typeArguments.mapIndexed { i, projection ->
            val variance = declared?.getOrNull(i)?.variance
            if (parameter && projection is KaTypeArgumentWithVariance && projection.variance == Variance.INVARIANT &&
                variance != null && variance != Variance.INVARIANT
            ) {
                val inner = map(projection.type, boxed = true, parameter = true)
                when {
                    variance == Variance.OUT_VARIANCE && !isFinal(projection.type) -> WildcardTypeName.subtypeOf(inner)
                    variance == Variance.IN_VARIANCE && projection.type.classIdString() != "kotlin/Any" -> WildcardTypeName.supertypeOf(inner)
                    else -> inner
                }
            } else {
                argument(projection, parameter = parameter)
            }
        }
        return ParameterizedTypeName.get(raw, *args.toTypedArray())
    }

    private fun isFinal(type: KaType): Boolean = with(session) {
        val symbol = (type as? KaClassType)?.symbol as? org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol ?: return false
        if (symbol.classKind == org.jetbrains.kotlin.analysis.api.symbols.KaClassKind.ENUM_CLASS) return true
        return symbol.modality == org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality.FINAL
    }

    private fun argument(projection: org.jetbrains.kotlin.analysis.api.types.KaTypeProjection, forArray: Boolean = false, parameter: Boolean = false): TypeName =
        when (projection) {
            is KaStarTypeProjection -> WildcardTypeName.subtypeOf(OBJECT)
            is KaTypeArgumentWithVariance -> {
                val type = map(projection.type, boxed = true, parameter = parameter)
                when (projection.variance) {
                    Variance.OUT_VARIANCE -> WildcardTypeName.subtypeOf(type)
                    Variance.IN_VARIANCE -> WildcardTypeName.supertypeOf(type)
                    Variance.INVARIANT -> type
                }
            }
            else -> OBJECT
        }

    /** Function types become the closest java.util.function interface. */
    fun functionType(type: KaFunctionType): TypeName {
        val params = buildList {
            type.receiverType?.let { add(it) }
            addAll(type.parameterTypes)
        }.map { map(it, boxed = true) }
        val unit = isUnit(type.returnType)
        val result = map(type.returnType, boxed = true)
        val fn = "java.util.function"
        return when (params.size) {
            0 -> if (unit) ClassName.get(Runnable::class.java) else ParameterizedTypeName.get(ClassName.get(fn, "Supplier"), result)
            1 -> if (unit) {
                ParameterizedTypeName.get(ClassName.get(fn, "Consumer"), params[0])
            } else {
                ParameterizedTypeName.get(ClassName.get(fn, "Function"), params[0], result)
            }
            2 -> if (unit) {
                ParameterizedTypeName.get(ClassName.get(fn, "BiConsumer"), params[0], params[1])
            } else {
                ParameterizedTypeName.get(ClassName.get(fn, "BiFunction"), params[0], params[1], result)
            }
            else -> {
                report("Function types with more than two parameters aren't supported: $type")
                OBJECT
            }
        }
    }

    /** The method name used to invoke a value of the given function type. */
    fun invokeName(type: KaType): String {
        val fn = type as? KaFunctionType ?: return "apply"
        val arity = fn.parameterTypes.size + if (fn.receiverType != null) 1 else 0
        val unit = isUnit(fn.returnType)
        return when {
            arity == 0 && unit -> "run"
            arity == 0 -> "get"
            unit -> "accept"
            else -> "apply"
        }
    }

    fun isUnit(type: KaType) = (type as? KaClassType)?.classId?.asString() == "kotlin/Unit"
}

fun KaType.classIdString(): String? = when (this) {
    is KaClassType -> classId.asString()
    is KaFlexibleType -> lowerBound.classIdString()
    is KaDefinitelyNotNullType -> original.classIdString()
    else -> null
}

private val javaKeywords = setOf(
    "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue",
    "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if",
    "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "package", "private",
    "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
    "throw", "throws", "transient", "try", "void", "volatile", "while", "var", "record", "yield", "_",
)

fun javaIdentifier(name: String) = if (name in javaKeywords) "${name}_" else name

fun capitalize(name: String) = name.replaceFirstChar { it.uppercaseChar() }

private fun isPrefixed(name: String) = name.length > 2 && name.startsWith("is") && name[2].isUpperCase()

fun getterName(property: String) = if (isPrefixed(property)) property else "get${capitalize(property)}"

fun setterName(property: String) = if (isPrefixed(property)) "set${property.drop(2)}" else "set${capitalize(property)}"
