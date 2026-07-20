// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.build;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/// Generates deterministic runtime resources from pinned source archives.
///
/// The generator does not consult Java charset providers. It parses the codec
/// and alias definitions in the fixed CPython source archive and the registry
/// in the fixed chardet archive. It also copies the latter's three statistical
/// resources without modification.
@NotNullByDefault
final class EncodingResourceGenerator {
    /// Runtime resource path below the generated resource root.
    private static final Path RESOURCE_PATH = Path.of("kala", "encdet", "internal");

    /// KDM1 single-byte decode resource magic.
    private static final int DECODE_MAGIC = 0x4b444d31;

    /// KVM1 multibyte validity resource magic.
    private static final int MULTIBYTE_MAGIC = 0x4b564d31;

    /// Number of byte values in a single-byte table.
    private static final int BYTE_VALUE_COUNT = 256;

    /// Expected upstream model resource digests.
    private static final @Unmodifiable Map<String, String> UPSTREAM_HASHES = Map.of(
            "confusion.bin", "1c853cd04b400bd2d3772fcaddc4466b32d32db703a7aa415df5a20810bf9ecb",
            "idf.bin", "8306e28aad48cac54db7834d09f49cbbb92d3928ede53c71f2b9b232b586bb8a",
            "models.bin", "07eb1dabcf4f8e714f9f866eaa355121bf7b3563dcde0d77ed7c3668de5f75f5"
    );

    /// Expected generated resource digests.
    private static final @Unmodifiable Map<String, String> GENERATED_HASHES = Map.of(
            "hz-validity.bin", "313e548cab6a250d0ae374c9b029475dbe9248632f4940f8aa95f30d7a16c3e2",
            "multibyte-validity.bin", "cabce96fd96e6bba5fff346a9d6c34bd9a0550f89c91be2a3c7f68ad364cf804",
            "registry.tsv", "75d70081930d8ca2960debaf642fbd76d1b3e01534cc3e6254983780426bcc2d",
            "single-byte-decode.bin", "63912710247ec04e923f411d7022cfa3bbc3f3af2a5af9c3eaa3c601e65ff030",
            "validity.tsv", "f03213c64ec130fc5c00520f8a69753235438e79f64ad4690b0e13d5d8183509"
    );

    /// Prevents instantiation.
    private EncodingResourceGenerator() {
    }

    /// Creates all generated and extracted runtime resources.
    ///
    /// @param chardetArchive fixed chardet source ZIP
    /// @param chardetRoot exact root directory in the chardet ZIP
    /// @param cpythonArchive fixed CPython source ZIP
    /// @param cpythonRoot exact root directory in the CPython ZIP
    /// @param outputRoot generated resource root
    /// @throws IOException if an input cannot be read or an output cannot be written
    static void generate(
            Path chardetArchive,
            String chardetRoot,
            Path cpythonArchive,
            String cpythonRoot,
            Path outputRoot
    ) throws IOException {
        Path resourceDirectory = outputRoot.resolve(RESOURCE_PATH);
        Files.createDirectories(resourceDirectory);

        extractUpstreamResources(chardetArchive, chardetRoot, resourceDirectory);
        @Unmodifiable List<UpstreamSourceParser.RegistryEntry> registry =
                UpstreamSourceParser.readRegistry(
                        chardetArchive,
                        chardetRoot,
                        cpythonArchive,
                        cpythonRoot
                );
        writeRegistry(resourceDirectory.resolve("registry.tsv"), registry);
        @Unmodifiable List<UpstreamSourceParser.SingleByteTable> singleTables =
                UpstreamSourceParser.readSingleByteTables(cpythonArchive, cpythonRoot, registry);
        writeSingleByteDecode(resourceDirectory.resolve("single-byte-decode.bin"), singleTables);
        writeSingleByteValidity(resourceDirectory.resolve("validity.tsv"), singleTables);

        CpythonMultibyteTables.Result multibyteTables =
                CpythonMultibyteTables.read(cpythonArchive, cpythonRoot);
        writeMultibyteValidity(
                resourceDirectory.resolve("multibyte-validity.bin"),
                multibyteTables.tables()
        );
        writeHzValidity(resourceDirectory.resolve("hz-validity.bin"), multibyteTables.hzPairs());

        verifyResources(resourceDirectory, UPSTREAM_HASHES);
        verifyResources(resourceDirectory, GENERATED_HASHES);
    }

