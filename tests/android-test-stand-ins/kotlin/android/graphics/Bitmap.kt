package android.graphics

/** The part of android.graphics.Bitmap upstream's tests compare: size and ARGB pixels. */
class Bitmap(val width: Int, val height: Int, private val pixels: IntArray) {
    fun sameAs(other: Bitmap?): Boolean =
        other != null && width == other.width && height == other.height && pixels.contentEquals(other.pixels)
}
