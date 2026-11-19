package com.example.ludo.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ludo.R
import ludo.engine.Color as PlayerColor

/*
 * ─────────────────────────────────────────────────────────────────────────────
 *  THEME: every color, font and corner radius in the app comes from this file.
 *
 *  To restyle the game:
 *   - tweak the values in [MidnightRoyal], or
 *   - write another [LudoPalette] (see [Daylight] for a light one) and point [ActivePalette] at it.
 *  Player colors only need their base value: lighter and darker shades are derived.
 * ─────────────────────────────────────────────────────────────────────────────
 */

/** All the colors the UI uses. */
data class LudoPalette(
    val isDark: Boolean,
    // app chrome
    val ground: Color,        // screen background
    val groundGlow: Color,    // soft light at the top of the screen background
    val surface: Color,       // cards, buttons
    val raised: Color,        // dialogs, elements on a card
    val outline: Color,       // card borders
    val text: Color,
    val textMuted: Color,
    val track: Color,         // empty progress dots, inactive chips
    // accent (primary buttons, highlights, movable-token glow)
    val accent: Color,
    val accentLight: Color,   // top of the primary button gradient
    val onAccent: Color,
    // players
    val red: Color,
    val green: Color,
    val yellow: Color,
    val blue: Color,
    // board
    val boardCell: Color,
    val boardLine: Color,
    val boardFrameTop: Color,
    val boardFrameBottom: Color,
    val star: Color,
    val yardPanel: Color,     // the light panel inside each yard
    // dice
    val diceFace: Color,
    val diceSide: Color,
    val dicePip: Color,
    // results medals (gold uses accent)
    val silver: Color,
    val bronze: Color,
    val onMedal: Color,
)

val MidnightRoyal = LudoPalette(
    isDark = true,
    ground = Color(0xFF0E1630),
    groundGlow = Color(0xFF1C2A5C),
    surface = Color(0xFF18234A),
    raised = Color(0xFF22305E),
    outline = Color(0xFF2C3A6B),
    text = Color(0xFFF4F6FB),
    textMuted = Color(0xFFA9B4D6),
    track = Color(0xFF3A4A80),
    accent = Color(0xFFFFC94A),
    accentLight = Color(0xFFFFD66B),
    onAccent = Color(0xFF1A1300),
    red = Color(0xFFE5484D),
    green = Color(0xFF2EB872),
    yellow = Color(0xFFFFC53D),
    blue = Color(0xFF3E8BFF),
    boardCell = Color(0xFFF7F8FC),
    boardLine = Color(0xFFCBD1E4),
    boardFrameTop = Color(0xFF2A3A70),
    boardFrameBottom = Color(0xFF1E2A55),
    star = Color(0xFF8A93AF),
    yardPanel = Color(0xFFFFFFFF),
    diceFace = Color(0xFFE6EAF5),
    diceSide = Color(0xFFB8C0D9),
    dicePip = Color(0xFF141B33),
    silver = Color(0xFFC9D1E6),
    bronze = Color(0xFFD9935B),
    onMedal = Color(0xFF141B33),
)

/** A light alternative, ready to use: set `ActivePalette = Daylight`. */
val Daylight = MidnightRoyal.copy(
    isDark = false,
    ground = Color(0xFFF3F5FB),
    groundGlow = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    raised = Color(0xFFE9EDF8),
    outline = Color(0xFFD5DBEC),
    text = Color(0xFF141B33),
    textMuted = Color(0xFF59627F),
    track = Color(0xFFD0D6E8),
    accent = Color(0xFFF5B000),
    accentLight = Color(0xFFFFC94A),
    boardFrameTop = Color(0xFF2A3A70),
    boardFrameBottom = Color(0xFF1E2A55),
)

/** The palette the app is drawn with. Change this one line to switch themes. */
val ActivePalette: LudoPalette = MidnightRoyal

/** Fonts (bundled in res/font, SIL Open Font License: see licenses/). */
val DisplayFont = FontFamily(
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
    Font(R.font.outfit_extrabold, FontWeight.ExtraBold),
)
val BodyFont = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

/** Corner radii and sizes shared across screens. */
object Dimens {
    val cornerSmall: Dp = 12.dp
    val cornerMedium: Dp = 18.dp
    val cornerLarge: Dp = 28.dp
    val iconButton: Dp = 48.dp      // also the minimum touch target
    val primaryButton: Dp = 58.dp
    val screenPadding: Dp = 16.dp
}

