"""Precomputed map of distinguishing bytes per single-byte encoding.

For each single-byte encoding that shares >=30% of its high-byte (0x80-0xFF)
character mappings with at least one other encoding, this module identifies the
byte values that *differ* from at least one overlapping partner.  These
"distinguishing bytes" are the ones that test data **must** contain in order for
a detector to tell the encodings apart.

The map is computed once at import time and exposed as :data:`DISTINGUISHING_BYTES`.
"""

from __future__ import annotations

import codecs
import sys

# ---------------------------------------------------------------------------
# All single-byte encodings covered by the project
# ---------------------------------------------------------------------------
# Excludes Unicode, ASCII, multibyte (CJK), and escape-sequence encodings.
_SINGLE_BYTE_ENCODINGS: list[str] = [
    # ISO 8859
    "iso-8859-1",
    "iso-8859-2",
    "iso-8859-3",
    "iso-8859-4",
    "iso-8859-5",
    "iso-8859-6",
    "iso-8859-7",
    "iso-8859-8",
    "iso-8859-9",
    "iso-8859-10",
    "iso-8859-11",
    "iso-8859-13",
    "iso-8859-14",
    "iso-8859-15",
    "iso-8859-16",
    # Windows code pages
    "windows-1250",
    "windows-1251",
    "windows-1252",
    "windows-1253",
    "windows-1254",
    "windows-1255",
    "windows-1256",
    "windows-1257",
    "windows-1258",
    # EBCDIC
    "cp037",
    "cp273",
    "cp424",
    "cp500",
    "cp875",
    "cp1026",
    "cp1140",
    # DOS
    "cp437",
    "cp720",
    "cp737",
    "cp775",
    "cp850",
    "cp852",
    "cp855",
    "cp856",
    "cp857",
    "cp858",
    "cp860",
    "cp861",
    "cp862",
    "cp863",
    "cp864",
    "cp865",
    "cp866",
    "cp869",
    # Windows / regional
    "cp874",
    "cp1006",
    "cp1125",
    # KOI8
    "koi8-r",
    "koi8-u",
    "koi8-t",
    # Kazakh
    "kz1048",
    "ptcp154",
    # Thai
    "tis-620",
    # Mac
    "macroman",
    "maccyrillic",
    "maclatin2",
    "macgreek",
    "macturkish",
    "maciceland",
    # HP
    "hp-roman8",
]

# Overlap threshold: fraction of high bytes (0x80-0xFF) that decode to the
# same Unicode character before two encodings are considered "overlapping".
_OVERLAP_THRESHOLD = 0.30

# High-byte range used for both overlap detection and distinguishing-byte
# collection.
_HIGH_RANGE = range(0x80, 0x100)


def _build_decode_table(encoding: str) -> list[str | None]:
    """Build a 256-entry table mapping each byte to its Unicode character.

    Bytes that are undefined in the encoding map to ``None``.
    """
    codec_name = codecs.lookup(encoding).name
    table: list[str | None] = [None] * 256
    for byte_val in range(256):
        try:
            table[byte_val] = bytes([byte_val]).decode(codec_name)
        except (UnicodeDecodeError, ValueError):
            table[byte_val] = None
    return table


def _compute_distinguishing_bytes() -> dict[str, frozenset[int]]:
    """Compute the distinguishing high-byte set for each overlapping encoding.

    Algorithm
    ---------
    1. Build decode tables for every valid single-byte encoding.
    2. For each pair of encodings, count how many bytes in 0x80-0xFF decode to
       the *same* Unicode character.  If ``count / 128 > 0.30``, the pair
       overlaps.
    3. For each encoding that has at least one overlapping partner, collect
       every byte in 0x80-0xFF where it decodes to a *different* character
       than at least one of its overlapping partners.
    4. Return ``{canonical_codec_name: frozenset_of_distinguishing_bytes}``.
    """
    # Step 1 — build decode tables keyed by original name, skipping
    # encodings Python can't handle.  Use canonical codec names internally
    # to avoid comparing the same codec twice under different aliases, but
    # map results back to original names at the end.
    tables: dict[str, list[str | None]] = {}
    to_canonical: dict[str, str] = {}
    from_canonical: dict[str, list[str]] = {}
    for enc in _SINGLE_BYTE_ENCODINGS:
        try:
            cname = codecs.lookup(enc).name
        except LookupError:
            continue
        to_canonical[enc] = cname
        from_canonical.setdefault(cname, []).append(enc)
        if cname not in tables:
            tables[cname] = _build_decode_table(enc)

    codec_names = sorted(tables)

    # Step 2 — find overlapping pairs (using canonical names to avoid
    # duplicate comparisons for aliases like iso-8859-11 / tis-620).
    overlaps: dict[str, set[str]] = {cn: set() for cn in codec_names}
    for i, enc_a in enumerate(codec_names):
        table_a = tables[enc_a]
        for enc_b in codec_names[i + 1 :]:
            table_b = tables[enc_b]
            same = sum(
                1
                for b in _HIGH_RANGE
                if table_a[b] is not None
                and table_b[b] is not None
                and table_a[b] == table_b[b]
            )
            if same / 128 > _OVERLAP_THRESHOLD:
                overlaps[enc_a].add(enc_b)
                overlaps[enc_b].add(enc_a)

    # Step 3 — for each encoding with overlaps, find distinguishing bytes.
    # Compute per canonical name, then expand to all original names.
    result: dict[str, frozenset[int]] = {}
    for cname, partners in sorted(overlaps.items()):
        if not partners:
            continue
        table_enc = tables[cname]
        distinguishing: set[int] = set()
        for b in _HIGH_RANGE:
            char_enc = table_enc[b]
            for partner in partners:
                if tables[partner][b] != char_enc:
                    distinguishing.add(b)
                    break
        dist = frozenset(distinguishing)
        for orig_name in from_canonical[cname]:
            result[orig_name] = dist

    return result


DISTINGUISHING_BYTES: dict[str, frozenset[int]] = _compute_distinguishing_bytes()


if __name__ == "__main__":
    # Print the map as Python source to stdout; summary to stderr.
    print("DISTINGUISHING_BYTES = {")
    for enc in sorted(DISTINGUISHING_BYTES):
        byte_vals = sorted(DISTINGUISHING_BYTES[enc])
        hex_list = ", ".join(f"0x{b:02X}" for b in byte_vals)
        print(f"    {enc!r}: frozenset({{{hex_list}}}),")
    print("}")

    total = len(DISTINGUISHING_BYTES)
    print(f"\n# {total} encodings with overlapping partners", file=sys.stderr)
    for enc in sorted(DISTINGUISHING_BYTES):
        count = len(DISTINGUISHING_BYTES[enc])
        print(
            f"#   {enc}: {count} distinguishing bytes out of 128 high bytes",
            file=sys.stderr,
        )
