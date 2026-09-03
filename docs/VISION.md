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
| **ChikuThread** | A curated megathread **repo** written by an author. One thread = one GitHub repo, owned and managed by its author. |
| **Author + their ChikuThread repo** | Discovery is keyed to authors: "who made this thread", "their repo". Authors maintain their own thread; they are not copies merged into one folder. |

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

- This repo is the **reference ChikuThread** (the author's main thread) containing ~25 clean kebab-case domain files (~1,914 URLs after within-file dedupe), the `archive-box/` inbox workflow, backup archives, and a cleanup script.
- Exploratory prototype code currently lives under `app/` from earlier scaffolding sessions. It is **not** the final structure: the long-term intent is for the **software to be its own root-level project** (not nested inside a thread repo) and for **each ChikuThread to be an independent author-owned repo**. Splitting software from thread content is part of the roadmap, not yet done.
- Historical scratch docs remain under `docs/superpowers/` — treat them as archived session work, not current design truth.

## 9. Related documents

- `docs/CHIKUTHREAD-SPEC.md` — the format contract ChikuThread authors follow
- `docs/taxonomy-blueprint.md` — content taxonomy for the reference thread
- `README.md` / `CONTRIBUTING.md` — thread content workflow and rules
