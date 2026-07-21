# Third-party notices

## chardet

Detection behavior and model data are based on chardet commit
`e3dfaa1c75256c9d2a06103b566ea92997844f70`:

<https://github.com/chardet/chardet/commit/e3dfaa1c75256c9d2a06103b566ea92997844f70>

The build extracts `models.bin`, `idf.bin`, and `confusion.bin` from that
source snapshot. These resources are included in published artifacts but are
not checked into this repository.

chardet and the extracted resources are available under the 0BSD license:

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

Project Java source is separately licensed under MPL-2.0.

## CPython codec data

Encoding aliases, byte-validity data, and decoding tables are derived from
CPython commit `c63aec69bd59c55314c06c23f4c22c03de76fe45`:

<https://github.com/python/cpython/commit/c63aec69bd59c55314c06c23f4c22c03de76fe45>

Alias metadata and single-byte validity masks are represented in Java source.
The build generates the remaining multibyte validity and decoding resources
from the pinned CPython source without invoking Python. Generated binary tables
are not checked into this repository.

CPython is distributed under the Python Software Foundation License Version 2
and additional component licenses. The license text for the pinned source is
available at:

<https://github.com/python/cpython/blob/c63aec69bd59c55314c06c23f4c22c03de76fe45/LICENSE>

## chardet test data

Tests use the chardet test-data snapshot at commit
`fa16e9ffde8fd55606e2c7be7423a5fa702cb4a1`:

<https://github.com/chardet/test-data/commit/fa16e9ffde8fd55606e2c7be7423a5fa702cb4a1>

The corpus is generated as a test resource and is not checked into this
repository. It does not have a single uniform license and is not relicensed
under MPL-2.0. Each file remains copyright its respective publisher. The
snapshot retains its upstream `README.md`, `CATALOG.md`, repository metadata,
and source information.

## Build, test, and benchmark dependencies

JetBrains Annotations is a compile-only dependency. JUnit is used only for
tests, and JMH is used only for benchmarks. These dependencies are not embedded
in the library or command-line distribution.
