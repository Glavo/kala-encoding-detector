// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet;

import kala.encdet.EncodingDetector.Encoding;
import kala.encdet.EncodingDetector.Candidate;

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

/// Verifies the generated upstream corpus against an independent behavioral oracle.
@NotNullByDefault
final class CorpusGoldenTest {
    /// Corpus classpath prefix.
    private static final String CORPUS_PREFIX = "chardet-test-data/";

    /// Detector configured to expose every candidate from the pinned reference behavior.
    private static final EncodingDetector REFERENCE_DETECTOR =
            EncodingDetector.DEFAULT
                    .withMaxBytes(200_000)
                    .withMinimumConfidence(0.0)
                    .withFallbackEncoding(Encoding.CP1252);

    /// Maximum mismatch details retained in one assertion message.
    private static final int MAX_DETAILS = 40;

    /// Verifies every result field, confidence, and candidate order.
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
                List<Candidate> actual = REFERENCE_DETECTOR.detect(data).candidates();
                compareResults(path, expected, actual, mismatches);
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
            List<Candidate> actual,
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
            Candidate actualCandidate = actual.get(index);
            if (!java.util.Objects.equals(expectedResult.encoding(), actualCandidate.encoding())
                    || !java.util.Objects.equals(expectedResult.language(), actualCandidate.language())
                    || !java.util.Objects.equals(expectedResult.mimeType(), actualCandidate.mimeType())
                    || Math.abs(expectedResult.confidence() - actualCandidate.confidence()) > 1e-12) {
                addMismatch(
                        mismatches,
                        path + "[" + index + "]: expected " + expectedResult
                                + " but was " + actualCandidate
                                + "; expected=" + encodingNamesExpected(expected)
                                + "; actual=" + encodingNamesActual(actual)
                );
                return;
            }
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
                    parseEncoding(fields[0]),
                    Double.parseDouble(fields[1]),
                    nullable(fields[2]),
                    nullable(fields[3])
            ));
        }
        return List.copyOf(results);
    }

    /// Converts an oracle encoding name to its enum identity.
    ///
    /// @param value serialized encoding or null sentinel
    /// @return resolved encoding, or `null`
    private static @Nullable Encoding parseEncoding(String value) {
        if (value.equals("~")) {
            return null;
        }
        @Nullable Encoding encoding = Encoding.lookup(value);
        if (encoding == null) {
            throw new IllegalArgumentException("Unknown oracle encoding: " + value);
        }
        return encoding;
    }

    /// Converts the compact null sentinel.
    ///
    /// @param value serialized field
    /// @return decoded field, or `null`
    private static @Nullable String nullable(String value) {
        return value.equals("~") ? null : value;
    }

    /// Returns expected encodings for mismatch diagnostics.
    ///
    /// @param results expected candidates
    /// @return encoding list
    private static List<@Nullable Encoding> encodingNamesExpected(List<ExpectedResult> results) {
        return results.stream().map(ExpectedResult::encoding).toList();
    }

    /// Returns actual encodings for mismatch diagnostics.
    ///
    /// @param candidates actual candidates
    /// @return encoding list
    private static List<@Nullable Encoding> encodingNamesActual(List<Candidate> candidates) {
        return candidates.stream().map(Candidate::encoding).toList();
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
            @Nullable Encoding encoding,
            double confidence,
            @Nullable String language,
            @Nullable String mimeType
    ) {
        /// Creates one expected result.
        private ExpectedResult {
        }
    }
}