    /// Extracts the three pinned statistical resources from the source ZIP.
    ///
    /// @param sourceArchive source ZIP
    /// @param archiveRoot exact ZIP root directory
    /// @param outputDirectory runtime resource directory
    /// @throws IOException if the archive is malformed or extraction fails
    private static void extractUpstreamResources(
            Path sourceArchive,
            String archiveRoot,
            Path outputDirectory
    ) throws IOException {
        String normalizedRoot = normalizeArchiveRoot(archiveRoot);
        try (ZipFile archive = new ZipFile(sourceArchive.toFile())) {
            for (Map.Entry<String, String> expected : UPSTREAM_HASHES.entrySet()) {
                String entryName = normalizedRoot + "/src/chardet/models/" + expected.getKey();
                @Nullable ZipEntry entry = archive.getEntry(entryName);
                if (entry == null || entry.isDirectory()) {
                    throw new IOException("Missing chardet source resource: " + entryName);
                }
                if (entry.getSize() < 0L || entry.getSize() > 1_000_000L) {
                    throw new IOException("Invalid chardet source resource length: " + entryName);
                }
                Path output = outputDirectory.resolve(expected.getKey());
                try (InputStream input = archive.getInputStream(entry);
                     OutputStream stream = Files.newOutputStream(output)) {
                    copyExactly(input, stream, entry.getSize(), entryName);
                }
                requireHash(output, expected.getValue());
            }
        }
    }

    /// Normalizes and validates the expected ZIP root directory.
    ///
    /// @param root configured root directory
    /// @return root without a trailing slash
    private static String normalizeArchiveRoot(String root) {
        String normalized = root.endsWith("/") ? root.substring(0, root.length() - 1) : root;
        if (normalized.isEmpty()
                || normalized.startsWith("/")
                || normalized.contains("\\")
                || normalized.contains("..")
                || normalized.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Invalid archive root: " + root);
        }
        return normalized;
    }

