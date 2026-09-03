# Contributing to KnowledgeBunker

Curated > Quantity. One good link beats ten mediocre ones.

## Where to add

- **Know the right domain?** Edit the clean root file directly (`music.md`, `games.md`, etc) under the correct `## Section` / `### Category` with `- https://...`.
- **Not sure / bulk / unsure quality?** Paste into `archive-box/inbox.md` — no formatting needed. AI + maintainer will triage.
- **Questionable / duplicate / dead?** → `archive-box/quarantine.md` with `> why`.

## Format

```md
### Category Name

- https://example.com — optional short note after —
- https://youtube.com/@channel
- bare-domain.example — if no https available

> [ No Links Yet ] becomes > note, not heading
```

- Keep original URL intact (fix `htt ps://` → `https://` and trailing `'` is ok).
- One bullet per resource. Preserve order if curation order matters.
- Add a short note after `—` if context helps (e.g. “great for perspective”).

## Rules — preservation first

- **Never delete to look cleaner.** Quarantine it.
- **Never invent metadata** (no guessing authors, difficulty, tags). If ambiguous, keep raw and add `> note`.
- **Never silently merge** — exact duplicates within a file are removed (keeps first), cross-file duplicates are kept and noted as related.
- **Preserve** uncertain/bare refs (`freeCodeCamp.org`, `samp.hzgaming.net:7777`, LLM prompts).

## Validation

```bash
python scripts/clean_markdown.py  # re-normalizes, dedupes within-file
```

Check URL count stays ~1914 (backup has 1931, -17 intra-file dupes removed). Originals in `backups/raw_archive_2026-09-03/`.

## Branches

- `main` — stable
- `early-release-develop` / `early-alpha-develop` / `early-beta-develop` / `testing-chamber` — integration layers per original Artist Hub workflow. Make feature branches off `testing-chamber`, PR to `early-alpha-develop`, promote upward. Don't force-push history.

## Attribution

By contributing you agree your edits are shared under CC BY-SA 4.0 (see LICENSE). Individual linked resources retain their own terms.
