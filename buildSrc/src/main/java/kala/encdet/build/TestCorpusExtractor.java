// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.build;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/// Safely extracts and verifies the pinned chardet test corpus.
@NotNullByDefault
final class TestCorpusExtractor {
    /// Classpath directory containing extracted corpus files.
    private static final String OUTPUT_PREFIX = "chardet-test-data";

    /// Maximum accepted size of one inventoried corpus file.
    private static final long MAXIMUM_FILE_SIZE = 32L * 1024L * 1024L;

    /// Maximum accepted aggregate corpus size.
    private static final long MAXIMUM_TOTAL_SIZE = 64L * 1024L * 1024L;

    /// Prevents instantiation.
    private TestCorpusExtractor() {
    }

    /// Extracts every and only inventoried file into a generated resource root.
    ///
    /// @param archivePath fixed test-data ZIP
    /// @param archiveRoot exact root directory in the ZIP
    /// @param inventoryPath path, size, and SHA-256 inventory
    /// @param outputRoot generated test-resource root
    /// @throws IOException if validation, extraction, or installation fails
    static void extract(
            Path archivePath,
            String archiveRoot,
            Path inventoryPath,
            Path outputRoot
    ) throws IOException {
        @Unmodifiable Map<String, CorpusEntry> expected = readInventory(inventoryPath);
        String normalizedRoot = validateArchiveRoot(archiveRoot);
        Path absoluteOutput = outputRoot.toAbsolutePath().normalize();
        Path parent = Objects.requireNonNull(
                absoluteOutput.getParent(),
                "Corpus output must have a parent directory"
        );
        Files.createDirectories(parent);
        Path prepared = Files.createTempDirectory(parent, absoluteOutput.getFileName() + ".");
        boolean installed = false;
        try {
            Path corpusRoot = prepared.resolve(OUTPUT_PREFIX);
            Files.createDirectories(corpusRoot);
            extractArchive(archivePath, normalizedRoot, expected, corpusRoot);
            replaceDirectory(prepared, absoluteOutput);
            installed = true;
        } finally {
            if (!installed) {
                deleteRecursively(prepared);
            }
        }
    }

