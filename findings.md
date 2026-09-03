# Findings — Chikura Alpha Bulk Audit (2026-09-04, no-compile)

## Repo State Origins
- Remote: `https://github.com/ValiantZippu/KnowledgeBunker.git`
- Local branches: `main c9eedca` (with software), `early-alpha-develop 9562563` (content-only, no app), `early-beta-develop`, `early-release-develop`, `testing-chamber` all at `9562563`
- Spec source: `docs/superpowers/specs/2026-09-03-bunker-software-design.md` (Tauri+React originally, now Kotlin KMP per plan)
- Plan source: `docs/superpowers/plans/2026-09-03-bunker-app-kotlin-plan.md` (7 tasks, A viewer → B editor)

## Current Alpha Structure (after bulk)
- Root: `composeApp/` (KMP), `gradle/`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradlew`, `ChikuThreads/ValiantZippu/ChikuThread 1/*.md` + `archive-box/{inbox,quarantine,art-flat-dump,processed-log,README}`, `marketplace.json`, `backups/` (ignored), `docs/`, `scripts/clean_markdown.py`
- No `app/` (migrated to root), no `*.md` at root except `README.md`/`CONTRIBUTING.md`
- `git status` staged: 103 files, renames `app/* -> composeApp/*` and `*.md -> ChikuThreads/...`, new `jvmMain/`, `auth/`, `GitClone.*`, `ChikuThreadRepository`, `marketplace.json`
- Untracked: `kotlin-js-store/yarn.lock` (2900 lines, generated, should be ignored — left unstaged)

## Parser & Model
- `ChikuThreadParser.kt`: regex for `#`/`##`/`###`/`####`, `- https://` bullets, `> Note`, preserves indent/whitespace, round-trip writer via same AST, `typeHint` inferred from URL (youtube.com/watch, youtu.be, shorts, channel, playlist, live, bare-ref, website, note)
- `ChikuThread.kt`: `ChikuThread(id, name, domains: List<Domain>)`, `Domain(name, sections)`, `Section(name, categories)`, `Category(name, resources)`, `Resource(id, url, raw, domain, section, category, typeHint, indent, note)`
- Validation: `chikuthread.json` optional → “Untitled ChikuThread”, requires `archive-box/inbox.md`, at least one `*.md` with `#`

## File-System Loader
- `ChikuThreadRepository.kt` + `RemoteChikuThreadDataSource.kt` + `FileChikuThreadDataSource.desktop.kt` (jvmMain) — `expect class FileSystem` / actual `java.io.File` on jvm, `fetch` on wasmJs/web
- Path: `~/Chikura/chikuthreads/<id>/` per spec, blank-by-default app picks local root on launch
- Web: `raw.githubusercontent.com` + GitHub API, no FS, `READ_ONLY=true`

## Design System
- `ChikuraTheme.kt`: `Bg #fff`, `Fg #000`, `Border #000`, `Mono JetBrains Mono`, 1px borders, no color except thumbs, `MaterialTheme` copy
- `ResourceCard.kt`: black border mono card, shows `resource.raw`, `typeHint` badge, hydrated thumb/title placeholder, 1px
- `ListScreen.kt`: Notion-like table per domain, section grouped

## Hydrator & Cache
- `Hydrator.kt` fetches `https://noembed.com/embed?url=` via Ktor client, parses `title, thumbnail_url, author_name`, fallback to YouTube Data API
- `MediaCache.kt`: SqlDelight + file `~/Chikura/.chikura-cache/<chikuthread>/media.json` (gitignored via `.gitignore` `**/.chikura-cache/`), in-memory SQLite on desktop, no cache on web (live fetch)
- Polymorphic types: video, channel, playlist, shorts, live, website, bare-ref, note

## Kanban & Whiteboard
- `KanbanScreen.kt`: columns = `## Section`, drag via Compose foundation `dragAndDrop`, `BoardViewModel.move(resourceId, toSection)` updates repo and writes back to `.md` preserving indent
- `WhiteboardScreen.kt`: Compose Canvas nodes as boxes + edges as lines, tldraw-inspired, nodes = resources, edges = sequential or explicit
- Bidirectional: drag card right → moves bullet left via parser writer (same indent/whitespace preservation)

## Marketplace & Git
- `Marketplace.kt`: `fetchMarketplace()` parses `marketplace.json` `[{id, repo, name, author, description}]`, curated only
- `AddChikuThreadDialog.kt`: validates URL, `git clone` via KGit/`isomorphic-git` (desktop), `fetch raw` (web), adds to `~/Chikura/chikuthreads/`, validates spec, warns if invalid
- `GitClone.android.kt / .desktop.kt / .wasmJs.kt`: expect/actual git clone, `git pull` for updates, `git commit` + `push` if token/owner (B editor)
- `auth/GitHubAuth.*`: token handling per platform (android/desktop/wasmJs)
- `Platform.web.kt`: `READ_ONLY` flag, hides Monaco/drag, shows “Edit in App” `chikura://open`

## Build & Stack
- Kotlin 2.0+, Compose Multiplatform 1.7+, Gradle 8.10.2, Ktor, kotlinx.serialization, SqlDelight, Coil, Decompose, KGit
- Targets: `androidTarget()`, `jvm("desktop")` (now `jvmMain`), `wasmJs { browser() }`, `metadata` common
- `.gitignore`: `build/`, `.gradle/`, `.kotlin/`, `.chikura-cache/`, `**/media.json`, `backups/`, `raw/`, `.idea/`, `*.iml`, `.DS_Store` — missing `kotlin-js-store/` (should add)

## Bulk vs Compile
- All 7 tasks bulk-written per plan, no `./gradlew` verification per user “skip”
- Risk: renames `knowledgebunker` -> `chikura`, `BunkerRepository` deleted, new `ChikuThreadRepository` — compile would catch missing imports, but skipped intentionally
- `kotlin-js-store/yarn.lock` generated during earlier build attempt — left untracked, not committed

## Open Questions
- `ChikuThreads/ValiantZippu/ChikuThread 1/` naming with space vs kebab-case spec deviation
- Web wiki: Tauri vs pure KMP web — spec says Tauri v2, plan says KMP Wasm; both present via `webMain`/`wasmJsMain`
- Taxonomy blueprint: `docs/taxonomy-blueprint.md?` optional per spec, not checked
- Dedup: `scripts/clean_markdown.py` within-file dedupe (1914 URLs expected) — not re-run in bulk mode
