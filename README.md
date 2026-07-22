# Kala Encoding Detector

Kala Encoding Detector is a Java port of
[chardet7](https://github.com/chardet/chardet), providing a pure Java character
encoding detection library.

Its main purpose is to infer a file's character encoding and MIME type from
binary data.

## Features

- Pure Java implementation with no native or runtime dependencies.
- Depends only on the `java.base` module at runtime.
- Supports detecting the character encoding and MIME type of data in a
  `byte[]` or `ByteBuffer`.
- Supports creating a `Reader` directly from a `Path`, `InputStream`, or
  `ReadableByteChannel` whose encoding is unknown.

## Requirements

- Java 17 or newer

## Basic Usage

### Detecting the encoding of data in a `byte[]` or `ByteBuffer`

When binary data is stored in a `byte[]` or `ByteBuffer`, use
`EncodingDetector` to detect its encoding:

```java
byte[] data = Files.readAllBytes(Path.of("document.txt"));

// Detect the encoding of data in a byte array.
EncodingDetector.Result result = EncodingDetector.DEFAULT.detect(data);
// ByteBuffer is also supported, which is convenient for off-heap data.
// EncodingDetector.Result result = EncodingDetector.DEFAULT.detect(ByteBuffer.wrap(data));
```

After obtaining an `EncodingDetector.Result`, its encoding, MIME type, and
detection confidence can be inspected:

```java
EncodingDetector.Encoding encoding = result.bestEncoding();
// Obtain the corresponding Java Charset.
Charset charset = encoding.charset();

// Use Candidate when more information is needed.
EncodingDetector.Candidate candidate = result.bestCandidate();
// Detected encoding.
EncodingDetector.Encoding candidateEncoding = candidate.encoding();
// Candidate confidence in the range [0.0, 1.0].
double confidence = candidate.confidence();
// Detected MIME type.
String mimeType = candidate.mimeType();

// All candidates are ordered from highest to lowest confidence.
List<EncodingDetector.Candidate> candidates = result.candidates();
```

When no detection details are needed, a `String` can be created directly from
a `byte[]` or `ByteBuffer`. The data is decoded using the best detected
encoding:

```java
String text0 = EncodingDetector.DEFAULT.toString(data);
String text1 = EncodingDetector.DEFAULT.toString(ByteBuffer.wrap(data));
```

### Reading a string from a `Path`, `InputStream`, or `ReadableByteChannel` with an unknown encoding

Use `EncodingDetector.readString` to read a string from a file or byte stream
whose encoding is unknown:

```java
Path file = Path.of("document.txt");
InputStream inputStream = Files.newInputStream(file);
ByteChannel channel = Files.newByteChannel(file);

// Read a string from a Path.
String text0 = EncodingDetector.DEFAULT.readString(file);
// Read a string from an InputStream.
String text1 = EncodingDetector.DEFAULT.readString(inputStream);
// Read a string from a ReadableByteChannel.
String text2 = EncodingDetector.DEFAULT.readString(channel);
```

For large files, create a `BufferedReader` from a `Path`, `InputStream`, or
`ReadableByteChannel` and process the input incrementally:

```java
Path file = Path.of("document.txt");

// Create a BufferedReader from a Path.
try (var reader = EncodingDetector.DEFAULT.newBufferedReader(file)) {
}

// Create a BufferedReader from an InputStream.
try (var reader = EncodingDetector.DEFAULT.newBufferedReader(Files.newInputStream(file))) {
}

// Create a BufferedReader from a ReadableByteChannel.
try (var reader = EncodingDetector.DEFAULT.newBufferedReader(Files.newByteChannel(file))) {
}
```

### Configuring `EncodingDetector`

`EncodingDetector` is immutable. Options can be adjusted with the `withXxx`
methods:

```java
// DEFAULT enables every supported encoding.
// MODERN_WEB enables only encodings commonly used by modern software and web content.
EncodingDetector detector = EncodingDetector.MODERN_WEB
        // Examine at most 100,000 leading bytes during detection.
        .withMaxBytes(100_000)
        // Retain candidates whose confidence is at least 0.35.
        .withMinimumConfidence(0.35)
        // Report certain encodings as a preferred superset when possible.
        .withPreferredSuperset(true)
        // Recommend CP1252 when nonempty input has no matching text candidate.
        .withFallbackEncoding(EncodingDetector.Encoding.CP1252)
        // Recommend UTF-8 for empty input.
        .withEmptyInputEncoding(EncodingDetector.Encoding.UTF_8)
        // Require an exact Java Charset when decoding detected text.
        .withCharsetApproximation(false);

// Use withEncodings, withEncodingEra, or withEncodingEras to select another encoding set.
```

## License

Project Java source is licensed under the
[Mozilla Public License 2.0](LICENSE). Model data, generated codec data, and
test data retain their upstream terms. See
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for details.
