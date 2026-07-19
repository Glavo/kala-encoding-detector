#!/usr/bin/env python3
"""Remove duplicate test data files across encoding directories.

Walks all test-data directories, groups files by MD5 hash, and classifies
each duplicate group using the rules defined in the cleanup plan.

Usage:
    python scripts/cleanup_duplicates.py                # dry-run report
    python scripts/cleanup_duplicates.py --execute      # perform actions
    python scripts/cleanup_duplicates.py --data-dir /path/to/test-data
"""

from __future__ import annotations

import argparse
import hashlib
import shutil
from collections import defaultdict
from pathlib import Path

# ---------------------------------------------------------------------------
# Encoding relationship maps
# ---------------------------------------------------------------------------

# Maps each superset encoding to its immediate subset.
SUPERSET_OF: dict[str, str] = {
    "windows-1252": "iso-8859-1",
    "windows-1250": "iso-8859-2",
    "windows-1251": "iso-8859-5",
    "windows-1253": "iso-8859-7",
    "windows-1254": "iso-8859-9",
    "windows-1255": "iso-8859-8",
    "windows-1257": "iso-8859-13",
    "cp874": "iso-8859-11",
    "iso-8859-11": "tis-620",
    "cp1140": "cp037",
}

EBCDIC_ENCODINGS = frozenset({"cp037", "cp500", "cp1026", "cp1140"})

ALIAS_PAIRS: list[frozenset[str]] = [
    frozenset({"cp932", "shift_jis"}),
    frozenset({"cp932", "shift-jis"}),
    frozenset({"cp949", "euc-kr"}),
    frozenset({"iso-2022-jp-2004", "iso-2022-jp-ext"}),
]

# Specific same-directory duplicates to remove (Rule 0).
# The plan says "keep the more descriptive filename".
RULE0_REMOVE: frozenset[str] = frozenset({
    "shift_jis-japanese/_ude_3.txt",
    "iso-8859-7-greek/_ude_3.txt",
    "iso-8859-2-hungarian/_ude_3.txt",
})


def parse_dir_name(dirname: str) -> tuple[str | None, str | None]:
    """Parse '{encoding}-{language}' directory name."""
    parts = dirname.rsplit("-", 1)
    if len(parts) != 2:
        return None, None
    enc = parts[0] if parts[0] != "None" else None
    lang = parts[1] if parts[1] != "None" else None
    return enc, lang


def is_pure_ascii(data: bytes) -> bool:
    """Return True if data is plain ASCII text (no high bytes, no ESC sequences).

    ISO-2022 encodings use 7-bit bytes with ESC (0x1B) shift sequences,
    so we exclude files containing ESC to avoid misclassifying them.
    """
    if not data:
        return True
    return max(data) < 128 and b"\x1b" not in data


def find_chain_root(encodings: set[str]) -> str | None:
    """If all encodings form a superset chain, return the smallest (root).

    A chain exists when every encoding can be ordered E1 ⊂ E2 ⊂ ... ⊂ En
    using the SUPERSET_OF map.

    Returns the root encoding if a chain exists, None otherwise.
    """
    if len(encodings) < 2:
        return None

    # SUPERSET_OF[X] = Y means X ⊃ Y (X is superset of Y)
    # Root = encoding that is NOT a superset of anyone else in the group
    root_candidates = [
        enc
        for enc in encodings
        if enc not in SUPERSET_OF or SUPERSET_OF[enc] not in encodings
    ]

    if len(root_candidates) != 1:
        return None

    root = root_candidates[0]

    # Build: for each encoding, what is its immediate superset in the group?
    superset_in_group: dict[str, str] = {}
    for enc in encodings:
        for other in encodings:
            if other != enc and SUPERSET_OF.get(other) == enc:
                superset_in_group[enc] = other
                break

    # Walk the chain from root upward through supersets
    visited = {root}
    current = root
    while current in superset_in_group:
        current = superset_in_group[current]
        if current in visited:
            return None  # cycle
        visited.add(current)

    return root if visited == encodings else None


# ---------------------------------------------------------------------------
# Duplicate group classification
# ---------------------------------------------------------------------------


