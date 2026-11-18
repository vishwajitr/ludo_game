# Ludo for Android

A Ludo game for 2–4 players: pass-and-play, or against Easy / Medium / Hard bots, with optional house rules.
Kotlin, Jetpack Compose (Material 3), no ads, no analytics, no network permission.

## Build

Requirements: JDK 17 and the Android SDK (platform 37; `local.properties` with `sdk.dir=...`, or `ANDROID_HOME`).

```bash
./gradlew test               # engine + ViewModel unit tests (includes the 1,000-game simulation)
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew connectedDebugAndroidTest   # Compose UI test; needs a running emulator or device
./gradlew assembleRelease    # R8-minified; signed only if keystore.properties exists
```

Package ID: `com.vishwajitr.ludo` (code package stays `com.example.ludo`). Play Store listing text, graphics and upload steps are in `store/LISTING.md`; the privacy policy is `PRIVACY.md`.

Release signing: copy `keystore.properties.example` to `keystore.properties` (git-ignored) and fill it in.
Bump `VERSION_CODE` / `VERSION_NAME` in `gradle.properties` for each release.
Warnings are treated as errors in both modules.

`tools/gen_sounds.py` regenerates the sound effects and music loop in `app/src/main/res/raw` (stdlib only).

## Architecture

| Module | Contents |
| --- | --- |
| `:engine` | Pure Kotlin, no Android dependencies. Board model, rules, `legalMoves` / `applyRoll` / `applyMove`, events, bots, dice (`DiceRoller`, `SeededDice`), and the save-game text codec. |
| `:app` | Compose UI, ViewModels, DataStore persistence, SoundPool/MediaPlayer, haptics. |

Unidirectional data flow: `GameViewModel` exposes `StateFlow<GameUiState>`; the UI sends intents (`onRoll`, `onTokenTap`).
The engine is immutable: `applyRoll` and `applyMove` return a `MoveResult(state, events)`; the ViewModel plays the
events as hop / capture animations, sounds and haptics. `AppViewModel` owns navigation, settings and the running game,
and survives rotation. The game is auto-saved after every move (DataStore) and offered as **Continue** on Home.

Token positions are one number per token: `-1` yard, `0..50` main track (0 = the color's start square),
`51..55` home column, `56` home.

Dependencies are manual (no Hilt) and persistence is DataStore only (no Room).

## Rules summary

- 2–4 players (Red, Green, Yellow, Blue), 4 tokens each; main track of 52 squares, a 5-square home column per color, then home.
- A token leaves the yard on a 6 and lands on its start square.
- A 6 gives an extra roll; three 6s in a row forfeit the turn (the third move is not made).
- Landing on an opponent on a non-safe square captures it (back to its yard) and gives an extra roll.
  Safe squares: the 4 start squares and the 4 stars. Any colors may share them and nothing is captured there.
- Reaching home needs an exact roll and gives an extra roll.
- No legal move: the turn passes (a 6 with no legal move gives no extra roll). One legal move: auto-played (toggle).
- The game runs until all but one player have finished; the full ranking is shown.
- House rules (Settings or New Game): **blockades** (two same-color tokens on a square block passing and landing),
  **must capture to enter the home column** (waived when it would leave the player with no move at all, so games cannot
  deadlock), **start with one token out**, **1 or 6 to leave the yard**.

Bots: *Easy* picks a random legal move. *Medium* uses the priority capture > reach home > leave yard > escape danger >
advance furthest. *Hard* scores progress, captures, blockades and safety, with a 1-ply lookahead over the opponents'
six dice outcomes. Bots wait 500–900 ms before acting.

## Accessibility

Tokens, dice and turn text have content descriptions (the turn text is a live region), touch targets are at least 48 dp,
and colorblind mode draws a distinct shape per color (circle, square, triangle, diamond).
