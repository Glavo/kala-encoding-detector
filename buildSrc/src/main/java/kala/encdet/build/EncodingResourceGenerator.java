// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.build;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.BufferedReader;
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
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/// Generates deterministic runtime resources from reviewable codec tables.
///
/// The generator does not consult Java charset providers. Its textual inputs
/// capture the pinned CPython 3.14.6 codec semantics used by the reference
/// implementation, while the three statistical resources are copied from the
/// fixed chardet source archive.
@NotNullByDefault
final class EncodingResourceGenerator {
    /// Runtime resource path below the generated resource root.
    private static final Path RESOURCE_PATH = Path.of("kala", "encdet", "internal");

    /// KDM1 single-byte decode resource magic.
    private static final int DECODE_MAGIC = 0x4b444d31;

    /// KVM1 multibyte validity resource magic.
    private static final int MULTIBYTE_MAGIC = 0x4b564d31;

    /// Expected single-byte table count.
    private static final int SINGLE_TABLE_COUNT = 64;

    /// Expected stateless multibyte table count.
    private static final int MULTIBYTE_TABLE_COUNT = 8;

    /// Number of byte values in a single-byte table.
    private static final int BYTE_VALUE_COUNT = 256;

    /// Number of bits in a complete two-byte domain.
    private static final int PAIR_BIT_COUNT = 65_536;

