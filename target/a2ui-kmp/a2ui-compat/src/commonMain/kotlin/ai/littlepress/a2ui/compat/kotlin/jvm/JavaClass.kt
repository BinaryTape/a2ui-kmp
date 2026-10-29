package ai.littlepress.a2ui.compat.kotlin.jvm

/**
 * kotlin.jvm.javaClass as upstream uses it: comparing and hashing classes, and their simple name in
 * messages. On the JVM it is java.lang.Class, so names are Java's ("Integer", not "Int").
 */
public expect class JavaClass<T>

public expect val <T : Any> T.javaClass: JavaClass<T>

public expect val JavaClass<*>.simpleName: String
