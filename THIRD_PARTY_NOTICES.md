# Third-party notices

## chardet runtime behavior and model resources

This project is a Java port based on chardet commit
`e3dfaa1c75256c9d2a06103b566ea92997844f70`:

<https://github.com/chardet/chardet/commit/e3dfaa1c75256c9d2a06103b566ea92997844f70>

The files `models.bin`, `idf.bin`, and `confusion.bin` are byte-for-byte copies
of resources at that commit. Their SHA-256 digests and the provenance of the
project-generated registry, validity, and decode resources are recorded in
`src/main/resources/kala/encdet/internal/RESOURCE-SOURCES.txt`.

chardet and its copied resources are available under the 0BSD license:

> Copyright (c) 2026 Dan Blanchard
>
> Permission to use, copy, modify, and/or distribute this software for any
> purpose with or without fee is hereby granted.
>
> THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
> REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND
> FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
> INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS
> OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER
> TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
> THIS SOFTWARE.

The Java source written for this port is separately licensed under MPL-2.0.

## chardet test-data snapshot

The complete test snapshot at
`fa16e9ffde8fd55606e2c7be7423a5fa702cb4a1` is included under
`src/test/resources/chardet-test-data`:

<https://github.com/chardet/test-data/commit/fa16e9ffde8fd55606e2c7be7423a5fa702cb4a1>

The corpus does not have one uniform license and is not relicensed under
MPL-2.0. Each test file remains copyright its respective publisher. The
upstream `README.md`, `CATALOG.md`, repository metadata, and source information
are preserved with the snapshot. The project inventory records every included
file's path, byte length, and SHA-256 digest.

## Build and test dependencies

JetBrains Annotations is used as a compile-only dependency and is not a runtime
dependency. JUnit is used only to execute tests. These dependencies are not
embedded in the library or CLI distribution.
