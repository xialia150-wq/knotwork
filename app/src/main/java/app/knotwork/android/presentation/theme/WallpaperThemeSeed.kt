package app.knotwork.android.presentation.theme

import androidx.compose.ui.graphics.Color
import app.knotwork.design.tokens.ThemeSeed
import app.knotwork.design.tokens.themeSeed

/** Converts wallpaper color to a seed, falling back to mist blue when it is unavailable. */
internal fun wallpaperThemeSeed(readColor: () -> Color?): ThemeSeed =
    runCatching { readColor()?.let(::themeSeed) ?: ThemeSeed() }.getOrDefault(ThemeSeed())
