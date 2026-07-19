#!/usr/bin/env python3
"""
Quality/sanity check for character encoding test data files.

Walks the test data directory tree and verifies that each file decodes
correctly with its directory's labeled encoding, then runs quality checks
on the decoded text.
"""

import argparse
import codecs
import json
import os
import re
import sys
import unicodedata
from collections import defaultdict

# ---------------------------------------------------------------------------
# Known languages (used to split directory names into encoding + language)
# ---------------------------------------------------------------------------
KNOWN_LANGUAGES = {
    "arabic", "belarusian", "breton", "bulgarian", "chinese", "croatian",
    "czech", "danish", "dutch", "english", "esperanto", "estonian", "farsi",
    "finnish", "french", "gaelic", "german", "greek", "hebrew", "hungarian",
    "icelandic", "indonesian", "irish", "italian", "japanese", "kazakh",
    "korean", "latvian", "lithuanian", "macedonian", "malay", "maltese",
    "norwegian", "polish", "portuguese", "romanian", "russian", "serbian",
    "slovak", "slovene", "spanish", "swedish", "tajik", "thai", "turkish",
    "ukrainian", "urdu", "vietnamese", "welsh",
}

# ---------------------------------------------------------------------------
# Binary magic bytes
# ---------------------------------------------------------------------------
BINARY_SIGNATURES = [
    (b"\x89PNG", "PNG image"),
    (b"\xff\xd8\xff", "JPEG image"),
    (b"GIF87a", "GIF image"),
    (b"GIF89a", "GIF image"),
    (b"RIFF", "RIFF container (WebP/AVI)"),
    (b"PK\x03\x04", "ZIP/XLSX archive"),
    (b"\x00\x00\x00\x18ftypmp4", "MP4 video"),
    (b"\x00\x00\x00\x1cftypisom", "MP4 video"),
    (b"\x00\x00\x00\x20ftypisom", "MP4 video"),
]

# Encodings where raw null bytes are legitimate (UTF-32 variants)
NULL_EXEMPT_ENCODINGS = set()
for _name in ("utf-32", "utf-32be", "utf-32le", "utf-16", "utf-16be", "utf-16le"):
    try:
        NULL_EXEMPT_ENCODINGS.add(codecs.lookup(_name).name)
    except LookupError:
        pass

# EBCDIC encodings (raw bytes look non-ASCII, skip raw-byte heuristics)
EBCDIC_ENCODINGS = set()
for _name in ("cp037", "cp273", "cp500", "cp875", "cp1026", "cp1140", "cp424"):
    try:
        EBCDIC_ENCODINGS.add(codecs.lookup(_name).name)
    except LookupError:
        pass

# Latin-1-compatible encodings for mojibake detection
LATIN1_COMPATIBLE = set()
for _name in (
    "iso-8859-1", "iso-8859-15", "windows-1252", "iso-8859-2",
    "iso-8859-3", "iso-8859-9", "iso-8859-14", "iso-8859-16",
    "windows-1250", "windows-1254", "macroman", "maclatin2",
    "cp850", "cp858", "cp437", "cp860", "cp863",
):
    try:
        LATIN1_COMPATIBLE.add(codecs.lookup(_name).name)
    except LookupError:
        pass

# ---------------------------------------------------------------------------
# Language -> expected Unicode script mapping
# ---------------------------------------------------------------------------
LANGUAGE_SCRIPTS = {
    "arabic": {"Arabic"},
    "belarusian": {"Cyrillic"},
    "breton": {"Latin"},
    "bulgarian": {"Cyrillic"},
    "chinese": {"CJK"},
    "croatian": {"Latin"},
    "czech": {"Latin"},
    "danish": {"Latin"},
    "dutch": {"Latin"},
    "english": {"Latin"},
    "esperanto": {"Latin"},
    "estonian": {"Latin"},
    "farsi": {"Arabic"},  # Farsi uses Arabic script
    "finnish": {"Latin"},
    "french": {"Latin"},
    "gaelic": {"Latin"},
    "german": {"Latin"},
    "greek": {"Greek"},
    "hebrew": {"Hebrew"},
    "hungarian": {"Latin"},
    "icelandic": {"Latin"},
    "indonesian": {"Latin"},
    "irish": {"Latin"},
    "italian": {"Latin"},
    "japanese": {"CJK", "Hiragana", "Katakana"},
    "kazakh": {"Cyrillic"},
    "korean": {"Hangul", "CJK"},
    "latvian": {"Latin"},
    "lithuanian": {"Latin"},
    "macedonian": {"Cyrillic"},
    "malay": {"Latin"},
    "maltese": {"Latin"},
    "norwegian": {"Latin"},
    "polish": {"Latin"},
    "portuguese": {"Latin"},
    "romanian": {"Latin"},
    "russian": {"Cyrillic"},
    "serbian": {"Cyrillic"},
    "slovak": {"Latin"},
    "slovene": {"Latin"},
    "spanish": {"Latin"},
    "swedish": {"Latin"},
    "tajik": {"Cyrillic"},
    "thai": {"Thai"},
    "turkish": {"Latin"},
    "ukrainian": {"Cyrillic"},
    "urdu": {"Arabic"},  # Urdu uses Arabic script
    "vietnamese": {"Latin"},
    "welsh": {"Latin"},
}


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
def parse_dir_name(dirname):
    """Parse directory name into (encoding, language_or_None).

    Uses rsplit('-', 1) and checks if the last segment is a known language.
    """
    if dirname in ("None", "ascii"):
        return dirname, None

    parts = dirname.rsplit("-", 1)
    if len(parts) == 2 and parts[1] in KNOWN_LANGUAGES:
        encoding_name = parts[0]
        language = parts[1]
    else:
        encoding_name = dirname
        language = None

    return encoding_name, language


