# MarbleOS

A bilingual (French / English) 3D marble shooter by **Martin B**.
24 levels across 12 carved-stone worlds, 10 jokers, a coin shop with boosters, upgrades and idol skins.

## Play and download
- **Web**: https://fisbal.github.io/MarbleOS/
- **Android, Windows, Mac**: the [latest release](../../releases/latest) has `MarbleOS.apk`, `MarbleOS-Setup.exe` and `MarbleOS.dmg`.
  Installed apps update themselves and keep your progress. The game works fully offline.
- **✦ Ask Claude** on the home and pause screens gives help and hints. It uses your own Claude API key, kept on your device only.

## How it is built
The game is `android/app/src/main/assets/index.html`, with Three.js and the fonts bundled, so no internet is needed.
- `android/` shows it full screen in a WebView and offers new versions when they come out.
- `src-tauri/` is the Windows and Mac app (Tauri) around the same page. It installs new versions by itself.
- `scripts/check-screens.mjs` opens the game on phones, tablets, PC and Mac sizes in French and English, and fails when something runs off the screen or is too small to tap.

Every pull request runs the screen check and builds every app. Every push to `main` publishes release `v1.0.N` with all of them and the web version.
Signing keys are never in this repo. The workflow reads them from GitHub secrets:
`MARBLEOS_KEYSTORE_B64`, `MARBLEOS_KEYSTORE_PASSWORD`, `TAURI_SIGNING_PRIVATE_KEY`, `TAURI_SIGNING_PRIVATE_KEY_PASSWORD`, `TAURI_SIGNING_PUBLIC_KEY`.
