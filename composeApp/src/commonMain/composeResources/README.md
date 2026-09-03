# composeResources — Chikura

Black & white terminal theme resources.

- Fonts: JetBrains Mono (monospace) — set in `App.kt` via `FontFamily.Monospace`; add `JetBrainsMono-Regular.ttf` here and reference via `Res.font.jetbrainsMono` when adding custom font loading.
- Colors: `#000000` / `#FFFFFF` only, 1px borders, no color except thumbnails (Task 4+).
- This folder is read by the Compose Multiplatform resources generator (`compose.resources`).

Build commands (from repo root):
```bash
./gradlew :composeApp:desktopRun                 # desktop window
./gradlew :composeApp:wasmJsBrowserDevelopmentRun # web (Wasm) dev server
./gradlew :composeApp:build                      # all targets (desktop + web + common)
```

Kotlin 2.0.21, Compose Multiplatform 1.7.3, Gradle 8.10.2.
