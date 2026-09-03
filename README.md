# KnowledgeBunker

> A massive curated knowledge archive — not a scrape. Continuation of **Artist Hub**. Preserve curation.

## What it is

- Carefully selected high-value resources for artists, creators, devs, researchers
- Started as large human-maintained markdown collections → now **clean, consistent markdown** (easier to handle than JSON, human + machine readable)
- Served via future website/app/AI — but browsable as-is

## Quick start — where to look

```
.                         ← clean domain files (one per topic, kebab-case)
  music.md                ← 479 links, ## Section / ### Category / - https://
  games.md                ← 166 links
  gadget-electronics.md   ← 78 links
  body-fashion.md, camera.md, japan.md, programming-cs.md ...
  2d-animation.md / 3d-animation.md / illustration.md / pirate-ship.md — placeholders
archive-box/              ← expanding workspace (THE inbox)
  inbox.md                ← dump new bulk URLs here (AI + you sort)
  art-flat-dump.md        ← 924 flat art links (waiting to be classified)
  quarantine.md           ← questionable / obsolete / duplicate (was Trash Warehouse)
  README.md               ← workflow
  processed-log.md        ← optional log
docs/
  taxonomy-blueprint.md   ← Discord blueprint: difficulty (absolute-beginner..advanced), resource types (channels/videos/books/websites/warehouse...)
backups/
  raw_archive_2026-09-03/ ← timestamped SHA256 backup of originals (recoverable)
scripts/
  clean_markdown.py       ← reproducible cleaning transform
```

## Naming — cleanly renamed

Old → New (all `kebab-case`, typos fixed):

- `Body & Fashion.md` → `body-fashion.md`
- `Book & Novels.md` → `books-novels.md`
- `CAMERA.md` → `camera.md`
- `GAMES.md` → `games.md`
- `MUSIC.md` → `music.md`
- `Gadget & Electronics.md` → `gadget-electronics.md`
- `Interview , Inspariton , ...` → `interview-inspiration.md` (typo `Inspariton` fixed)
- `Software & Game Devlopment.md` → `software-game-development.md` (typo fixed)
- `Software , OS & Websites.md` → `software-os-websites.md`
- `UI&UX.md` → `ui-ux.md`
- `Discord Server Art Organize...` → `archive-box/art-flat-dump.md`
- `Discord Server Template.md` → `docs/taxonomy-blueprint.md`
- `Trash Warehouse.md` → `archive-box/quarantine.md`
- All others lowercased.

Git history preserved via `git mv` — `git log --follow` still traces.

## File format — clean markdown

Every domain file:

```md
# Title

## Section (from 'Quoted Section' in legacy)

### Category (mid heading)

- https://example.com — optional note after —
- https://youtube.com/@channel
- bare-ref.example.com
> [ No Links Yet ] → > Note
> also LLM prompts preserved as > Note

---
> Cleaned: removed N duplicate(s) within this file
```

- Preserved: all 1931 original URLs → 1914 after **within-file** dedupe (17 removed, cross-file dupes kept as related).
- Fixed: `htt ps://` → `https://`, trailing `'`, `b` artifacts, smart quotes.
- Originals recoverable in `backups/raw_archive_2026-09-03/`.

## Archive Box — how to expand infinitely

1. Dump bulk into `archive-box/inbox.md` (just paste).
2. Ask AI: “triage inbox, dedupe vs root, suggest domain/section”.
3. Move each link to proper root file (`- https://...` under correct `##`/`###`).
4. Unsure? → `archive-box/quarantine.md`.
5. Log in `archive-box/processed-log.md` (optional).
6. Re-run `python scripts/clean_markdown.py` to re-normalize.

`archive-box/art-flat-dump.md` is your first big job — 924 art channels/videos/playlists to chip into `2d-animation.md`, `3d-animation.md`, `illustration.md` etc per blueprint.

## Taxonomy (from blueprint)

- **Difficulty:** `absolute-beginner` / `beginner` / `intermediate` / `advanced`
- **Resource types:** `channels` / `videos` / `books` / `websites` / `warehouse` (+ `playlists`, `software`, `tools` inferred)
- **Domains:** 25 clean files above + `art-flat-dump` inbox.

See `docs/taxonomy-blueprint.md` for full art/music breakdown.

## Philosophy & Preservation

> **Curation, not collection.** Find and preserve the resources worth knowing about — don’t dump the internet. SEO garbage, link farms, and unfiltered dumps are rejected.

**Migration rules (how we cleaned):**
1. Inspect before changing — understand hierarchy, not blind line-to-line conversion.
2. Preserve information — raw URL kept verbatim as `- raw`, malformed kept as-is, bare refs kept.
3. Only dedupe exact duplicates within a file (kept first, logged `> Cleaned: removed N`). Cross-file dupes kept as related.
4. No invented authors/tags/difficulty — only add notes when justified.
5. Reversible via `backups/raw_archive_2026-09-03/` + `scripts/clean_markdown.py`.

**Backup / archive philosophy:**
- `backups/raw_archive_2026-09-03/` — timestamped SHA256 manifest, filesystem backup (git is not the backup).
- `archive-box/` — live expanding inbox (quarantine for obsolete/dup/unsure). Never delete to look cleaner.
- Treat data loss as unacceptable.

## Safety & Validation

- Never delete to look cleaner — quarantine to `archive-box/quarantine.md`.
- Validate: `python scripts/clean_markdown.py` + count URLs (should stay ~1914, backup 1931 −17 intra-file dupes).
- Check `git diff` before committing.

## Branches

Original Artist Hub workflow (keep, don’t rewrite history):
- `main` — stable
- `early-release-develop` / `early-alpha-develop` / `early-beta-develop` / `testing-chamber` — promote upward, feature branches off `testing-chamber` → PR to `early-alpha-develop`. No force-push.

See `CONTRIBUTING.md` for workflow details.

## License

**CC BY-SA 4.0** — see [LICENSE](LICENSE) for intent, deeds, and attribution expectations. Individual linked resources retain their own terms. This is not legal advice.

## Contributing

- Edit root files directly (they’re clean now) or dump to `archive-box/inbox.md` for collaborative sorting.
- Keep bullets, keep hierarchy, keep raw URL intact. See [CONTRIBUTING.md](CONTRIBUTING.md).
