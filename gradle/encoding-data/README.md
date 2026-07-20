# Encoding resource source data

These text files are deterministic inputs to the pure-Java resource generator
under `buildSrc`. They capture the strict codec behavior used by
`chardet@e3dfaa1c75256c9d2a06103b566ea92997844f70` without depending on the
charset providers installed in the JVM that runs Gradle.

- `single-byte-mappings.tsv` contains 64 ordered byte-to-Unicode tables. Each
  row has a canonical codec name and exactly 256 comma-separated code points;
  `-` marks a byte that strict decoding rejects.
- `multibyte-validity.ranges` contains the packed-domain source ranges for the
  eight stateless multibyte codecs. Ranges are sorted, non-overlapping,
  inclusive hexadecimal intervals.
- `hz-validity.ranges` contains the valid shifted GB2312 pairs used by HZ.

The Gradle task validates every row, expands the ranges, writes the fixed KDM1
and KVM1 formats with explicit big-endian ordering, and verifies the SHA-256 of
every generated resource. The source tables are never packaged into the
runtime artifact.

The tables were derived by exhaustively observing strict decoding behavior in
CPython 3.14.6. They contain codec mapping facts, not CPython implementation
code. CPython is distributed under the Python Software Foundation License
Version 2; its license and additional terms are available at
<https://docs.python.org/3.14/license.html>.
