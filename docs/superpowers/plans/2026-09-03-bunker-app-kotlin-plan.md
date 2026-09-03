# Chikura Cross-Platform App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a blank-by-default Kotlin cross-platform app (Android, Windows, macOS, Linux) + read-only web wiki that loads any `Chikura`-spec GitHub repo as whiteboard/kanban/list in black & white terminal style.

**Architecture:** Kotlin Multiplatform + Compose Multiplatform single codebase: `commonMain` holds parsing, domain model, hydrator cache, viewmodels; `androidMain`/`desktopMain` use file-system + isomorphic-git via KMP; `webMain` compiles to Wasm, read-only via raw.githubusercontent.com. Markdown is DB, `.chikura-cache/media.json` is sidecar.

**Tech Stack:** Kotlin 2.0+, Compose Multiplatform 1.7+, Gradle 8, Ktor client, kotlinx.serialization, SqlDelight (cache), Coil (thumbs), tldraw port / Compose Canvas for whiteboard, Git via KGit, Decompose for nav

## Global Constraints

- Cross-platform: Android, Windows, Linux, macOS + Web — one repo, neat folder `app/`
- Black & white terminal: #000/#FFF only, JetBrains Mono, 1px borders, no color except thumbs
- Valid chikuthread spec: kebab-case `*.md` + `archive-box/inbox.md` + `quarantine.md`, `#`/`##`/`###` + `- https://`, `chikuthread.json` optional
- Each chikuthread = separate GitHub repo in `~/Chikura/chikuthreads/<name>/` (not nested)
- Marketplace = curated `marketplace.json` index, not open store; direct add by URL also
- 1931→1914 deduped URLs preserved; `backups/` untracked

---

### Task 1: Scaffold KMP Project & Neat Folder Structure

**Files:**
- Create: `app/build.gradle.kts`
- Create: `app/settings.gradle.kts`
- Create: `composeApp/build.gradle.kts`
- Create: `app/gradle/libs.versions.toml`
- Create: `composeApp/src/commonMain/kotlin/com/chikura/app/App.kt`
- Create: `composeApp/src/androidMain/kotlin/com/chikura/app/Platform.android.kt`
- Create: `composeApp/src/desktopMain/kotlin/com/chikura/app/Platform.desktop.kt`
- Create: `composeApp/src/webMain/kotlin/com/chikura/app/Platform.web.kt`
- Create: `composeApp/src/commonMain/composeResources/` + `README.md`

**Interfaces:**
- Consumes: none
- Produces: `expect fun getPlatformName(): String` per target, `App()` composable rendering "Chikura — blank"

- [ ] **Step 1: Create KMP scaffold via wizard**

Run in `app/`:
```bash
# Use KMP Wizard or manual gradle init with compose multiplatform plugin
gradle init --type kotlin-application
# Add in settings.gradle.kts: include(":composeApp")
```

- [ ] **Step 2: Configure composeApp build.gradle.kts**

```kotlin
plugins { id("org.jetbrains.compose") ; kotlin("multiplatform") }
kotlin {
  androidTarget(); jvm("desktop"); wasmJs { browser() }
  sourceSets {
    commonMain.dependencies { implementation(compose.runtime); implementation(compose.foundation) }
  }
}
```

- [ ] **Step 3: Write minimal App.kt**

```kotlin
@Composable fun App() { Text("Chikura — blank", fontFamily = FontFamily.Monospace) }
```

- [ ] **Step 4: Verify builds**

Run: `./gradlew :composeApp:desktopRun` and `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`
Expected: window shows blank text, no errors

- [ ] **Step 5: Commit**

```bash
git add app/
git commit -m "feat: scaffold KMP Compose app (android/desktop/web)"
```

### Task 2: ChikuThread Spec Parser — Markdown AST Preserving Indent

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/chikura/parser/ChikuThreadParser.kt`
- Create: `composeApp/src/commonMain/kotlin/com/chikura/model/ChikuThread.kt`
- Test: `composeApp/src/commonTest/kotlin/com/chikura/parser/ChikuThreadParserTest.kt`

**Interfaces:**
- Consumes: raw `.md` strings
- Produces: `data class ChikuThread(id, name, domains: List<Domain>)`, `data class Resource(id, url, raw, domain, section, category, typeHint)`, `fun parseChikuThread(root: File): ChikuThread`, `fun parseMarkdown(text: String, domain: String): List<Resource>`

- [ ] **Step 1: Write failing test**

```kotlin
@Test fun parseMusic() {
  val text = "# Music\n## Sound\n### EQ\n- https://youtu.be/RIuqjFP2cHg?si=xxx\n"
  val res = parseMarkdown(text, "music")
  assertEquals(1, res.size); assertEquals("EQ", res[0].category); assertEquals("https://youtu.be/RIuqjFP2cHg?si=xxx", res[0].url)
}
```

- [ ] **Step 2: Run test fails**

Run: `./gradlew :composeApp:commonTest`
Expected: FAIL — function not defined

- [ ] **Step 3: Implement ChikuThreadParser preserving indent**

Port `scripts/clean_markdown.py` logic to Kotlin: regex for `SECTION_RE`, `URL_RE`, indent counting, typeHint via url contains.

- [ ] **Step 4: Pass**

Run tests again — PASS

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/chikura/parser/ composeApp/src/commonTest/
git commit -m "feat: chikuthread markdown parser preserving indent"
```

