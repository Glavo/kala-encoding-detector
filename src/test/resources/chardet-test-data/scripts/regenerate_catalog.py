#!/usr/bin/env python3
"""Regenerate CATALOG.md from the current state of the repository."""

import codecs
import os
import re
import struct
import sys
from pathlib import Path


# Encoding group definitions: (group_name, encoding_prefixes)
# Order matters - this determines the section order in the catalog.
ENCODING_GROUPS = [
    ("Unicode", [
        "utf-16", "utf-32", "utf-7", "utf-8", "utf-8-sig",
    ]),
    ("ISO 8859", ["iso-8859-", "iso8859-"]),
    ("Windows code pages", ["windows-", "cp1250", "cp1251", "cp1252", "cp1253",
                             "cp1254", "cp1255", "cp1256", "cp1257", "cp1258"]),
    ("IBM/DOS code pages", [
        "cp037", "cp273", "cp424", "cp437", "cp500", "cp720", "cp737",
        "cp775", "cp850", "cp852", "cp855", "cp856", "cp857", "cp858",
        "cp860", "cp861", "cp862", "cp863", "cp864", "cp865", "cp866",
        "cp869", "cp874", "cp875", "cp932", "cp949", "cp950",
        "cp1006", "cp1026", "cp1125", "cp1140",
    ]),
    ("Mac encodings", ["mac", "macroman", "maclatin2"]),
    ("KOI8", ["koi8"]),
    ("HP encodings", ["hp-roman8"]),
    ("Chinese encodings", ["big5", "gb2312", "gb18030", "gbk", "hz"]),
    ("Japanese encodings", ["euc-jp", "iso-2022-jp", "shift_jis"]),
    ("Korean encodings", ["euc-kr", "johab", "iso-2022-kr"]),
    ("EUC (Extended Unix Code)", ["euc-tw", "euc-jis-2004"]),
    ("Thai encodings", ["tis-620"]),
    ("Central Asian encodings", ["kz1048", "ptcp154"]),
    ("Vietnamese encodings", ["viscii"]),
]


# Source detection: prefix -> (source_name, source_link_or_none)
SOURCE_INFO = {
    "culturax_": ("CulturaX", "https://huggingface.co/datasets/uonlp/CulturaX"),
    "_ude_": ("Ude", None),
    "_chromium_": ("Chromium", None),
    "_mozilla_": ("Mozilla", None),
    "_enca_": ("ENCA", None),
    "_uchardet_": ("uchardet", None),
    "archive_": ("Web Archive", None),
}


# Map old directory names to new ones for note migration
DIR_RENAMES = {
    "ascii": "ascii-english",
    "None": "None-None",
}


def load_existing_notes(catalog_path):
    """Load hand-curated notes from an existing CATALOG.md.

    Returns a dict of (dirname, filename) -> notes_string.
    Only returns entries with non-empty notes.
    """
    notes = {}
    if not catalog_path.exists():
        return notes

    current_dir = None
    with open(catalog_path, encoding="utf-8") as f:
        for line in f:
            line = line.rstrip()

            # Match directory headings: ## `dir/`, ### `dir/`, #### `dir/`
            m = re.match(r'^#{2,4} .*`([^/`]+)/?`', line)
            if m:
                current_dir = m.group(1)
                continue

            if not current_dir:
                continue

            # 4-column: | `file` | source | size | notes |
            m = re.match(r'^\| `([^`]+)` \| [^|]+ \| [\d,]+ \| (.*?) \|$', line)
            if m:
                fname, note = m.group(1), m.group(2).strip()
                if note:
                    # Apply directory renames
                    dname = DIR_RENAMES.get(current_dir, current_dir)
                    notes[(dname, fname)] = note
                continue

            # 3-column for ASCII section: | `file` | source | description |
            if current_dir in ("ascii", "ascii-english"):
                m = re.match(r'^\| `([^`]+)` \| [^|]+ \| (.+?) \|$', line)
                if m:
                    fname, desc = m.group(1), m.group(2).strip()
                    if desc and not re.match(r'^[\d,]+$', desc):
                        dname = DIR_RENAMES.get(current_dir, current_dir)
                        notes[(dname, fname)] = desc

    return notes