    /// Copies exactly the declared number of bytes and rejects trailing data.
    ///
    /// @param input source stream
    /// @param output destination stream
    /// @param expectedLength exact byte count
    /// @param label diagnostic label
    /// @throws IOException if the stream is truncated, longer than declared, or unreadable
    private static void copyExactly(
            InputStream input,
            OutputStream output,
            long expectedLength,
            String label
    ) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        long remaining = expectedLength;
        while (remaining > 0L) {
            int read = input.read(buffer, 0, (int) Math.min(buffer.length, remaining));
            if (read < 0) {
                throw new IOException("Truncated ZIP entry: " + label);
            }
            output.write(buffer, 0, read);
            remaining -= read;
        }
        if (input.read() >= 0) {
            throw new IOException("ZIP entry exceeds its declared length: " + label);
        }
    }

    /// Writes the augmented ordered encoding registry as deterministic UTF-8 TSV.
    ///
    /// @param path output path
    /// @param entries ordered registry entries
    /// @throws IOException if the resource cannot be written
    private static void writeRegistry(
            Path path,
            @Unmodifiable List<UpstreamSourceParser.RegistryEntry> entries
    ) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writeLine(
                    writer,
                    "# Generated from chardet e3dfaa1c75256c9d2a06103b566ea92997844f70"
            );
            writeLine(writer, "# name\tera\tmultibyte\tlanguages\taliases");
            for (UpstreamSourceParser.RegistryEntry entry : entries) {
                writeLine(
                        writer,
                        entry.name()
                                + '\t' + entry.era()
                                + '\t' + entry.multibyte()
                                + '\t' + String.join(",", entry.languages())
                                + '\t' + String.join("\u001f", entry.aliases())
                );
            }
        }
    }

    /// Writes the KDM1 byte-to-code-point resource.
    ///
    /// @param path output path
    /// @param tables ordered single-byte tables
    /// @throws IOException if the resource cannot be written
    private static void writeSingleByteDecode(
            Path path,
            @Unmodifiable List<UpstreamSourceParser.SingleByteTable> tables
    )
            throws IOException {
        try (DataOutputStream output = new DataOutputStream(Files.newOutputStream(path))) {
            output.writeInt(DECODE_MAGIC);
            output.writeShort(tables.size());
            for (UpstreamSourceParser.SingleByteTable table : tables) {
                writeName(output, table.name());
                for (int codePoint : table.mappings()) {
                    output.writeInt(codePoint);
                }
            }
        }
    }

    /// Writes strict single-byte masks as deterministic ASCII TSV.
    ///
    /// @param path output path
    /// @param tables ordered single-byte tables
    /// @throws IOException if the resource cannot be written
    private static void writeSingleByteValidity(
            Path path,
            @Unmodifiable List<UpstreamSourceParser.SingleByteTable> tables
    )
            throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.US_ASCII)) {
            writeLine(
                    writer,
                    "# Generated from Python codecs used by chardet "
                            + "e3dfaa1c75256c9d2a06103b566ea92997844f70"
            );
            writeLine(writer, "# name\t256-bit-valid-byte-mask-little-bit-order");
            for (UpstreamSourceParser.SingleByteTable table : tables) {
                byte[] mask = new byte[BYTE_VALUE_COUNT / Byte.SIZE];
                for (int byteValue = 0; byteValue < BYTE_VALUE_COUNT; byteValue++) {
                    if (table.mappings()[byteValue] >= 0) {
                        setBit(mask, byteValue);
                    }
                }
                writeLine(writer, table.name() + '\t' + HexFormat.of().formatHex(mask));
            }
        }
    }

    /// Writes the KVM1 stateless multibyte validity resource.
    ///
    /// @param path output path
    /// @param tables ordered multibyte tables
    /// @throws IOException if the resource cannot be written
    private static void writeMultibyteValidity(
            Path path,
            @Unmodifiable List<CpythonMultibyteTables.MultibyteTable> tables
    )
            throws IOException {
        try (DataOutputStream output = new DataOutputStream(Files.newOutputStream(path))) {
            output.writeInt(MULTIBYTE_MAGIC);
            output.writeShort(tables.size());
            for (CpythonMultibyteTables.MultibyteTable table : tables) {
                writeName(output, table.name());
                output.write(table.singles());
                output.write(table.pairs());
                output.writeByte(table.kind());
                if (table.kind() == 1) {
                    output.write(table.extra());
                } else if (table.kind() == 2) {
                    output.writeInt(table.extraBitCount());
                    output.write(table.extra());
                }
            }
        }
    }

    /// Writes the fixed-size HZ shifted-pair mask.
    ///
    /// @param outputPath output path
    /// @param mask generated HZ pair mask
    /// @throws IOException if the output cannot be written
    private static void writeHzValidity(Path outputPath, byte @Unmodifiable [] mask)
            throws IOException {
        Files.write(outputPath, mask);
    }

    /// Sets one bit using the runtime's little-bit-order packing.
    ///
    /// @param mask packed mask
    /// @param value bit index
    private static void setBit(byte[] mask, int value) {
        mask[value >>> 3] |= (byte) (1 << (value & 7));
    }

    /// Writes a one-byte-length ASCII codec name.
    ///
    /// @param output binary output
    /// @param name codec name
    /// @throws IOException if the name cannot be written
    private static void writeName(DataOutputStream output, String name) throws IOException {
        byte[] encoded = name.getBytes(StandardCharsets.US_ASCII);
        if (encoded.length == 0 || encoded.length > 255) {
            throw new IOException("Invalid codec name length: " + name);
        }
        output.writeByte(encoded.length);
        output.write(encoded);
    }

    /// Writes one line using an explicit LF terminator.
    ///
    /// @param writer destination writer
    /// @param line line contents
    /// @throws IOException if writing fails
    private static void writeLine(BufferedWriter writer, String line) throws IOException {
        writer.write(line);
        writer.write('\n');
    }

    /// Verifies every resource in one expected digest map.
    ///
    /// @param directory resource directory
    /// @param expected expected leaf-name digests
    /// @throws IOException if a resource is missing or differs
    private static void verifyResources(
            Path directory,
            @Unmodifiable Map<String, String> expected
    )
            throws IOException {
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            requireHash(directory.resolve(entry.getKey()), entry.getValue());
        }
    }

    /// Requires a file to have one exact SHA-256 digest.
    ///
    /// @param path file to verify
    /// @param expected expected lower-case digest
    /// @throws IOException if the file cannot be read or differs
    private static void requireHash(Path path, String expected) throws IOException {
        String actual = sha256(path);
        if (!actual.equals(expected)) {
            throw new IOException(
                    "SHA-256 mismatch for " + path + ": expected " + expected + " but found " + actual
            );
        }
    }

    /// Computes a lower-case SHA-256 file digest.
    ///
    /// @param path file to hash
    /// @return lower-case hexadecimal digest
    /// @throws IOException if the file cannot be read
    private static String sha256(Path path) throws IOException {
        MessageDigest digest = newDigest();
        try (InputStream input = new DigestInputStream(Files.newInputStream(path), digest)) {
            input.transferTo(OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
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
}
