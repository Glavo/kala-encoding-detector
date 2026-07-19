// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the fixed upstream corpus inventory and complete oracle candidates.
@NotNullByDefault
final class CorpusGoldenTest {
    /// Corpus classpath prefix.
    private static final String CORPUS_PREFIX = "chardet-test-data/";

    /// Maximum mismatch details retained in one assertion message.
    private static final int MAX_DETAILS = 40;

    /// Verifies every vendored file's byte length and SHA-256 digest.
    ///
    /// @throws IOException if a test resource cannot be read
    @Test
    void snapshotMatchesInventory() throws IOException {
        int count = 0;
        try (BufferedReader reader = resourceReader("chardet-test-data-inventory.tsv")) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] fields = line.split("\\t", -1);
                assertEquals(3, fields.length, "Malformed inventory line: " + line);
                byte[] data = resourceBytes(CORPUS_PREFIX + fields[0]);
                assertEquals(Long.parseLong(fields[1]), data.length, fields[0]);
                assertEquals(fields[2], sha256(data), fields[0]);
                count++;
            }
        }
        assertEquals(2531, count);
    }

    /// Verifies every result field, confidence, order, and streaming top result.
    ///
    /// @throws IOException if a test resource cannot be read
    @Test
    void corpusMatchesPinnedOracle() throws IOException {
        ArrayList<String> mismatches = new ArrayList<>();
        int samples = 0;
        try (BufferedReader reader = resourceReader("chardet-test-data-golden.tsv")) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] fields = line.split("\\t", -1);
                if (fields.length != 3) {
                    addMismatch(mismatches, "Malformed golden line: " + line);
                    continue;
                }
                String path = fields[0];
                byte[] data = resourceBytes(CORPUS_PREFIX + path);
                if (!fields[1].equals(sha256(data))) {
                    addMismatch(mismatches, path + ": input SHA-256 differs from golden");
                    continue;
                }
                List<ExpectedResult> expected = parseResults(fields[2]);
                List<DetectionResult> actual = EncodingDetector.DEFAULT.detectAllUnfiltered(data);
                compareResults(path, expected, actual, mismatches);
                compareStreaming(path, data, actual.get(0), mismatches);
                samples++;
            }
        }
        assertEquals(2517, samples);
        assertTrue(
                mismatches.isEmpty(),
                () -> "Corpus mismatches (showing at most " + MAX_DETAILS + "):\n"
                        + String.join("\n", mismatches)
        );
    }

    /// Compares complete candidate lists for one corpus path.
    ///
    /// @param path       corpus path
    /// @param expected   oracle candidates
    /// @param actual     Java candidates
    /// @param mismatches accumulated mismatch details
    private static void compareResults(
            String path,
            List<ExpectedResult> expected,
            List<DetectionResult> actual,
            List<String> mismatches
    ) {
        if (expected.size() != actual.size()) {
            addMismatch(
                    mismatches,
                    path + ": candidate count expected " + expected.size() + " but was " + actual.size()
                            + "; expected=" + encodingNamesExpected(expected)
                            + "; actual=" + encodingNamesActual(actual)
            );
            return;
        }
        for (int index = 0; index < expected.size(); index++) {
            ExpectedResult expectedResult = expected.get(index);
            DetectionResult actualResult = actual.get(index);
            if (!java.util.Objects.equals(expectedResult.encoding(), actualResult.encoding())
                    || !java.util.Objects.equals(expectedResult.language(), actualResult.language())
                    || !java.util.Objects.equals(expectedResult.mimeType(), actualResult.mimeType())
                    || Math.abs(expectedResult.confidence() - actualResult.confidence()) > 1e-12) {
                addMismatch(
                        mismatches,
                        path + "[" + index + "]: expected " + expectedResult + " but was " + actualResult
                                + "; expected=" + encodingNamesExpected(expected)
                                + "; actual=" + encodingNamesActual(actual)
                );
                return;
            }
        }
    }

    /// Verifies streaming detection with deterministic variable chunk sizes.
    ///
    /// @param path        corpus path
    /// @param data        input bytes
    /// @param expectedTop expected top result
    /// @param mismatches  accumulated mismatch details
    private static void compareStreaming(
            String path,
            byte @Unmodifiable [] data,
            DetectionResult expectedTop,
            List<String> mismatches
    ) {
        StreamingEncodingDetector detector = new StreamingEncodingDetector();
        int chunk = Math.floorMod(path.hashCode(), 257) + 1;
        for (int offset = 0; offset < data.length; offset += chunk) {
            detector.feed(data, offset, Math.min(chunk, data.length - offset));
        }
        DetectionResult actual = detector.finish();
        if (!expectedTop.equals(actual)) {
            addMismatch(mismatches, path + ": streaming expected " + expectedTop + " but was " + actual);
        }
    }

    /// Parses a compact semicolon-separated candidate list.
    ///
    /// @param value serialized candidates
    /// @return immutable parsed candidates
    private static @Unmodifiable List<ExpectedResult> parseResults(String value) {
        ArrayList<ExpectedResult> results = new ArrayList<>();
        for (String encodedResult : value.split(";", -1)) {
            String[] fields = encodedResult.split("\\|", -1);
            if (fields.length != 4) {
                throw new IllegalArgumentException("Malformed encoded result: " + encodedResult);
            }
            results.add(new ExpectedResult(
                    nullable(fields[0]),
                    Double.parseDouble(fields[1]),
                    nullable(fields[2]),
                    nullable(fields[3])
            ));
        }
        return List.copyOf(results);
    }

    /// Converts the compact null sentinel.
    ///
    /// @param value serialized field
    /// @return decoded field, or `null`
    private static @Nullable String nullable(String value) {
        return value.equals("~") ? null : value;
    }

    /// Returns expected encoding names for mismatch diagnostics.
    ///
    /// @param results expected candidates
    /// @return encoding-name list
    private static List<@Nullable String> encodingNamesExpected(List<ExpectedResult> results) {
        return results.stream().map(ExpectedResult::encoding).toList();
    }

    /// Returns actual encoding names for mismatch diagnostics.
    ///
    /// @param results actual candidates
    /// @return encoding-name list
    private static List<@Nullable String> encodingNamesActual(List<DetectionResult> results) {
        return results.stream().map(DetectionResult::encoding).toList();
    }

    /// Adds one diagnostic while enforcing the output cap.
    ///
    /// @param mismatches accumulated details
    /// @param detail     new detail
    private static void addMismatch(List<String> mismatches, String detail) {
        if (mismatches.size() < MAX_DETAILS) {
            mismatches.add(detail);
        }
    }

    /// Opens a UTF-8 classpath resource reader.
    ///
    /// @param name resource name
    /// @return buffered reader
    private static BufferedReader resourceReader(String name) {
        return new BufferedReader(new InputStreamReader(resourceStream(name), StandardCharsets.UTF_8));
    }

    /// Reads all bytes from a classpath resource.
    ///
    /// @param name resource name
    /// @return resource contents
    /// @throws IOException if reading fails
    private static byte[] resourceBytes(String name) throws IOException {
        try (InputStream input = resourceStream(name)) {
            return input.readAllBytes();
        }
    }

    /// Opens a required classpath resource.
    ///
    /// @param name resource name
    /// @return resource stream
    private static InputStream resourceStream(String name) {
        @Nullable InputStream input = CorpusGoldenTest.class.getClassLoader().getResourceAsStream(name);
        if (input == null) {
            throw new IllegalStateException("Missing test resource: " + name);
        }
        return input;
    }

    /// Computes lower-case SHA-256 hexadecimal text.
    ///
    /// @param data bytes to hash
    /// @return digest
    private static String sha256(byte @Unmodifiable [] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }

    /// Holds one parsed oracle candidate.
    ///
    /// @param encoding   expected encoding, or `null`
    /// @param confidence expected confidence
    /// @param language   expected language, or `null`
    /// @param mimeType   expected MIME type, or `null`
    @NotNullByDefault
    private record ExpectedResult(
            @Nullable String encoding,
            double confidence,
            @Nullable String language,
            @Nullable String mimeType
    ) {
        /// Creates one expected result.
        private ExpectedResult {
        }
    }
}
