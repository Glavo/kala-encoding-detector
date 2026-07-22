# kala-encoding-detector

`kala-encoding-detector` is a pure-Java character-encoding detector for
Java 17 and later. Its detection behavior is based on
[`chardet@e3dfaa1`](https://github.com/chardet/chardet/commit/e3dfaa1c75256c9d2a06103b566ea92997844f70).

The library recognizes 99 text encodings through 86 ordered detection targets,
with language and MIME-type classification. The detection runtime uses only the
Java standard library: it does not invoke Python, load native code, download
data, or use installed `Charset` providers. Decoding through
`Encoding.charset()` or `EncodingDetector.newReader(...)` uses the charset
providers available in the current JVM.

## Requirements and build

- Java 17 or later
- No runtime dependencies
- JPMS module: `kala.encdet`

Build the library, run the tests, generate API documentation, and install the
CLI distribution with:

```text
./gradlew test javadoc installDist
```

Resource-producing build tasks download pinned source archives for the model
data and generated codec tables. Test tasks also download a pinned corpus.
Downloaded inputs are verified before use; published artifacts are
self-contained and perform no runtime downloads. See
[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) for sources and licenses.

The installed launcher is
`build/install/kala-encdet/bin/kala-encdet`, or `kala-encdet.bat` on
Windows.

## Detecting bytes

```java
import kala.encdet.EncodingDetector;
import kala.encdet.EncodingDetector.Candidate;
import kala.encdet.EncodingDetector.Encoding;
import kala.encdet.EncodingDetector.Result;

import java.nio.file.Files;
import java.nio.file.Path;

byte[] input = Files.readAllBytes(Path.of("document.txt"));
Result result = EncodingDetector.DEFAULT.detect(input);

Encoding encoding = result.bestEncoding();
System.out.println(encoding == null ? null : encoding.canonicalName());

Candidate candidate = result.bestCandidate();
if (candidate != null) {
    System.out.println(candidate.confidence());
    System.out.println(candidate.language());
    System.out.println(candidate.mimeType());
}
```

`detect` accepts either a `byte[]` or a `ByteBuffer`. For a buffer, it
examines the remaining bytes without changing the content, position, limit, or
mark. Both overloads read the caller's underlying storage directly and do not
retain it after the call; the bytes must not be modified while detection is in
progress. At most `maxBytes()` leading bytes are examined.

A `Result` contains an immutable, stably ordered candidate list. The list
includes candidates whose confidence is greater than or equal to the
detector's `minimumConfidence()`. Set the threshold to `0.0` to retain every
candidate produced by the pipeline.

`bestCandidate()` returns the first retained candidate or `null`.
`bestEncoding()` returns the recommended encoding. The empty-input
recommendation and fallback encoding may provide a result without creating a
candidate. A binary classification has a candidate with a `null` encoding and
an appropriate MIME type.

## Configuring detection

`EncodingDetector` is immutable and safe for concurrent use. Each
`withXxx` method returns the same instance when the requested value is already
configured and otherwise returns a new detector.

```java
import kala.encdet.EncodingDetector;
import kala.encdet.EncodingDetector.Encoding;

EncodingDetector detector = EncodingDetector.MODERN_WEB
        .withMaxBytes(100_000)
        .withMinimumConfidence(0.35)
        .withFallbackEncoding(Encoding.CP1252)
        .withCharsetApproximation(false)
        .withPreferredSuperset(false);
```

`EncodingDetector.DEFAULT` uses:

- `maxBytes = EncodingDetector.DEFAULT_MAX_BYTES` (`200_000`)
- `minimumConfidence = EncodingDetector.DEFAULT_MINIMUM_CONFIDENCE` (`0.20`)
- every registered detection target
- no preferred-superset remapping
- charset approximation enabled for readers
- no recommendation for unmatched nonempty text
- `Encoding.UTF_8` as the empty-input recommendation

`EncodingDetector.MODERN_WEB` has the same settings but permits only targets
in `Era.MODERN_WEB`.

The detector has one effective encoding set. `withEncodingEra(...)`,
`withEncodingEras(...)`, and `withEncodings(...)` replace that set, so the
last selector in a chain wins. An empty set permits no text encoding or
configured recommendation, but binary classification remains enabled.

`withFallbackEncoding(...)` controls the optional recommendation used when
nonempty input has no retained text candidate. The fallback must also be
present in `encodings()`. It does not create a candidate or replace a detected
binary classification. Passing `null` disables the recommendation.

## Encoding identities and Java charsets

`EncodingDetector.Encoding` represents detection targets rather than arbitrary
charset names. `Encoding.lookup(String)` resolves registered canonical names
and aliases without consulting JVM charset providers, and `Encoding.all()`
returns all targets in enum declaration order.

`Encoding.charset()` returns an exact Java `Charset` mapping when the current
JVM provides one, or `null` otherwise. It does not substitute a related
encoding with different character mappings. `Encoding.UTF_8_SIG` returns the
UTF-8 payload charset; its leading signature is framing outside that charset.
`Encoding.approximateCharset()` first returns the exact mapping, then tries a
configured related charset when only partial compatibility is available. It
returns `null` when neither mapping is available.
Use `Encoding.isCharsetSupported()` to test availability in the current JVM.
The Javadoc for each enum constant describes its Java charset support.

## Reading detected text

```java
import kala.encdet.EncodingDetector;

import java.io.Reader;
import java.io.StringWriter;
import java.nio.file.Path;

StringWriter text = new StringWriter();
try (Reader reader = EncodingDetector.DEFAULT.newReader(
        Path.of("document.txt")
)) {
    reader.transferTo(text);
}
System.out.println(text);
```

`newReader` also accepts an `InputStream` or `ReadableByteChannel`. The first
read with a nonempty target obtains up to
`maxBytes()` leading bytes, selects `Result.bestEncoding()`, replays the
detection prefix, and continues decoding the source. A UTF-8 signature is
consumed when `Encoding.UTF_8_SIG` is selected. Readers use
`Encoding.approximateCharset()` by default; use
`withCharsetApproximation(false)` to require an exact mapping.

`newBufferedReader` provides the same source overloads and returns a
`BufferedReader` for line-oriented input.

The reader owns and closes its source. Malformed and unmappable input is
replaced using the selected charset. If the permitted charset mapping is
unavailable, read operations throw `UnsupportedEncodingException`. Detection
and source I/O failures are likewise reported by read operations.

## Command line

With file arguments, `kala-encdet` prints one result for each readable file.
Without a file argument, it reads standard input.

```text
kala-encdet document.txt
kala-encdet --minimal document.txt
kala-encdet --language document.txt
kala-encdet --encoding-era modern_web document.txt
kala-encdet --include-encodings utf-8,windows-1252 document.txt
kala-encdet --fallback-encoding cp1252 document.txt
```

Use `kala-encdet --help` for all options. Argument errors return status 2.
Detection or input failure returns status 1 when no input succeeds; partial
file failure still returns status 0 when at least one file succeeds.

## Benchmarks

JMH benchmarks cover `byte[]`, heap `ByteBuffer`, and direct `ByteBuffer`
inputs without adding runtime dependencies to the library or CLI. Run them
with:

```text
./gradlew jmh
```

Additional JMH arguments may be supplied with `--args`.

## License and provenance

Project Java source is licensed under the Mozilla Public License 2.0.
Third-party model data, generated codec data, and the test corpus retain their
respective upstream terms. See
[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) for the pinned sources and
applicable notices.
