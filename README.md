# MarbleOS

A bilingual (French / English) 3D marble shooter by **Martin B**.
24 levels across 12 carved-stone worlds, 10 jokers, a coin shop with boosters, upgrades and idol skins.

## Download for Android
Get the latest **MarbleOS.apk** from the [Releases page](../../releases/latest), open it on the phone and tap **Install**
(allow *install unknown apps* for your browser if Android asks). The game works fully offline.

## How it is built
`android/` is a small Android app that shows the game in a full-screen WebView.
The game itself is `android/app/src/main/assets/index.html` with Three.js and the fonts bundled, so no internet is needed.
Every push to `main` builds a signed APK with GitHub Actions and publishes it as a new release.
