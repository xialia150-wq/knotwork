@file:Suppress("MagicNumber")

package app.knotwork.design.tokens

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** A named hue and its maximum chroma. IDs are stable for stored preferences. */
data class ThemePreset(val id: String, val label: String, val group: String, val hue: Double, val chroma: Double)

/** One registry is shared by the theme and its picker. */
val themePresets = listOf(
    ThemePreset("mist_blue", "雾蓝", "低饱和", 240.0, 0.09),
    ThemePreset("slate_cyan", "黛青", "低饱和", 210.0, 0.09),
    ThemePreset("dusk_purple", "暮紫", "低饱和", 300.0, 0.09),
    ThemePreset("turquoise", "松石", "低饱和", 180.0, 0.09),
    ThemePreset("amber", "琥珀", "低饱和", 75.0, 0.09),
    ThemePreset("graphite", "石墨", "低饱和", 250.0, 0.025),
    ThemePreset("blue", "蓝", "鲜亮", 260.0, 0.19),
    ThemePreset("green", "绿", "鲜亮", 150.0, 0.19),
    ThemePreset("orange", "橙", "鲜亮", 55.0, 0.19),
    ThemePreset("red", "红", "鲜亮", 25.0, 0.19),
    ThemePreset("purple", "紫", "鲜亮", 310.0, 0.19),
    ThemePreset("pink", "粉", "鲜亮", 350.0, 0.19),
)

/** Chroma and hue are independent from the fixed lightness of each role. */
data class ThemeSeed(val hue: Double = 240.0, val chroma: Double = 0.09)

/** Extract the hue/chroma from an sRGB seed, ignoring its lightness for readable output. */
fun themeSeed(color: Color): ThemeSeed {
    fun linear(v: Float): Double = if (v <= 0.04045f) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    val r = linear(color.red)
    val g = linear(color.green)
    val b = linear(color.blue)
    val l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
    val m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
    val s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
    val a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
    val bb = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
    val chroma = sqrt(a * a + bb * bb)
    return ThemeSeed(if (chroma < 0.0001) 240.0 else (atan2(bb, a) * 180 / PI + 360) % 360, chroma)
}

/** OKLCH to sRGB with chroma reduced to fit the display gamut. */
fun themeTone(lightness: Double, chroma: Double, hue: Double): Color {
    fun rgb(c: Double): Triple<Double, Double, Double> {
        val a = c * cos(hue * PI / 180)
        val b = c * sin(hue * PI / 180)
        val l = (lightness + 0.3963377774 * a + 0.2158037573 * b).pow(3)
        val m = (lightness - 0.1055613458 * a - 0.0638541728 * b).pow(3)
        val s = (lightness - 0.0894841775 * a - 1.2914855480 * b).pow(3)
        return Triple(4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
            -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
            -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s)
    }
    var low = 0.0
    var high = chroma.coerceAtLeast(0.0)
    repeat(20) {
        val mid = (low + high) / 2
        val (r, g, b) = rgb(mid)
        if (r in 0.0..1.0 && g in 0.0..1.0 && b in 0.0..1.0) low = mid else high = mid
    }
    fun encode(v: Double): Float = (if (v <= 0.0031308) 12.92 * v else 1.055 * v.pow(1 / 2.4) - 0.055)
        .coerceIn(0.0, 1.0).toFloat()
    val (r, g, b) = rgb(low)
    return Color(encode(r), encode(g), encode(b))
}

/** Fixed-lightness surface and accent roles; semantic signal and node colours remain untouched. */
fun themedColorScheme(seed: ThemeSeed, vivid: Boolean, dark: Boolean, pureBlack: Boolean): ColorScheme {
    val chroma = if (vivid) seed.chroma.coerceIn(0.08, 0.20) else seed.chroma.coerceIn(0.025, 0.09)
    fun tone(l: Double, c: Double = chroma) = themeTone(l, c, seed.hue)
    fun neutral(l: Double) = tone(l, minOf(chroma * 0.12, 0.012))
    val base = if (dark) knotworkDarkColorScheme() else knotworkLightColorScheme()
    val bg = if (dark && pureBlack) Color.Black else neutral(if (dark) 0.19 else 0.98)
    val on = neutral(if (dark) 0.94 else 0.19)
    val onVariant = neutral(if (dark) 0.79 else 0.36)
    val primary = tone(if (dark) 0.80 else 0.48)
    val onPrimary = neutral(if (dark) 0.19 else 0.99)
    val container = tone(if (dark) 0.30 else 0.92, chroma * 0.55)
    val onContainer = tone(if (dark) 0.92 else 0.22, chroma * 0.45)
    return base.copy(
        primary = primary, onPrimary = onPrimary, primaryContainer = container, onPrimaryContainer = onContainer,
        secondary = primary, onSecondary = onPrimary, secondaryContainer = neutral(if (dark) 0.30 else 0.93),
        onSecondaryContainer = on, background = bg, onBackground = on, surface = bg, onSurface = on,
        surfaceVariant = neutral(if (dark) 0.28 else 0.94), onSurfaceVariant = onVariant,
        surfaceTint = primary, outline = neutral(if (dark) 0.48 else 0.65),
        outlineVariant = neutral(if (dark) 0.32 else 0.89),
        surfaceDim = neutral(if (dark) 0.19 else 0.92), surfaceBright = neutral(if (dark) 0.34 else 0.98),
        surfaceContainerLowest = bg, surfaceContainerLow = neutral(if (dark) 0.22 else 0.97),
        surfaceContainer = neutral(if (dark) 0.25 else 0.95), surfaceContainerHigh = neutral(if (dark) 0.28 else 0.93),
        surfaceContainerHighest = neutral(if (dark) 0.32 else 0.91),
    )
}
