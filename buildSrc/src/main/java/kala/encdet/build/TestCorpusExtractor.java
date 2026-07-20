// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.build;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/// Safely extracts and verifies the pinned chardet test corpus.
@NotNullByDefault
final class TestCorpusExtractor {
    /// Classpath directory containing extracted corpus files.
    private static final String OUTPUT_PREFIX = "chardet-test-data";

    /// Maximum accepted size of one corpus file.
    private static final long MAXIMUM_FILE_SIZE = 32L * 1024L * 1024L;

    /// Maximum accepted aggregate corpus size.
    private static final long MAXIMUM_TOTAL_SIZE = 64L * 1024L * 1024L;

    /// Maximum initial list capacity used for an externally supplied file count.
    private static final int MAXIMUM_INITIAL_ENTRY_CAPACITY = 4_096;

    /// Prevents instantiation.
    private TestCorpusExtractor() {
    }

    /// Extracts the exact corpus tree identified by fixed aggregate metadata.
    ///
    /// The tree digest is computed over entries sorted by portable path. Each
    /// entry contributes a four-byte big-endian UTF-8 path length, the path
    /// bytes, an eight-byte big-endian file length, and the file's 32-byte
    /// SHA-256 digest.
    ///
    /// @param archivePath fixed test-data ZIP
    /// @param archiveRoot exact root directory in the ZIP
    /// @param expectedFileCount exact regular-file count
    /// @param expectedTotalBytes exact aggregate uncompressed length
    /// @param expectedTreeSha256 expected canonical tree digest
    /// @param outputRoot generated test-resource root
    /// @throws IOException if validation, extraction, or installation fails
    static void extract(
            Path archivePath,
            String archiveRoot,
            int expectedFileCount,
            long expectedTotalBytes,
            String expectedTreeSha256,
            Path outputRoot
    ) throws IOException {
        if (expectedFileCount <= 0) {
            throw new IllegalArgumentException("expectedFileCount must be positive");
        }
        if (expectedTotalBytes < 0L || expectedTotalBytes > MAXIMUM_TOTAL_SIZE) {
            throw new IllegalArgumentException("expectedTotalBytes is outside the accepted range");
        }
        String expectedDigest = normalizeDigest(expectedTreeSha256);
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
            extractArchive(
                    archivePath,
                    normalizedRoot,
                    expectedFileCount,
                    expectedTotalBytes,
                    expectedDigest,
                    corpusRoot
            );
            replaceDirectory(prepared, absoluteOutput);
            installed = true;
        } finally {
            if (!installed) {
                deleteRecursively(prepared);
            }
        }
    }

    /// Collects, extracts, and verifies all regular ZIP entries below one root.
    ///
    /// @param archivePath source ZIP
    /// @param archiveRoot validated archive root
    /// @param expectedFileCount exact regular-file count
    /// @param expectedTotalBytes exact aggregate uncompressed length
    /// @param expectedTreeSha256 expected canonical tree digest
    /// @param outputRoot prepared corpus directory
    /// @throws IOException if any ZIP entry or aggregate identity differs
    private static void extractArchive(
            Path archivePath,
            String archiveRoot,
            int expectedFileCount,
            long expectedTotalBytes,
            String expectedTreeSha256,
            Path outputRoot
    ) throws IOException {
        try (ZipFile archive = new ZipFile(archivePath.toFile())) {
            @Unmodifiable List<ArchiveEntry> entries = collectEntries(
                    archive,
                    archiveRoot,
                    expectedFileCount,
                    expectedTotalBytes
            );
            MessageDigest treeDigest = newDigest();
            for (ArchiveEntry entry : entries) {
                Path output = outputRoot.resolve(entry.path()).normalize();
                if (!output.startsWith(outputRoot)) {
                    throw new IOException("ZIP entry escapes output directory: " + entry.path());
                }
                Files.createDirectories(output.getParent());
                String fileDigest;
                try (InputStream input = archive.getInputStream(entry.zipEntry());
                     OutputStream stream = Files.newOutputStream(output)) {
                    fileDigest = copyAndHash(input, stream, entry.size(), entry.path());
                }
                updateTreeDigest(treeDigest, entry.path(), entry.size(), fileDigest);
            }
            String actualTreeDigest = HexFormat.of().formatHex(treeDigest.digest());
            if (!actualTreeDigest.equals(expectedTreeSha256)) {
                throw new IOException(
                        "Corpus tree SHA-256 mismatch: expected " + expectedTreeSha256
                                + " but found " + actualTreeDigest
                );
            }
        }
    }

    /// Collects and validates every regular entry below the configured ZIP root.
    ///
    /// @param archive open ZIP archive
    /// @param archiveRoot validated archive root
    /// @param expectedFileCount exact regular-file count
    /// @param expectedTotalBytes exact aggregate uncompressed length
    /// @return immutable entries sorted by portable relative path
    /// @throws IOException if paths, lengths, or aggregate metadata differ
    private static @Unmodifiable List<ArchiveEntry> collectEntries(
            ZipFile archive,
            String archiveRoot,
            int expectedFileCount,
            long expectedTotalBytes
    ) throws IOException {
        String prefix = archiveRoot + '/';
        ArrayList<ArchiveEntry> entries = new ArrayList<>(
                Math.min(expectedFileCount, MAXIMUM_INITIAL_ENTRY_CAPACITY)
        );
        Set<String> exactPaths = new HashSet<>();
        Set<String> foldedPaths = new HashSet<>();
        long totalBytes = 0L;
        Enumeration<? extends ZipEntry> enumeration = archive.entries();
        try {
            while (enumeration.hasMoreElements()) {
                ZipEntry zipEntry = enumeration.nextElement();
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
                validateRelativePath(relative);
                if (!exactPaths.add(relative)) {
                    throw new IOException("ZIP contains a duplicate file: " + entryName);
                }
                String folded = relative.toLowerCase(Locale.ROOT);
                if (!foldedPaths.add(folded)) {
                    throw new IOException("ZIP contains a case-folding path collision: " + entryName);
                }
                long size = zipEntry.getSize();
                if (size < 0L || size > MAXIMUM_FILE_SIZE) {
                    throw new IOException("ZIP entry length is outside the accepted range: " + entryName);
                }
                totalBytes = Math.addExact(totalBytes, size);
                if (totalBytes > MAXIMUM_TOTAL_SIZE || entries.size() >= expectedFileCount) {
                    throw new IOException("ZIP corpus exceeds its expected aggregate bounds");
                }
                entries.add(new ArchiveEntry(relative, size, zipEntry));
            }
        } catch (ArithmeticException exception) {
            throw new IOException("ZIP corpus aggregate length overflow", exception);
        }
        if (entries.size() != expectedFileCount) {
            throw new IOException(
                    "ZIP corpus file count mismatch: expected " + expectedFileCount
                            + " but found " + entries.size()
            );
        }
        if (totalBytes != expectedTotalBytes) {
            throw new IOException(
                    "ZIP corpus length mismatch: expected " + expectedTotalBytes
                            + " but found " + totalBytes
            );
        }
        entries.sort(Comparator.comparing(ArchiveEntry::path));
        return List.copyOf(entries);
    }

    /// Validates one platform-independent path relative to the archive root.
    ///
    /// @param value relative path
    /// @throws IOException if the path is empty, ambiguous, or traversal-capable
    private static void validateRelativePath(String value) throws IOException {
        if (value.isEmpty()
                || value.startsWith("/")
                || value.endsWith("/")
                || value.contains("\\")
                || value.contains(":")
                || value.indexOf('\0') >= 0) {
            throw new IOException("Unsafe relative ZIP path: " + value);
        }
        for (String segment : value.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new IOException("Unsafe relative ZIP path: " + value);
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

    /// Copies exactly one file while computing SHA-256.
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
            throw new IOException("ZIP entry exceeds its declared length: " + label);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /// Adds one canonical file identity to the aggregate tree digest.
    ///
    /// @param digest aggregate digest
    /// @param path portable relative path
    /// @param size exact file length
    /// @param fileSha256 lower-case file digest
    private static void updateTreeDigest(
            MessageDigest digest,
            String path,
            long size,
            String fileSha256
    ) {
        byte[] pathBytes = path.getBytes(StandardCharsets.UTF_8);
        updateInt(digest, pathBytes.length);
        digest.update(pathBytes);
        updateLong(digest, size);
        digest.update(HexFormat.of().parseHex(fileSha256));
    }

    /// Adds one four-byte big-endian integer to a digest.
    ///
    /// @param digest destination digest
    /// @param value integer value
    private static void updateInt(MessageDigest digest, int value) {
        digest.update((byte) (value >>> 24));
        digest.update((byte) (value >>> 16));
        digest.update((byte) (value >>> 8));
        digest.update((byte) value);
    }

    /// Adds one eight-byte big-endian integer to a digest.
    ///
    /// @param digest destination digest
    /// @param value integer value
    private static void updateLong(MessageDigest digest, long value) {
        for (int shift = 56; shift >= 0; shift -= 8) {
            digest.update((byte) (value >>> shift));
        }
    }

    /// Validates and normalizes one SHA-256 string.
    ///
    /// @param value configured digest
    /// @return lower-case digest
    private static String normalizeDigest(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if (!normalized.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Invalid SHA-256 digest: " + value);
        }
        return normalized;
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
            ) throws IOException {
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

    /// Stores one validated ZIP file entry and its portable relative path.
    ///
    /// @param path portable path relative to the archive root
    /// @param size exact uncompressed byte length
    /// @param zipEntry archive entry handle valid while its ZIP remains open
    @NotNullByDefault
    private record ArchiveEntry(String path, long size, ZipEntry zipEntry) {
        /// Creates one validated archive entry descriptor.
        private ArchiveEntry {
        }
    }
}
