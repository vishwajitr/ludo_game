# Google Play listing – Ludo

Package ID: `com.vishwajitr.ludo` · Version 1.0.0 (code 1) · Upload file: `app/build/outputs/bundle/release/app-release.aab`, built after the AdMob setup below

## Store listing

**App name** (max 30): `Ludo Classic: Board Game`

**Short description** (max 80):
`Classic Ludo for 2-4 players. Play friends or smart bots, even offline.`

**Full description** (max 4000):

```
Roll the dice, race your tokens around the board and send your rivals back to their yard. Ludo is the classic family board game, made to play anywhere, even offline.

PLAY YOUR WAY
- Pass-and-play with 2 to 4 people on one phone
- Play against the computer, or mix humans and bots in any seats
- Three bot levels: Easy, Medium and Hard. Hard plans ahead and weighs the risk of every move

CLASSIC RULES, YOUR HOUSE RULES
- Roll a 6 to leave the yard, and roll again after a 6, a capture or reaching home
- Safe squares keep your tokens from being captured
- Optional house rules: blockades, capture before entering home, start with one token out, 1 or 6 to leave the yard

BUILT TO FEEL GOOD
- Smooth dice roll and hopping token animations
- Sound effects with a different pitch for each player, quiet background music, and haptic feedback (each can be switched off)
- Dark mode, 2x animation speed, and landscape play
- Your game is saved after every move, so you can continue any time
- A local leaderboard that tracks wins on your device

ACCESSIBLE
- Colorblind mode gives every player a different token shape
- Labelled tokens and dice for screen readers, and large touch targets

NO DISTRACTIONS DURING PLAY
No account needed. A small banner ad appears on the menus only, never on the board or during a turn. Your games, names and scores stay on your phone.
```

**App icon** (512x512): `store/icon-512.png`
**Feature graphic** (1024x500): `store/feature-graphic.png`
**Phone screenshots** (min 2): `store/screenshots/1-home.png` ... `4-game.png`

## App content (Play Console)

- **App or game:** Game. **Category:** Board. **Tags:** board, multiplayer, offline.
- **Contact email / website:** your own (the email is shown publicly on the store page).
- **Contains ads:** Yes (AdMob banners on menu screens).
- **Privacy policy URL:** must be a public web page. The text is in `PRIVACY.md`. Add your email where it says `[your-email@example.com]`, then host it (for example on a public GitHub repo or GitHub Pages) and paste the URL.
- **Ads:** Yes. **In-app purchases:** No. **Government / news / health app:** No.
- **Content rating (IARC questionnaire):** answer No to violence, blood, sexual content, profanity, drugs, gambling (no real money), user-generated content and location sharing. Expected rating: Everyone / PEGI 3.
- **Target audience:** 13 and over. With ads, a group that includes children under 13 would put the app under Google's Families policy and its certified-ads rules.
- **Data safety:** Yes, data is collected and shared, by the AdMob SDK only. Follow Google's AdMob guide ("Prepare for Google Play's Data safety section" in the AdMob help) and declare at least: **Device or other IDs** (advertising ID), **Approximate location**, **App interactions** and **Diagnostics / crash logs**, collected and shared for **Advertising** (and fraud prevention), encrypted in transit. Game data (saved game, settings, names, history) stays on the device and is not collected.
- **Advertising ID:** Yes, used for advertising.
- **Permissions:** internet and network state (ads), advertising ID. Nothing else.

## AdMob setup (before uploading)

1. Create an AdMob account at https://admob.google.com, add an Android app with package `com.vishwajitr.ludo`, and create one **Banner** ad unit.
2. Put the ids in `~/.gradle/gradle.properties` (not in the project, never committed):
   ```
   ADMOB_APP_ID=ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX
   ADMOB_BANNER_ID=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX
   ```
   Without them the build uses Google's test ids and prints a warning (test ads earn nothing).
3. In AdMob, open **Privacy & messaging** and publish a GDPR consent message for the app, so EEA/UK users see the consent form.
4. Add an `app-ads.txt` file on your developer website (the site in your Play listing), using the line AdMob gives you.
5. Rebuild: `./gradlew bundleRelease`.

## Upload steps

1. Create a Google Play developer account (one-time $25 fee, identity verification) at https://play.google.com/console.
2. Create app -> name above, language, Game, Free. Accept the declarations.
3. Fill the "App content" section above, then the store listing and upload the graphics.
4. Release -> Testing (closed or internal) -> create release -> upload `app-release.aab`. Keep **Play App Signing** on (default). The `release.jks` key is your *upload key*: back it up with its password somewhere safe.
5. New personal developer accounts are usually required to run a closed test with a minimum number of testers for a minimum period before production access. Check the Console's current requirement, then apply for production.
6. Production -> create release with the same bundle, submit for review.

## Before every update

Increase `VERSION_CODE` in `gradle.properties`, then run `./gradlew bundleRelease` and upload `app/build/outputs/bundle/release/app-release.aab`.