    /// Reads and validates the fixed corpus inventory.
    ///
    /// @param path inventory path
    /// @return entries keyed by portable relative path
    /// @throws IOException if the inventory cannot be read or is malformed
    private static @Unmodifiable Map<String, CorpusEntry> readInventory(Path path)
            throws IOException {
        LinkedHashMap<String, CorpusEntry> entries = new LinkedHashMap<>();
        Set<String> foldedPaths = new HashSet<>();
        long totalSize = 0L;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            @Nullable String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] fields = line.split("\\t", -1);
                if (fields.length != 3) {
                    throw malformed(path, lineNumber, "expected path, size, and SHA-256");
                }
                validateRelativePath(path, lineNumber, fields[0]);
                String folded = fields[0].toLowerCase(Locale.ROOT);
                if (!foldedPaths.add(folded)) {
                    throw malformed(path, lineNumber, "case-folding path collision for " + fields[0]);
                }
                long size;
                try {
                    size = Long.parseLong(fields[1]);
                } catch (NumberFormatException exception) {
                    throw malformed(path, lineNumber, "invalid size " + fields[1], exception);
                }
                if (size < 0L || size > MAXIMUM_FILE_SIZE) {
                    throw malformed(path, lineNumber, "file size outside accepted range: " + size);
                }
                String digest = fields[2].toLowerCase(Locale.ROOT);
                if (!digest.matches("[0-9a-f]{64}")) {
                    throw malformed(path, lineNumber, "invalid SHA-256 " + fields[2]);
                }
                CorpusEntry entry = new CorpusEntry(fields[0], size, digest);
                if (entries.putIfAbsent(fields[0], entry) != null) {
                    throw malformed(path, lineNumber, "duplicate path " + fields[0]);
                }
                totalSize = Math.addExact(totalSize, size);
                if (totalSize > MAXIMUM_TOTAL_SIZE) {
                    throw malformed(path, lineNumber, "aggregate corpus size exceeds limit");
                }
            }
        } catch (ArithmeticException exception) {
            throw new IOException("Malformed " + path + ": aggregate size overflow", exception);
        }
        if (entries.isEmpty()) {
            throw new IOException("Malformed " + path + ": inventory is empty");
        }
        return Collections.unmodifiableMap(entries);
    }

    /// Validates one platform-independent inventory path.
    ///
    /// @param inventory inventory path
    /// @param lineNumber source line number
    /// @param value relative path
    private static void validateRelativePath(Path inventory, int lineNumber, String value) {
        if (value.isEmpty()
                || value.startsWith("/")
                || value.endsWith("/")
                || value.contains("\\")
                || value.contains(":")
                || value.indexOf('\0') >= 0) {
            throw malformed(inventory, lineNumber, "unsafe path " + value);
        }
        for (String segment : value.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw malformed(inventory, lineNumber, "unsafe path " + value);
            }
        }
    }

    /// Validates the configured source-archive root.
    ///
    /// @param value configured root
    /// @return root without a trailing slash
    private static String validateArchiveRoot(String value) {
        String root = value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
        if (root.isEmpty()
                || root.startsWith("/")
                || root.contains("\\")
                || root.contains(":")
                || root.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Invalid archive root: " + value);
        }
        for (String segment : root.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new IllegalArgumentException("Invalid archive root: " + value);
            }
        }
        return root;
    }

    /// Extracts validated ZIP entries into a prepared empty corpus directory.
    ///
    /// @param archivePath source ZIP
    /// @param archiveRoot validated archive root
    /// @param expected inventory entries
    /// @param outputRoot prepared corpus directory
    /// @throws IOException if any ZIP entry violates the inventory
    private static void extractArchive(
            Path archivePath,
            String archiveRoot,
            @Unmodifiable Map<String, CorpusEntry> expected,
            Path outputRoot
    ) throws IOException {
        String prefix = archiveRoot + '/';
        Set<String> extracted = new HashSet<>();
        try (ZipFile archive = new ZipFile(archivePath.toFile())) {
            Enumeration<? extends ZipEntry> entries = archive.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                String entryName = zipEntry.getName();
                validateZipEntryName(entryName);
                if (entryName.equals(archiveRoot + "/") && zipEntry.isDirectory()) {
                    continue;
                }
                if (!entryName.startsWith(prefix)) {
                    throw new IOException("ZIP entry lies outside expected root: " + entryName);
                }
                String relative = entryName.substring(prefix.length());
                if (zipEntry.isDirectory()) {
                    continue;
                }
                @Nullable CorpusEntry expectedEntry = expected.get(relative);
                if (expectedEntry == null) {
                    throw new IOException("ZIP contains an uninventoried file: " + entryName);
                }
                if (!extracted.add(relative)) {
                    throw new IOException("ZIP contains a duplicate file: " + entryName);
                }
                if (zipEntry.getSize() != expectedEntry.size()) {
                    throw new IOException(
                            "ZIP entry length mismatch for " + relative + ": expected "
                                    + expectedEntry.size() + " but found " + zipEntry.getSize()
                    );
                }
                Path output = outputRoot.resolve(relative).normalize();
                if (!output.startsWith(outputRoot)) {
                    throw new IOException("ZIP entry escapes output directory: " + entryName);
                }
                Files.createDirectories(Objects.requireNonNull(output.getParent()));
                String digest;
                try (InputStream input = archive.getInputStream(zipEntry);
                     OutputStream stream = Files.newOutputStream(output)) {
                    digest = copyAndHash(input, stream, expectedEntry.size(), relative);
                }
                if (!digest.equals(expectedEntry.sha256())) {
                    throw new IOException(
                            "SHA-256 mismatch for " + relative + ": expected "
                                    + expectedEntry.sha256() + " but found " + digest
                    );
                }
            }
        }
        if (extracted.size() != expected.size()) {
            for (String path : expected.keySet()) {
                if (!extracted.contains(path)) {
                    throw new IOException("ZIP is missing inventoried file: " + path);
                }
            }
            throw new IOException("ZIP inventory count mismatch");
        }
    }

    /// Rejects ambiguous or traversal-capable ZIP entry names.
    ///
    /// @param value ZIP entry name
    /// @throws IOException if the name is unsafe
    private static void validateZipEntryName(String value) throws IOException {
        if (value.isEmpty()
                || value.startsWith("/")
                || value.contains("\\")
                || value.contains(":")
                || value.indexOf('\0') >= 0) {
            throw new IOException("Unsafe ZIP entry name: " + value);
        }
        String withoutTrailingSlash = value.endsWith("/")
                ? value.substring(0, value.length() - 1)
                : value;
        for (String segment : withoutTrailingSlash.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new IOException("Unsafe ZIP entry name: " + value);
            }
        }
    }

    /// Copies exactly one expected file while computing SHA-256.
    ///
    /// @param input ZIP entry stream
    /// @param output destination stream
    /// @param expectedSize expected byte count
    /// @param label diagnostic label
    /// @return lower-case hexadecimal digest
    /// @throws IOException if the stream is truncated, longer than expected, or unreadable
    private static String copyAndHash(
            InputStream input,
            OutputStream output,
            long expectedSize,
            String label
    ) throws IOException {
        MessageDigest digest = newDigest();
        byte[] buffer = new byte[64 * 1024];
        long remaining = expectedSize;
        while (remaining > 0L) {
            int read = input.read(buffer, 0, (int) Math.min(buffer.length, remaining));
            if (read < 0) {
                throw new IOException("Truncated ZIP entry: " + label);
            }
            output.write(buffer, 0, read);
            digest.update(buffer, 0, read);
            remaining -= read;
        }
        if (input.read() >= 0) {
            throw new IOException("ZIP entry exceeds inventoried length: " + label);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /// Replaces the declared output directory with a completely verified tree.
    ///
    /// @param prepared verified sibling directory
    /// @param output final output directory
    /// @throws IOException if replacement fails
    private static void replaceDirectory(Path prepared, Path output) throws IOException {
        deleteRecursively(output);
        try {
            Files.move(prepared, output, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(prepared, output);
        }
    }

    /// Deletes one generated directory tree if it exists.
    ///
    /// @param root exact generated directory
    /// @throws IOException if a path cannot be removed
    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            /// Deletes each visited file.
            ///
            /// @param file visited file
            /// @param attributes file attributes
            /// @return visit continuation
            /// @throws IOException if deletion fails
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            /// Deletes a directory after all children have been removed.
            ///
            /// @param directory visited directory
            /// @param exception traversal failure, or `null`
            /// @return visit continuation
            /// @throws IOException if traversal or deletion failed
            @Override
            public FileVisitResult postVisitDirectory(
                    Path directory,
                    @Nullable IOException exception
            )
                    throws IOException {
                if (exception != null) {
                    throw exception;
                }
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /// Creates a SHA-256 digest instance.
    ///
    /// @return new digest
    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }

    /// Creates an inventory format exception.
    ///
    /// @param path inventory path
    /// @param lineNumber source line number
    /// @param detail failure detail
    /// @return format exception
    private static IllegalArgumentException malformed(Path path, int lineNumber, String detail) {
        return new IllegalArgumentException("Malformed " + path + " at line " + lineNumber + ": " + detail);
    }

    /// Creates an inventory format exception with a cause.
    ///
    /// @param path inventory path
    /// @param lineNumber source line number
    /// @param detail failure detail
    /// @param cause parsing cause
    /// @return format exception
    private static IllegalArgumentException malformed(
            Path path,
            int lineNumber,
            String detail,
            RuntimeException cause
    ) {
        return new IllegalArgumentException(
                "Malformed " + path + " at line " + lineNumber + ": " + detail,
                cause
        );
    }

    /// Stores one immutable inventory entry.
    ///
    /// @param path portable path relative to the corpus root
    /// @param size exact byte length
    /// @param sha256 lower-case SHA-256 digest
    @NotNullByDefault
    private record CorpusEntry(String path, long size, String sha256) {
        /// Creates an already validated entry.
        private CorpusEntry {
        }
    }
}