def resolve_encoding(name):
    """Resolve an encoding name via codecs.lookup, return canonical name or None."""
    try:
        return codecs.lookup(name).name
    except LookupError:
        return None


def is_binary(raw, canonical_encoding):
    """Check if raw bytes look like a known binary format."""
    for sig, _desc in BINARY_SIGNATURES:
        # Skip null-byte-starting signatures for encodings that legitimately
        # start with nulls
        if sig[0:1] == b"\x00" and canonical_encoding in NULL_EXEMPT_ENCODINGS:
            continue
        if raw.startswith(sig):
            return True
    return False


def classify_char_script(ch):
    """Classify a character into a broad script category."""
    cp = ord(ch)
    cat = unicodedata.category(ch)

    # Skip common/neutral characters (numbers, punctuation, symbols, whitespace)
    if cat[0] in ("Z", "P", "S", "N", "C"):
        return None

    # CJK Unified Ideographs and extensions
    if (0x4E00 <= cp <= 0x9FFF or 0x3400 <= cp <= 0x4DBF or
            0x20000 <= cp <= 0x2A6DF or 0xF900 <= cp <= 0xFAFF or
            0x2F800 <= cp <= 0x2FA1F):
        return "CJK"

    name = unicodedata.name(ch, "")
    upper = name.upper()

    if "HANGUL" in upper:
        return "Hangul"
    if "HIRAGANA" in upper:
        return "Hiragana"
    if "KATAKANA" in upper:
        return "Katakana"
    if "THAI" in upper:
        return "Thai"
    if "ARABIC" in upper:
        return "Arabic"
    if "HEBREW" in upper:
        return "Hebrew"
    if "GREEK" in upper:
        return "Greek"
    if "CYRILLIC" in upper:
        return "Cyrillic"
    if "LATIN" in upper or cat[0] == "L" and cp < 0x0250:
        return "Latin"

    # Fallback: check Unicode block ranges
    if 0x0400 <= cp <= 0x04FF or 0x0500 <= cp <= 0x052F:
        return "Cyrillic"
    if 0x0370 <= cp <= 0x03FF or 0x1F00 <= cp <= 0x1FFF:
        return "Greek"
    if 0x0590 <= cp <= 0x05FF:
        return "Hebrew"
    if 0x0600 <= cp <= 0x06FF or 0x0750 <= cp <= 0x077F or 0xFB50 <= cp <= 0xFDFF or 0xFE70 <= cp <= 0xFEFF:
        return "Arabic"
    if 0x0E00 <= cp <= 0x0E7F:
        return "Thai"
    if 0xAC00 <= cp <= 0xD7AF or 0x1100 <= cp <= 0x11FF:
        return "Hangul"
    if 0x3040 <= cp <= 0x309F:
        return "Hiragana"
    if 0x30A0 <= cp <= 0x30FF:
        return "Katakana"

    # If it's a letter but we can't classify, call it Latin as fallback
    if cat[0] == "L":
        return "Latin"

    return None


def check_mojibake(text, canonical_encoding):
    """Check for UTF-8-decoded-as-Latin-1 mojibake patterns."""
    if canonical_encoding not in LATIN1_COMPATIBLE:
        return None

    # Look for Ã followed by a character in 0x80-0xBF range (classic UTF-8 in Latin-1)
    pattern = re.compile(r"[\xc3][\x80-\xbf]")
    matches = pattern.findall(text)
    if len(matches) <= 2:
        return None

    # Verify by attempting round-trip
    try:
        recovered = text.encode("latin-1").decode("utf-8")
        if recovered != text:
            return f"Likely mojibake: {len(matches)} UTF-8-as-Latin-1 patterns found (round-trip succeeded)"
    except (UnicodeDecodeError, UnicodeEncodeError):
        pass

    return None


