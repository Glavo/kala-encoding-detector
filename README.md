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

The source tree does not contain the detector's binary tables, generated codec
tables, or the binary test corpus. Encoding names, eras, language associations,
multibyte classifications, and aliases are encoded directly in the
`EncodingDetector.Encoding` enum; strict single-byte validity masks are also
encoded in Java source. Tasks that process main resources download the fixed
`chardet@e3dfaa1` and
`CPython@c63aec69bd59c55314c06c23f4c22c03de76fe45` source ZIPs, verify their
SHA-256 digests, extract the three upstream model files, and generate the
multibyte validity and codec resources by parsing the pinned sources with pure
Java code under `buildSrc`. Test tasks independently download and verify
`chardet/test-data@fa16e9f`. Archives are retained in
`.gradle/upstream-archives`, so later clean builds can run with `--offline`.
The resulting JAR and application distribution are self-contained and never
perform downloads at runtime.

The generated launcher is `build/install/kala-encdet/bin/kala-encdet` (or
`kala-encdet.bat` on Windows).

## Benchmarks

JMH 1.37 benchmarks live in the independent `jmh` source set and do not add
dependencies to the library or CLI runtime. The benchmark matrix compares
`byte[]`, heap `ByteBuffer`, and direct `ByteBuffer` detection, plus complete
candidate-list access, using ASCII, UTF-8, and Windows-1252 inputs from 1 KiB
through the default 200,000-byte scan limit.

Run the complete warmed benchmark matrix with:

```text
./gradlew jmh
```

JMH options and a benchmark-name regular expression can be passed through the
Gradle task. For example, this command performs a short focused run:

```text
./gradlew jmh --args='EncodingDetectorBenchmark.detectByteArray -p content=UTF_8 -p size=16384 -wi 1 -i 3 -f 1'
```

## Basic API

```java
import kala.encdet.EncodingDetector;
import kala.encdet.EncodingDetector.Candidate;
import kala.encdet.EncodingDetector.Result;

import java.nio.file.Files;
import java.nio.file.Path;

byte[] input = Files.readAllBytes(Path.of("document.txt"));
Result result = EncodingDetector.DEFAULT.detect(input);
Candidate candidate = result.bestCandidate();

System.out.println(
        candidate.encoding() == null ? null : candidate.encoding().canonicalName()
);
System.out.println(candidate.confidence());
System.out.println(candidate.language());
System.out.println(candidate.mimeType());
```

`detect` accepts either a `byte[]` or a `ByteBuffer` and returns one immutable
`Result`. Buffer inputs are inspected between `position` and `limit`,
including direct and read-only buffers, without changing the buffer's content,
position, limit, or mark. Both input forms use the same zero-copy `ByteBuffer`
pipeline: arrays are wrapped, while buffers are sliced over their current
remaining region. Callers must not modify the underlying bytes while a
detection call is in progress.

`Result.candidates()` contains every candidate in stable
descending-confidence order, while `Result.likelyCandidates()` contains the
prefix whose confidence is greater than or equal to the detector's configured
minimum, which defaults to `0.20`. If no candidate reaches the threshold, the
likely list contains every candidate. Both lists are immutable and nonempty;
`Result.bestCandidate()` returns their first candidate.

Each `Candidate` carries an encoding, confidence, language, and MIME type. A
candidate returned by the detector may have a `null` encoding for binary input
or when no permitted fallback exists, and may have a `null` language when it
cannot be determined.

```java
import kala.encdet.EncodingDetector;
import kala.encdet.EncodingDetector.Encoding;
import kala.encdet.EncodingDetector.Era;
import kala.encdet.EncodingDetector.Result;

EncodingDetector detector = EncodingDetector.DEFAULT
        .withEncodingEras(Era.MODERN_WEB)
        .withMaxBytes(100_000)
        .withMinimumConfidence(0.35)
        .withNoMatchEncoding(Encoding.CP1252)
        .withPreferredSuperset(false);

Result result = detector.detect(input);
```

`EncodingDetector` is immutable. Every `withXxx` method leaves its receiver
unchanged. It returns that receiver when the requested value is already
configured and otherwise returns an independent detector, so configured
instances can be reused safely across detection calls and threads.
The detector stores one effective encoding set. `withEncodingEras` and
`withEncodingEra` replace it with the encodings classified in the selected
eras, while `withEncodings` replaces it with the supplied set; when these
methods are chained, the last selector wins. The plural methods accept either
varargs or a `Collection`; they read their input during the call and do not
retain it. Argument order and duplicates have no effect, and an empty selection
permits no text encoding.

The `EncodingDetector.Encoding` enum represents all 86 detection targets
throughout the public API and owns their fixed registry metadata. Its
`canonicalName()` and `displayName()` methods provide text only at interchange
and presentation boundaries; `era()`, `isMultibyte()`, `languages()`, and
`aliases()` expose the immutable detection metadata. Encoding names are not
guaranteed to be accepted by `Charset.forName`, because Java 17's charset
providers do not cover every target. A target is not always an exact decoder
identity: lookup may fold related aliases such as `cp037` into
`EncodingDetector.Encoding.CP1140`.
`EncodingDetector.lookupEncoding` resolves canonical, IANA, WHATWG, and codec
aliases to enum values without consulting a JDK charset provider;
`supportedEncodings` returns the enum values in declaration order.

## Default behavior

`EncodingDetector.DEFAULT` permits all supported encodings and uses:

- `maxBytes = 200_000`
- no preferred-superset remapping
- every supported encoding in the effective encoding set
- `EncodingDetector.Encoding.CP1252` when no candidate survives
- `EncodingDetector.Encoding.UTF_8` for empty input

The configured encoding set contains all supported targets by default; an
empty set permits no text encoding. It also gates BOM, markup, escape, and
fallback results. Binary classification is not filtered and is reported with a
`null` encoding and an appropriate MIME type.

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
`EncodingDetector.Encoding.displayName()` for chardet-compatible output. Its
include and exclude options are combined into the effective encoding set before
detection; exclusion wins when a target is present in both options.

## Verification data

The test suite uses the complete `chardet/test-data` snapshot at commit
[`fa16e9f`](https://github.com/chardet/test-data/commit/fa16e9ffde8fd55606e2c7be7423a5fa702cb4a1).
The snapshot is extracted as a generated test resource rather than checked
into this repository. Extraction verifies the source archive digest, exactly
2,531 files, 52,675,437 total bytes, and a canonical tree digest. A committed
fixed oracle covers all 2,517 detection samples and checks each input digest
and the complete candidate sequence, encoding, confidence, language, and MIME
type. This oracle remains independent test data; generating it with the Java
detector would make the verification self-referential. The build and tests
never read a local reference checkout.

## License and provenance

Java source code is licensed under the Mozilla Public License 2.0. The pinned
upstream implementation and the extracted `models.bin`, `idf.bin`, and
`confusion.bin` resources are licensed under the 0BSD license. Reviewable alias
metadata and single-byte validity masks are encoded in Java source, while codec
resources are generated from pinned CPython commit
`c63aec69bd59c55314c06c23f4c22c03de76fe45`, whose source is distributed under
the Python Software Foundation License Version 2.
The test corpus does not have one uniform license; each file remains copyright
its respective publisher. See
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md), the generated corpus
`README.md` and `CATALOG.md`, and the pinned digests in the Gradle build and
resource generator for details.
