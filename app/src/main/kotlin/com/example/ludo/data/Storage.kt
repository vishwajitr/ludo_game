package com.example.ludo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*
import ludo.engine.Rules
import ludo.engine.SaveCodec
import ludo.engine.SavedGame

data class Settings(
    val sound: Boolean = true,
    val music: Boolean = true,
    val haptics: Boolean = true,
    val animSpeed: Int = 1, // 1x or 2x
    val autoMove: Boolean = true,
    val dark: Boolean = false,
    val colorblind: Boolean = false, // shapes on tokens
    val rules: Rules = Rules(), // default house rules for new games
)

private val Context.store: DataStore<Preferences> by preferencesDataStore("ludo")

private val SOUND = booleanPreferencesKey("sound")
private val MUSIC = booleanPreferencesKey("music")
private val HAPTICS = booleanPreferencesKey("haptics")
private val SPEED = intPreferencesKey("speed")
private val AUTO = booleanPreferencesKey("auto")
private val DARK = booleanPreferencesKey("dark")
private val CBLIND = booleanPreferencesKey("colorblind")
private val BLOCK = booleanPreferencesKey("blockades")
private val MUST = booleanPreferencesKey("mustCapture")
private val ONE_OUT = booleanPreferencesKey("oneOut")
private val ONE_SIX = booleanPreferencesKey("oneOrSix")
private val SAVED = stringPreferencesKey("savedGame")

class AppContainer(context: Context) {
    private val ds = context.applicationContext.store

    val settings: Flow<Settings> = ds.data.map { p ->
        Settings(
            p[SOUND] ?: true, p[MUSIC] ?: true, p[HAPTICS] ?: true, p[SPEED] ?: 1, p[AUTO] ?: true, p[DARK] ?: false, p[CBLIND] ?: false,
            Rules(p[BLOCK] ?: false, p[MUST] ?: false, p[ONE_OUT] ?: false, p[ONE_SIX] ?: false),
        )
    }

    suspend fun updateSettings(f: (Settings) -> Settings) {
        val n = f(settings.first())
        ds.edit {
            it[SOUND] = n.sound; it[MUSIC] = n.music; it[HAPTICS] = n.haptics; it[SPEED] = n.animSpeed
            it[AUTO] = n.autoMove; it[DARK] = n.dark; it[CBLIND] = n.colorblind
            it[BLOCK] = n.rules.blockades; it[MUST] = n.rules.mustCaptureToEnterHome
            it[ONE_OUT] = n.rules.startWithOneOut; it[ONE_SIX] = n.rules.oneOrSixToLeave
        }
    }

    val savedGame: Flow<SavedGame?> = ds.data.map { p -> p[SAVED]?.let(SaveCodec::decode) }
    suspend fun saveGame(g: SavedGame) { ds.edit { it[SAVED] = SaveCodec.encode(g) } }
    suspend fun clearGame() { ds.edit { it.remove(SAVED) } }
}
