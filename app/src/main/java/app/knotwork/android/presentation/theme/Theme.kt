package app.knotwork.android.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import app.knotwork.android.domain.repositories.AppearanceSettings
import app.knotwork.design.theme.KnotworkTheme
import app.knotwork.design.tokens.ThemeSeed
import app.knotwork.design.tokens.themePresets
import app.knotwork.design.tokens.themeSeed

/** Wallpaper supplies only a seed; our fixed-lightness OKLCH roles construct the palette. */
@Composable
fun KnotworkAppTheme(
    appearance: AppearanceSettings = AppearanceSettings(),
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // Read configuration so changes to wallpaper-derived resources trigger recomposition.
    LocalConfiguration.current
    val seed = when (appearance.source) {
        "preset" -> themePresets.firstOrNull { it.id == appearance.preset }
            ?.let { ThemeSeed(it.hue, it.chroma) } ?: ThemeSeed()
        "custom" -> themeSeed(Color(appearance.customColor))
        else -> wallpaperThemeSeed {
            val id = context.resources.getIdentifier("system_accent1_500", "color", "android")
            if (id == 0) null else Color(context.getColor(id))
        }
    }
    val dark = when (appearance.mode) {
        "light" -> false
        "dark" -> true
        else -> darkTheme
    }
    KnotworkTheme(
        darkTheme = dark,
        seed = seed,
        vivid = appearance.vivid,
        pureBlack = appearance.pureBlack,
        content = content,
    )
}
