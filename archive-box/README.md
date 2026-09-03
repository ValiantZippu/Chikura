# Archive Box — Bulk Intake & Sorting Bay

This is the **expanding workspace** for KnowledgeBunker. Dump raw URLs here, then collaboratively sort them into the clean domain files at the root.

## Why it exists

- Root files (`music.md`, `games.md`, `gadget-electronics.md`, etc) are **curated, clean, deduped** — one resource, one place, with `## Section` / `### Category` / `- https://` structure.
- New finds, bulk imports, unsorted dumps, or questionable links **do not go straight to root**. They go here first. This preserves curation and makes progress traceable.

> Think of it as an **inbox → sorting table → archive** conveyor belt.

## Structure

```
archive-box/
  README.md              ← you are here (workflow)
  inbox.md               ← dump ANY bulk URL here (one per line or bullet)
  art-flat-dump.md       ← 924 art/animation links from Discord bulk import (needs classification)
  quarantine.md          ← Trash Warehouse — questionable, obsolete, duplicated, awaiting review
  processed-log.md       ← optional log of what was moved where (if you want traceability)
```

## Workflow — full expanding way

1. **Dump** — Paste new links into `inbox.md` (no need to format, just dump). AI can dump too.
   ```md
   https://youtu.be/xxxx
   https://youtube.com/@somechannel
   https://example.com/article
   // or with notes
   https://youtu.be/xxxx — great for perspective
   ```

2. **Triage with AI** — Run or ask the assistant to:
   - dedupe against root + archive-box
   - guess domain/section (e.g. `music` → `## Music Theory`, `gadget-electronics` → `### Gamepads`)
   - flag bare refs / malformed / duplicates

3. **Sort** — Move each link from `inbox.md` into its proper domain file at root (e.g. `- https://youtu.be/xxxx` under `music.md` → `## Music Itself` → `### Music Theory`). Keep bullets, add a short note if needed.

4. **Quarantine if unsure** — If a link is dead, duplicate, low-value, or you’re not sure where it belongs, move it to `quarantine.md` with a note (`> why quarantined`). Don’t delete.

5. **Log (optional)** — Note in `processed-log.md` what you moved, so history is reversible.

6. **Empty inbox habit** — Aim to keep `inbox.md` empty or with only today’s finds. `art-flat-dump.md` is the big existing bulk — chip away at it file by file.

## Rules (same as root)

- **Never delete to look cleaner.** Move to `quarantine.md`.
- **Preserve raw** — keep original URL, add note after `—` if you add context.
- **Deduplicate** — AI will check `backups/raw_archive_2026-09-03/` + all root files. If duplicate, note `> duplicate of music.md:42`.
- **Fix malformed** — `htt ps://` → `https://` but leave a comment if you fix.

## AI helper script

`scripts/clean_markdown.py` is the reproducible transform that produced the current clean root files. Re-run it after sorting if you want to re-normalize formatting:

```bash
python scripts/clean_markdown.py
```

It only dedupes **within each file** — cross-file dupes are flagged, not deleted, so you can see related resources.

## Current bulk to expand

- `art-flat-dump.md` — 924 art/animation channels, videos, playlists from the Discord bulk import. Flat, undifferentiated. Expand by moving ~20-50 at a time into `2d-animation.md`, `3d-animation.md`, `illustration.md`, etc per `docs/taxonomy-blueprint.md`.
- `inbox.md` — start empty, your daily dump.
- `quarantine.md` — starts empty, your “trash warehouse” but named honestly.

When a file in `archive-box/` is fully sorted, delete it or move it to `backups/` — the root is the durable archive.
