package id.web.agentvaedah.strideway.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class StridewayColors(
    val bg: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val outline: Color,
    val text: Color,
    val textMuted: Color,
    val accent: Color,
    val onAccent: Color,
    val info: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val hrZones: List<Color>,
    val isDark: Boolean,
)

/** Brand colours from assets/branding/BRAND.md: Signal for accents only, Ink for type and dark surfaces. */
object Brand {
    val Signal = Color(0xFFFF4F00)
    val Ink = Color(0xFF0A0A0B)
}

// Dark suits dawn and night rides where a bright screen glares; light reads better in direct midday sun.
// Dark sits on Ink with Signal as the accent (6.0:1). Surfaces step up from Ink in small, neutral increments.
val DarkColors = StridewayColors(
    bg = Brand.Ink,
    surface = Color(0xFF15161A),
    surfaceHigh = Color(0xFF202228),
    outline = Color(0xFF2C2F38),
    text = Color(0xFFF6F6F8),
    textMuted = Color(0xFF9E9FA8),
    accent = Brand.Signal,
    onAccent = Color(0xFFFFFFFF),
    info = Color(0xFF4FA3E0),
    success = Color(0xFF47C97E),
    warning = Color(0xFFF5B83D),
    error = Color(0xFFF0524F),
    hrZones = listOf(Color(0xFF7D868E), Color(0xFF4FA3E0), Color(0xFF47C97E), Color(0xFFF5B83D), Color(0xFFF0524F)),
    isDark = true,
)

// Signal on white is only 3.3:1, so BRAND.md keeps it off small text. The light accent is a deeper Signal
// (5.4:1 on white) used for text, buttons and lines; Ink carries everything else.
val LightColors = StridewayColors(
    bg = Color(0xFFF4F5F7),
    surface = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFECEEF2),
    outline = Color(0xFFDFE2E8),
    text = Brand.Ink,
    textMuted = Color(0xFF5D606B),
    accent = Color(0xFFD44100),
    onAccent = Color(0xFFFFFFFF),
    info = Color(0xFF1C6BA8),
    success = Color(0xFF22864B),
    warning = Color(0xFFB27400),
    error = Color(0xFFC22F2B),
    hrZones = listOf(Color(0xFF6B737A), Color(0xFF1C6BA8), Color(0xFF22864B), Color(0xFFB27400), Color(0xFFC22F2B)),
    isDark = false,
)

val LocalStridewayColors = staticCompositionLocalOf { DarkColors }

/**
 * Theme-aware colours with the same names screens already use. Values come from the active
 * [StridewayTheme], so they can only be read during composition: inside a Canvas, LaunchedEffect or
 * map callback, read the colour into a local first.
 */
object Palette {
    val Bg: Color @Composable @ReadOnlyComposable get() = LocalStridewayColors.current.bg
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalStridewayColors.current.surface
    val SurfaceHigh: Color @Composable @ReadOnlyComposable get() = LocalStridewayColors.current.surfaceHigh
    val Outline: Color @Composable @ReadOnlyComposable get() = LocalStridewayColors.current.outline
    val Text: Color @Composable @ReadOnlyComposable get() = LocalStridewayColors.current.text
    val TextMuted: Color @Composable @ReadOnlyComposable get() = LocalStridewayColors.current.textMuted
    val Accent: Color @Composable @ReadOnlyComposable get() = LocalStridewayColors.current.accent
    val OnAccent: Color @Composable @ReadOnlyComposable get() = LocalStridewayColors.current.onAccent
    val Info: Color @Composable @ReadOnlyComposable get() = LocalStridewayColors.current.info
    val Success: Color @Composable @ReadOnlyComposable get() = LocalStridewayColors.current.success
    val Warning: Color @Composable @ReadOnlyComposable get() = LocalStridewayColors.current.warning
    val Error: Color @Composable @ReadOnlyComposable get() = LocalStridewayColors.current.error

    /** Zone 0 means below Z1 and uses the muted text colour. */
    @Composable
    @ReadOnlyComposable
    fun hrZone(zone: Int): Color {
        val c = LocalStridewayColors.current
        return if (zone in 1..5) c.hrZones[zone - 1] else c.textMuted
    }
}
