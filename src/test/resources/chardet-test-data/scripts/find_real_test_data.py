#!/usr/bin/env python3
"""Download real-world encoded text from curated sources.

Downloads test files from three vetted sources:
  1. Wayback Machine -- archived pages with confirmed legacy encodings
  2. BYVoid/uchardet -- test corpus from the uchardet project (GitHub)
  3. nijel/enca -- test corpus from the ENCA project (GitHub)

Each file is validated (round-trip decode/encode, non-ASCII content) before
saving.  Files already present are skipped.

Usage:
    python3 scripts/find_real_test_data.py --dry-run
    python3 scripts/find_real_test_data.py --encodings shift_jis euc-jp
    python3 scripts/find_real_test_data.py
"""

from __future__ import annotations

import argparse
import hashlib
import html
import re
import sys
import time
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

if __name__ == "__main__" and __package__ is None:
    sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from scripts.encoding_gaps import (  # noqa: E402
    ENCODING_LANGUAGES,
    ISO_TO_LANGUAGE,
    get_codec,
)

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------

RATE_LIMIT_SECONDS = 2.0
USER_AGENT = "chardet-test-data/1.0 (https://github.com/chardet/test-data)"

# Filename prefixes that indicate real (non-generated) test data.
# Used to check whether a directory already has non-CulturaX files.
REAL_FILE_PREFIXES = ("archive_", "_ude_", "_mozilla_", "_uchardet_", "_enca_")

# ---------------------------------------------------------------------------
# Source 1: Wayback Machine -- curated URLs with confirmed encodings
#
# Each entry: (domain, timestamp, encoding_prefix, language)
# Verified via HTTP Content-Type headers and/or HTML <meta> charset tags
# using the Wayback Machine id_ (raw) URL format.
# ---------------------------------------------------------------------------

# fmt: off
WAYBACK_SOURCES: list[tuple[str, str, str, str]] = [
    # Russian - windows-1251 (Content-Type confirmed)
    ("lenta.ru",            "20050601", "windows-1251", "russian"),
    ("www.mail.ru",         "20050601", "windows-1251", "russian"),
    ("www.rbc.ru",          "20050601", "windows-1251", "russian"),
    ("www.gazeta.ru",       "20050601", "windows-1251", "russian"),
    ("www.kommersant.ru",   "20050601", "windows-1251", "russian"),
    ("www.lib.ru",          "20050601", "windows-1251", "russian"),
    ("www.pravda.ru",       "20050601", "windows-1251", "russian"),
    ("www.aif.ru",          "20050601", "windows-1251", "russian"),
    # Russian - koi8-r (Content-Type confirmed)
    ("www.rambler.ru",      "20050601", "koi8-r",       "russian"),
    # German - iso-8859-15 (Content-Type confirmed, 2002 snapshot)
    ("www.spiegel.de",      "20020601", "iso-8859-15",  "german"),
    # Polish - iso-8859-2 (Content-Type confirmed)
    ("www.wp.pl",           "20030601", "iso-8859-2",   "polish"),
    ("www.onet.pl",         "20030601", "iso-8859-2",   "polish"),
    # French - iso-8859-1 (meta tag confirmed, pre-UTF-8 era)
    ("www.lemonde.fr",      "20020601", "iso-8859-1",   "french"),
    ("www.lefigaro.fr",     "20020601", "iso-8859-1",   "french"),
    ("www.liberation.fr",   "20020601", "iso-8859-1",   "french"),
    # Spanish - iso-8859-1 (meta tag confirmed)
    ("www.elpais.com",      "20030601", "iso-8859-1",   "spanish"),
    # Italian - iso-8859-1 (meta tag confirmed)
    ("www.repubblica.it",   "20030601", "iso-8859-1",   "italian"),
    # Japanese - shift_jis / euc-jp (meta tag confirmed)
    ("www.nhk.or.jp",       "20020601", "shift_jis",    "japanese"),
    ("goo.ne.jp",           "20020601", "euc-jp",       "japanese"),
    # Chinese - gb2312 / big5 (meta tag confirmed)
    ("www.sina.com.cn",     "20020601", "gb2312",       "chinese"),
    ("www.people.com.cn",   "20020601", "gb2312",       "chinese"),
    ("www.ltn.com.tw",      "20030601", "big5",         "chinese"),
    ("www.chinatimes.com",  "20020601", "big5",         "chinese"),
    # Korean - euc-kr (meta tag confirmed)
    ("www.chosun.com",      "20020601", "euc-kr",       "korean"),
    ("www.donga.com",       "20020601", "euc-kr",       "korean"),
]
# fmt: on

