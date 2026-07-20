# kala-encoding-detector

`kala-encoding-detector` is a pure-Java universal character-encoding detector.
It ports the runtime behavior of
[`chardet@e3dfaa1`](https://github.com/chardet/chardet/commit/e3dfaa1c75256c9d2a06103b566ea92997844f70)
to Java 17 without invoking Python, loading native code, consulting installed
charset providers, or reading an upstream checkout.

The detector covers 99 text encodings through 86 ordered detection targets,
six encoding eras, 49 languages, and 62 MIME types. It includes BOM and
BOM-less Unicode detection, ISO-2022/HZ/UTF-7 state machines, markup and PEP
263 declarations, binary magic, strict byte-validity filtering, CJK structural
gating, IDF-weighted bigram models, confusion resolution, and stable candidate
ranking.

## Requirements and build

- Java 17 or newer
- No runtime dependencies
- JPMS module name: `kala.encdet`

JetBrains annotations are a compile-only, `requires static` dependency and are
not needed at runtime. Build and verify the library, API documentation, and CLI
distribution with:

```text
./gradlew test javadoc installDist
```

The source tree does not contain the detector's binary tables or the binary
test corpus. Tasks that process main resources download the fixed
`chardet@e3dfaa1` source ZIP, verify its SHA-256, extract the three upstream
model files, and generate the kala-specific tables with pure Java code from
the reviewable text data under `gradle/encoding-data`. Test tasks independently
download and verify `chardet/test-data@fa16e9f`. Archives are retained in
`.gradle/upstream-archives`, so later clean builds can run with `--offline`.
The resulting JAR and application distribution are self-contained and never
perform downloads at runtime.

The generated launcher is `build/install/kala-encdet/bin/kala-encdet` (or
`kala-encdet.bat` on Windows).

## Basic API

```java
import kala.encdet.DetectionResult;
import kala.encdet.EncodingDetector;

import java.nio.file.Files;
import java.nio.file.Path;

byte[] input = Files.readAllBytes(Path.of("document.txt"));
DetectionResult result = EncodingDetector.DEFAULT.detect(input);

System.out.println(
        result.encoding() == null ? null : result.encoding().canonicalName()
);
System.out.println(result.confidence());
System.out.println(result.language());
System.out.println(result.mimeType());
```

`detect`, `detectAll`, and `detectAllUnfiltered` accept either a `byte[]` or a
`ByteBuffer`. Buffer overloads inspect the bytes between `position` and
`limit`, including direct and read-only buffers, without changing the buffer's
content, position, limit, or mark. Both input forms use the same zero-copy
`ByteBuffer` pipeline: arrays are wrapped, while buffers are sliced over their
current remaining region. Callers must not modify the underlying bytes while a
detection call is in progress.

`detectAll` keeps candidates whose confidence is strictly greater than `0.20`.
If that would remove every candidate, it returns the unfiltered list.
`detectAllUnfiltered` always returns every candidate. Both lists are immutable
and use stable descending-confidence order.

```java
import kala.encdet.DetectionResult;
import kala.encdet.Encoding;
import kala.encdet.EncodingDetector;
import kala.encdet.EncodingEra;

import java.util.Set;

EncodingDetector detector = EncodingDetector.DEFAULT
        .withEncodingEras(Set.of(EncodingEra.MODERN_WEB))
        .withMaxBytes(100_000)
        .withIncludedEncodings(Set.of(Encoding.UTF_8, Encoding.CP1252))
        .withNoMatchEncoding(Encoding.CP1252)
        .withPreferredSuperset(false);

DetectionResult result = detector.detect(input);
```

`EncodingDetector` is immutable. Every `withXxx` method returns an independent
detector and leaves its receiver unchanged, so configured instances can be
reused safely across detection calls and threads.

The `Encoding` enum represents all 86 detection targets throughout the public
API. Its `canonicalName()` and `displayName()` methods provide text only at
interchange and presentation boundaries. Those names are not guaranteed to be
accepted by `Charset.forName`, because Java 17's charset providers do not cover
every target. A target is not always an exact decoder identity: lookup may fold
related aliases such as `cp037` into `Encoding.CP1140`.
`EncodingDetector.lookupEncoding` resolves canonical, IANA, WHATWG, and codec
aliases to enum values without consulting a JDK charset provider;
`supportedEncodings` returns the enum values in registry order.

## Default behavior

`EncodingDetector.DEFAULT` selects all six eras and uses:

- `maxBytes = 200_000`
- no preferred-superset remapping
- no include or exclude filter
- `Encoding.CP1252` when no candidate survives
- `Encoding.UTF_8` for empty input

Filters apply in era, include, then exclude order. They also gate BOM, markup,
escape, and fallback results. Binary classification is not filtered and is
reported with a `null` encoding and an appropriate MIME type.

`EncodingDetector` instances are safe for concurrent use. Registry, validity,
decode, model, and confusion data are immutable after thread-safe lazy
initialization; each detection has independent working state.

## Command line

With file arguments, one result is printed for every readable file. Without a
file argument, the command reads standard input. If some files fail and at
least one succeeds, the exit status remains zero; it is one when every file
fails. Unknown encoding names are detection failures and use status one for
standard input. Argument syntax errors use status two.

```text
kala-encdet document.txt
kala-encdet --minimal document.txt
kala-encdet --language document.txt
kala-encdet --encoding-era modern_web document.txt
kala-encdet --include-encodings utf-8,windows-1252 document.txt
kala-encdet --exclude-encodings cp037,cp500 document.txt
kala-encdet --no-match-encoding cp1252 document.txt
kala-encdet --empty-input-encoding utf-8 empty.txt
type document.txt | kala-encdet
```

Use `kala-encdet --help` for the complete option summary.

The CLI accepts textual aliases and renders each detected enum value through
`Encoding.displayName()` for chardet-compatible output.

## Verification data

The test suite uses the complete `chardet/test-data` snapshot at commit
[`fa16e9f`](https://github.com/chardet/test-data/commit/fa16e9ffde8fd55606e2c7be7423a5fa702cb4a1).
The snapshot is extracted as a generated test resource rather than checked
into this repository. A committed text inventory verifies all 2,531 files by
path, size, and SHA-256. A fixed oracle covers all 2,517 detection samples and
checks the complete candidate sequence, encoding, confidence, language, and
MIME type. The build and tests never read a local reference checkout.

## License and provenance

Java source code is licensed under the Mozilla Public License 2.0. The pinned
upstream implementation and the extracted `models.bin`, `idf.bin`, and
`confusion.bin` resources are licensed under the 0BSD license. Reviewable codec
mapping data records behavior derived from CPython 3.14.6, which is distributed
under the Python Software Foundation License Version 2. The test corpus does
not have one uniform license; each file remains copyright its respective
publisher. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md), the generated
corpus `README.md` and `CATALOG.md`, and the bundled `RESOURCE-SOURCES.txt`
manifest for details and hashes.