object LudoShapes {
    val small = RoundedCornerShape(Dimens.cornerSmall)
    val medium = RoundedCornerShape(Dimens.cornerMedium)
    val large = RoundedCornerShape(Dimens.cornerLarge)
    val iconButton = RoundedCornerShape(14.dp)
}

/** Shortcut used throughout the UI: `Ludo.colors.accent`, `Ludo.colors.surface`... */
object Ludo {
    val colors: LudoPalette get() = ActivePalette
}

// ── player colors ────────────────────────────────────────────────────────────

/** A player's base color. */
val PlayerColor.ui: Color get() = when (this) {
    PlayerColor.RED -> Ludo.colors.red
    PlayerColor.GREEN -> Ludo.colors.green
    PlayerColor.YELLOW -> Ludo.colors.yellow
    PlayerColor.BLUE -> Ludo.colors.blue
}
/** Highlight shade (token gloss, yard slots). */
val PlayerColor.light: Color get() = lerp(ui, Color.White, .5f)
/** Shadow shade (yard gradient, token rim). */
val PlayerColor.dark: Color get() = lerp(ui, Color.Black, .25f)
/** Readable text/icon color on top of the player color. */
val PlayerColor.on: Color get() = if (ui.luminance() > .45f) Color(0xFF141B33) else Color.White

val PlayerColor.label: String get() = name.lowercase().replaceFirstChar { it.uppercase() }

// ── Material wiring ──────────────────────────────────────────────────────────

private fun LudoPalette.scheme() = if (isDark) darkColorScheme(
    primary = accent, onPrimary = onAccent, primaryContainer = raised, onPrimaryContainer = text,
    secondary = blue, onSecondary = Color.White,
    background = ground, onBackground = text, surface = ground, onSurface = text,
    surfaceVariant = raised, onSurfaceVariant = textMuted, outline = outline, outlineVariant = outline,
    surfaceContainerLowest = ground, surfaceContainerLow = surface, surfaceContainer = surface,
    surfaceContainerHigh = raised, surfaceContainerHighest = raised,
) else lightColorScheme(
    primary = accent, onPrimary = onAccent, primaryContainer = raised, onPrimaryContainer = text,
    secondary = blue, onSecondary = Color.White,
    background = ground, onBackground = text, surface = ground, onSurface = text,
    surfaceVariant = raised, onSurfaceVariant = textMuted, outline = outline, outlineVariant = outline,
    surfaceContainerLowest = ground, surfaceContainerLow = surface, surfaceContainer = surface,
    surfaceContainerHigh = surface, surfaceContainerHighest = raised,
)

private val base = Typography()
private fun TextStyle.display(w: FontWeight) = copy(fontFamily = DisplayFont, fontWeight = w)
private fun TextStyle.body(w: FontWeight = FontWeight.Normal) = copy(fontFamily = BodyFont, fontWeight = w)

private val LudoTypography = Typography(
    displayLarge = base.displayLarge.display(FontWeight.ExtraBold).copy(fontSize = 56.sp, letterSpacing = 6.sp),
    displayMedium = base.displayMedium.display(FontWeight.ExtraBold),
    displaySmall = base.displaySmall.display(FontWeight.ExtraBold),
    headlineLarge = base.headlineLarge.display(FontWeight.ExtraBold),
    headlineMedium = base.headlineMedium.display(FontWeight.Bold),
    headlineSmall = base.headlineSmall.display(FontWeight.Bold),
    titleLarge = base.titleLarge.display(FontWeight.SemiBold),
    titleMedium = base.titleMedium.body(FontWeight.SemiBold),
    titleSmall = base.titleSmall.body(FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.body(),
    bodyMedium = base.bodyMedium.body(),
    bodySmall = base.bodySmall.body(),
    labelLarge = base.labelLarge.body(FontWeight.SemiBold),
    labelMedium = base.labelMedium.body(FontWeight.SemiBold),
    labelSmall = base.labelSmall.body(FontWeight.SemiBold).copy(letterSpacing = 1.2.sp),
)

@Composable
fun LudoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Ludo.colors.scheme(), typography = LudoTypography, content = content)
}
