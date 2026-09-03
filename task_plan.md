# Task Plan — Chikura Alpha Bulk Implementation (no-compile mode)

**Goal:** Bulk-implement the 7-task KMP app from `docs/superpowers/plans/2026-09-03-bunker-app-kotlin-plan.md` on `early-alpha-develop` only, skipping compile verification per user request (“yes skip and work document everything”). Document everything to disk.

**Current branch:** `early-alpha-develop` @ `c9eedca` + staged refactor (composeApp at root, ChikuThreads, jvmMain). Stash `stash@{0}` applied. `kotlin-js-store/` left untracked.

**Constraints:** No `gradlew` runs, no compile checks, bulk code only, document all decisions. Don't touch `main`/`early-beta-develop`/`early-release-develop`/`testing-chamber`.

## Phases

### Phase 1: Scaffold KMP Project & Neat Folder Structure — Status: complete (bulk, no-compile)
- Source: Plan Task 1 — `app/` -> `composeApp` migration done via stash
- Files: `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/*`, `gradlew`, `composeApp/build.gradle.kts`, `composeApp/src/commonMain/kotlin/com/chikura/app/App.kt`, `composeApp/src/androidMain/.../Platform.android.kt`, `composeApp/src/jvmMain/.../Main.kt`, `composeApp/src/wasmJsMain/...`, `composeApp/src/webMain/...`
- Theme: Black & white terminal #000/#FFF, JetBrains Mono, 1px border — in `App.kt`
- Next: No action — bulk scaffold presumed done, skipped verification (`./gradlew :composeApp:desktopRun` not run per user)

### Phase 2: ChikuThread Spec Parser — Status: complete (bulk, no-compile)
- Files: `composeApp/src/commonMain/kotlin/com/chikura/parser/ChikuThreadParser.kt`, `composeApp/src/commonMain/kotlin/com/chikura/model/ChikuThread.kt`, `composeApp/src/commonTest/kotlin/com/chikura/parser/ChikuThreadParserTest.kt`
- Logic: Regex `SECTION_RE`, `URL_RE`, indent preservation, `typeHint` via url contains, `parseMarkdown(text, domain)`, `parseChikuThread(root)`
- Test fixture: `composeApp/src/commonTest/resources/sample-chikuthread/{music,tech}.md`
- No-compile: Did not run `./gradlew :composeApp:commonTest`

### Phase 3: Domain Model & File-System Loader — Status: complete (bulk, no-compile)
- Files: `composeApp/src/commonMain/kotlin/com/chikura/repo/ChikuThreadRepository.kt`, `composeApp/src/commonMain/kotlin/com/chikura/repo/RemoteChikuThreadDataSource.kt`, `composeApp/src/jvmMain/kotlin/com/chikura/repo/FileChikuThreadDataSource.desktop.kt` (renamed from `FileBunkerDataSource`), `composeApp/src/commonMain/kotlin/com/chikura/platform/FileSystem.kt` (expect/actual)
- Interface: `ChikuThreadDataSource { listChikuThreads(), loadChikuThread(id) }`
- Desktop uses `java.io.File`, Web uses `fetch` raw.githubusercontent.com — expect/actual `FileSystem`

### Phase 4: Black & White Design System + List View — Status: complete (bulk, no-compile)
- Files: `composeApp/src/commonMain/kotlin/com/chikura/ui/theme/ChikuraTheme.kt`, `composeApp/src/commonMain/kotlin/com/chikura/ui/components/ResourceCard.kt`, `composeApp/src/commonMain/kotlin/com/chikura/ui/screens/ListScreen.kt`
- Tokens: `Bg #fff`, `Fg #000`, `Border #000`, `Mono JetBrains Mono`, 1px borders, no color except thumbs
- `ResourceCard` shows `resource.raw` + `typeHint` badge + hydrator thumb/title placeholder
- No snapshot test run

### Phase 5: Hydrator — YouTube OEmbed → Sidecar Cache — Status: complete (bulk, no-compile)
- Files: `composeApp/src/commonMain/kotlin/com/chikura/hydrator/Hydrator.kt`, `composeApp/src/commonMain/kotlin/com/chikura/cache/MediaCache.kt`
- Interface: `data class Hydrated(url,title,thumb,author)`, `suspend fun hydrate(url): Hydrated` via `https://noembed.com/embed?url=`, cached to `~/Chikura/.chikura-cache/media.json` + SqlDelight + file
- Web fetches live, no cache. `.md` stays `- https://youtu.be/xxxx`

### Phase 6: Kanban Board + Whiteboard Nodes — Status: complete (bulk, no-compile)
- Files: `composeApp/src/commonMain/kotlin/com/chikura/ui/screens/KanbanScreen.kt`, `composeApp/src/commonMain/kotlin/com/chikura/ui/screens/WhiteboardScreen.kt`, `composeApp/src/commonMain/kotlin/com/chikura/ui/board/BoardViewModel.kt`
- Kanban columns = `## Section`, Whiteboard nodes+edges via Compose Canvas (tldraw port / dnd-kit analog)
- `BoardViewModel.move(resourceId, toSection)` rewrites file preserving indent (reuse parser writer), bidirectional drag

### Phase 7: Git Marketplace & Add-by-URL + Web Wiki Read-Only — Status: complete (bulk, no-compile)
- Files: `composeApp/src/commonMain/kotlin/com/chikura/marketplace/Marketplace.kt`, `composeApp/src/commonMain/kotlin/com/chikura/marketplace/AddChikuThreadDialog.kt`, `composeApp/src/commonMain/kotlin/com/chikura/marketplace/GitClone.*` (android/desktop/wasmJs), `composeApp/src/commonMain/kotlin/com/chikura/auth/*`, `composeApp/src/webMain/kotlin/com/chikura/app/Platform.web.kt`, `marketplace.json`
- Marketplace curated JSON in main repo, direct add by URL via KGit / `isomorphic-git`, `READ_ONLY` flag for web (no Monaco, no drag, “Edit in App” deep link)

## Decisions Made
| Decision | Rationale |
|----------|-----------|
| Skip compile | User explicitly requested “yes skip” — bulk code + docs only |
| Fast-forward alpha to main `c9eedca` then apply stash | Required to get refactor (composeApp at root + ChikuThreads) onto alpha which was at `9562563` |
| `git add -A` + `git reset HEAD kotlin-js-store/yarn.lock` | Bulk staged refactor but left generated `kotlin-js-store/` untracked (should be gitignored, 2900-line yarn.lock) |
| Keep `stash@{0}` | Safety until user confirms refactor |

## Errors Encountered
| Error | Attempt | Resolution |
|-------|---------|------------|
| `git stash apply` on `9562563` → rename/delete conflicts (app/* missing) | 1 | `git reset --hard`, merged `main` first, then re-applied stash — clean |
| `git clean -fd` would remove `app/` (empty with .gradle) | 1 | Selective removal of `.gradle/build/composeApp/.kotlin` only, then `app/` emptied and removed via check |
| `timeout` not valid on Windows PowerShell | 1 | Use `.\gradlew.bat` directly; later skipped per user |

## Next Step
All 7 phases bulk-written (no-compile). Next: commit staged refactor on `early-alpha-develop` when user approves, or continue adding docs per “work document everything”. No other branches touched.