def check_control_chars(text):
    """Check for problematic control characters in decoded text."""
    issues = []
    text_len = len(text)
    if text_len == 0:
        return issues

    # Null bytes
    null_count = text.count("\x00")
    if null_count > 0:
        issues.append(("warning", f"Contains {null_count} null byte(s) in decoded text"))

    # C0 control characters (0x00-0x1F) except tab (0x09), newline (0x0A), CR (0x0D)
    c0_count = sum(1 for ch in text if "\x00" < ch < "\x20" and ch not in ("\t", "\n", "\r"))
    c0_pct = c0_count / text_len * 100
    if c0_pct > 1:
        issues.append(("warning", f"High C0 control character ratio: {c0_count}/{text_len} ({c0_pct:.1f}%)"))

    # C1 control characters (U+0080-U+009F)
    c1_count = sum(1 for ch in text if "\u0080" <= ch <= "\u009f")
    c1_pct = c1_count / text_len * 100
    if c1_pct > 2:
        issues.append(("warning", f"High C1 control character ratio: {c1_count}/{text_len} ({c1_pct:.1f}%)"))

    return issues


def check_special_unicode(text):
    """Check for replacement characters and Private Use Area characters."""
    issues = []
    text_len = len(text)
    if text_len == 0:
        return issues

    # U+FFFD replacement character
    fffd_count = text.count("\ufffd")
    if fffd_count > 0:
        issues.append(("warning", f"Contains {fffd_count} U+FFFD replacement character(s)"))

    # Private Use Area (U+E000-U+F8FF)
    pua_count = sum(1 for ch in text if "\ue000" <= ch <= "\uf8ff")
    pua_pct = pua_count / text_len * 100 if text_len > 0 else 0
    if pua_count > 5 or pua_pct > 1:
        issues.append(("info", f"Private Use Area characters: {pua_count} ({pua_pct:.1f}%)"))

    return issues


def check_language_mismatch(text, language, filename):
    """Check if decoded text contains characters from the expected script."""
    if language is None or language not in LANGUAGE_SCRIPTS:
        return None

    expected_scripts = LANGUAGE_SCRIPTS[language]

    # Count characters by script
    script_counts = defaultdict(int)
    total_script_chars = 0

    for ch in text:
        script = classify_char_script(ch)
        if script is not None:
            script_counts[script] += 1
            total_script_chars += 1

    if total_script_chars == 0:
        return None

    # Check if expected script accounts for >= 30% of classified characters
    expected_count = sum(script_counts.get(s, 0) for s in expected_scripts)
    expected_pct = expected_count / total_script_chars * 100

    if expected_pct < 30:
        dominant = max(script_counts, key=script_counts.get) if script_counts else "unknown"
        dominant_pct = script_counts[dominant] / total_script_chars * 100 if script_counts else 0
        return (
            f"Language mismatch: expected {'/'.join(expected_scripts)} for '{language}', "
            f"but dominant script is {dominant} ({dominant_pct:.0f}%); "
            f"expected scripts only {expected_pct:.0f}% of {total_script_chars} classified chars"
        )

    return None