# ---------------------------------------------------------------------------
# Source 2: uchardet test files (BYVoid/uchardet on GitHub)
#
# Each entry: (iso_lang_code, uchardet_encoding_name)
# URL: https://raw.githubusercontent.com/BYVoid/uchardet/master/test/{lang}/{enc}.txt
# ---------------------------------------------------------------------------

_UCHARDET_BASE = (
    "https://raw.githubusercontent.com/BYVoid/uchardet/master/test"
)

# uchardet encoding name -> test-data directory prefix (only where they differ)
_UCHARDET_ENC_MAP: dict[str, str] = {
    "ibm855": "cp855",
    "ibm866": "cp866",
    "mac-cyrillic": "maccyrillic",
}

# fmt: off
UCHARDET_FILES: list[tuple[str, str]] = [
    # Arabic
    ("ar", "iso-8859-6"),  ("ar", "windows-1256"),
    # Bulgarian
    ("bg", "windows-1251"),
    # Danish
    ("da", "iso-8859-1"),  ("da", "iso-8859-15"),  ("da", "windows-1252"),
    # German
    ("de", "iso-8859-1"),  ("de", "windows-1252"),
    # Greek
    ("el", "iso-8859-7"),  ("el", "windows-1253"),
    # Esperanto
    ("eo", "iso-8859-3"),
    # Spanish
    ("es", "iso-8859-1"),  ("es", "iso-8859-15"),  ("es", "windows-1252"),
    # French
    ("fr", "iso-8859-1"),  ("fr", "iso-8859-15"),  ("fr", "windows-1252"),
    # Hebrew
    ("he", "iso-8859-8"),  ("he", "windows-1255"),
    # Hungarian
    ("hu", "iso-8859-2"),  ("hu", "windows-1250"),
    # Japanese
    ("ja", "euc-jp"),      ("ja", "iso-2022-jp"),  ("ja", "shift_jis"),
    # Korean
    ("ko", "euc-kr"),      ("ko", "iso-2022-kr"),
    # Russian
    ("ru", "ibm855"),      ("ru", "ibm866"),       ("ru", "iso-8859-5"),
    ("ru", "koi8-r"),      ("ru", "mac-cyrillic"), ("ru", "windows-1251"),
    # Thai
    ("th", "iso-8859-11"), ("th", "tis-620"),
    # Turkish
    ("tr", "iso-8859-3"),  ("tr", "iso-8859-9"),
    # Vietnamese
    ("vi", "windows-1258"),
    # Chinese
    ("zh", "big5"),        ("zh", "gb18030"),
]
# fmt: on

# ---------------------------------------------------------------------------
# Source 3: ENCA test files (nijel/enca on GitHub)
#
# Each entry: (iso_lang_code, enca_encoding_name)
# URL: .../test/{lang}-utf8.{enc}  or  .../test/{lang}-s.{enc}
# ---------------------------------------------------------------------------

_ENCA_BASE = "https://raw.githubusercontent.com/nijel/enca/master/test"

