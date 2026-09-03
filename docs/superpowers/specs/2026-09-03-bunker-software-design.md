# KnowledgeBunker Software + Web Wiki — Design

> Status: Draft for review. No implementation until approved. This spec is the single source for the cross-platform “blank app + loadable megathread bunkers” vision.

## 1. Summary

KnowledgeBunker is a **blank-by-default, cross-platform app** (Windows, macOS, Linux, Android) plus a **read-only web wiki** with the same black & white terminal look. It loads any GitHub repo that follows the KnowledgeBunker bunker spec (kebab-case Markdown + archive-box). Each bunker is a curated megathread — a moelist-type catalog but modern, monospace, high-contrast, not colorful. The app solves the current pain: Markdown is a perfect database (git, forever) but unusable for 2000 YouTube cards (no previews, no drag), while Discord gave previews but was tedious to keep updated and unmigratable.

**Tagline:** Software + megathread collection you can load.

## 2. Goals / Non-Goals

**Goals:**
- Make expanding not tedious: paste in inbox → drag to domain → commit → push → pull for everyone (10s).
- Own UI for everything: inline YouTube thumbs/titles/player without opening youtube.com, same for websites, all black & white.
- Note-taking + code-editor feel (Ctrl+], indent, command palette) because resources are indented hierarchy, not code.
- Any bunker that follows the spec loads; curated marketplace stays tiny, not huge.

**Non-Goals (v1):**
- No user accounts, no in-bunker comments (use GitHub issues), no hosting videos, no search beyond Fuse.js, no open store.

## 3. Architecture — Where Bunkers Live

```
Installed app (blank on first launch)
 └─ ~/KnowledgeBunker/
     ├─ bunkers/
     │   ├─ knowledgebunker/      # git clone ValiantZippu/KnowledgeBunker (default)
     │   └─ japan-bunker/         # git clone user/repo (added via URL)
     ├─ marketplace-index.json    # curated list (cached)
     └─ .bunker-cache/
         └─ <bunker>/
             └─ media.json        # fetched thumbs/titles, gitignored
```

- Each bunker = **separate GitHub repo**, not subfolder inside KnowledgeBunker. Clone is the “download template” action.
- App blank by default; user picks local root on launch. “Add Bunker” pre-fills `ValiantZippu/KnowledgeBunker`.
- GitHub is backend; app uses `isomorphic-git` (no Git install needed).

## 4. Two Ways to Add, How Updates Work

**Way 1 — Direct add (no approval, private):** Paste any GitHub URL → validate → `git clone` → appears in sidebar. Owner manages repo on GitHub; app “Pull” does `git pull`. Edit inside app does `git commit` + `push` if token/owner.

**Way 2 — Formal marketplace listing (curated discoverability):** To be discoverable, author opens PR to `marketplace.json` in main repo:
```json
[{ "id": "japan-bunker", "repo": "user/japan-bunker", "name": "Japan Bunker", "author": "user", "description": "..." }]
```
Approver (main user, private at core) merges → appears in Marketplace browser as “Install”. Data stays in author’s repo; marketplace is index only. Updates still via author’s repo; app warns if spec invalid.

## 5. Valid Bunker Spec (what makers follow)

```
my-bunker/
  LICENSE
  README.md
  bunker.json?  { name, version, description, author }
  *.md          # kebab-case domains: music.md, games.md, japan.md
  archive-box/
    inbox.md      # required (can be empty)
    quarantine.md # required (can be empty)
  docs/taxonomy-blueprint.md?
```

Inside `*.md`:
```md
# Title
## Section          # from old 'Quoted Section'
### Category
#### Subcategory?
- https://... — optional note
- bare.example.com
> Note
```
Validator: at least one `*.md` with `#`, `archive-box/inbox.md` exists, bullets parseable. Missing `bunker.json` → “Untitled Bunker”.

## 6. App vs Web Wiki — Same UI, Different Mode

- **Shared:** Black & white terminal tokens: `--bg #fff, --fg #000, --border #000, --mono JetBrains Mono`, 1px borders, no color except thumbs, `Ctrl+K` palette.
- **App (Tauri, editable):** Split pane: Left Monaco editor on real `.md` (Ctrl+], vim, diff) + Right Canvas (List / Kanban columns=`##` / Whiteboard nodes+edges via tldraw/dnd-kit). Bidirectional: drag card right → moves bullet left. Video expand: LiteYouTube thumb → iframe + title/channel/description/comments, cached to `.bunker-cache`. Git commit button.
- **Web Wiki (read-only):** Same React build with `READ_ONLY=true`. Reads via `raw.githubusercontent.com` / GitHub API, no FS. Same three views locked (no drag, no Monaco). “Edit in App” deep link. Fetches live, no cache.

## 7. Data Model & Hydration (preserving .md as DB)

- **Parse:** Custom AST preserving indent/whitespace (not `marked`). Round-trip without reformatting hierarchy.
- **Hydrate:** For each URL, fetch `https://noembed.com/embed?url=` (or YouTube Data API) → `{title, thumbnail_url, author_name}`. Save to `.bunker-cache/media.json` (gitignored) + in-memory SQLite. Web fetches live. `.md` stays `- https://youtu.be/xxxx`.
- **Types:** Polymorphic nodes: video, channel, playlist, shorts, live, website, bare-ref, note. Type hint inferred.
- **Inbox triage:** `archive-box/inbox.md` shown as Inbox Kanban (one column). Drag → choose domain → append to correct `##`/`###` (create if missing), remove from inbox.

## 8. Cross-Platform

- **Desktop:** Tauri v2 (Rust + WebView) for Win/Mac/Linux, single pnpm React codebase.
- **Mobile:** Same web build wrapped via Tauri Android / Capacitor.
- **Offline:** Desktop caches media after first fetch; PWA offline not v1.

## 9. Expanding Workflow (why not tedious)

1. Paste bulk in `archive-box/inbox.md` (Discord-like speed).
2. Open app → see rich cards.
3. Drag to domain → 1-line diff.
4. `git commit` + `push` → everyone `git pull` gets update. 10s vs 10m forum edit.

`archive-box/art-flat-dump.md` (924 flat art links) is first bulk to chip into `2d-animation.md`, etc per blueprint.

## 10. Testing & Validation

- `scripts/clean_markdown.py` re-normalizes and dedupes within-file (1914 URLs expected).
- Validator runs on add: warns if missing `archive-box/` or invalid bullets.
- Manual QA: load knowledgebunker, add second bunker via URL, drag inbox card, expand video, pull update.

## 11. Open Questions (resolved for v1)

- Marketplace stays curated JSON in main repo, not open DB.
- Cross-file dupes kept as related, not deduped.
- No user auth v1.

## 12. Roadmap — A → B

- **A (viewer):** Loader + List/Kanban/Whiteboard read-only + black/white + YouTube hydrator.
- **B (editor):** Add split Monaco + bidirectional drag + git push.

---

*This design preserves curation, keeps .md forever, and makes the megathread live-updatable without Discord hell.*
