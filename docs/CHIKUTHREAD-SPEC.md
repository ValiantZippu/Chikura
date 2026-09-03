# ChikuThread Spec — the format contract

> A **ChikuThread** is a GitHub repo of curated markdown that Chikura (知蔵) can load, browse, and edit.
> If a repo follows this small contract, it renders everywhere — app, web wiki, any author's copy.
> Keep it light: the spec exists so tools stay simple, not to burden authors.

## 1. Layout

```
my-chikuthread/                 ← one thread = one GitHub repo (author-owned)
├── LICENSE                     ← CC BY-SA 4.0 by default (deeds in repo)
├── README.md                   ← what this thread is, who curates it
├── thread.json                 ← optional, recommended: metadata
│     { "name": "...", "version": "1.0", "description": "...", "author": "..." }
├── music.md                    ← one kebab-case domain file per topic
├── games.md
├── japan.md
├── …                           ← as many domains as the author curates
├── archive-box/
│   ├── inbox.md                ← required (may be empty) — bulk intake
│   └── quarantine.md           ← required (may be empty) — unsure/dead/dup
└── docs/                       ← optional (e.g. a taxonomy blueprint)
```

Rules that matter:

- **One thread = one repo.** Threads are never nested inside other thread repos, and no content is copied into a marketplace — the index only points at the author's repo.
- `archive-box/inbox.md` and `archive-box/quarantine.md` **must exist** (can be empty) — they power the intake workflow.
- Domain files are **kebab-case `*.md`** at the repo root, one per topic. No deep folder spaghetti.

## 2. Markdown grammar

Every domain file follows a single shape (this is the "indent as code" the UI reads):

```md
# Music                    ← domain title (one per file)

## Music Theory            ← Section — Kanban column, top-level group

### Scales & Modes         ← Category — grouping inside a section

- https://youtu.be/xxxx    ← one resource per bullet, raw URL kept verbatim
- https://example.com — short note after the em dash (optional)
- https://youtube.com/@channel
- bare-ref.example.com     ← bare refs allowed when no https exists

> Note                    ← notes live in blockquotes, never as metadata
```

- Headings are `#` / `##` / `###` (deeper sub-levels allowed for nesting).
- Each resource is one **`- ` bullet**; URLs are kept verbatim — never rewritten, shortened, or decorated.
- Notes / difficulty / "why this is good" go in `> blockquote` lines under the bullet or category.
- Blank lines between blocks are fine; the parser must round-trip a file **without reformatting** the author's hierarchy, indentation, or ordering.

## 3. Validation (what Chikura checks)

A repo is a valid thread when:

1. At least one kebab-case `*.md` domain file exists at root with an `#` title.
2. `archive-box/inbox.md` exists.
3. Bullets are parseable (`- ` + URL or bare ref).

Missing `thread.json`? Still loads — shown as "Untitled ChikuThread". Broken structure? Loads what it can and shows a warning; nothing is ever destructive.

## 4. Author workflow (how threads stay alive)

1. **Expand:** paste bulk links into `archive-box/inbox.md` — no formatting needed.
2. **Triage (in Chikura):** inbox renders as cards → drag each to the correct domain file and `##`/`###` (section created on the fly if missing) → removed from inbox.
3. **Unsure?** → `archive-box/quarantine.md` with a `> why` note. Never delete to look cleaner.
4. **Publish:** commit + push. `git diff` shows one clean line per moved link.
5. **Followers:** pull the repo or hit "Update" — the app diffs and reloads. Content updates need **no re-review** by anyone but the author.

## 5. Discovery (the index, not the store)

- **Add by URL** — anyone pastes any GitHub repo URL; it is validated against §3 and cloned locally. Personal, instant, no approval.
- **Marketplace listing** (optional, curated) — an author who wants discoverability opens a PR adding their entry to the curated index:

```json
{
  "id": "japan-thread",
  "repo": "author/japan-thread",
  "name": "Japan ChikuThread",
  "author": "author",
  "description": "…"
}
```

- The index is a **reading list**, not a folder merge. Data stays in the author's repo, updates flow by `git pull`, and broken threads degrade gracefully (warning, still browseable).
- Delisting is possible if a thread stops following the spec; the author's repo itself is never touched by the marketplace.

## 6. Preservation rules (shared with the reference thread)

- Curation over collection — no link farms or SEO dumps.
- Raw URLs preserved verbatim; never "fix" a link into a tracking mess.
- Exact duplicates within one file: first kept, removal logged. **Cross-file duplicates are kept as related** — not deduped.
- No invented authors/tags/difficulty; add context only as justified `> notes`.
- Everything is reversible (backups + clean script on the reference thread); data loss is unacceptable.
