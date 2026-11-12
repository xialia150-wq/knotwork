@file:Suppress("MagicNumber")

package app.knotwork.design.tokens

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Color components use unbounded doubles until gamut mapping is performed. */
data class Srgb(val red: Double, val green: Double, val blue: Double)
data class LinearSrgb(val red: Double, val green: Double, val blue: Double)
data class Oklab(val lightness: Double, val a: Double, val b: Double)
/** Polar OKLab coordinates: lightness, chroma, and hue in degrees. */
data class Oklch(val lightness: Double, val chroma: Double, val hue: Double)

/** Standard OKLab, sRGB transfer, gamut mapping and WCAG contrast operations. */
object OklchColorMath {
    fun toOklab(value: Oklch): Oklab {
        val angle = value.hue * PI / 180.0
        return Oklab(value.lightness, value.chroma * cos(angle), value.chroma * sin(angle))
    }

    fun toOklch(value: Oklab): Oklch {
        val chroma = sqrt(value.a * value.a + value.b * value.b)
        val hue = if (chroma < 0.0001) 240.0 else (atan2(value.b, value.a) * 180.0 / PI + 360.0) % 360.0
        return Oklch(value.lightness, chroma, hue)
    }

    fun toLinear(value: Oklab): LinearSrgb {
        val l = (value.lightness + 0.3963377774 * value.a + 0.2158037573 * value.b).pow(3)
        val m = (value.lightness - 0.1055613458 * value.a - 0.0638541728 * value.b).pow(3)
        val s = (value.lightness - 0.0894841775 * value.a - 1.2914855480 * value.b).pow(3)
        return LinearSrgb(
            4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
            -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
            -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s,
        )
    }

    fun toOklab(value: LinearSrgb): Oklab {
        val l = cbrt(0.4122214708 * value.red + 0.5363325363 * value.green + 0.0514459929 * value.blue)
        val m = cbrt(0.2119034982 * value.red + 0.6806995451 * value.green + 0.1073969566 * value.blue)
        val s = cbrt(0.0883024619 * value.red + 0.2817188376 * value.green + 0.6299787005 * value.blue)
        return Oklab(
            0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
            1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
            0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s,
        )
    }

    fun toLinear(value: Srgb): LinearSrgb {
        fun decode(v: Double) = if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        return LinearSrgb(decode(value.red), decode(value.green), decode(value.blue))
    }

    fun toSrgb(value: LinearSrgb): Srgb {
        fun encode(v: Double) = if (v <= 0.0031308) 12.92 * v else 1.055 * v.pow(1.0 / 2.4) - 0.055
        return Srgb(encode(value.red), encode(value.green), encode(value.blue))
    }

    /** Reduce chroma only, keeping lightness and hue fixed, until linear sRGB is in gamut. */
    fun gamutMap(value: Oklch): Srgb {
        require(value.lightness in 0.0..1.0 && value.chroma >= 0.0 && value.hue.isFinite())
        fun linear(chroma: Double) = toLinear(toOklab(value.copy(chroma = chroma)))
        fun inGamut(rgb: LinearSrgb) = rgb.red in 0.0..1.0 && rgb.green in 0.0..1.0 && rgb.blue in 0.0..1.0
        val exact = linear(value.chroma)
        if (inGamut(exact)) return toSrgb(exact)
        // Neutral black/white are the endpoints of every hue; avoid floating-point overshoot there.
        if (value.lightness == 0.0) return Srgb(0.0, 0.0, 0.0)
        if (value.lightness == 1.0) return Srgb(1.0, 1.0, 1.0)
        var low = 0.0
        var high = value.chroma
        repeat(30) {
            val mid = (low + high) / 2.0
            if (inGamut(linear(mid))) low = mid else high = mid
        }
        return toSrgb(linear(low))
    }

    fun relativeLuminance(value: Srgb): Double {
        val rgb = toLinear(value)
        return 0.2126 * rgb.red + 0.7152 * rgb.green + 0.0722 * rgb.blue
    }

    fun contrast(a: Srgb, b: Srgb): Double {
        val x = relativeLuminance(a)
        val y = relativeLuminance(b)
        return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05)
    }
}
