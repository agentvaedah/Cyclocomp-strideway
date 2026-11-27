package id.web.agentvaedah.strideway.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import id.web.agentvaedah.strideway.R

/**
 * Outfit (SIL OFL 1.1) for clean, readable body copy and controls.
 */
val Outfit = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
)

/**
 * Barlow Condensed (SIL OFL 1.1): athletic condensed display face for screen/section headers
 * and mid-size telemetry figures so headings and numbers stand apart from running text.
 */
val BarlowCondensed = FontFamily(
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
)

/**
 * IBM Plex Mono (SIL OFL 1.1): crisp technical monospace for telemetry labels, units, axis ticks,
 * and tabular data badges.
 */
val IbmPlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
)

/**
 * Strideway Sans Bold Italic (tools/font/build_font.py): the brand face, cut at 45 degrees and slanted 12 degrees
 * like the logo. Carries primary screen titles and hero numbers.
 */
val StridewaySans = FontFamily(Font(R.font.strideway_sans_bold_italic, FontWeight.Bold, FontStyle.Italic))

val StridewayTypography = Typography(
    headlineLarge = TextStyle(fontFamily = StridewaySans, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = 0.2.sp),
    headlineSmall = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = 0.3.sp),
    titleLarge = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = 0.3.sp),
    titleMedium = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 24.sp, letterSpacing = 0.25.sp),
    titleSmall = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = IbmPlexMono, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontFamily = IbmPlexMono, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.4.sp),
)

object MetricText {
    private const val TABULAR = "tnum"

    val Huge = TextStyle(fontFamily = StridewaySans, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, fontSize = 54.sp, lineHeight = 56.sp, fontFeatureSettings = TABULAR)
    val Large = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold, fontSize = 42.sp, lineHeight = 44.sp, letterSpacing = 0.4.sp, fontFeatureSettings = TABULAR)
    val Medium = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 32.sp, letterSpacing = 0.3.sp, fontFeatureSettings = TABULAR)
    val Small = TextStyle(fontFamily = IbmPlexMono, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp, fontFeatureSettings = TABULAR)
    val Label = TextStyle(fontFamily = IbmPlexMono, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp)
}
