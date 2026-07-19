#!/usr/bin/env python3
"""Generate missing encoding-language test data files.

Three generation methods:

1. **utf-8-sig** (BOM prepend) -- prepend EF BB BF to existing utf-8 files
2. **utf-7** (re-encode) -- decode utf-8, re-encode as utf-7
3. **CulturaX transcoding** -- download CulturaX text, normalize, encode

Usage:
    python3 scripts/generate_test_files.py --dry-run
    python3 scripts/generate_test_files.py --encodings utf-8-sig
    python3 scripts/generate_test_files.py --encodings cp437 --dry-run
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
from datetime import date
from pathlib import Path

# Support running both as `python3 scripts/generate_test_files.py` (from repo root)
# and as `python3 -m scripts.generate_test_files`.
if __name__ == "__main__" and __package__ is None:
    sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from scripts.encoding_gaps import (  # noqa: E402
    LANGUAGE_TO_ISO,
    find_gaps,
    get_codec,
)
from scripts.substitutions import (  # noqa: E402
    apply_substitutions,
    get_substitutions,
    normalize_text,
)
from scripts.encoding_overlaps import DISTINGUISHING_BYTES  # noqa: E402

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------

UTF8_BOM = b"\xef\xbb\xbf"

# CulturaX dataset on HuggingFace
CULTURAX_DATASET = "uonlp/CulturaX"

# Target sizes for CulturaX transcoded files (in bytes of encoded output).
TARGET_SIZES = (500, 2000, 5000)

# Maximum number of CulturaX articles to download per language.
MAX_ARTICLES = 20

# Encodings that use EBCDIC (skip ASCII ratio gate).
EBCDIC_ENCODINGS = {
    "cp037", "cp424", "cp500", "cp875", "cp1026", "cp273", "cp1140",
}

# Multibyte encodings (skip ASCII ratio gate).
MULTIBYTE_ENCODINGS = {
    "big5", "euc-jp", "euc-kr", "gb2312", "gb18030",
    "cp932", "cp949", "shift_jis", "shift-jis", "johab",
}

# Escape-sequence encodings (skip ASCII ratio gate).
ESCAPE_ENCODINGS = {
    "iso-2022-jp", "iso-2022-jp-2004", "iso-2022-jp-ext",
    "iso-2022-kr", "hz-gb-2312",
}

# Flexible multibyte encodings that can represent pure ASCII.  Test files
# for these must contain actual multibyte sequences.
FLEXIBLE_MULTIBYTE_ENCODINGS = {"utf-8", "utf-8-sig"}

# Languages where text is naturally >90% ASCII (Latin script) -- skip ASCII
# ratio gate because high ASCII content is expected and realistic.
LATIN_SCRIPT_LANGUAGES = {
    "breton", "croatian", "czech", "danish", "dutch", "english", "esperanto",
    "estonian", "finnish", "french", "gaelic", "german", "hungarian",
    "icelandic", "indonesian", "irish", "italian", "latvian", "lithuanian",
    "malay", "maltese", "norwegian", "polish", "portuguese", "romanian",
    "slovak", "slovene", "spanish", "swedish", "turkish", "vietnamese",
    "welsh",
}


# ---------------------------------------------------------------------------
# CulturaX caching (follows same pattern as chardet/scripts/train.py)
# ---------------------------------------------------------------------------

_lang_text_cache: dict[str, list[str]] = {}


def _article_cache_dir(cache_dir: str, iso_lang: str) -> str:
    return os.path.join(cache_dir, "culturax", iso_lang)


def _load_cached_articles(
    cache_dir: str, iso_lang: str, max_articles: int,
) -> list[str]:
    d = _article_cache_dir(cache_dir, iso_lang)
    if not os.path.isdir(d):
        return []
    texts: list[str] = []
    for name in sorted(os.listdir(d)):
        if not name.endswith(".txt"):
            continue
        if len(texts) >= max_articles:
            break
        with open(os.path.join(d, name), encoding="utf-8") as f:
            texts.append(f.read())
    return texts


def _save_article(
    cache_dir: str, iso_lang: str, index: int, text: str,
) -> None:
    d = _article_cache_dir(cache_dir, iso_lang)
    os.makedirs(d, exist_ok=True)
    path = os.path.join(d, f"{index:06d}.txt")
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


def get_texts(
    iso_lang: str, max_articles: int, cache_dir: str,
) -> list[str]:
    """Download and cache CulturaX texts for a language."""
    if iso_lang in _lang_text_cache and len(_lang_text_cache[iso_lang]) >= max_articles:
        return _lang_text_cache[iso_lang][:max_articles]

    cached = _load_cached_articles(cache_dir, iso_lang, max_articles)
    if len(cached) >= max_articles:
        _lang_text_cache[iso_lang] = cached
        return cached[:max_articles]

    needed = max_articles - len(cached)
    start_index = len(cached)
    print(
        f"  Downloading CulturaX ({iso_lang}): "
        f"have {len(cached)}, need {needed} more...",
    )

    try:
        from datasets import load_dataset
    except ImportError:
        print(
            "  ERROR: 'datasets' library not installed. "
            "Install with: pip install datasets",
            file=sys.stderr,
        )
        _lang_text_cache[iso_lang] = cached
        return cached[:max_articles]

    try:
        ds = load_dataset(
            CULTURAX_DATASET,
            iso_lang,
            split="train",
            streaming=True,
        )
    except Exception as exc:
        print(f"  WARNING: Could not load CulturaX for '{iso_lang}': {exc}")
        _lang_text_cache[iso_lang] = cached
        return cached[:max_articles]

    new_texts: list[str] = []
    try:
        for i, example in enumerate(ds):
            if i < start_index:
                continue
            if len(new_texts) >= needed:
                break
            text = example.get("text", "")
            if text and len(text) > 100:
                _save_article(cache_dir, iso_lang, start_index + len(new_texts), text)
                new_texts.append(text)
    except Exception as exc:
        print(f"  WARNING: Error streaming CulturaX for '{iso_lang}': {exc}")

    all_texts = cached + new_texts
    _lang_text_cache[iso_lang] = all_texts
    if new_texts:
        print(
            f"  Cached {len(new_texts)} new articles for '{iso_lang}' "
            f"(total: {len(all_texts)})",
        )
    return all_texts[:max_articles]


# ---------------------------------------------------------------------------
# File selection helpers
# ---------------------------------------------------------------------------


def pick_varied_files(directory: Path, count: int = 3) -> list[Path]:
    """Pick *count* files from *directory* with varied sizes.

    Sorts by size and picks: smallest, middle, largest.
    If fewer than *count* files exist, returns all of them.
    """
    files = sorted(directory.iterdir())
    files = [f for f in files if f.is_file()]
    if not files:
        return []
    if len(files) <= count:
        return files

    # Sort by size
    files.sort(key=lambda f: f.stat().st_size)

    # Pick smallest, middle, largest
    indices = [0, len(files) // 2, len(files) - 1]
    # Deduplicate indices (in case of very few files)
    seen: set[int] = set()
    picked: list[Path] = []
    for idx in indices:
        if idx not in seen:
            seen.add(idx)
            picked.append(files[idx])
    return picked


# ---------------------------------------------------------------------------
# Quality gates for CulturaX transcoding
# ---------------------------------------------------------------------------


def _skip_ascii_gate(encoding_prefix: str, language: str) -> bool:
    """Return True if the ASCII ratio gate should be skipped.

    Skipped for EBCDIC, multibyte, escape-sequence encodings, and also
    for Latin-script languages where >95% ASCII is natural and expected.
    """
    return (
        encoding_prefix in EBCDIC_ENCODINGS
        or encoding_prefix in MULTIBYTE_ENCODINGS
        or encoding_prefix in ESCAPE_ENCODINGS
        or language in LATIN_SCRIPT_LANGUAGES
    )


def passes_quality_gates(
    encoded: bytes,
    text: str,
    codec: str,
    encoding_prefix: str,
    language: str,
) -> bool:
    """Check whether encoded bytes pass all quality gates."""
    # Too short
    if len(encoded) < 20:
        return False

    # Too many chars dropped.  Threshold is 0.3 rather than 0.5 because
    # single-byte Arabic/Urdu encodings (cp1006, cp864, cp720) have a natural
    # ratio of ~0.5 vs UTF-8 (2-byte chars → 1-byte), and any overhead drops
    # it further.  0.3 still catches real encoding failures.
    utf8_len = len(text.encode("utf-8"))
    if utf8_len > 0 and len(encoded) / utf8_len < 0.3:
        return False

    # ASCII ratio check (only for non-Latin, non-EBCDIC, non-multibyte,
    # non-escape encodings -- Latin-script text is naturally >90% ASCII).
    if not _skip_ascii_gate(encoding_prefix, language):
        ascii_count = sum(1 for b in encoded if b < 128)
        if len(encoded) > 0 and ascii_count / len(encoded) > 0.95:
            return False

    # Round-trip check
    try:
        encoded.decode(codec)
    except (UnicodeDecodeError, LookupError):
        return False

    # Distinguishing-byte check: encoded bytes must contain at least one
    # byte value that differs between this encoding and an overlapping one.
    dist_bytes = DISTINGUISHING_BYTES.get(encoding_prefix)
    if dist_bytes is not None:
        if not any(b in dist_bytes for b in encoded):
            return False

    # Escape-sequence check: escape encodings must contain actual escape
    # sequences, otherwise the file is indistinguishable from ASCII.
    if encoding_prefix in ESCAPE_ENCODINGS:
        if encoding_prefix == "hz-gb-2312":
            if b"~{" not in encoded:
                return False
        else:
            if b"\x1b" not in encoded:
                return False

    # Multibyte check: flexible multibyte encodings must contain actual
    # multibyte sequences, otherwise the file is indistinguishable from ASCII.
    if encoding_prefix in FLEXIBLE_MULTIBYTE_ENCODINGS:
        if max(encoded) < 128:
            return False

    return True


# ---------------------------------------------------------------------------
# Generation methods
# ---------------------------------------------------------------------------


def generate_utf8sig(
    gap: tuple[str, str],
    base_dir: Path,
    dry_run: bool,
    manifest: list[dict],
) -> bool:
    """Generate a utf-8-sig directory by prepending BOM to utf-8 files."""
    _enc_prefix, language = gap
    src_dir = base_dir / f"utf-8-{language}"
    dst_dir = base_dir / f"utf-8-sig-{language}"

    if not src_dir.is_dir():
        print(f"  SKIP utf-8-sig-{language}: no utf-8-{language}/ source directory")
        return False

    files = pick_varied_files(src_dir)
    if not files:
        print(f"  SKIP utf-8-sig-{language}: no files in utf-8-{language}/")
        return False

    if dry_run:
        print(f"  DRY-RUN: would create utf-8-sig-{language}/ with {len(files)} file(s)")
        for f in files:
            print(f"    {f.name} ({f.stat().st_size} bytes)")
        return True

    dst_dir.mkdir(exist_ok=True)
    for src_file in files:
        raw = src_file.read_bytes()
        dst_path = dst_dir / src_file.name
        dst_path.write_bytes(UTF8_BOM + raw)
        manifest.append({
            "path": f"utf-8-sig-{language}/{src_file.name}",
            "source": "utf-8",
            "method": "bom-prepend",
            "retrieved": str(date.today()),
            "notes": f"BOM prepended to utf-8-{language}/{src_file.name}",
        })

    print(f"  CREATED utf-8-sig-{language}/ ({len(files)} files)")
    return True


def generate_utf7(
    gap: tuple[str, str],
    base_dir: Path,
    dry_run: bool,
    manifest: list[dict],
) -> bool:
    """Generate a utf-7 directory by re-encoding utf-8 files."""
    _enc_prefix, language = gap
    src_dir = base_dir / f"utf-8-{language}"
    dst_dir = base_dir / f"utf-7-{language}"

    if not src_dir.is_dir():
        print(f"  SKIP utf-7-{language}: no utf-8-{language}/ source directory")
        return False

    files = pick_varied_files(src_dir)
    if not files:
        print(f"  SKIP utf-7-{language}: no files in utf-8-{language}/")
        return False

    if dry_run:
        print(f"  DRY-RUN: would create utf-7-{language}/ with {len(files)} file(s)")
        for f in files:
            print(f"    {f.name} ({f.stat().st_size} bytes)")
        return True

    dst_dir.mkdir(exist_ok=True)
    for src_file in files:
        raw = src_file.read_bytes()
        text = raw.decode("utf-8")
        encoded = text.encode("utf-7")
        dst_path = dst_dir / src_file.name
        dst_path.write_bytes(encoded)
        manifest.append({
            "path": f"utf-7-{language}/{src_file.name}",
            "source": "utf-8",
            "method": "transcoded",
            "retrieved": str(date.today()),
            "notes": f"Re-encoded from utf-8-{language}/{src_file.name}",
        })

    print(f"  CREATED utf-7-{language}/ ({len(files)} files)")
    return True


def generate_culturax(
    gap: tuple[str, str],
    base_dir: Path,
    cache_dir: str,
    dry_run: bool,
    manifest: list[dict],
    existing_md5s: set[str],
) -> bool:
    """Generate test files via CulturaX transcoding."""
    enc_prefix, language = gap
    codec = get_codec(enc_prefix)
    dst_dir = base_dir / f"{enc_prefix}-{language}"

    iso_lang = LANGUAGE_TO_ISO.get(language)
    if iso_lang is None:
        print(f"  SKIP {enc_prefix}-{language}: no ISO code for '{language}'")
        return False

    if dry_run:
        print(
            f"  DRY-RUN: would create {enc_prefix}-{language}/ "
            f"via CulturaX ({iso_lang})",
        )
        return True

    # Get articles
    articles = get_texts(iso_lang, MAX_ARTICLES, cache_dir)
    if not articles:
        print(f"  SKIP {enc_prefix}-{language}: no CulturaX articles for '{iso_lang}'")
        return False

    # Prepare substitutions
    subs = get_substitutions(enc_prefix, language)

    # Encode all articles and collect those that pass quality gates.
    candidates: list[tuple[str, bytes]] = []
    for raw_text in articles:
        text = normalize_text(raw_text, enc_prefix)
        text = apply_substitutions(text, subs)

        # Try the full article first.
        encoded = text.encode(codec, errors="ignore")
        if passes_quality_gates(encoded, text, codec, enc_prefix, language):
            md5 = hashlib.md5(encoded).hexdigest()
            if md5 not in existing_md5s:
                candidates.append((text, encoded))
                continue

        # Try truncated versions at each target size.
        added = False
        for target_size in TARGET_SIZES:
            if added:
                break
            for char_limit in (target_size * 2, target_size, target_size // 2):
                if char_limit <= 0:
                    continue
                trimmed = text[:char_limit]
                if not trimmed:
                    continue
                enc = trimmed.encode(codec, errors="ignore")
                if passes_quality_gates(enc, trimmed, codec, enc_prefix, language):
                    md5 = hashlib.md5(enc).hexdigest()
                    if md5 not in existing_md5s:
                        candidates.append((trimmed, enc))
                        added = True
                        break

    if not candidates:
        print(
            f"  SKIP {enc_prefix}-{language}: no articles passed quality gates "
            f"(tried {len(articles)} articles)",
        )
        return False

    # Pick up to 3 candidates with varied sizes: smallest, middle, largest.
    candidates.sort(key=lambda pair: len(pair[1]))
    if len(candidates) <= 3:
        selected = candidates
    else:
        selected = [
            candidates[0],
            candidates[len(candidates) // 2],
            candidates[-1],
        ]

    # Build final file list
    generated: list[tuple[str, bytes]] = []
    for idx, (_text, encoded) in enumerate(selected):
        fname = f"culturax_{idx:05d}.txt"
        generated.append((fname, encoded))

    dst_dir.mkdir(exist_ok=True)
    for fname, encoded in generated:
        (dst_dir / fname).write_bytes(encoded)
        existing_md5s.add(hashlib.md5(encoded).hexdigest())
        manifest.append({
            "path": f"{enc_prefix}-{language}/{fname}",
            "source": "culturax",
            "method": "transcoded",
            "retrieved": str(date.today()),
            "notes": "",
        })

    print(
        f"  CREATED {enc_prefix}-{language}/ "
        f"({len(generated)} files, sizes: "
        f"{', '.join(str(len(e)) for _, e in generated)})",
    )
    return True


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Generate missing encoding-language test data files.",
    )
    parser.add_argument(
        "--base-dir",
        default=".",
        help="Root of the test-data repo (default: .)",
    )
    parser.add_argument(
        "--encodings",
        nargs="+",
        default=None,
        help="Filter to specific encoding prefixes (default: all gaps)",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print what would be created without writing files",
    )
    parser.add_argument(
        "--cache-dir",
        default="scripts/.cache",
        help="Where to cache CulturaX downloads (default: scripts/.cache)",
    )
    args = parser.parse_args()

    base_dir = Path(args.base_dir).resolve()
    cache_dir = str(Path(args.cache_dir).resolve())

    # Build MD5 index of all existing test files for dedup guard.
    existing_md5s: set[str] = set()
    if not args.dry_run:
        for enc_dir in base_dir.iterdir():
            if not enc_dir.is_dir() or enc_dir.name.startswith((".", "scripts")):
                continue
            for f in enc_dir.iterdir():
                if f.is_file():
                    existing_md5s.add(hashlib.md5(f.read_bytes()).hexdigest())
        print(f"MD5 index: {len(existing_md5s)} existing files")

    # Find all gaps
    all_gaps = find_gaps(base_dir)
    if not all_gaps:
        print("No gaps found -- all encoding-language directories exist.")
        return

    # Filter to requested encodings
    if args.encodings:
        gaps = [(e, l) for e, l in all_gaps if e in args.encodings]
        if not gaps:
            print(f"No gaps match --encodings {args.encodings}")
            print(f"Available gap prefixes: {sorted({e for e, _ in all_gaps})}")
            return
    else:
        gaps = all_gaps

    print(f"Gaps to fill: {len(gaps)}")
    if args.dry_run:
        print("(DRY RUN -- no files will be written)\n")
    else:
        print()

    manifest: list[dict] = []

    # Load existing manifest if present
    manifest_path = base_dir / "scripts" / "manifest.json"
    if manifest_path.is_file() and not args.dry_run:
        with open(manifest_path, encoding="utf-8") as f:
            manifest = json.load(f)

    created = 0
    skipped = 0

    # Categorize gaps.  CulturaX runs first so that utf-8-{lang} dirs are
    # created before the mechanical generators (utf-8-sig, utf-7) need them.
    utf8sig_gaps = [(e, l) for e, l in gaps if e == "utf-8-sig"]
    utf7_gaps = [(e, l) for e, l in gaps if e == "utf-7"]
    other_gaps = [(e, l) for e, l in gaps if e not in ("utf-8-sig", "utf-7")]

    # Phase 1: CulturaX transcoding (creates utf-8 and other base dirs)
    if other_gaps:
        print(f"=== Phase 1: CulturaX transcoding ({len(other_gaps)} gaps) ===")
        for gap in other_gaps:
            ok = generate_culturax(
                gap, base_dir, cache_dir, args.dry_run, manifest, existing_md5s,
            )
            if ok:
                created += 1
            else:
                skipped += 1
        print()

    # Phase 2: utf-8-sig (BOM prepend from utf-8)
    if utf8sig_gaps:
        print(f"=== Phase 2: utf-8-sig BOM prepend ({len(utf8sig_gaps)} gaps) ===")
        for gap in utf8sig_gaps:
            ok = generate_utf8sig(gap, base_dir, args.dry_run, manifest)
            if ok:
                created += 1
            else:
                skipped += 1
        print()

    # Phase 3: utf-7 (re-encode from utf-8)
    if utf7_gaps:
        print(f"=== Phase 3: utf-7 re-encode ({len(utf7_gaps)} gaps) ===")
        for gap in utf7_gaps:
            ok = generate_utf7(gap, base_dir, args.dry_run, manifest)
            if ok:
                created += 1
            else:
                skipped += 1
        print()

    # Write manifest
    if not args.dry_run and manifest:
        manifest_path.parent.mkdir(exist_ok=True)
        with open(manifest_path, "w", encoding="utf-8") as f:
            json.dump(manifest, f, indent=2, ensure_ascii=False)
            f.write("\n")
        print(f"Manifest written: {manifest_path} ({len(manifest)} entries)")

    print(f"\nDone: {created} created, {skipped} skipped")


if __name__ == "__main__":
    main()
