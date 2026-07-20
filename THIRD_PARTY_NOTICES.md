# Third-party notices

## chardet runtime behavior and model resources

This project is a Java port based on chardet commit
`e3dfaa1c75256c9d2a06103b566ea92997844f70`:

<https://github.com/chardet/chardet/commit/e3dfaa1c75256c9d2a06103b566ea92997844f70>

During the Gradle build, the files `models.bin`, `idf.bin`, and `confusion.bin`
are extracted byte-for-byte from the fixed commit's source archive. They are
included in published artifacts but are not checked into this repository.
The source archive and every extracted or generated resource are verified by
SHA-256. Digests and provenance are recorded in
`src/main/resources/kala/encdet/internal/RESOURCE-SOURCES.txt`.

chardet and its extracted resources are available under the 0BSD license:

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

## CPython codec behavior

The reviewable text tables under `gradle/encoding-data` were derived by
exhaustively observing strict codec behavior in CPython 3.14.6. They are used
by pure Java build logic to generate deterministic byte-validity and decoding
resources; no CPython code or executable is invoked by the build.

CPython is distributed under the Python Software Foundation License Version 2
and additional component licenses. The applicable license text is available
at <https://docs.python.org/3.14/license.html>.

## chardet test-data snapshot

The complete test snapshot at
`fa16e9ffde8fd55606e2c7be7423a5fa702cb4a1` is downloaded from its fixed source
archive and extracted into generated test resources:

<https://github.com/chardet/test-data/commit/fa16e9ffde8fd55606e2c7be7423a5fa702cb4a1>

The corpus is not checked into this repository. It does not have one uniform
license and is not relicensed under MPL-2.0. Each test file remains copyright
its respective publisher. The upstream `README.md`, `CATALOG.md`, repository
metadata, and source information are preserved in the generated snapshot. The
committed text inventory records every extracted file's path, byte length, and
SHA-256 digest.

## Build and test dependencies

JetBrains Annotations is used as a compile-only dependency and is not a runtime
dependency. JUnit is used only to execute tests. These dependencies are not
embedded in the library or CLI distribution.
