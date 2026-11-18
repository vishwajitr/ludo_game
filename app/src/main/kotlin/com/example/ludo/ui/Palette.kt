package com.example.ludo.ui

import androidx.compose.ui.graphics.Color as UiColor
import ludo.engine.Color

val Color.ui: UiColor get() = when (this) {
    Color.RED -> UiColor(0xFFE53935)
    Color.GREEN -> UiColor(0xFF43A047)
    Color.YELLOW -> UiColor(0xFFFDD835)
    Color.BLUE -> UiColor(0xFF1E88E5)
}

val Color.label: String get() = name.lowercase().replaceFirstChar { it.uppercase() }
