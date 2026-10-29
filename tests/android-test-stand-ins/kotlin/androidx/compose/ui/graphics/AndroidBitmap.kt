package androidx.compose.ui.graphics

/** asAndroidBitmap for comparing captured images on desktop: the image's ARGB pixels. */
fun ImageBitmap.asAndroidBitmap(): android.graphics.Bitmap {
    val pixels = toPixelMap()
    return android.graphics.Bitmap(width, height, IntArray(width * height) { pixels[it % width, it / width].toArgb() })
}
