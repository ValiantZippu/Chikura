#!/usr/bin/env python3
"""
Clean KnowledgeBunker markdown in-place, preservation-first.
- Normalizes headers to # / ## / ###
- Converts indented URLs to bullet lists
- Fixes malformed urls (htt ps:// -> https://)
- Deduplicates within file (keeps first, logs duplicates)
- Preserves all notes, bare refs, prompts
- Removes === separators, uses markdown HR
"""

import pathlib, re, collections

ROOT = pathlib.Path(__file__).parent.parent

FILE_TITLE_FIX = {
    # clean names (current)
    "2d-animation.md": "2D Animation",
    "3d-animation.md": "3D Animation",
    "body-fashion.md": "Body & Fashion",
    "books-novels.md": "Books & Novels",
    "camera.md": "Camera",
    "editing.md": "Editing",
    "emulation.md": "Emulation",
    "furniture.md": "Furniture",
    "gadget-electronics.md": "Gadget & Electronics",
    "games.md": "Games",
    "illustration.md": "Illustration",
    "interview-inspiration.md": "Interview, Inspiration, Podcast, Advice & Life Stories",
    "japan.md": "Japan",
    "knowledge-skills.md": "Knowledge & Skills",
    "moving-guide.md": "Moving Guide",
    "music.md": "Music",
    "organize.md": "Organize — House & Apartment",
    "otaku.md": "Otaku",
    "pirate-ship.md": "Pirate Ship",
    "privacy.md": "Privacy",
    "programming-cs.md": "Programming & Computer Science",
    "software-game-development.md": "Software & Game Development",
    "software-os-websites.md": "Software, OS & Websites",
    "technology.md": "Technology",
    "ui-ux.md": "UI & UX",
    # legacy names (for backwards compat if script run on old checkout)
    "2D Animation.md": "2D Animation",
    "3D Animation.md": "3D Animation",
    "Body & Fashion.md": "Body & Fashion",
    "Book & Novels.md": "Books & Novels",
    "CAMERA.md": "Camera",
    "GAMES.md": "Games",
    "MUSIC.md": "Music",
    "UI&UX.md": "UI & UX",
    "Software & Game Devlopment.md": "Software & Game Development",
    "Software , OS & Websites.md": "Software, OS & Websites",
}

SECTION_RE = re.compile(r"^\s*'.*'\s*$")
SEPARATOR_RE = re.compile(r"^\s*={3,}\s*$")

def is_section(s): return bool(SECTION_RE.match(s)) and len(s.strip())>=2
def extract_section(s):
    s=s.strip()
    if s.startswith("'") and s.endswith("'"):
        s=s[1:-1]
    return s.strip().strip("'").strip()
def is_separator(s): return bool(SEPARATOR_RE.match(s)) or s.strip()=="b"

