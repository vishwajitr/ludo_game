package com.example.ludo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ludo.AppViewModel
import com.example.ludo.Screen

@Composable
fun LudoApp(vm: AppViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val screen by vm.screen.collectAsStateWithLifecycle()
    val setup by vm.setup.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    val game by vm.game.collectAsStateWithLifecycle()

    MaterialTheme(colorScheme = if (settings.dark) darkColorScheme() else lightColorScheme()) {
        Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
            if (screen != Screen.Home && screen != Screen.Game) BackHandler { vm.go(Screen.Home) }
            when (val s = screen) {
                Screen.Home -> HomeScreen(saved != null, vm::continueGame, vm::openSetup, { vm.go(Screen.HowTo) }, { vm.go(Screen.Settings) })
                Screen.Setup -> SetupScreen(setup, { c -> vm.updateSetup { c } }, vm::startGame) { vm.go(Screen.Home) }
                Screen.Settings -> SettingsScreen(settings, vm::updateSettings) { vm.go(Screen.Home) }
                Screen.HowTo -> HowToPlayScreen { vm.go(Screen.Home) }
                Screen.Game -> game?.let { GameScreen(it, settings.haptics, vm::quit, vm::restart, vm::finish) }
                    ?: LaunchedEffect(Unit) { vm.go(Screen.Home) }
                is Screen.Results -> ResultsScreen(s.ranking, vm::restart) { vm.quit() }
            }
        }
    }
}