def classify_group(
    files: list[tuple[str | None, str | None, Path]],
    file_bytes: dict[Path, bytes],
    data_dir: Path,
) -> list[tuple[str, Path, Path | None]]:
    """Classify a duplicate group and return actions.

    Returns list of (action, filepath, destination_or_none):
      - ("remove-ruleN", path, None)
      - ("move-rule5", path, None)
      - ("keep", path, None)
    """
    actions: list[tuple[str, Path, Path | None]] = []

    # Separate same-directory groups from cross-directory groups
    by_dir: dict[Path, list[tuple[str | None, str | None, Path]]] = defaultdict(list)
    for enc, lang, fp in files:
        by_dir[fp.parent].append((enc, lang, fp))

    # --- Rule 0: Same-directory duplicates ---
    for dir_files in by_dir.values():
        if len(dir_files) > 1:
            # Use the hardcoded list to decide which file to remove
            to_remove = []
            to_keep = []
            for item in dir_files:
                rel = str(item[2].relative_to(data_dir))
                if rel in RULE0_REMOVE:
                    to_remove.append(item)
                else:
                    to_keep.append(item)
            if not to_remove:
                # Fallback: keep all but last sorted
                sorted_files = sorted(dir_files, key=lambda x: x[2].name)
                to_keep = sorted_files[:1]
                to_remove = sorted_files[1:]
            for _, _, fp in to_keep:
                actions.append(("keep", fp, None))
            for _, _, fp in to_remove:
                actions.append(("remove-rule0", fp, None))

    if len(by_dir) < 2:
        return actions

    # For cross-directory analysis, take one representative file per directory
    cross_files = []
    for dir_files in by_dir.values():
        cross_files.append(dir_files[0])

    if len(cross_files) < 2:
        return actions

    encodings = {enc for enc, _, _ in cross_files if enc is not None}

    if not encodings:
        return actions

    # --- Rule 5: Pure ASCII check ---
    sample_file = cross_files[0][2]
    if is_pure_ascii(file_bytes[sample_file]):
        for enc, lang, fp in cross_files:
            if enc is not None and lang is not None:
                actions.append(("move-rule5", fp, None))
        return actions

    # --- Rule 1: EBCDIC pairs ---
    if encodings.issubset(EBCDIC_ENCODINGS):
        for _, _, fp in cross_files:
            actions.append(("remove-rule1", fp, None))
        return actions

    # --- Rule 2: Encoding aliases ---
    for alias_pair in ALIAS_PAIRS:
        if encodings == alias_pair:
            for _, _, fp in cross_files:
                actions.append(("remove-rule2", fp, None))
            return actions

    # --- Rules 3/4: Superset chains ---
    chain_root = find_chain_root(encodings)
    if chain_root is not None:
        for enc, _, fp in cross_files:
            if enc == chain_root:
                actions.append(("keep", fp, None))
            else:
                actions.append(("remove-rule3", fp, None))
        return actions

    # --- Rule 4 non-chain: remove all ---
    for _, _, fp in cross_files:
        actions.append(("remove-rule4", fp, None))

    return actions


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------


def collect_all_files(
    data_dir: Path,
) -> list[tuple[str | None, str | None, Path]]:
    """Collect (encoding, language, filepath) tuples from test data."""
    test_files = []
    for encoding_dir in sorted(data_dir.iterdir()):
        if not encoding_dir.is_dir() or encoding_dir.name.startswith("."):
            continue
        enc, lang = parse_dir_name(encoding_dir.name)
        if enc is None and lang is None and encoding_dir.name != "None-None":
            continue
        for filepath in sorted(encoding_dir.iterdir()):
            if filepath.is_file():
                test_files.append((enc, lang, filepath))
    return test_files