### Task 3: Domain Model & File-System Loader (Desktop/Android)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/chikura/model/ChikuThread.kt:10-30`
- Create: `composeApp/src/commonMain/kotlin/com/chikura/repo/ChikuThreadRepository.kt`
- Create: `composeApp/src/desktopMain/kotlin/com/chikura/repo/FileChikuThreadDataSource.desktop.kt`

**Interfaces:**
- Consumes: `parseMarkdown`
- Produces: `interface ChikuThreadDataSource { suspend fun listChikuThreads(): List<ChikuThreadMeta>; suspend fun loadChikuThread(id: String): ChikuThread }`, `class FileChikuThreadDataSource(root: File): ChikuThreadDataSource`

- [ ] **Step 1: Write failing test for loader**

```kotlin
@Test fun loadChikuThreadFromTestResources() { val ds = FileChikuThreadDataSource(File("src/commonTest/resources/sample-chikuthread")); assertEquals(2, ds.loadChikuThread("sample").domains.size) }
```

- [ ] **Step 2: Implement expect/actual file IO**

Use `expect class FileSystem` with desktop `java.io.File` and web `fetch`.

- [ ] **Step 3: Commit**

### Task 4: Black & White Design System + List View (Moelist-like but terminal)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/chikura/ui/theme/ChikuraTheme.kt`
- Create: `composeApp/src/commonMain/kotlin/com/chikura/ui/components/ResourceCard.kt`
- Create: `composeApp/src/commonMain/kotlin/com/chikura/ui/screens/ListScreen.kt`

**Interfaces:**
- Consumes: `ChikuThread`, `Resource`
- Produces: `ChikuraTheme { Bg #fff, Fg #000, mono }`, `ResourceCard(resource, onExpand)`, `ListScreen(chikuthread)`

- [ ] **Step 1: Write snapshot test for theme**

Assert `ChikuraTheme.colors.background == Color.White`.

- [ ] **Step 2: Build ResourceCard with thumbnail placeholder**

Black border, mono, 1px, shows `resource.raw` + `typeHint` badge.

- [ ] **Step 3: Run desktop preview**

Should show list of domains as Notion-like table.

- [ ] **Step 4: Commit**

### Task 5: Hydrator — YouTube OEmbed → Sidecar Cache (no .md bloat)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/chikura/hydrator/Hydrator.kt`
- Create: `composeApp/src/commonMain/kotlin/com/chikura/cache/MediaCache.kt`

**Interfaces:**
- Consumes: `Resource.url`
- Produces: `data class Hydrated(url, title, thumb, author)`, `suspend fun hydrate(url): Hydrated`, cached to `chikuthreads/<id>/.chikura-cache/media.json`

- [ ] **Step 1: Test with noembed**

```kotlin
@Test fun hydratesYouTube() { val h = runBlocking { hydrate("https://youtu.be/RIuqjFP2cHg") }; assertTrue(h.title.isNotEmpty()) }
```

- [ ] **Step 2: Implement Ktor fetch to https://noembed.com/embed?url=**

Cache via SqlDelight + file.

- [ ] **Step 3: Wire ResourceCard to show thumb/title after hydrate**

### Task 6: Kanban Board (columns = ## Section) + Whiteboard Nodes

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/chikura/ui/screens/KanbanScreen.kt`
- Create: `composeApp/src/commonMain/kotlin/com/chikura/ui/screens/WhiteboardScreen.kt`
- Create: `composeApp/src/commonMain/kotlin/com/chikura/ui/board/BoardViewModel.kt`

**Interfaces:**
- Consumes: `ChikuThread`, `Hydrated`
- Produces: `KanbanScreen` drag `Resource` between `Section`, `WhiteboardScreen` nodes with edges, `BoardViewModel.move(resourceId, toSection)`

- [ ] **Step 1: Kanban test drag**

```kotlin
@Test fun moveResource() { vm.move("music-0001", "Music Theory"); assertEquals("Music Theory", repo.find("music-0001")!!.section) }
```

- [ ] **Step 2: Implement Kanban via drag-and-drop (compose foundation)**

- [ ] **Step 3: Whiteboard via Compose Canvas (nodes as boxes, edges as lines)**

- [ ] **Step 4: Bidirectional write-back to .md** — `BoardViewModel.move` rewrites file preserving indent (reuse parser writer)

### Task 7: Git Marketplace & Add-by-URL + Web Wiki Read-Only Build

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/chikura/marketplace/Marketplace.kt`
- Create: `composeApp/src/commonMain/kotlin/com/chikura/marketplace/AddChikuThreadDialog.kt`
- Modify: `composeApp/src/webMain/kotlin/com/chikura/app/Platform.web.kt`

**Interfaces:**
- Consumes: `ChikuThreadDataSource`
- Produces: `fun fetchMarketplace(): List<ChikuThreadMeta>`, `fun addByUrl(url: String)`, `READ_ONLY` flag for web

- [ ] **Step 1: Marketplace JSON fetch test**

```kotlin
@Test fun parsesMarketplace() { val list = Json.decodeFromString<Marketplace>(json); assertEquals(1, list.size) }
```

- [ ] **Step 2: Implement Add dialog → git clone via KGit (desktop) / fetch raw (web)**

- [ ] **Step 3: Web build with READ_ONLY true, no Monaco, no drag, show “Edit in App”**

- [ ] **Step 4: Commit final**

```bash
git add app/
git commit -m "feat: marketplace + web wiki read-only"
```