# ENCA encoding name -> test-data directory prefix
_ENCA_ENC_MAP: dict[str, str] = {
    "cp1125": "cp1125",
    "cp1250": "windows-1250",
    "cp1251": "windows-1251",
    "cp1257": "windows-1257",
    "cp866": "cp866",
    "ibm775": "cp775",
    "ibm852": "cp852",
    "ibm855": "cp855",
    "ibm866": "cp866",
    "iso88592": "iso-8859-2",
    "iso88594": "iso-8859-4",
    "iso88595": "iso-8859-5",
    "iso885913": "iso-8859-13",
    "iso885916": "iso-8859-16",
    "koi8r": "koi8-r",
    "koi8u": "koi8-u",
    "macce": "maclatin2",
    "maccyr": "maccyrillic",
    "big5": "big5",
    "gbk": "gb2312",
    "hz": "hz-gb-2312",
}

# fmt: off
ENCA_FILES: list[tuple[str, str]] = [
    # (iso_lang_code, enca_encoding_name)
    # windows-1250 (Central European)
    ("cs", "cp1250"), ("hr", "cp1250"), ("hu", "cp1250"),
    ("pl", "cp1250"), ("sk", "cp1250"), ("sl", "cp1250"),
    # windows-1251 (Cyrillic)
    ("be", "cp1251"), ("bg", "cp1251"), ("ru", "cp1251"), ("uk", "cp1251"),
    # windows-1257 (Baltic)
    ("et", "cp1257"), ("lt", "cp1257"), ("lv", "cp1257"),
    # iso-8859-2
    ("cs", "iso88592"), ("hr", "iso88592"), ("hu", "iso88592"),
    ("pl", "iso88592"), ("sk", "iso88592"), ("sl", "iso88592"),
    # iso-8859-4
    ("et", "iso88594"), ("lt", "iso88594"), ("lv", "iso88594"),
    # iso-8859-5
    ("be", "iso88595"), ("bg", "iso88595"), ("ru", "iso88595"),
    ("uk", "iso88595"),
    # iso-8859-13
    ("et", "iso885913"), ("lt", "iso885913"), ("lv", "iso885913"),
    # iso-8859-16
    ("pl", "iso885916"),
    # koi8-r / koi8-u
    ("ru", "koi8r"),
    ("uk", "koi8u"),
    # cp775 (Baltic DOS)
    ("et", "ibm775"), ("lt", "ibm775"), ("lv", "ibm775"),
    # cp852 (Central European DOS)
    ("cs", "ibm852"), ("hr", "ibm852"), ("hu", "ibm852"),
    ("pl", "ibm852"), ("sk", "ibm852"), ("sl", "ibm852"),
    # cp855 (Cyrillic DOS)
    ("be", "ibm855"), ("bg", "ibm855"), ("uk", "ibm855"),
    # cp866 (Cyrillic DOS)
    ("be", "ibm866"), ("ru", "cp866"),
    # maclatin2 (Mac Central European)
    ("cs", "macce"), ("hr", "macce"), ("hu", "macce"),
    ("sk", "macce"), ("sl", "macce"),
    # maccyrillic
    ("be", "maccyr"), ("bg", "maccyr"), ("ru", "maccyr"), ("uk", "maccyr"),
    # cp1125
    ("uk", "cp1125"),
    # Chinese
    ("zh", "big5"), ("zh", "gbk"), ("zh", "hz"),
]
# fmt: on

# ---------------------------------------------------------------------------
# HTML processing (for Wayback Machine pages)
# ---------------------------------------------------------------------------

_STRIP_TAGS_RE = re.compile(
    rb"<\s*(script|style|noscript)[^>]*>.*?</\s*\1\s*>",
    re.IGNORECASE | re.DOTALL,
)
_TAG_RE = re.compile(rb"<[^>]+>")


def strip_html_to_text(raw: bytes, codec: str) -> bytes:
    """Strip HTML from raw bytes, return plain text re-encoded in *codec*."""
    cleaned = _STRIP_TAGS_RE.sub(b" ", raw)
    cleaned = _TAG_RE.sub(b" ", cleaned)

    try:
        text = cleaned.decode(codec, errors="ignore")
    except (UnicodeDecodeError, LookupError):
        return b""

    text = html.unescape(text)
    text = re.sub(r"\s+", " ", text).strip()

    try:
        result = text.encode(codec, errors="ignore")
    except (UnicodeEncodeError, LookupError):
        return b""

    # Truncate to ~8 KB
    if len(result) > 8192:
        result = result[:8192]
    return result