def process_file(fp: pathlib.Path):
    title = FILE_TITLE_FIX.get(fp.name, fp.stem)
    text = fp.read_text(encoding="utf-8", errors="ignore")
    lines = text.splitlines()
    # Parse into structure: sections -> categories -> items
    # Reuse logic similar to migrate.py but output markdown
    out_lines = []
    out_lines.append(f"# {title}")
    out_lines.append("")
    if fp.name == "Discord Server Template.md":
        # This is taxonomy blueprint, keep as is but clean slightly
        out_lines.append("> Taxonomy blueprint from Discord — defines difficulty levels and resource types. Not a resource collection.")
        out_lines.append("")
        # Just preserve original content cleaned
        for l in lines:
            s=l.strip()
            if s=="" or is_separator(s): continue
            if s.startswith("---"): continue
            out_lines.append(l.rstrip())
        return "\n".join(out_lines).strip()+"\n"

    current_section = None
    current_category = None
    current_subcategory = None
    pending_items = []  # list of (section, cat, subcat, raw, line_no)
    seen_urls = set()
    dup_log = []
    # Also track structure for output grouping
    # We'll collect grouped dict
    grouped = collections.OrderedDict()  # key: (section, category, subcategory) -> list of raws

    def key():
        return (current_section, current_category, current_subcategory)

    for i, line in enumerate(lines, start=1):
        s = line.strip()
        if s=="":
            continue
        if is_separator(line):
            continue
        if is_section(s):
            current_section = extract_section(s)
            current_category = None
            current_subcategory = None
            # ensure key exists even if empty
            continue
        # Detect resource vs heading
        has_url = "https://" in s or "http://" in s or "htt ps://" in s
        # Bare ref detection
        is_bare = False
        if not has_url and " " not in s and re.match(r"^(?:www\.)?[\w.-]+\.[a-z]{2,}(?:/.*)?$", s, re.I):
            is_bare=True
        if re.match(r"^[\w.-]+\.exe$", s, re.I):
            is_bare=True
        if re.match(r"^[\w.-]+\.[a-z]{2,}:\d+", s):
            is_bare=True

        leading = len(line) - len(line.lstrip(" "))
        indented = leading >= 4

        # If URL/bare -> item
        if has_url or is_bare:
            raw = s
            # fix malformed
            raw_fixed = raw.replace("htt ps://", "https://").replace("htt p://", "http://")
            # clean trailing stray ' from GAMES.md
            # but preserve original? fix for usability
            if raw_fixed.endswith("'") and "https://" in raw_fixed:
                raw_fixed = raw_fixed.rstrip("'")
            # dedup within file (normalize url for check)
            m = re.search(r"https?://[^\s'\"<>]+", raw_fixed)
            norm = m.group(0).rstrip("',).]") if m else raw_fixed if is_bare else None
            if norm and norm in seen_urls:
                dup_log.append((i, norm))
                continue
            if norm:
                seen_urls.add(norm)
            raw = raw_fixed
            # Skip pure annotation like "[ Emulator List Reddit ] https..." -> keep as is, it's already with url
            k = key()
            if k not in grouped:
                grouped[k]=[]
            grouped[k].append(raw)
            continue

        # Not URL/bare -> heading or note
        # Check deeply indented prompt notes (Body & Fashion)
        if indented and current_category is not None and leading >=8 and (len(s)>40 or "?" in s or s.startswith("Now do")):
            # treat as note item
            k = key()
            if k not in grouped:
                grouped[k]=[]
            # prefix as note:
            grouped[k].append(f"> Note: {s}")
            continue
        # Pure note markers like "[ No Links Yet ]" etc - keep as note under heading but not as heading
        if s.startswith("[") and s.endswith("]") and "https" not in s:
            k = key()
            if k not in grouped:
                grouped[k]=[]
            grouped[k].append(f"> {s}")
            continue
        if s=="b" and len(lines)<20:
            continue
        # Otherwise heading
        if leading >=8:
            if current_category is None:
                current_category=s
                current_subcategory=None
            else:
                current_subcategory=s
        elif leading >=4 or current_category is None:
            current_category=s
            current_subcategory=None
        else:
            current_category=s
            current_subcategory=None
        # ensure group key exists
        k = key()
        if k not in grouped:
            grouped[k]=[]

    # Now emit grouped preserving order of discovery (OrderedDict already)
    # But need to emit in order sections/categories appeared.
    # Iterate grouped in insertion order, emitting headers when section/cat changes
    last_sec=None
    last_cat=None
    last_sub=None
    for (sec, cat, sub), items in grouped.items():
        # Emit section header if changed and not None
        if sec != last_sec and sec is not None:
            out_lines.append(f"## {sec}")
            out_lines.append("")
            last_sec=sec
            last_cat=None
            last_sub=None
        # If sec is None and we have never emitted a section, we are in flat file
        if sec is None and last_sec is None and cat is not None:
            # flat file will emit categories as ## directly
            pass
        if cat != last_cat and cat is not None:
            # Decide header level
            if sec is not None:
                out_lines.append(f"### {cat}")
            else:
                # flat dump with no sections: cat is actually not expected, but treat as ## if sec is None
                # Most flat dump has no cats at all, so cats will be None
                out_lines.append(f"## {cat}")
            out_lines.append("")
            last_cat=cat
            last_sub=None
        if sub != last_sub and sub is not None:
            out_lines.append(f"#### {sub}")
            out_lines.append("")
            last_sub=sub
        # Emit items
        for it in items:
            if it.startswith(">"):
                out_lines.append(it)
            else:
                # if it is bare ref without scheme, keep as is but bullet
                out_lines.append(f"- {it}")
        if items:
            out_lines.append("")

    # If file had no grouped items (empty placeholder)
    if not grouped or all(len(v)==0 for v in grouped.values()):
        # check if original had zero lines
        if not any(v for v in grouped.values()):
            out_lines.append("> Empty placeholder — reserved for future curation. No links yet.")
            out_lines.append("")
            # Preserve section placeholder if existed
            if current_section is None and title not in ["2D Animation","3D Animation","Pirate Ship","Trash Warehouse — Archive / Quarantine"]:
                pass

    # Deduplication note
    if dup_log:
        out_lines.append("---")
        out_lines.append(f"> Cleaned: removed {len(dup_log)} duplicate URL(s) within this file (kept first occurrence).")
        out_lines.append("")

    # For flat dump 924 urls, grouped will have single key (None,None,None) with 900+ items
    # Ensure we add header note
    if fp.name.startswith("Discord Server Art Organize"):
        # Insert note after title
        out_lines.insert(2, "> Flat dump of ~900 art/animation resources — deduplicated, bullet-listed. Future work: classify per taxonomy blueprint.")
        out_lines.insert(3, "")

    cleaned = "\n".join(out_lines)
    # Fix multiple blank lines
    cleaned = re.sub(r"\n{3,}", "\n\n", cleaned)
    return cleaned.strip()+"\n"

def main():
    mds = list(ROOT.glob("*.md"))
    # Exclude backups/raw if any at root? Only *.md at root is intended
    for fp in sorted(mds):
        if fp.parent != ROOT: continue
        if fp.name.startswith("README") or fp.name.startswith("LICENSE"): continue
        cleaned = process_file(fp)
        # Backup original already in backups/raw_archive_*/ so overwrite
        fp.write_text(cleaned, encoding="utf-8")
        orig_lines = len(cleaned.splitlines())
        print(f"cleaned {fp.name:60} -> {orig_lines:4} lines")

if __name__=="__main__":
    main()
