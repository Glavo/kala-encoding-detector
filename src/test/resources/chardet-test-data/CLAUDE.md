# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Purpose

Test data repository for the [chardet](https://github.com/chardet/chardet) character encoding detection library. Contains ~2,178 text files in 82 character encodings across 47 languages. This is a **data-only** repo — no build system, no tests, no dependencies.

## Directory Structure

Each subdirectory is named `{encoding}` or `{encoding}-{language}` (e.g., `big5-chinese`, `utf-8-english`, `cp037-breton`). The encoding portion must resolve via Python's `codecs.lookup()`. The special `None/` directory holds binary files (images, video, xlsx) used as negative test cases.

## File Naming Conventions

Filename prefixes indicate provenance:

- `culturax_` — CulturaX dataset (mC4/OSCAR web crawl data), ~1,700 files
- `_ude_` — Ude (C# Universal Detector Engine) test suite
- `_chromium_` — Chromium browser encoding detection tests
- `_mozilla_` — Mozilla charset detection regression tests
- Domain-name `.xml` files (e.g., `kapranoff.ru.xml`) — Mark Pilgrim's original chardet RSS/Atom feeds

## Key Files

- **`scripts/check_test_data.py`** — Standalone Python 3 script (stdlib only) that validates all test files. Checks: decoding correctness, mojibake detection, control character ratios, language/script mismatches, binary file detection.
- **`CATALOG.md`** — Comprehensive catalog documenting every file's source, size, and notable characteristics.

## Common Commands

```bash
# Run the full quality check (from repo root)
python3 scripts/check_test_data.py .

# JSON output for machine processing
python3 scripts/check_test_data.py . --json
```

## Encoding Gotchas

- **EBCDIC** encodings (cp037, cp424, cp500, cp875, cp1026) have raw bytes that look entirely non-ASCII — don't use raw-byte ASCII heuristics on them.
- **UTF-32/UTF-16** files legitimately start with null bytes — don't treat `\x00` as a binary signature for these.
- Files may self-declare an encoding in XML/HTML headers that differs from the directory name. The directory name is the ground truth encoding for the raw bytes.

## Git

Branch: `main`.