def _looks_like_utf8(data: bytes) -> bool:
    """Return True if *data* decodes as UTF-8 and contains non-ASCII chars."""
    try:
        text = data.decode("utf-8")
    except UnicodeDecodeError:
        return False
    return any(ord(c) > 127 for c in text[:2000])


# ---------------------------------------------------------------------------
# Validation
# ---------------------------------------------------------------------------


def validate_candidate(
    data: bytes,
    codec: str,
    *,
    min_size: int = 100,
    min_non_ascii: int = 5,
    reject_utf8_ambiguous: bool = False,
) -> bool:
    """Check that *data* is a valid test file for *codec*.

    Checks: minimum size, round-trip encode/decode, and non-ASCII content.
    When *reject_utf8_ambiguous* is True, also rejects files whose bytes
    are valid UTF-8 with non-ASCII content (ambiguous encoding).
    """
    if len(data) < min_size:
        return False

    try:
        decoded = data.decode(codec)
        re_encoded = decoded.encode(codec)
    except (UnicodeDecodeError, UnicodeEncodeError, LookupError):
        return False

    if re_encoded != data:
        return False

    non_ascii = sum(1 for b in data if b >= 128)
    if non_ascii < min_non_ascii:
        return False

    if reject_utf8_ambiguous and codec not in ("utf-8", "utf_8"):
        if _looks_like_utf8(data):
            return False

    return True


# ---------------------------------------------------------------------------
# Download helper
# ---------------------------------------------------------------------------


def fetch_url(url: str, cache_dir: Path) -> bytes | None:
    """Fetch *url* with disk caching.  Returns raw bytes or None on error."""
    cache_key = hashlib.sha256(url.encode()).hexdigest()[:16]
    cache_file = cache_dir / f"{cache_key}.bin"

    if cache_file.is_file():
        return cache_file.read_bytes()

    req = Request(url, headers={"User-Agent": USER_AGENT})
    try:
        with urlopen(req, timeout=60) as resp:
            data = resp.read()
    except (HTTPError, URLError, TimeoutError, OSError) as exc:
        print(f"    SKIP (download failed): {url}: {exc}")
        return None

    cache_dir.mkdir(parents=True, exist_ok=True)
    cache_file.write_bytes(data)
    return data


# ---------------------------------------------------------------------------
# Per-source download logic
# ---------------------------------------------------------------------------


def _is_duplicate(data: bytes, dst_dir: Path) -> bool:
    """Return True if any file in *dst_dir* has the same SHA-256 as *data*."""
    if not dst_dir.is_dir():
        return False
    h = hashlib.sha256(data).digest()
    for f in dst_dir.iterdir():
        if f.is_file() and hashlib.sha256(f.read_bytes()).digest() == h:
            return True
    return False


def _should_skip(
    enc_prefix: str,
    language: str,
    target_encodings: set[str] | None,
) -> bool:
    """Return True if this (encoding, language) pair should be skipped."""
    if target_encodings and enc_prefix not in target_encodings:
        return True
    expected_langs = ENCODING_LANGUAGES.get(enc_prefix)
    if not expected_langs or language not in expected_langs:
        return True
    return False


def _has_source_file(dst_dir: Path, prefix: str) -> bool:
    """Return True if *dst_dir* already has a file starting with *prefix*."""
    if not dst_dir.is_dir():
        return False
    return any(f.name.startswith(prefix) for f in dst_dir.iterdir() if f.is_file())