    /// Number of syntactically possible GB18030 four-byte pointers.
    private static final int GB18030_POINTER_COUNT = 1_587_600;

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
            "single-byte-decode.bin", "63912710247ec04e923f411d7022cfa3bbc3f3af2a5af9c3eaa3c601e65ff030",
            "validity.tsv", "f03213c64ec130fc5c00520f8a69753235438e79f64ad4690b0e13d5d8183509"
    );

    /// Prevents instantiation.
    private EncodingResourceGenerator() {
    }

    /// Creates all generated and extracted runtime resources.
    ///
    /// @param sourceArchive fixed chardet source ZIP
    /// @param archiveRoot exact root directory in the ZIP
    /// @param singleMappings reviewable single-byte mapping table
    /// @param multibyteRanges reviewable multibyte validity ranges
    /// @param hzRanges reviewable HZ pair ranges
    /// @param outputRoot generated resource root
    /// @throws IOException if an input cannot be read or an output cannot be written
    static void generate(
            Path sourceArchive,
            String archiveRoot,
            Path singleMappings,
            Path multibyteRanges,
            Path hzRanges,
            Path outputRoot
    ) throws IOException {
        Path resourceDirectory = outputRoot.resolve(RESOURCE_PATH);
        Files.createDirectories(resourceDirectory);

        extractUpstreamResources(sourceArchive, archiveRoot, resourceDirectory);
        @Unmodifiable List<SingleByteTable> singleTables = readSingleByteTables(singleMappings);
        writeSingleByteDecode(resourceDirectory.resolve("single-byte-decode.bin"), singleTables);
        writeSingleByteValidity(resourceDirectory.resolve("validity.tsv"), singleTables);

        @Unmodifiable List<MultibyteTable> multibyteTables = readMultibyteTables(multibyteRanges);
        writeMultibyteValidity(resourceDirectory.resolve("multibyte-validity.bin"), multibyteTables);
        writeHzValidity(resourceDirectory.resolve("hz-validity.bin"), hzRanges);

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

    /// Parses all deterministic single-byte decode tables.
    ///
    /// @param path textual table path
    /// @return immutable tables in file order
    /// @throws IOException if the table cannot be read
    private static @Unmodifiable List<SingleByteTable> readSingleByteTables(Path path)
            throws IOException {
        ArrayList<SingleByteTable> tables = new ArrayList<>();
        Map<String, Boolean> names = new LinkedHashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            @Nullable String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] fields = line.split("\\t", -1);
                if (fields.length != 2 || !isCodecName(fields[0])) {
                    throw malformed(path, lineNumber, "expected codec name and mapping list");
                }
                if (names.putIfAbsent(fields[0], Boolean.TRUE) != null) {
                    throw malformed(path, lineNumber, "duplicate codec " + fields[0]);
                }
                String[] values = fields[1].split(",", -1);
                if (values.length != BYTE_VALUE_COUNT) {
                    throw malformed(
                            path,
                            lineNumber,
                            "expected " + BYTE_VALUE_COUNT + " mappings but found " + values.length
                    );
                }
                int[] mappings = new int[BYTE_VALUE_COUNT];
                for (int byteValue = 0; byteValue < BYTE_VALUE_COUNT; byteValue++) {
                    String value = values[byteValue];
                    if (value.equals("-")) {
                        mappings[byteValue] = -1;
                        continue;
                    }
                    int codePoint = parseHex(path, lineNumber, value);
                    if (!Character.isValidCodePoint(codePoint)
                            || codePoint >= Character.MIN_SURROGATE
                            && codePoint <= Character.MAX_SURROGATE) {
                        throw malformed(path, lineNumber, "invalid Unicode scalar " + value);
                    }
                    mappings[byteValue] = codePoint;
                }
                tables.add(new SingleByteTable(fields[0], mappings));
            }
        }
        if (tables.size() != SINGLE_TABLE_COUNT) {
            throw new IOException(
                    "Malformed " + path + ": expected " + SINGLE_TABLE_COUNT
                            + " codecs but found " + tables.size()
            );
        }
        return List.copyOf(tables);
    }

    /// Writes the KDM1 byte-to-code-point resource.
    ///
    /// @param path output path
    /// @param tables ordered single-byte tables
    /// @throws IOException if the resource cannot be written
    private static void writeSingleByteDecode(Path path, @Unmodifiable List<SingleByteTable> tables)
            throws IOException {
        try (DataOutputStream output = new DataOutputStream(Files.newOutputStream(path))) {
            output.writeInt(DECODE_MAGIC);
            output.writeShort(tables.size());
            for (SingleByteTable table : tables) {
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
    private static void writeSingleByteValidity(Path path, @Unmodifiable List<SingleByteTable> tables)
            throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.US_ASCII)) {
            writeLine(
                    writer,
                    "# Generated from Python codecs used by chardet "
                            + "e3dfaa1c75256c9d2a06103b566ea92997844f70"
            );
            writeLine(writer, "# name\t256-bit-valid-byte-mask-little-bit-order");
            for (SingleByteTable table : tables) {
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

    /// Parses all deterministic multibyte validity masks.
    ///
    /// @param path textual range path
    /// @return immutable tables in first-appearance order
    /// @throws IOException if the table cannot be read
    private static @Unmodifiable List<MultibyteTable> readMultibyteTables(Path path)
            throws IOException {
        LinkedHashMap<String, MultibyteBuilder> builders = new LinkedHashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            @Nullable String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] fields = line.split("\\t", -1);
                if (fields.length != 4 || !isCodecName(fields[0])) {
                    throw malformed(path, lineNumber, "expected name, section, bit count, and ranges");
                }
                int bitCount;
                try {
                    bitCount = Integer.parseInt(fields[2]);
                } catch (NumberFormatException exception) {
                    throw malformed(path, lineNumber, "invalid bit count " + fields[2], exception);
                }
                byte[] mask = parseRanges(path, lineNumber, bitCount, fields[3]);
                MultibyteBuilder builder = builders.computeIfAbsent(fields[0], MultibyteBuilder::new);
                builder.add(path, lineNumber, fields[1], bitCount, mask);
            }
        }
        if (builders.size() != MULTIBYTE_TABLE_COUNT) {
            throw new IOException(
                    "Malformed " + path + ": expected " + MULTIBYTE_TABLE_COUNT
                            + " codecs but found " + builders.size()
            );
        }
        ArrayList<MultibyteTable> tables = new ArrayList<>(builders.size());
        for (MultibyteBuilder builder : builders.values()) {
            tables.add(builder.build(path));
        }
        return List.copyOf(tables);
    }

    /// Writes the KVM1 stateless multibyte validity resource.
    ///
    /// @param path output path
    /// @param tables ordered multibyte tables
    /// @throws IOException if the resource cannot be written
    private static void writeMultibyteValidity(Path path, @Unmodifiable List<MultibyteTable> tables)
            throws IOException {
        try (DataOutputStream output = new DataOutputStream(Files.newOutputStream(path))) {
            output.writeInt(MULTIBYTE_MAGIC);
            output.writeShort(tables.size());
            for (MultibyteTable table : tables) {
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

    /// Expands and writes the fixed-size HZ shifted-pair mask.
    ///
    /// @param outputPath output path
    /// @param rangePath textual range path
    /// @throws IOException if input parsing or output writing fails
    private static void writeHzValidity(Path outputPath, Path rangePath) throws IOException {
        byte @Nullable [] mask = null;
        try (BufferedReader reader = Files.newBufferedReader(rangePath, StandardCharsets.UTF_8)) {
            @Nullable String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                if (mask != null) {
                    throw malformed(rangePath, lineNumber, "expected exactly one HZ range row");
                }
                String[] fields = line.split("\\t", -1);
                if (fields.length != 4
                        || !fields[0].equals("hz")
                        || !fields[1].equals("pairs")
                        || !fields[2].equals(Integer.toString(PAIR_BIT_COUNT))) {
                    throw malformed(rangePath, lineNumber, "invalid HZ range row");
                }
                mask = parseRanges(rangePath, lineNumber, PAIR_BIT_COUNT, fields[3]);
            }
        }
        if (mask == null) {
            throw new IOException("Malformed " + rangePath + ": missing HZ range row");
        }
        Files.write(outputPath, mask);
    }

    /// Parses sorted, non-overlapping inclusive hexadecimal ranges.
    ///
    /// @param path input path
    /// @param lineNumber source line number
    /// @param bitCount number of meaningful bits
    /// @param encoded comma-separated ranges
    /// @return packed little-bit-order mask
    private static byte[] parseRanges(
            Path path,
            int lineNumber,
            int bitCount,
            String encoded
    ) {
        if (bitCount <= 0 || bitCount > GB18030_POINTER_COUNT) {
            throw malformed(path, lineNumber, "invalid bit count " + bitCount);
        }
        byte[] mask = new byte[(bitCount + 7) >>> 3];
        int previousEnd = -1;
        if (encoded.isEmpty()) {
            return mask;
        }
        for (String range : encoded.split(",", -1)) {
            int separator = range.indexOf('-');
            int start = parseHex(path, lineNumber, separator < 0 ? range : range.substring(0, separator));
            int end = parseHex(path, lineNumber, separator < 0 ? range : range.substring(separator + 1));
            if (start > end || start <= previousEnd || end >= bitCount) {
                throw malformed(path, lineNumber, "invalid or overlapping range " + range);
            }
            for (int value = start; value <= end; value++) {
                setBit(mask, value);
            }
            previousEnd = end;
        }
        return mask;
    }

    /// Parses one nonempty hexadecimal integer.
    ///
    /// @param path input path
    /// @param lineNumber source line number
    /// @param value encoded integer
    /// @return parsed value
    private static int parseHex(Path path, int lineNumber, String value) {
        if (value.isEmpty()) {
            throw malformed(path, lineNumber, "empty hexadecimal value");
        }
        try {
            return Integer.parseInt(value, 16);
        } catch (NumberFormatException exception) {
            throw malformed(path, lineNumber, "invalid hexadecimal value " + value, exception);
        }
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

    /// Tests the restricted codec-name syntax used by generated resources.
    ///
    /// @param value candidate name
    /// @return whether the name contains only portable ASCII characters
    private static boolean isCodecName(String value) {
        return !value.isEmpty() && value.matches("[a-z0-9_-]+");
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

    /// Creates a table-format exception.
    ///
    /// @param path input path
    /// @param lineNumber source line number
    /// @param detail failure detail
    /// @return format exception
    private static IllegalArgumentException malformed(Path path, int lineNumber, String detail) {
        return new IllegalArgumentException("Malformed " + path + " at line " + lineNumber + ": " + detail);
    }

    /// Creates a table-format exception with a cause.
    ///
    /// @param path input path
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

    /// Stores one immutable single-byte mapping table.
    ///
    /// @param name canonical codec name
    /// @param mappings byte-to-code-point mappings, with `-1` for undefined bytes
    @NotNullByDefault
    private record SingleByteTable(String name, int @Unmodifiable [] mappings) {
        /// Creates a table whose mapping array is owned by the generator.
        private SingleByteTable {
        }
    }

    /// Accumulates the named sections of one multibyte table.
    @NotNullByDefault
    private static final class MultibyteBuilder {
        /// Canonical codec name.
        private final String name;

        /// Standalone-byte mask, or `null` until read.
        private byte @Nullable [] singles;

        /// Complete two-byte input mask, or `null` until read.
        private byte @Nullable [] pairs;

        /// Optional extra-sequence mask.
        private byte @Nullable [] extra;

        /// Number of meaningful extra-mask bits.
        private int extraBitCount;

        /// Creates an empty table builder.
        ///
        /// @param name canonical codec name
        private MultibyteBuilder(String name) {
            this.name = name;
        }

        /// Adds one unique named mask section.
        ///
        /// @param path input path
        /// @param lineNumber source line number
        /// @param section section name
        /// @param bitCount meaningful mask bits
        /// @param mask packed mask
        private void add(
                Path path,
                int lineNumber,
                String section,
                int bitCount,
                byte[] mask
        ) {
            switch (section) {
                case "singles" -> {
                    if (bitCount != BYTE_VALUE_COUNT || singles != null) {
                        throw malformed(path, lineNumber, "invalid or duplicate singles section");
                    }
                    singles = mask;
                }
                case "pairs" -> {
                    if (bitCount != PAIR_BIT_COUNT || pairs != null) {
                        throw malformed(path, lineNumber, "invalid or duplicate pairs section");
                    }
                    pairs = mask;
                }
                case "extra" -> {
                    if (extra != null) {
                        throw malformed(path, lineNumber, "duplicate extra section");
                    }
                    extra = mask;
                    extraBitCount = bitCount;
                }
                default -> throw malformed(path, lineNumber, "unknown section " + section);
            }
        }

        /// Validates and freezes the accumulated table.
        ///
        /// @param path input path for diagnostics
        /// @return immutable table
        private MultibyteTable build(Path path) {
            if (singles == null || pairs == null) {
                throw new IllegalArgumentException("Malformed " + path + ": incomplete table " + name);
            }
            int kind;
            if (name.equals("euc_jis_2004")) {
                kind = 1;
                if (extra == null || extraBitCount != PAIR_BIT_COUNT) {
                    throw new IllegalArgumentException("Malformed " + path + ": invalid EUC-JIS-2004 extra table");
                }
            } else if (name.equals("gb18030")) {
                kind = 2;
                if (extra == null || extraBitCount != GB18030_POINTER_COUNT) {
                    throw new IllegalArgumentException("Malformed " + path + ": invalid GB18030 extra table");
                }
            } else {
                kind = 0;
                if (extra != null) {
                    throw new IllegalArgumentException("Malformed " + path + ": unexpected extra table for " + name);
                }
                extra = new byte[0];
                extraBitCount = 0;
            }
            return new MultibyteTable(name, singles, pairs, kind, extra, extraBitCount);
        }
    }

    /// Stores one immutable packed multibyte validity table.
    ///
    /// @param name canonical codec name
    /// @param singles standalone-byte mask
    /// @param pairs complete two-byte input mask
    /// @param kind extra-table format discriminator
    /// @param extra optional sequence mask, empty for kind zero
    /// @param extraBitCount number of meaningful extra bits
    @NotNullByDefault
    private record MultibyteTable(
            String name,
            byte @Unmodifiable [] singles,
            byte @Unmodifiable [] pairs,
            int kind,
            byte @Unmodifiable [] extra,
            int extraBitCount
    ) {
        /// Creates a table whose arrays are owned by the generator.
        private MultibyteTable {
        }
    }
}