def main() -> None:
    parser = argparse.ArgumentParser(description="Clean up duplicate test data files")
    parser.add_argument(
        "--execute",
        action="store_true",
        help="Actually perform removals/moves (default: dry-run)",
    )
    parser.add_argument(
        "--data-dir",
        type=Path,
        default=Path(__file__).parent.parent,
        help="Path to test-data directory",
    )
    args = parser.parse_args()

    data_dir: Path = args.data_dir.resolve()
    print(f"Scanning: {data_dir}")

    # Collect all files and read their bytes (for MD5 + ASCII checks)
    all_files = collect_all_files(data_dir)
    print(f"Found {len(all_files)} files")

    file_bytes: dict[Path, bytes] = {}
    by_md5: dict[str, list[tuple[str | None, str | None, Path]]] = defaultdict(list)
    for enc, lang, fp in all_files:
        data = fp.read_bytes()
        file_bytes[fp] = data
        md5 = hashlib.md5(data).hexdigest()
        by_md5[md5].append((enc, lang, fp))

    # Filter to groups with 2+ files
    dup_groups = {md5: files for md5, files in by_md5.items() if len(files) >= 2}
    print(f"Found {len(dup_groups)} duplicate groups")

    # Classify and collect all actions
    all_actions: list[tuple[str, Path, Path | None]] = []
    rule_counts: dict[str, int] = defaultdict(int)

    for md5, files in sorted(dup_groups.items()):
        actions = classify_group(files, file_bytes, data_dir)
        for action, fp, dest in actions:
            if action != "keep":
                rule_counts[action] += 1
        all_actions.extend(actions)

    # Report
    removals = [(a, fp, d) for a, fp, d in all_actions if a.startswith("remove")]
    moves = [(a, fp, d) for a, fp, d in all_actions if a.startswith("move")]
    keeps = [(a, fp, d) for a, fp, d in all_actions if a == "keep"]

    print(f"\n{'='*60}")
    print("SUMMARY")
    print(f"{'='*60}")
    for rule, count in sorted(rule_counts.items()):
        print(f"  {rule}: {count} files")
    print(f"  Total removals: {len(removals)}")
    print(f"  Total moves: {len(moves)}")
    print(f"  Total keeps: {len(keeps)}")

    # Detailed report
    print(f"\n{'='*60}")
    print("REMOVALS")
    print(f"{'='*60}")
    for action, fp, _ in sorted(removals, key=lambda x: str(x[1])):
        rel = fp.relative_to(data_dir)
        print(f"  [{action}] {rel}")

    if moves:
        print(f"\n{'='*60}")
        print("MOVES (to ascii-{language})")
        print(f"{'='*60}")
        for action, fp, _ in sorted(moves, key=lambda x: str(x[1])):
            rel = fp.relative_to(data_dir)
            enc, lang = parse_dir_name(fp.parent.name)
            print(f"  [{action}] {rel} -> ascii-{lang}/{fp.name}")

    if keeps:
        print(f"\n{'='*60}")
        print("KEEPS")
        print(f"{'='*60}")
        for action, fp, _ in sorted(keeps, key=lambda x: str(x[1])):
            rel = fp.relative_to(data_dir)
            print(f"  [keep] {rel}")

    if not args.execute:
        print(f"\nDry run — pass --execute to perform these actions.")
        return

    # Execute
    print(f"\n{'='*60}")
    print("EXECUTING")
    print(f"{'='*60}")

    removed_count = 0
    moved_count = 0

    for action, fp, dest in all_actions:
        if action.startswith("remove"):
            rel = fp.relative_to(data_dir)
            print(f"  Removing {rel}")
            fp.unlink()
            removed_count += 1
        elif action.startswith("move"):
            enc, lang = parse_dir_name(fp.parent.name)
            if lang is None:
                continue
            target_dir = data_dir / f"ascii-{lang}"
            target_dir.mkdir(exist_ok=True)
            target = target_dir / fp.name
            if target.exists():
                print(f"  Removing {fp.relative_to(data_dir)} (already in ascii-{lang})")
                fp.unlink()
                removed_count += 1
            else:
                print(f"  Moving {fp.relative_to(data_dir)} -> ascii-{lang}/{fp.name}")
                shutil.move(str(fp), str(target))
                moved_count += 1

    # Clean up empty directories
    empty_removed = 0
    for encoding_dir in sorted(data_dir.iterdir()):
        if not encoding_dir.is_dir() or encoding_dir.name.startswith("."):
            continue
        remaining = [f for f in encoding_dir.iterdir() if f.is_file()]
        if not remaining:
            print(f"  Removing empty directory: {encoding_dir.name}")
            encoding_dir.rmdir()
            empty_removed += 1

    print(
        f"\nDone: {removed_count} removed, {moved_count} moved, "
        f"{empty_removed} empty dirs removed"
    )


if __name__ == "__main__":
    main()
