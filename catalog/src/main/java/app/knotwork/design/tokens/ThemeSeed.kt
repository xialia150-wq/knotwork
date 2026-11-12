@file:Suppress("MagicNumber")

package app.knotwork.design.tokens

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

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
    val rgb = Srgb(color.red.toDouble(), color.green.toDouble(), color.blue.toDouble())
    val lch = OklchColorMath.toOklch(OklchColorMath.toOklab(OklchColorMath.toLinear(rgb)))
    return ThemeSeed(lch.hue, lch.chroma)
}

/** Maps a fixed-lightness role into the sRGB display gamut. */
fun themeTone(lightness: Double, chroma: Double, hue: Double): Color {
    val rgb = OklchColorMath.gamutMap(Oklch(lightness, chroma, hue))
    return Color(rgb.red.toFloat(), rgb.green.toFloat(), rgb.blue.toFloat())
}

/** Fixed-lightness surface and accent roles; semantic signal and node colours remain untouched. */
fun themedColorScheme(seed: ThemeSeed, vivid: Boolean, dark: Boolean, pureBlack: Boolean): ColorScheme {
    val chroma = if (vivid) (seed.chroma * 2).coerceIn(0.08, 0.20) else (seed.chroma * 0.55).coerceIn(0.025, 0.09)
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
