# Kala Encoding Detector - Pure Java Character Encoding Detector

A dependency-free, pure Java character encoding detector library. It supports
86 ordered detection targets covering 99 encodings, and can also report
language and MIME type.

The detection behavior and model data are based on
[chardet commit e3dfaa1](https://github.com/chardet/chardet/commit/e3dfaa1c75256c9d2a06103b566ea92997844f70).

## Features

- Pure Java implementation with no native or runtime dependencies.
- JPMS module: `kala.encdet`.
- Accepts `byte[]` and heap or direct `ByteBuffer` inputs.
- Provides `Reader` and `BufferedReader` APIs for `Path`, `InputStream`,
  and `ReadableByteChannel`.
- Reports ranked candidates with confidence, language, and MIME type.
- Immutable detectors are safe to reuse between threads.
- Includes the `kala-encdet` command-line application.

## Requirements

- Java 17 or newer

## Basic Usage

Detect a byte array:

```java
byte[] data = Files.readAllBytes(Path.of("document.txt"));
EncodingDetector.Result result = EncodingDetector.DEFAULT.detect(data);

EncodingDetector.Encoding encoding = result.bestEncoding();
System.out.println(encoding == null ? "unknown" : encoding.canonicalName());
```

Use `result.candidates()` to inspect all retained candidates. `detect` also
accepts a `ByteBuffer`; its position, limit, and content are not modified.

Read a file using the detected encoding:

```java
try (BufferedReader reader = EncodingDetector.DEFAULT.newBufferedReader(
        Path.of("document.txt")
)) {
    reader.lines().forEach(System.out::println);
}
```

`newReader` and `newBufferedReader` also accept `InputStream` and
`ReadableByteChannel`. The returned reader owns and closes its source.

Not every detection target has an exact `Charset` implementation in the JDK.
Readers use `Encoding.approximateCharset()` by default. Use
`withCharsetApproximation(false)` to require exact mappings.

Create a customized detector with the `withXxx` methods:

```java
EncodingDetector detector = EncodingDetector.MODERN_WEB
        .withMinimumConfidence(0.35)
        .withMaxBytes(100_000)
        .withFallbackEncoding(EncodingDetector.Encoding.CP1252)
        .withCharsetApproximation(false);
```

`EncodingDetector.DEFAULT` enables every detection target.
`EncodingDetector.MODERN_WEB` limits detection to encodings in
`EncodingDetector.Era.MODERN_WEB`.

## Command Line

Create the command-line distribution:

```text
./gradlew installDist
```

Run it against one or more files:

```text
build/install/kala-encdet/bin/kala-encdet document.txt
build/install/kala-encdet/bin/kala-encdet --language document.txt
build/install/kala-encdet/bin/kala-encdet --encoding-era modern_web document.txt
```

Use `kala-encdet.bat` on Windows. Run `kala-encdet --help` to list all
options. Without file arguments, the command reads standard input.

## Building and Testing

Run all tests:

```text
./gradlew test
```

The build downloads pinned chardet and CPython source archives and generates
the model and codec resources used by the library. Tests also use the pinned
chardet test corpus. All downloaded inputs are verified before use.

Generate Javadoc:

```text
./gradlew javadoc
```

Run the JMH benchmarks:

```text
./gradlew jmh
```

## License

Project Java source is licensed under the
[Mozilla Public License 2.0](LICENSE). Model data, generated codec data, and
test data retain their upstream terms. See
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for details.
