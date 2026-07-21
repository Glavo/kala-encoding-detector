// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal.cli;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies command-line parsing, output, and partial-failure status behavior.
@NotNullByDefault
final class MainTest {
    /// Verifies standard input is used when no file is supplied.
    @Test
    void detectsStandardInput() {
        RunResult result = invoke(new String[0], "Hello world".getBytes(StandardCharsets.US_ASCII));
        assertEquals(0, result.status());
        assertEquals("stdin: ascii with confidence 1.0" + System.lineSeparator(), result.output());
        assertEquals("", result.error());
    }

    /// Verifies compact output with the undetermined language sentinel.
    @Test
    void supportsMinimalLanguageOutput() {
        RunResult result = invoke(new String[]{"--minimal", "--language"}, new byte[0]);
        assertEquals(0, result.status());
        assertEquals("utf-8 und" + System.lineSeparator(), result.output());
        assertEquals("", result.error());
    }

    /// Verifies file input and the explicit encoding-era option.
    ///
    /// @param tempDirectory temporary file directory
    /// @throws IOException if the fixture cannot be written
    @Test
    void detectsFiles(@TempDir Path tempDirectory) throws IOException {
        Path file = tempDirectory.resolve("sample.txt");
        Files.writeString(file, "Hello world", StandardCharsets.US_ASCII);
        RunResult result = invoke(
                new String[]{"--minimal", "--encoding-era", "modern_web", file.toString()},
                new byte[0]
        );
        assertEquals(0, result.status());
        assertEquals("ascii" + System.lineSeparator(), result.output());
        assertEquals("", result.error());
    }

    /// Verifies one successful file makes a mixed batch return success.
    ///
    /// @param tempDirectory temporary file directory
    /// @throws IOException if the fixture cannot be written
    @Test
    void partialFileFailureReturnsSuccess(@TempDir Path tempDirectory) throws IOException {
        Path valid = tempDirectory.resolve("valid.txt");
        Path missing = tempDirectory.resolve("missing.txt");
        Files.writeString(valid, "Hello world", StandardCharsets.US_ASCII);
        RunResult result = invoke(
                new String[]{"--minimal", missing.toString(), valid.toString()},
                new byte[0]
        );
        assertEquals(0, result.status());
        assertEquals("ascii" + System.lineSeparator(), result.output());
        assertTrue(result.error().startsWith("kala-encdet: " + missing + ":"));
    }

    /// Verifies a batch in which every file fails returns status one.
    ///
    /// @param tempDirectory temporary file directory
    @Test
    void completeFileFailureReturnsStatusOne(@TempDir Path tempDirectory) {
        Path first = tempDirectory.resolve("missing-one.txt");
        Path second = tempDirectory.resolve("missing-two.txt");
        RunResult result = invoke(new String[]{first.toString(), second.toString()}, new byte[0]);
        assertEquals(1, result.status());
        assertEquals("", result.output());
        assertTrue(result.error().contains(first.toString()));
        assertTrue(result.error().contains(second.toString()));
    }

    /// Verifies argument syntax errors use the command-line status and usage prefix.
    @Test
    void syntaxErrorsReturnStatusTwo() {
        RunResult unknown = invoke(new String[]{"--unknown"}, new byte[0]);
        assertEquals(2, unknown.status());
        assertTrue(unknown.error().startsWith("usage: kala-encdet"));
        assertTrue(unknown.error().contains("unrecognized argument: --unknown"));

        RunResult invalidEra = invoke(new String[]{"--encoding-era", "future"}, new byte[0]);
        assertEquals(2, invalidEra.status());
        assertTrue(invalidEra.error().contains("invalid choice: 'future'"));
    }

    /// Verifies invalid encoding filters are reported as detection failures.
    @Test
    void invalidEncodingFilterReturnsStatusOne() {
        RunResult result = invoke(
                new String[]{"--include-encodings", "utf-8, not-real"},
                "Hello".getBytes(StandardCharsets.US_ASCII)
        );
        assertEquals(1, result.status());
        assertEquals("", result.output());
        assertTrue(result.error().contains("stdin: detection failed: Unknown encoding 'not-real'"));
    }