# ---------------------------------------------------------------------------
# Main check logic
# ---------------------------------------------------------------------------
def check_directory(base_path):
    """Walk the test data tree and check all files. Returns list of issues."""
    issues = []
    files_checked = 0
    dirs_checked = 0

    for entry in sorted(os.listdir(base_path)):
        dir_path = os.path.join(base_path, entry)
        if not os.path.isdir(dir_path):
            continue

        # Skip dotfiles/directories
        if entry.startswith("."):
            continue

        # Skip non-test-data directories
        if entry in ("None", "None-None", "scripts"):
            continue

        dirs_checked += 1
        encoding_name, language = parse_dir_name(entry)
        canonical = resolve_encoding(encoding_name)

        if canonical is None:
            issues.append({
                "file": dir_path,
                "type": "encoding_lookup_error",
                "severity": "error",
                "message": f"Cannot resolve encoding: '{encoding_name}'"
            })
            continue

        for fname in sorted(os.listdir(dir_path)):
            fpath = os.path.join(dir_path, fname)
            if not os.path.isfile(fpath):
                continue

            files_checked += 1
            rel_path = os.path.relpath(fpath, base_path)

            # Read raw bytes
            try:
                with open(fpath, "rb") as f:
                    raw = f.read()
            except OSError as e:
                issues.append({
                    "file": rel_path,
                    "type": "read_error",
                    "severity": "error",
                    "message": str(e)
                })
                continue

            if len(raw) == 0:
                issues.append({
                    "file": rel_path,
                    "type": "empty_file",
                    "severity": "warning",
                    "message": "File is empty (0 bytes)"
                })
                continue

            # Binary detection
            if is_binary(raw, canonical):
                issues.append({
                    "file": rel_path,
                    "type": "binary_in_text_dir",
                    "severity": "error",
                    "message": "Binary file detected in text encoding directory"
                })
                continue

            # Decode check
            try:
                text = raw.decode(canonical)
            except UnicodeDecodeError as e:
                issues.append({
                    "file": rel_path,
                    "type": "decode_error",
                    "severity": "error",
                    "message": f"UnicodeDecodeError: {e}"
                })
                continue

            # Post-decode quality checks

            # Mojibake
            mojibake_msg = check_mojibake(text, canonical)
            if mojibake_msg:
                issues.append({
                    "file": rel_path,
                    "type": "mojibake",
                    "severity": "warning",
                    "message": mojibake_msg
                })

            # Control characters
            for severity, msg in check_control_chars(text):
                issues.append({
                    "file": rel_path,
                    "type": "control_chars",
                    "severity": severity,
                    "message": msg
                })

            # Special Unicode
            for severity, msg in check_special_unicode(text):
                issues.append({
                    "file": rel_path,
                    "type": "special_unicode",
                    "severity": severity,
                    "message": msg
                })

            # Language mismatch
            lang_msg = check_language_mismatch(text, language, fpath)
            if lang_msg:
                issues.append({
                    "file": rel_path,
                    "type": "language_mismatch",
                    "severity": "warning",
                    "message": lang_msg
                })

    return issues, files_checked, dirs_checked


def print_report(issues, files_checked, dirs_checked):
    """Print a human-readable report grouped by issue type."""
    print(f"Checked {files_checked} files in {dirs_checked} directories\n")

    if not issues:
        print("No issues found!")
        return

    # Group by type
    by_type = defaultdict(list)
    for issue in issues:
        by_type[issue["type"]].append(issue)

    # Severity ordering for display
    type_order = [
        "encoding_lookup_error",
        "read_error",
        "decode_error",
        "binary_in_text_dir",
        "empty_file",
        "mojibake",
        "control_chars",
        "special_unicode",
        "language_mismatch",
    ]

    # Count by severity
    severity_counts = defaultdict(int)
    for issue in issues:
        severity_counts[issue["severity"]] += 1

    print(f"Found {len(issues)} issue(s): "
          f"{severity_counts.get('error', 0)} error(s), "
          f"{severity_counts.get('warning', 0)} warning(s), "
          f"{severity_counts.get('info', 0)} info\n")

    for issue_type in type_order:
        if issue_type not in by_type:
            continue
        type_issues = by_type[issue_type]
        label = issue_type.replace("_", " ").title()
        print(f"{'=' * 60}")
        print(f"{label} ({len(type_issues)} file(s))")
        print(f"{'=' * 60}")
        for issue in type_issues:
            sev = issue["severity"].upper()
            print(f"  [{sev}] {issue['file']}")
            print(f"         {issue['message']}")
        print()

    # Any types not in our ordered list
    for issue_type in sorted(by_type.keys()):
        if issue_type not in type_order:
            type_issues = by_type[issue_type]
            label = issue_type.replace("_", " ").title()
            print(f"{'=' * 60}")
            print(f"{label} ({len(type_issues)} file(s))")
            print(f"{'=' * 60}")
            for issue in type_issues:
                sev = issue["severity"].upper()
                print(f"  [{sev}] {issue['file']}")
                print(f"         {issue['message']}")
            print()


def main():
    parser = argparse.ArgumentParser(
        description="Quality check for character encoding test data files"
    )
    parser.add_argument(
        "directory",
        help="Root directory of test data (containing encoding subdirectories)"
    )
    parser.add_argument(
        "--json",
        action="store_true",
        help="Output results as JSON"
    )
    args = parser.parse_args()

    if not os.path.isdir(args.directory):
        print(f"Error: '{args.directory}' is not a directory", file=sys.stderr)
        sys.exit(1)

    issues, files_checked, dirs_checked = check_directory(args.directory)

    if args.json:
        output = {
            "files_checked": files_checked,
            "dirs_checked": dirs_checked,
            "issue_count": len(issues),
            "issues": issues
        }
        json.dump(output, sys.stdout, indent=2)
        print()
    else:
        print_report(issues, files_checked, dirs_checked)

    # Exit with error code if there are errors
    has_errors = any(i["severity"] == "error" for i in issues)
    sys.exit(1 if has_errors else 0)


if __name__ == "__main__":
    main()
