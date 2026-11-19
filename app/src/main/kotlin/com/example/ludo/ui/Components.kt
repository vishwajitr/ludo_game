package com.example.ludo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Screen background: the ground color with a soft glow at the top. */
fun Modifier.ludoBackground(): Modifier = drawBehind {
    drawRect(Ludo.colors.ground)
    drawRect(Brush.radialGradient(listOf(Ludo.colors.groundGlow, Color.Transparent), Offset(size.width / 2, 0f), size.maxDimension * .7f))
}

/** A card-like surface: filled with [Ludo.colors.surface] and a thin outline. */
fun Modifier.card(shape: androidx.compose.ui.graphics.Shape = LudoShapes.medium): Modifier =
    clip(shape).background(Ludo.colors.surface).border(1.dp, Ludo.colors.outline, shape)

/** Square icon button with an outline: back, pause, sound, settings. */
@Composable
fun IconSquare(icon: ImageVector, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(Dimens.iconButton).card(LudoShapes.iconButton).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription, Modifier.size(22.dp), tint = Ludo.colors.text) }
}

/** The one main action on a screen: accent gradient, display font. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val c = Ludo.colors
    Row(
        modifier.fillMaxWidth().height(Dimens.primaryButton)
            .shadow(10.dp, LudoShapes.medium, ambientColor = c.accent, spotColor = c.accent)
            .clip(LudoShapes.medium)
            .background(Brush.verticalGradient(listOf(c.accentLight, c.accent)))
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(22.dp), tint = c.onAccent); Spacer(Modifier.width(10.dp)) }
        Text(text, color = c.onAccent, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 19.sp))
    }
}

/** Secondary action: surface card with an outline. */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().heightIn(min = 54.dp).card().clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = MaterialTheme.typography.titleMedium, color = Ludo.colors.text) }
}

/** Back button + title, at the top of menu screens. */
@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, subtitle: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconSquare(LudoIcons.Back, "Back", onBack)
        Column {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = Ludo.colors.text)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Ludo.colors.textMuted)
        }
    }
}

/** Small uppercase label above a group. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier, style = MaterialTheme.typography.labelSmall, color = Ludo.colors.textMuted)
}

/** A titled card that groups related controls. */
@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(title, Modifier.padding(start = 4.dp))
        Column(Modifier.fillMaxWidth().card().padding(horizontal = 16.dp, vertical = 8.dp), content = content)
    }
}

/** Whole row toggles; 48dp minimum touch target. */
@Composable
fun SwitchRow(label: String, checked: Boolean, hint: String? = null, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = Ludo.colors.text)
            if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = Ludo.colors.textMuted)
        }
        Switch(
            checked, null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Ludo.colors.accent, checkedThumbColor = Ludo.colors.onAccent,
                uncheckedTrackColor = Ludo.colors.raised, uncheckedThumbColor = Ludo.colors.textMuted,
                uncheckedBorderColor = Ludo.colors.outline,
            ),
        )
    }
}

/** Pill selector: one option is highlighted in [accent]. */
@Composable
fun <T> Segmented(
    options: List<Pair<String, T>>,
    selected: T,
    modifier: Modifier = Modifier,
    accent: Color = Ludo.colors.accent,
    onSelect: (T) -> Unit,
) {
    val c = Ludo.colors
    Row(modifier.fillMaxWidth().card(LudoShapes.medium).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { (label, v) ->
            val on = selected == v
            val tinted = accent != c.accent // player colors: translucent fill + outline, readable on any hue
            Box(
                Modifier.weight(1f).heightIn(min = 44.dp).clip(LudoShapes.small)
                    .background(if (!on) Color.Transparent else if (tinted) accent.copy(alpha = .22f) else accent)
                    .border(if (on && tinted) 1.5.dp else 0.dp, if (on && tinted) accent else Color.Transparent, LudoShapes.small)
                    .selectable(on, role = Role.RadioButton) { onSelect(v) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) (if (tinted) c.text else c.onAccent) else c.textMuted,
                )
            }
        }
    }
}
