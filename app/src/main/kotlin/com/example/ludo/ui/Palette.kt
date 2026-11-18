package com.example.ludo.ui

import androidx.compose.ui.graphics.Color as UiColor
import ludo.engine.Color

val Color.ui: UiColor get() = when (this) {
    Color.RED -> UiColor(0xFFED1C24)
    Color.GREEN -> UiColor(0xFF00A14B)
    Color.YELLOW -> UiColor(0xFFFFDD1A)
    Color.BLUE -> UiColor(0xFF29B0FF)
}

val Color.label: String get() = name.lowercase().replaceFirstChar { it.uppercase() }