def download_wayback_sources(
    base_dir: Path,
    cache_dir: Path,
    target_encodings: set[str] | None,
    *,
    dry_run: bool,
    max_per_pair: int,
) -> int:
    """Download and process Wayback Machine pages.  Returns files saved."""
    saved = 0
    # Track how many files we've saved per (encoding, language) pair
    pair_counts: dict[tuple[str, str], int] = {}

    for domain, timestamp, enc_prefix, language in WAYBACK_SOURCES:
        if _should_skip(enc_prefix, language, target_encodings):
            continue

        pair_key = (enc_prefix, language)
        if pair_counts.get(pair_key, 0) >= max_per_pair:
            continue

        dst_dir = base_dir / f"{enc_prefix}-{language}"
        domain_slug = domain.replace(".", "_")
        fname = f"archive_{domain_slug}_{timestamp}.txt"

        # Skip if this specific file already exists
        if (dst_dir / fname).is_file():
            continue

        if dry_run:
            print(f"  DRY-RUN: {enc_prefix}-{language}/{fname}")
            continue

        # Fetch the raw page via Wayback Machine id_ URL
        url = f"https://web.archive.org/web/{timestamp}id_/http://{domain}/"
        raw = fetch_url(url, cache_dir)
        if raw is None or len(raw) < 200:
            continue

        codec = get_codec(enc_prefix)

        # Safety check: if the raw HTML is valid UTF-8 with non-ASCII
        # content, the Wayback Machine likely transcoded it.
        if codec not in ("utf-8", "utf_8") and _looks_like_utf8(raw):
            print(
                f"    SKIP (looks UTF-8, expected {enc_prefix}): "
                f"{domain}/{timestamp}"
            )
            continue

        # Strip HTML to plain text in the target encoding
        text_bytes = strip_html_to_text(raw, codec)
        if not validate_candidate(text_bytes, codec):
            print(f"    SKIP (validation failed): {domain}/{timestamp}")
            continue

        if _is_duplicate(text_bytes, dst_dir):
            print(f"    SKIP (duplicate): {domain}/{timestamp}")
            continue

        dst_dir.mkdir(exist_ok=True)
        (dst_dir / fname).write_bytes(text_bytes)
        pair_counts[pair_key] = pair_counts.get(pair_key, 0) + 1
        saved += 1
        print(f"    SAVED {enc_prefix}-{language}/{fname} ({len(text_bytes)} bytes)")
        time.sleep(RATE_LIMIT_SECONDS)

    return saved


def download_uchardet_sources(
    base_dir: Path,
    cache_dir: Path,
    target_encodings: set[str] | None,
    *,
    dry_run: bool,
) -> int:
    """Download uchardet test files from GitHub.  Returns files saved."""
    saved = 0

    for iso_code, ude_enc_name in UCHARDET_FILES:
        enc_prefix = _UCHARDET_ENC_MAP.get(ude_enc_name, ude_enc_name)
        language = ISO_TO_LANGUAGE.get(iso_code)
        if not language:
            continue
        if _should_skip(enc_prefix, language, target_encodings):
            continue

        dst_dir = base_dir / f"{enc_prefix}-{language}"
        fname = f"_uchardet_{ude_enc_name.replace('-', '_')}.txt"

        if (dst_dir / fname).is_file():
            continue

        if dry_run:
            print(f"  DRY-RUN: {enc_prefix}-{language}/{fname}")
            continue

        url = f"{_UCHARDET_BASE}/{iso_code}/{ude_enc_name}.txt"
        raw = fetch_url(url, cache_dir)
        if raw is None:
            continue

        codec = get_codec(enc_prefix)
        if not validate_candidate(
            raw, codec, min_size=10, min_non_ascii=2, reject_utf8_ambiguous=True
        ):
            print(f"    SKIP (validation failed): uchardet {iso_code}/{ude_enc_name}")
            continue

        if _is_duplicate(raw, dst_dir):
            print(f"    SKIP (duplicate): uchardet {iso_code}/{ude_enc_name}")
            continue

        dst_dir.mkdir(exist_ok=True)
        (dst_dir / fname).write_bytes(raw)
        saved += 1
        print(f"    SAVED {enc_prefix}-{language}/{fname} ({len(raw)} bytes)")

    return saved


