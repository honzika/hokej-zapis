# Hokejový zápis

Kotlin Multiplatform (Compose Multiplatform) aplikace pro zápis mini‑zápasů v hokeji:
3 třetiny s nastavitelnou délkou, časomíra, góly se střelci, barvy týmů.
Stav zápasu půjde vysílat na Google TV / Chromecast přes vlastní Cast receiver (`receiver/`).

## Struktura
- `composeApp/src/commonMain` – model, logika zápasu (`MatchController`), UI všech obrazovek, `ScoreboardSnapshot` pro TV
- `composeApp/src/androidMain` – `MainActivity`, platformní drobnosti (displej nezhasne, tlačítko Zpět)
- `composeApp/src/iosMain` – `MainViewController` pro iOS
- `iosApp/` – SwiftUI vstupní bod pro iOS (Xcode projekt se doplní po instalaci Xcode)
- `receiver/index.html` – TV scoreboard (Google Cast Web Receiver, `?demo` pro náhled v prohlížeči)

## Spuštění
```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:installDebug
```

## Cast – co zbývá (fáze 2)
1. Zaregistrovat Custom Receiver v [Google Cast SDK Developer Console](https://cast.google.com/publish) (jednorázově 5 USD), URL = hostovaný `receiver/index.html` (HTTPS, např. GitHub Pages).
2. Zaregistrovat Google TV / Chromecast jako testovací zařízení (jinak neschválená app neběží).
3. Android: `play-services-cast-framework`, implementace `ScoreboardPublisher` posílající JSON na namespace `urn:x-cast:cz.hokej.zapis`.
4. iOS: Google Cast SDK, stejná implementace.