    /// Verifies aliases resolve to enum identities and use compatible display names.
    @Test
    void resolvesEncodingAliasesAndPrintsDisplayNames() {
        RunResult result = invoke(
                new String[]{"--minimal", "--include-encodings", "windows-1252"},
                "Héllo café".getBytes(StandardCharsets.ISO_8859_1)
        );
        assertEquals(0, result.status());
        assertEquals("Windows-1252" + System.lineSeparator(), result.output());
        assertEquals("", result.error());
    }

    /// Verifies no-match fallback is disabled by default and remains configurable.
    @Test
    void configuresOptionalNoMatchFallback() {
        byte[] input = {(byte) 0xe9, (byte) 0xe9, (byte) 0xe9};
        RunResult disabled = invoke(
                new String[]{"--minimal", "--include-encodings", "ascii"},
                input
        );
        assertEquals(0, disabled.status());
        assertEquals("None" + System.lineSeparator(), disabled.output());
        assertEquals("", disabled.error());

        RunResult detailed = invoke(
                new String[]{"--include-encodings", "ascii"},
                input
        );
        assertEquals(0, detailed.status());
        assertEquals(
                "stdin: None with confidence 0.0" + System.lineSeparator(),
                detailed.output()
        );
        assertEquals("", detailed.error());

        RunResult configured = invoke(
                new String[]{
                        "--minimal",
                        "--include-encodings", "ascii",
                        "--no-match-encoding", "ascii"
                },
                input
        );
        assertEquals(0, configured.status());
        assertEquals("ascii" + System.lineSeparator(), configured.output());
        assertEquals("", configured.error());
    }

    /// Verifies CLI inclusion and exclusion produce one effective encoding set.
    @Test
    void combinesEncodingFilters() {
        RunResult result = invoke(
                new String[]{
                        "--minimal",
                        "--include-encodings", "ascii",
                        "--exclude-encodings", "ascii"
                },
                "Hello".getBytes(StandardCharsets.US_ASCII)
        );
        assertEquals(0, result.status());
        assertEquals("None" + System.lineSeparator(), result.output());
        assertEquals("", result.error());
    }

    /// Verifies the era selector is intersected with explicit CLI filters.
    @Test
    void combinesEraAndEncodingFilters() {
        RunResult result = invoke(
                new String[]{
                        "--minimal",
                        "--encoding-era", "dos",
                        "--include-encodings", "utf-8"
                },
                "Hello".getBytes(StandardCharsets.US_ASCII)
        );
        assertEquals(0, result.status());
        assertEquals("None" + System.lineSeparator(), result.output());
        assertEquals("", result.error());
    }

    /// Verifies version and help requests do not consume standard input.
    @Test
    void printsVersionAndHelp() {
        RunResult version = invoke(new String[]{"--version"}, new byte[0]);
        assertEquals(0, version.status());
        assertEquals("kala-encdet 0.1.0-SNAPSHOT" + System.lineSeparator(), version.output());

        RunResult help = invoke(new String[]{"--help"}, new byte[0]);
        assertEquals(0, help.status());
        assertTrue(help.output().startsWith("usage: kala-encdet"));
        assertTrue(help.output().contains("no-match fallback (default: none)"));
        assertEquals("", help.error());
    }

    /// Invokes the CLI with isolated byte streams.
    ///
    /// @param arguments command-line arguments
    /// @param input     standard-input bytes
    /// @return captured status and text streams
    private static RunResult invoke(String @Unmodifiable [] arguments, byte[] input) {
        ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errorBytes = new ByteArrayOutputStream();
        int status;
        try (PrintStream output = new PrintStream(outputBytes, true, StandardCharsets.UTF_8);
             PrintStream error = new PrintStream(errorBytes, true, StandardCharsets.UTF_8)) {
            status = Main.run(arguments, new ByteArrayInputStream(input), output, error);
        }
        return new RunResult(
                status,
                outputBytes.toString(StandardCharsets.UTF_8),
                errorBytes.toString(StandardCharsets.UTF_8)
        );
    }

    /// Stores one isolated CLI invocation.
    ///
    /// @param status returned process status
    /// @param output captured standard output
    /// @param error  captured standard error
    @NotNullByDefault
    private record RunResult(int status, String output, String error) {
        /// Creates a captured invocation result.
        private RunResult {
        }
    }
}
