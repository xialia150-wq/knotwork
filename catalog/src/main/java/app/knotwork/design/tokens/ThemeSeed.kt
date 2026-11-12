@file:Suppress("MagicNumber")

package app.knotwork.design.tokens

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import app.knotwork.design.R

/** A named hue and its maximum chroma. IDs are stable for stored preferences. */
data class ThemePreset(
    val id: String,
    @StringRes val label: Int,
    @StringRes val group: Int,
    val hue: Double,
    val chroma: Double,
)

/** One registry is shared by the theme and its picker. */
val themePresets = listOf(
    ThemePreset("mist_blue", R.string.appearance_preset_mist_blue, R.string.appearance_group_muted, 240.0, 0.09),
    ThemePreset("slate_cyan", R.string.appearance_preset_slate_cyan, R.string.appearance_group_muted, 210.0, 0.09),
    ThemePreset("dusk_purple", R.string.appearance_preset_dusk_purple, R.string.appearance_group_muted, 300.0, 0.09),
    ThemePreset("turquoise", R.string.appearance_preset_turquoise, R.string.appearance_group_muted, 180.0, 0.09),
    ThemePreset("amber", R.string.appearance_preset_amber, R.string.appearance_group_muted, 75.0, 0.09),
    ThemePreset("graphite", R.string.appearance_preset_graphite, R.string.appearance_group_muted, 250.0, 0.025),
    ThemePreset("blue", R.string.appearance_preset_blue, R.string.appearance_group_vivid, 260.0, 0.19),
    ThemePreset("green", R.string.appearance_preset_green, R.string.appearance_group_vivid, 150.0, 0.19),
    ThemePreset("orange", R.string.appearance_preset_orange, R.string.appearance_group_vivid, 55.0, 0.19),
    ThemePreset("red", R.string.appearance_preset_red, R.string.appearance_group_vivid, 25.0, 0.19),
    ThemePreset("purple", R.string.appearance_preset_purple, R.string.appearance_group_vivid, 310.0, 0.19),
    ThemePreset("pink", R.string.appearance_preset_pink, R.string.appearance_group_vivid, 350.0, 0.19),
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
        surfaceTint = primary, outline = neutral(if (dark) 0.60 else 0.55),
        inversePrimary = tone(if (dark) 0.48 else 0.80),
        outlineVariant = neutral(if (dark) 0.32 else 0.89),
        surfaceDim = neutral(if (dark) 0.19 else 0.92), surfaceBright = neutral(if (dark) 0.34 else 0.98),
        surfaceContainerLowest = bg, surfaceContainerLow = neutral(if (dark) 0.22 else 0.97),
        surfaceContainer = neutral(if (dark) 0.25 else 0.95), surfaceContainerHigh = neutral(if (dark) 0.28 else 0.93),
        surfaceContainerHighest = neutral(if (dark) 0.32 else 0.91),
    )
}

/** Theme surfaces follow the seed while signals and all node hues remain fixed. */
fun themedExtendedColors(seed: ThemeSeed, scheme: ColorScheme, dark: Boolean): KnotworkExtendedColors {
    val original = if (dark) knotworkExtendedColorsDark() else knotworkExtendedColorsLight()
    val neutralChroma = minOf(seed.chroma.coerceAtLeast(0.025) * 0.12, 0.012)
    fun neutral(lightness: Double) = themeTone(lightness, neutralChroma, seed.hue)
    return original.copy(
        surface1 = scheme.surfaceContainerLow,
        surface2 = scheme.surfaceContainer,
        surface3 = scheme.surfaceContainerHigh,
        surface4 = scheme.surfaceContainerHighest,
        divider = scheme.outlineVariant,
        outlineStrong = scheme.outline,
        onSurface2 = scheme.onSurfaceVariant,
        onSurfaceMuted = neutral(if (dark) 0.72 else 0.45),
        onSurfaceDim = neutral(if (dark) 0.60 else 0.55),
    )
}
