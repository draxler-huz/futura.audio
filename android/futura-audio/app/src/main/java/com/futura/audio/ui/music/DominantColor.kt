package com.futura.audio.ui.music

import android.content.Context
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Hue (degrees) of the most saturated pixels in a 32x32 downsample, matching the prototype's
 * useDominantColor hook. Returns null for greyscale art.
 */
private fun dominantHue(context: Context, @DrawableRes res: Int): Float? {
    val options = BitmapFactory.Options().apply { inSampleSize = 8 }
    val source = BitmapFactory.decodeResource(context.resources, res, options) ?: return null
    val bitmap = android.graphics.Bitmap.createScaledBitmap(source, 32, 32, true)
    var x = 0.0
    var y = 0.0
    var weight = 0.0
    for (py in 0 until 32) {
        for (px in 0 until 32) {
            val pixel = bitmap.getPixel(px, py)
            val r = android.graphics.Color.red(pixel) / 255.0
            val g = android.graphics.Color.green(pixel) / 255.0
            val b = android.graphics.Color.blue(pixel) / 255.0
            val hi = maxOf(r, g, b)
            val lo = minOf(r, g, b)
            val delta = hi - lo
            if (delta < 0.15 || hi < 0.25) continue
            val hue = when {
                hi == r -> ((g - b) / delta) % 6
                hi == g -> (b - r) / delta + 2
                else -> (r - g) / delta + 4
            }
            val rad = hue * 60 * PI / 180
            val w = delta * hi
            x += cos(rad) * w
            y += sin(rad) * w
            weight += w
        }
    }
    if (weight == 0.0) return null
    var deg = atan2(y, x) * 180 / PI
    if (deg < 0) deg += 360
    return deg.toFloat()
}

/** Accent colour derived from the artwork, or [fallback] while it is computed. */
@Composable
fun rememberDominantColor(@DrawableRes art: Int, fallback: Color): Color {
    val context = LocalContext.current
    val color by produceState(initialValue = fallback, art, fallback) {
        value = withContext(Dispatchers.Default) {
            dominantHue(context, art)
                ?.let { hue -> Color(ColorUtils.HSLToColor(floatArrayOf(hue, 1f, 0.58f))) }
                ?: fallback
        }
    }
    return color
}
