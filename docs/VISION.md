# Chikura (知蔵) — Vision & Goals

> Status: living document — the clean, single source for what this project is and why.
> It deliberately summarizes the real vision; it is not a dump of session logs or scratch thinking.

## 1. What Chikura is

**Chikura** (知蔵 — *chi* = knowledge/wisdom, *kura* = vault/storehouse) is a **blank-by-default, cross-platform app plus a read-only web wiki** with one shared black & white terminal look.

It loads **ChikuThread** repos — curated, author-maintained "megathreads" stored as plain Markdown on GitHub — and turns them into a usable interactive workspace:

- **List** (table-like), **Kanban** (columns = `##` sections), **Whiteboard** (nodes + edges)
- **Code-editor feel** for structured text: indentation is hierarchy, `Ctrl+]` indent, command palette, git diff — not because it is code, but because curated links are organized by indent
- **Hydrated cards**: links render with thumbnails, titles, authors, and inline play (videos, channels, websites, bare refs) without ever storing that metadata in the `.md`

**The core idea:** *Markdown is the database, git is the truth, the UI is the window.* A `- https://…` bullet stays a dumb bullet in the file forever; Chikura makes it usable.

## 2. Why this exists (the problem)

The journey that produced this vision:

1. **Discord** gave inline previews and instant play, but it was locked in, manual to keep organized, tedious to maintain, and nearly impossible to migrate out of. Bad decision — learned.
2. **Plain Markdown megathreads** fixed the storage side: git-tracked, diffable, human, survives 20 years. But ~1,900 curated links of mixed types (video vs channel vs website vs bare domain vs note/prompt) in raw `.md` is unusable as a daily workspace — no previews, no drag, no organization beyond headings.
3. So the product is: **`.md` = database, canvas/views = usable, editor = code feeling** — and expanding a thread must be fast enough that curation actually stays alive (10 seconds, not 10 minutes of forum drag-and-drop).

This is **not a YouTube clone**. Video is one node type among many. At heart it is organized note-taking over links, with an editor that behaves like a code editor because indent-structured text already *is* a tree.

## 3. Naming

| Term | Meaning |
|---|---|
| **Chikura 知蔵** | The software (knowledge vault/storehouse). |
| **ChikuThread チクスレ** | A curated megathread written by an author — the content unit Chikura loads. In this repo each thread is a folder under `ChikuThreads/<author>/<name>/`; an author may equally host a thread as its own independent repo. |
| **Author + their thread** | Discovery is keyed to authors: "who made this thread". Content is organized by author under `ChikuThreads/` and is never merged or copied across authors — the marketplace index only points at it. |

## 4. Goals

1. **Make expanding effortless.** Paste bulk links into the inbox → see rich cards → drag each to the right domain/section → commit → push → everyone who follows the thread `pull`s the update. Discord-speed intake, git-grade upkeep.
2. **Keep `.md` as the durable database.** Human-readable, git-tracked, forever-compatible. Rich metadata lives in a **sidecar cache** (`.gitignored`), never bloats the markdown, and the parser round-trips the file **without reformatting the author's hierarchy**.
3. **Editor feel for structured text.** Indentation is the organizing mechanism (that is what the author already did by hand). Editing should support `Ctrl+]`-style indent, keyboard-first flows, command palette, and clean diffs.
4. **Multiple views over one source.** List / Kanban (`##` = column) / Whiteboard, kept in sync with the same `.md`. Dragging a card moves the bullet in the file; editing the file moves the card. Bidirectional.
5. **Blank app + curated marketplace.** The app starts empty, not bloated. Threads are added either **by URL** (personal, instant) or from a **curated index** (small, not an app store). Marketplace is an index, not a copy of content.
6. **One codebase, two surfaces.** The editable app (desktop/mobile, installable) and a **read-only web wiki** — same UI, but the web surface is watch/read-only with an "open in app" path. Private at the core, public when the owner says so.
7. **Own aesthetic.** Black & white terminal: monospace, 1px borders, no color except media thumbnails. High contrast, calm, distinct from generic colorful SaaS.

## 5. Non-goals (v1)

- No user accounts / auth system
- No comments *inside* threads (GitHub issues are the discussion layer)
- No video hosting — YouTube stays YouTube, Chikura embeds
- No open app store — curated index only
- No heavy search index (local search only)
- Not a general note-taking app for arbitrary documents — it is for **curated link megathreads** with a defined structure

## 6. Guardrails / principles

- **Curation, not collection.** Reject SEO garbage, link farms, and unfiltered dumps. One good link beats ten mediocre ones.
- **Preserve everything.** Never delete to look cleaner — quarantine. Raw URLs stay verbatim. Backups are recoverable; data loss is unacceptable.
- **No invented metadata.** No guessed authors/tags/difficulty in the files — notes only when justified.
- **`.md` stays dumb, the sidecar is smart.** Never write thumbnails/titles/comments into the thread files.
- **Structure is a contract.** ChikuThreads follow the spec (see `docs/CHIKUTHREAD-SPEC.md`) so any thread renders in any surface. Spec-strict, not spec-heavy.

## 7. Product shape

| | **Editable app** | **Web wiki** |
|---|---|---|
| Platform | Desktop (Win/macOS/Linux) + mobile, installable | Any browser |
| Source of threads | Local git clones under a user-chosen root | Fetched read-only (raw GitHub / API) |
| Editing | Full: code-editor pane + drag views, git commit/push | None — view only |
| Cache | Sidecar media cache (offline after first fetch) | Live fetch, no cache |
| Job | **work** | **watch / share** |

The **inbox triage loop** is the heart of daily use:

1. Dump bulk links into `archive-box/inbox.md` (Discord-like speed, zero formatting).
2. Open Chikura → inbox shows as rich cards.
3. Drag each card to the correct domain/section (created if missing) — removes it from inbox.
4. `git commit` shows a clean 1-line-per-move diff; push publishes.
5. Followers `pull` — the marketplace index needs no content review per update.

## 8. Current state of this repository

One repo, two halves, no `app/` wrapper:

- **Software at the root** — the Kotlin Multiplatform app lives top-level: `composeApp/` (desktop JVM, web wasmJs, Android placeholder), Gradle wrapper, `settings.gradle.kts`. The read-only **web wiki viewer** lives in `web/` (Vite + React, same black & white look).
- **Thread content under `ChikuThreads/<author>/`** — this repo is also the **reference ChikuThread**: `ChikuThreads/ValiantZippu/ChikuThread 1/` holds ~25 clean kebab-case domain files (~1,914 URLs after within-file dedupe), the `archive-box/` inbox workflow, backups, and a cleanup script.
- **`marketplace.json`** at the root is the curated index — one entry per thread/author, added by PR, never a copy of content.
- Historical scratch docs remain under `docs/superpowers/` — treat them as archived session work, not current design truth.

Long-term shape stays as §3 describes: an author may publish a thread as its own repo and simply list it in the index — the loader treats `ChikuThreads/<author>/<name>/` and a standalone thread repo the same way.

## 9. Related documents

- `docs/CHIKUTHREAD-SPEC.md` — the format contract ChikuThread authors follow
- `marketplace.json` — the curated index of known threads
- `docs/taxonomy-blueprint.md` — content taxonomy for the reference thread
- `README.md` / `CONTRIBUTING.md` — repo layout, thread content workflow and rules