def detect_source(filename):
    """Detect the source of a file based on its name."""
    # Known charset-normalizer files (checked first to override prefix matches)
    charset_norm = {
        "book-stats.json", "books.json", "dummy-1.pem", "empty.json",
        "parchments.json", "simple.json", "iris.csv", "iris.json",
        "anzeige-value-stars.html", "useful-sentences.html",
        "sample_chinese_no_bom.txt", "github_bug_672.txt",
        "_ude_1.md", "_ude_1.rst",
    }
    if filename in charset_norm:
        return "charset-normalizer"

    for prefix, (name, _) in SOURCE_INFO.items():
        if filename.startswith(prefix):
            return name

    # Domain-name XML files
    if filename.endswith(".xml") and "." in filename[:-4]:
        return "chardet"

    # Known contributed files
    if filename.endswith((".rst", ".md")) and not filename.startswith("culturax"):
        return "charset-normalizer"

    # Contributed patterns
    contributed = ["queeup", "hlpro", "iyagi", "mdir", "hashy", "plane1"]
    for pat in contributed:
        if pat in filename.lower():
            return "Contributed"

    return "unknown"


def detect_binary_format(filepath):
    """Detect the format of a binary file based on magic bytes."""
    try:
        with open(filepath, "rb") as f:
            header = f.read(16)
    except OSError:
        return "unknown"

    if header[:3] == b"GIF":
        return "GIF image"
    if header[:2] == b"\xff\xd8":
        return "JPEG image"
    if header[4:8] == b"ftyp":
        return "MP4 video"
    if header[:8] == b"\x89PNG\r\n\x1a\n":
        return "PNG image"
    if header[:4] == b"RIFF" and header[8:12] == b"WEBP":
        return "WebP image"
    if header[:2] == b"PK":
        return "Excel spreadsheet"
    return "binary"


def get_encoding_from_dirname(dirname):
    """Extract the encoding part from a directory name like 'utf-8-english'."""
    # Try the full name first
    try:
        codecs.lookup(dirname)
        return dirname
    except LookupError:
        pass

    # Try progressively shorter prefixes (handle encoding-language splits)
    parts = dirname.split("-")
    for i in range(len(parts) - 1, 0, -1):
        candidate = "-".join(parts[:i])
        try:
            codecs.lookup(candidate)
            return candidate
        except LookupError:
            continue

    # Special cases
    if dirname.startswith("maclatin2"):
        return "mac-latin2"
    if dirname.startswith("macroman"):
        return "mac-roman"

    return None


def classify_encoding(dirname):
    """Return which encoding group a directory belongs to."""
    enc = get_encoding_from_dirname(dirname)
    lower = dirname.lower()

    for group_name, prefixes in ENCODING_GROUPS:
        for prefix in prefixes:
            if lower.startswith(prefix) or (enc and enc.lower().startswith(prefix)):
                return group_name

    return None


def get_notes(dirname, filename, size, existing_notes):
    """Generate notes for a file, preserving existing hand-curated notes."""
    # Check for existing hand-curated notes first
    existing = existing_notes.get((dirname, filename), "")
    if existing:
        return existing

    # Auto-generate basic notes for new files
    notes = []
    if size < 100:
        notes.append(f"Very small ({size} bytes)")
    return "; ".join(notes) if notes else ""


def count_sources(all_dirs):
    """Count files per source across all directories."""
    counts = {}
    for dirpath, files in all_dirs.items():
        for fname, _ in files:
            source = detect_source(fname)
            counts[source] = counts.get(source, 0) + 1
    return counts


