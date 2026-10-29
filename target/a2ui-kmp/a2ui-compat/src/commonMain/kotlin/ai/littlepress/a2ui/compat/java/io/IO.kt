package ai.littlepress.a2ui.compat.java.io

/** java.io.IOException; on the JVM it is that class. */
public expect open class IOException(message: String?) : Exception

/** java.io.EOFException; on the JVM it is that class. */
public expect open class EOFException(message: String?) : IOException

/** A java.io.StringReader stand-in: the text for JsonReader to read. */
public class StringReader(internal val text: String)