def download_enca_sources(
    base_dir: Path,
    cache_dir: Path,
    target_encodings: set[str] | None,
    *,
    dry_run: bool,
) -> int:
    """Download ENCA test files from GitHub.  Returns files saved."""
    saved = 0

    for iso_code, enca_enc_name in ENCA_FILES:
        enc_prefix = _ENCA_ENC_MAP.get(enca_enc_name)
        if not enc_prefix:
            continue
        language = ISO_TO_LANGUAGE.get(iso_code)
        if not language:
            continue
        if _should_skip(enc_prefix, language, target_encodings):
            continue

        dst_dir = base_dir / f"{enc_prefix}-{language}"
        fname = f"_enca_{enca_enc_name}_{iso_code}.txt"

        if (dst_dir / fname).is_file():
            continue

        if dry_run:
            print(f"  DRY-RUN: {enc_prefix}-{language}/{fname}")
            continue

        # ENCA files use {lang}-utf8.{enc} or {lang}-s.{enc} naming.
        # Try both; pick the first one that validates.
        codec = get_codec(enc_prefix)
        raw = None
        for variant in ("utf8", "s"):
            url = f"{_ENCA_BASE}/{iso_code}-{variant}.{enca_enc_name}"
            candidate = fetch_url(url, cache_dir)
            if candidate is not None and validate_candidate(
                candidate, codec, min_size=10, min_non_ascii=2,
                reject_utf8_ambiguous=True,
            ):
                raw = candidate
                break
        if raw is None:
            print(f"    SKIP (no valid variant): enca {iso_code}/{enca_enc_name}")
            continue

        if _is_duplicate(raw, dst_dir):
            print(f"    SKIP (duplicate): enca {iso_code}/{enca_enc_name}")
            continue

        dst_dir.mkdir(exist_ok=True)
        (dst_dir / fname).write_bytes(raw)
        saved += 1
        print(f"    SAVED {enc_prefix}-{language}/{fname} ({len(raw)} bytes)")

    return saved


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Download real-world encoded text from curated sources.",
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
        help="Filter to specific encoding prefixes (default: all)",
    )
    parser.add_argument(
        "--max-per-pair",
        type=int,
        default=3,
        help="Max Wayback files per encoding-language pair (default: 3)",
    )
    parser.add_argument(
        "--cache-dir",
        default=None,
        help="HTTP cache directory (default: scripts/.cache/downloads/)",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Show what would be downloaded without fetching",
    )
    parser.add_argument(
        "--skip-wayback",
        action="store_true",
        help="Skip Wayback Machine downloads",
    )
    parser.add_argument(
        "--skip-uchardet",
        action="store_true",
        help="Skip uchardet test file downloads",
    )
    parser.add_argument(
        "--skip-enca",
        action="store_true",
        help="Skip ENCA test file downloads",
    )
    args = parser.parse_args()

    base_dir = Path(args.base_dir).resolve()
    cache_dir = (
        Path(args.cache_dir)
        if args.cache_dir
        else base_dir / "scripts" / ".cache" / "downloads"
    )
    target_encodings = set(args.encodings) if args.encodings else None

    total = 0

    if not args.skip_wayback:
        print("\n=== Wayback Machine ===")
        n = download_wayback_sources(
            base_dir,
            cache_dir,
            target_encodings,
            dry_run=args.dry_run,
            max_per_pair=args.max_per_pair,
        )
        total += n
        print(f"  Wayback: {n} files saved")

    if not args.skip_uchardet:
        print("\n=== uchardet (BYVoid/uchardet) ===")
        n = download_uchardet_sources(
            base_dir,
            cache_dir,
            target_encodings,
            dry_run=args.dry_run,
        )
        total += n
        print(f"  uchardet: {n} files saved")

    if not args.skip_enca:
        print("\n=== ENCA (nijel/enca) ===")
        n = download_enca_sources(
            base_dir,
            cache_dir,
            target_encodings,
            dry_run=args.dry_run,
        )
        total += n
        print(f"  ENCA: {n} files saved")

    print(f"\nTotal: {total} files saved")


if __name__ == "__main__":
    main()