def main():
    repo = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(".")
    repo = repo.resolve()

    # Collect all data directories and their files
    skip_dirs = {".git", "scripts", "__pycache__"}
    skip_files = {
        "CATALOG.md", "CLAUDE.md", "README.md", ".gitignore", ".gitattributes",
    }

    all_dirs = {}  # dirname -> [(filename, size), ...]
    for entry in sorted(repo.iterdir()):
        if not entry.is_dir():
            continue
        if entry.name in skip_dirs or entry.name.startswith("."):
            continue

        files = []
        for f in sorted(entry.iterdir()):
            if f.is_file() and f.name not in skip_files:
                files.append((f.name, f.stat().st_size))
        if files:
            all_dirs[entry.name] = files

    # Count totals
    total_files = sum(len(files) for files in all_dirs.values())
    total_dirs = len(all_dirs)

    # Count unique encodings
    encodings = set()
    for dirname in all_dirs:
        enc = get_encoding_from_dirname(dirname)
        if enc:
            try:
                encodings.add(codecs.lookup(enc).name)
            except LookupError:
                encodings.add(enc)
        else:
            encodings.add(dirname.split("-")[0])
    total_encodings = len(encodings)

    # Load existing notes from current CATALOG.md (if any)
    existing_notes = load_existing_notes(repo / "CATALOG.md")
    print(f"Loaded {len(existing_notes)} existing notes from CATALOG.md")

    # Count sources
    source_counts = count_sources(all_dirs)

    # Start building output
    lines = []
    lines.append("# Test Data Catalog")
    lines.append("")
    lines.append("This repository contains character encoding test data for the")
    lines.append("[chardet](https://github.com/chardet/chardet) Python library. Each")
    lines.append("subdirectory is named `{encoding}` or `{encoding}-{language}` and")
    lines.append("contains files encoded in that encoding.")
    lines.append("")
    lines.append(
        f"**{total_files} files** across **{total_dirs} directories** "
        f"covering **{total_encodings} encodings**."
    )
    lines.append("")

    # Sources section (keep the manually-written descriptions)
    lines.append("## Sources")
    lines.append("")
    lines.append("Files in this repository come from the following sources, identified by")
    lines.append("filename prefix, git history, or content:")
    lines.append("")
    lines.append("| Source | Prefix/Pattern | Files | Description |")
    lines.append("|--------|---------------|------:|-------------|")
    lines.append(
        f'| [CulturaX](https://huggingface.co/datasets/uonlp/CulturaX) | `culturax_` '
        f'| {source_counts.get("CulturaX", 0):,} '
        f'| Multilingual web text from the CulturaX dataset (built on mC4 and OSCAR '
        f'Common Crawl snapshots). Row indices are preserved in filenames (e.g., '
        f'`culturax_mC4_84511.txt`, `culturax_OSCAR-2301_58265.txt`). Many files are '
        f'transcoded copies of the same source text across multiple encodings. |'
    )
    lines.append(
        f'| [Mark Pilgrim\'s chardet](https://github.com/puzzlet/chardet/tree/MarkPilgrim/tests) '
        f'| `*.xml` (domain names) | {source_counts.get("chardet", 0):,} '
        f'| Web-scraped RSS/Atom feeds from the original chardet test suite by Mark '
        f'Pilgrim. Imported by Puzzlet Chung in 2012. Each filename is the source '
        f'website\'s domain. |'
    )
    lines.append(
        f'| [Ude](http://code.google.com/p/ude/) (Universal Detector Engine) | `_ude_` '
        f'| {source_counts.get("Ude", 0):,} '
        f'| Test files from the Ude charset detection library (a C# port of Mozilla\'s '
        f'universal charset detector). |'
    )
    lines.append(
        f'| [charset-normalizer](https://github.com/Ousret/charset_normalizer) '
        f'([char-dataset](https://github.com/Ousret/char-dataset)) | various '
        f'| ~{source_counts.get("charset-normalizer", 0)} '
        f'| Test data from the charset-normalizer test dataset by Ahmed TAHRI. Iris '
        f'CSV/JSON datasets originally from [Capital One DataProfiler]'
        f'(https://github.com/capitalone/DataProfiler). UTF-8 `.md`/`.rst` files are '
        f'READMEs from urllib3 and charset-normalizer. `anzeige-value-stars.html` from '
        f'charset-normalizer [issue #104](https://github.com/Ousret/charset_normalizer/issues/104). '
        f'ASCII JSON files (books, parchments, etc.) added to avoid false positives on '
        f'structured data. `dummy-1.pem` added after '
        f'[certbot #8964](https://github.com/certbot/certbot/issues/8964). Binary '
        f'samples ensure non-text is correctly rejected. |'
    )
    lines.append(
        f'| [ENCA](https://cihar.com/software/enca/) | `_enca_` '
        f'| {source_counts.get("ENCA", 0):,} '
        f'| Test files from the ENCA (Extremely Naive Charset Analyser) library. |'
    )
    lines.append(
        f'| [uchardet](https://www.freedesktop.org/wiki/Software/uchardet/) | `_uchardet_` '
        f'| {source_counts.get("uchardet", 0):,} '
        f'| Test files from the uchardet encoding detection library. |'
    )
    lines.append(
        f'| [Chromium](https://chromium.googlesource.com/chromium/src/) | `_chromium_` '
        f'| {source_counts.get("Chromium", 0):,} '
        f'| Test files from the Chromium browser\'s encoding detection test suite. |'
    )
    lines.append(
        f'| [Mozilla](https://hg.mozilla.org/mozilla-central/) | `_mozilla_` '
        f'| {source_counts.get("Mozilla", 0):,} '
        f'| Test files from Mozilla\'s charset detection test suite, including '
        f'regression tests for specific bugs (bug numbers in filenames). |'
    )
    lines.append(
        f'| [Web Archive](https://web.archive.org/) | `archive_` '
        f'| {source_counts.get("Web Archive", 0):,} '
        f'| Test files sourced from the Wayback Machine web archive. |'
    )
    lines.append(
        f'| Contributed | various | ~{source_counts.get("Contributed", 0) + source_counts.get("unknown", 0)} '
        f'| Community contributions: Turkish test files by queeup, CP932 tests by hashy, '
        f'Johab Korean texts (hlpro-readme, iyagi-readme, mdir-doc), UTF-16/32 plane 1 '
        f'tests by Jason Zavaglia. |'
    )
    lines.append("")

    # Binary test files section
    none_dirs = [d for d in all_dirs if d.startswith("None")]
    if none_dirs:
        none_dir = none_dirs[0]
        lines.append(f"## Binary Test Files (`{none_dir}/`)")
        lines.append("")
        lines.append("These files are used to test that the detector correctly identifies")
        lines.append("binary/non-text content and returns `None`.")
        lines.append("")
        lines.append("| File | Format | Size |")
        lines.append("|------|--------|-----:|")
        for fname, size in all_dirs[none_dir]:
            fmt = detect_binary_format(repo / none_dir / fname)
            lines.append(f"| `{fname}` | {fmt} | {size:,} |")
        lines.append("")

    # ASCII test files section
    ascii_dirs = sorted(d for d in all_dirs if d.startswith("ascii"))
    if ascii_dirs:
        lines.append("## ASCII Test Files")
        lines.append("")
        lines.append("Pure ASCII files for baseline testing.")
        lines.append("")
        for adir in ascii_dirs:
            lang = adir.split("-", 1)[1] if "-" in adir else ""
            lines.append(f"### `{adir}/` — {len(all_dirs[adir])} files")
            lines.append("")
            lines.append("| File | Source | Size | Notes |")
            lines.append("|------|--------|-----:|-------|")
            for fname, size in all_dirs[adir]:
                source = detect_source(fname)
                notes = get_notes(adir, fname, size, existing_notes)
                lines.append(f"| `{fname}` | {source} | {size:,} | {notes} |")
            lines.append("")

    # Encoding directories section
    lines.append("## Encoding Directories")
    lines.append("")
    lines.append("Each encoding directory contains files transcoded into that encoding.")
    lines.append("Many source texts appear across multiple encoding directories — the same")
    lines.append("content transcoded to test detection across encodings.")
    lines.append("")

    # Skip already-handled dirs
    handled = set(none_dirs) | set(ascii_dirs)

    # Group remaining directories
    grouped = {}
    ungrouped = []
    for dirname in sorted(all_dirs):
        if dirname in handled:
            continue
        group = classify_encoding(dirname)
        if group:
            grouped.setdefault(group, []).append(dirname)
        else:
            ungrouped.append(dirname)

    # Output each group in order
    for group_name, _ in ENCODING_GROUPS:
        if group_name not in grouped:
            continue
        dirs = grouped[group_name]
        group_files = sum(len(all_dirs[d]) for d in dirs)
        group_dirs = len(dirs)

        lines.append(f"### {group_name} ({group_files} files in {group_dirs} directories)")
        lines.append("")

        for dirname in dirs:
            files = all_dirs[dirname]
            lines.append(f"#### `{dirname}/` — {len(files)} files")
            lines.append("")
            lines.append("| File | Source | Size | Notes |")
            lines.append("|------|--------|-----:|-------|")
            for fname, size in files:
                source = detect_source(fname)
                notes = get_notes(dirname, fname, size, existing_notes)
                lines.append(f"| `{fname}` | {source} | {size:,} | {notes} |")
            lines.append("")

    # Handle ungrouped
    if ungrouped:
        ug_files = sum(len(all_dirs[d]) for d in ungrouped)
        lines.append(f"### Other encodings ({ug_files} files in {len(ungrouped)} directories)")
        lines.append("")
        for dirname in ungrouped:
            files = all_dirs[dirname]
            lines.append(f"#### `{dirname}/` — {len(files)} files")
            lines.append("")
            lines.append("| File | Source | Size | Notes |")
            lines.append("|------|--------|-----:|-------|")
            for fname, size in files:
                source = detect_source(fname)
                notes = get_notes(dirname, fname, size, existing_notes)
                lines.append(f"| `{fname}` | {source} | {size:,} | {notes} |")
            lines.append("")

    # Write output
    output = "\n".join(lines)
    outpath = repo / "CATALOG.md"
    outpath.write_text(output, encoding="utf-8")
    print(f"Wrote {outpath} ({len(lines)} lines)")
    print(f"  {total_files} files, {total_dirs} directories, {total_encodings} encodings")


if __name__ == "__main__":
    main()
