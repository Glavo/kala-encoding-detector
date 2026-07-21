// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.build;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/// Reads CPython's generated CJK codec maps and reproduces strict multibyte validity tables.
///
/// This parser understands only the generated mapping declarations and the small Johab lookup
/// arrays used by the pinned CPython source. Codec validity rules are direct Java translations of
/// the corresponding CPython decoder branches; no Python runtime or Java charset provider is used.
@NotNullByDefault
final class CpythonMultibyteTables {
    /// Maximum accepted uncompressed size of one CPython source entry.
    private static final int MAX_SOURCE_ENTRY_BYTES = 2_000_000;

    /// Number of possible byte values.
    private static final int BYTE_VALUE_COUNT = 256;

    /// Number of possible ordered byte pairs.
    private static final int PAIR_BIT_COUNT = 65_536;

    /// Number of syntactically possible GB18030 four-byte pointers.
    private static final int GB18030_POINTER_COUNT = 126 * 10 * 126 * 10;

    /// Table kind without an extra sequence mask.
    private static final int KIND_NONE = 0;

    /// Table kind whose extra mask indexes the final two bytes after an EUC `0x8f` lead.
    private static final int KIND_EUC_JIS_2004 = 1;

    /// Table kind whose extra mask indexes GB18030 four-byte pointers.
    private static final int KIND_GB18030 = 2;

    /// CPython mapping directory below the archive root.
    private static final String CJK_DIRECTORY = "Modules/cjkcodecs/";

    /// Prevents instantiation.
    private CpythonMultibyteTables() {
    }

    /// Reads the pinned CPython source archive and creates all multibyte and HZ masks.
    ///
    /// Returned tables are ordered as required by the KVM1 runtime resource. All arrays are newly
    /// allocated and owned by the returned result.
    ///
    /// @param cpythonArchive CPython source ZIP
    /// @param archiveRoot    exact root directory expected in the ZIP
    /// @return immutable generated table collection and HZ pair mask
    /// @throws IOException if a required entry is missing or malformed
    static Result read(Path cpythonArchive, String archiveRoot) throws IOException {
        String root = normalizeArchiveRoot(archiveRoot);
        try (ZipFile archive = new ZipFile(cpythonArchive.toFile())) {
            String mappingsCn = readSource(archive, root, CJK_DIRECTORY + "mappings_cn.h");
            String mappingsHk = readSource(archive, root, CJK_DIRECTORY + "mappings_hk.h");
            String mappingsJp = readSource(archive, root, CJK_DIRECTORY + "mappings_jp.h");
            String mappingsJisPair = readSource(
                    archive,
                    root,
                    CJK_DIRECTORY + "mappings_jisx0213_pair.h"
            );
            String mappingsKr = readSource(archive, root, CJK_DIRECTORY + "mappings_kr.h");
            String mappingsTw = readSource(archive, root, CJK_DIRECTORY + "mappings_tw.h");
            String codecsKr = readSource(archive, root, CJK_DIRECTORY + "_codecs_kr.c");

            DecodeMap gb2312 = parseDecodeMap(mappingsCn, "gb2312");
            DecodeMap gbkExtension = parseDecodeMap(mappingsCn, "gbkext");
            DecodeMap gb18030Extension = parseDecodeMap(mappingsCn, "gb18030ext");
            DecodeMap big5Hkscs = parseDecodeMap(mappingsHk, "big5hkscs");
            DecodeMap jisX0208 = parseDecodeMap(mappingsJp, "jisx0208");
            DecodeMap jisX0212 = parseDecodeMap(mappingsJp, "jisx0212");
            DecodeMap cp932Extension = parseDecodeMap(mappingsJp, "cp932ext");
            DecodeMap jisX0213Plane1Bmp = parseDecodeMap(mappingsJp, "jisx0213_1_bmp");
            DecodeMap jisX0213Plane2Bmp = parseDecodeMap(mappingsJp, "jisx0213_2_bmp");
            DecodeMap jisX0213Plane1Emp = parseDecodeMap(mappingsJp, "jisx0213_1_emp");
            DecodeMap jisX0213Plane2Emp = parseDecodeMap(mappingsJp, "jisx0213_2_emp");
            DecodeMap jisX0213Pair = parseDecodeMap(mappingsJisPair, "jisx0213_pair");
            DecodeMap ksX1001 = parseDecodeMap(mappingsKr, "ksx1001");
            DecodeMap cp949Extension = parseDecodeMap(mappingsKr, "cp949ext");
            DecodeMap big5 = parseDecodeMap(mappingsTw, "big5");
            JohabIndexes johab = parseJohabIndexes(codecsKr);

            BitSet asciiSingles = asciiSingles();
            BitSet cp932Singles = cp932Singles();
            BitSet shiftJis2004Singles = shiftJis2004Singles();

            byte[] eucJisExtra = createEucJis2004Extra(
                    jisX0212,
                    jisX0213Plane2Bmp,
                    jisX0213Plane2Emp
            );
            byte[] gb18030Extra = createGb18030Extra();

            List<MultibyteTable> tables = List.of(
                    createTable(
                            "big5hkscs",
                            asciiSingles,
                            (first, second) -> acceptsBig5Hkscs(
                                    first,
                                    second,
                                    big5,
                                    big5Hkscs
                            ),
                            KIND_NONE,
                            new byte[0],
                            0
                    ),
                    createTable(
                            "cp932",
                            cp932Singles,
                            (first, second) -> acceptsCp932Pair(
                                    first,
                                    second,
                                    cp932Extension,
                                    jisX0208
                            ),
                            KIND_NONE,
                            new byte[0],
                            0
                    ),
                    createTable(
                            "cp949",
                            asciiSingles,
                            (first, second) -> acceptsCp949Pair(
                                    first,
                                    second,
                                    ksX1001,
                                    cp949Extension
                            ),
                            KIND_NONE,
                            new byte[0],
                            0
                    ),
                    createTable(
                            "euc_jis_2004",
                            asciiSingles,
                            (first, second) -> acceptsEucJis2004Pair(
                                    first,
                                    second,
                                    jisX0208,
                                    jisX0213Plane1Bmp,
                                    jisX0213Plane1Emp,
                                    jisX0213Pair
                            ),
                            KIND_EUC_JIS_2004,
                            eucJisExtra,
                            PAIR_BIT_COUNT
                    ),
                    createTable(
                            "euc_kr",
                            asciiSingles,
                            (first, second) -> acceptsEucKrPair(first, second, ksX1001),
                            KIND_NONE,
                            new byte[0],
                            0
                    ),
                    createTable(
                            "gb18030",
                            asciiSingles,
                            (first, second) -> acceptsGb18030Pair(
                                    first,
                                    second,
                                    gb2312,
                                    gbkExtension,
                                    gb18030Extension
                            ),
                            KIND_GB18030,
                            gb18030Extra,
                            GB18030_POINTER_COUNT
                    ),
                    createTable(
                            "shift_jis_2004",
                            shiftJis2004Singles,
                            (first, second) -> acceptsShiftJis2004Pair(
                                    first,
                                    second,
                                    jisX0208,
                                    jisX0213Plane1Bmp,
                                    jisX0213Plane2Bmp,
                                    jisX0213Plane1Emp,
                                    jisX0213Plane2Emp,
                                    jisX0213Pair
                            ),
                            KIND_NONE,
                            new byte[0],
                            0
                    ),
                    createTable(
                            "johab",
                            asciiSingles,
                            (first, second) -> acceptsJohabPair(
                                    first,
                                    second,
                                    johab,
                                    ksX1001
                            ),
                            KIND_NONE,
                            new byte[0],
                            0
                    )
            );
            return new Result(tables, toMask(gb2312.pairs(), PAIR_BIT_COUNT));
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

    /// Reads one bounded UTF-8 source entry from the archive.
    ///
    /// @param archive      open source archive
    /// @param root         normalized archive root
    /// @param relativePath source path below the root
    /// @return decoded source text
    /// @throws IOException if the entry is absent, oversized, truncated, or malformed UTF-8
    private static String readSource(ZipFile archive, String root, String relativePath)
            throws IOException {
        String entryName = root + '/' + relativePath;
        @Nullable ZipEntry entry = archive.getEntry(entryName);
        if (entry == null || entry.isDirectory()) {
            throw new IOException("Missing CPython source entry: " + entryName);
        }
        long declaredSize = entry.getSize();
        if (declaredSize < 0L || declaredSize > MAX_SOURCE_ENTRY_BYTES) {
            throw new IOException("Invalid CPython source entry length: " + entryName);
        }
        int expectedSize = (int) declaredSize;
        byte[] bytes;
        try (InputStream input = archive.getInputStream(entry)) {
            bytes = input.readNBytes(expectedSize + 1);
        }
        if (bytes.length != expectedSize) {
            throw new IOException(
                    "CPython source entry length mismatch for " + entryName
                            + ": expected " + expectedSize + " but read " + bytes.length
            );
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new IOException("Malformed UTF-8 CPython source entry: " + entryName, exception);
        }
    }

    /// Parses one generated two-level CPython decode map.
    ///
    /// @param source mapping header source
    /// @param name   mapping stem
    /// @return valid byte pairs represented by the map
    /// @throws IOException if either generated declaration is malformed
    private static DecodeMap parseDecodeMap(String source, String name) throws IOException {
        String quotedName = Pattern.quote(name);
        Pattern valuesDeclaration = Pattern.compile(
                "\\b__" + quotedName + "_decmap\\s*\\[\\s*([0-9]+)\\s*]\\s*=\\s*\\{"
        );
        Matcher valuesMatcher = valuesDeclaration.matcher(source);
        if (!valuesMatcher.find()) {
            throw malformed("missing value array __" + name + "_decmap");
        }
        int declaredValueCount = parseDecimal(valuesMatcher.group(1), name + " value count");
        String valuesBody = extractInitializer(source, valuesMatcher.end() - 1, name + " values");
        List<String> values = splitCommaTokens(valuesBody, name + " values");
        if (values.size() != declaredValueCount) {
            throw malformed(
                    name + " value count is " + values.size() + " instead of " + declaredValueCount
            );
        }
        boolean[] validValues = new boolean[values.size()];
        for (int index = 0; index < values.size(); index++) {
            String token = values.get(index);
            if (!token.equals("U")) {
                parseCInteger(token, name + " value " + index);
                validValues[index] = true;
            }
        }

        Pattern indexesDeclaration = Pattern.compile(
                "\\b" + quotedName + "_decmap\\s*\\[\\s*256\\s*]\\s*=\\s*\\{"
        );
        Matcher indexesMatcher = indexesDeclaration.matcher(source);
        if (!indexesMatcher.find()) {
            throw malformed("missing index array " + name + "_decmap");
        }
        String indexesBody = extractInitializer(source, indexesMatcher.end() - 1, name + " indexes");
        List<String> indexes = splitStructInitializers(indexesBody, name + " indexes");
        if (indexes.size() != BYTE_VALUE_COUNT) {
            throw malformed(name + " index count is " + indexes.size() + " instead of 256");
        }

        Pattern pointerPattern = Pattern.compile(
                "__" + quotedName + "_decmap(?:\\s*\\+\\s*([0-9]+))?"
        );
        BitSet pairs = new BitSet(PAIR_BIT_COUNT);
        for (int first = 0; first < indexes.size(); first++) {
            List<String> fields = splitCommaTokens(indexes.get(first), name + " index " + first);
            if (fields.size() != 3) {
                throw malformed(name + " index " + first + " does not contain three fields");
            }
            if (fields.get(0).equals("0")) {
                if (!fields.get(1).equals("0") || !fields.get(2).equals("0")) {
                    throw malformed(name + " null index " + first + " has nonzero bounds");
                }
                continue;
            }
            Matcher pointerMatcher = pointerPattern.matcher(fields.get(0));
            if (!pointerMatcher.matches()) {
                throw malformed(name + " index " + first + " has invalid pointer " + fields.get(0));
            }
            int offset = pointerMatcher.group(1) == null
                    ? 0
                    : parseDecimal(pointerMatcher.group(1), name + " index offset");
            int bottom = parseCInteger(fields.get(1), name + " lower bound");
            int top = parseCInteger(fields.get(2), name + " upper bound");
            if (bottom < 0 || top < bottom || top >= BYTE_VALUE_COUNT) {
                throw malformed(name + " index " + first + " has invalid bounds");
            }
            long finalValueIndex = (long) offset + top - bottom;
            if (offset < 0 || finalValueIndex >= validValues.length) {
                throw malformed(name + " index " + first + " exceeds its value array");
            }
            for (int second = bottom; second <= top; second++) {
                if (validValues[offset + second - bottom]) {
                    pairs.set((first << Byte.SIZE) | second);
                }
            }
        }
        return new DecodeMap(pairs);
    }

    /// Parses the three small Johab decoder index arrays and their sentinels.
    ///
    /// @param source `_codecs_kr.c` source
    /// @return immutable lookup arrays
    /// @throws IOException if a definition or array is malformed
    private static JohabIndexes parseJohabIndexes(String source) throws IOException {
        int none = parseLastDefine(source, "NONE");
        int fill = parseLastDefine(source, "FILL");
        Map<String, Integer> macros = Map.of("NONE", none, "FILL", fill);
        return new JohabIndexes(
                parseIntegerArray(source, "johabidx_choseong", 32, macros),
                parseIntegerArray(source, "johabidx_jungseong", 32, macros),
                parseIntegerArray(source, "johabidx_jongseong", 32, macros),
                none,
                fill
        );
    }

    /// Parses the final definition of one integer preprocessor macro.
    ///
    /// @param source C source
    /// @param name   macro name
    /// @return macro value
    /// @throws IOException if no valid definition exists
    private static int parseLastDefine(String source, String name) throws IOException {
        Pattern pattern = Pattern.compile(
                "(?m)^\\s*#\\s*define\\s+" + Pattern.quote(name) + "\\s+([^\\s/]+)"
        );
        Matcher matcher = pattern.matcher(source);
        @Nullable String last = null;
        while (matcher.find()) {
            last = matcher.group(1);
        }
        if (last == null) {
            throw malformed("missing macro " + name);
        }
        return parseCInteger(last, "macro " + name);
    }

    /// Parses one fixed-size C integer array, resolving a restricted macro map.
    ///
    /// @param source         C source
    /// @param name           array name
    /// @param expectedLength required element count
    /// @param macros         recognized symbolic integer values
    /// @return parsed array
    /// @throws IOException if the declaration or an element is malformed
    private static int @Unmodifiable [] parseIntegerArray(
            String source,
            String name,
            int expectedLength,
            Map<String, Integer> macros
    ) throws IOException {
        Pattern pattern = Pattern.compile(
                "\\b" + Pattern.quote(name) + "\\s*\\[\\s*" + expectedLength
                        + "\\s*]\\s*=\\s*\\{"
        );
        Matcher matcher = pattern.matcher(source);
        if (!matcher.find()) {
            throw malformed("missing integer array " + name);
        }
        String body = extractInitializer(source, matcher.end() - 1, name);
        List<String> tokens = splitCommaTokens(body, name);
        if (tokens.size() != expectedLength) {
            throw malformed(name + " length is " + tokens.size() + " instead of " + expectedLength);
        }
        int[] result = new int[expectedLength];
        for (int index = 0; index < result.length; index++) {
            @Nullable Integer macro = macros.get(tokens.get(index));
            result[index] = macro != null
                    ? macro
                    : parseCInteger(tokens.get(index), name + " element " + index);
        }
        return result;
    }

    /// Extracts the body of a balanced C initializer.
    ///
    /// @param source       complete source
    /// @param openingBrace index of the opening brace
    /// @param label        diagnostic label
    /// @return initializer contents without outer braces
    /// @throws IOException if the brace is missing or unbalanced
    private static String extractInitializer(String source, int openingBrace, String label)
            throws IOException {
        if (openingBrace < 0 || openingBrace >= source.length() || source.charAt(openingBrace) != '{') {
            throw malformed("missing opening brace for " + label);
        }
        int depth = 0;
        for (int index = openingBrace; index < source.length(); index++) {
            char character = source.charAt(index);
            if (character == '{') {
                depth++;
            } else if (character == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(openingBrace + 1, index);
                }
                if (depth < 0) {
                    break;
                }
            }
        }
        throw malformed("unbalanced initializer for " + label);
    }

    /// Splits a flat comma-separated C initializer and rejects empty interior fields.
    ///
    /// @param body  initializer body
    /// @param label diagnostic label
    /// @return immutable trimmed tokens
    /// @throws IOException if an empty token occurs before the optional trailing comma
    private static @Unmodifiable List<String> splitCommaTokens(String body, String label)
            throws IOException {
        String commentFree = removeComments(body);
        String[] raw = commentFree.split(",", -1);
        int end = raw.length;
        while (end > 0 && raw[end - 1].trim().isEmpty()) {
            end--;
        }
        ArrayList<String> result = new ArrayList<>(end);
        for (int index = 0; index < end; index++) {
            String token = raw[index].trim();
            if (token.isEmpty()) {
                throw malformed("empty token in " + label);
            }
            result.add(token);
        }
        return List.copyOf(result);
    }

    /// Splits an initializer containing only first-level structure initializers.
    ///
    /// @param body  outer initializer body
    /// @param label diagnostic label
    /// @return immutable inner initializer bodies
    /// @throws IOException if unexpected text or unbalanced braces are present
    private static @Unmodifiable List<String> splitStructInitializers(String body, String label)
            throws IOException {
        String source = removeComments(body);
        ArrayList<String> result = new ArrayList<>();
        int index = 0;
        while (index < source.length()) {
            char character = source.charAt(index);
            if (Character.isWhitespace(character) || character == ',') {
                index++;
                continue;
            }
            if (character != '{') {
                throw malformed("unexpected text in " + label + " at character " + index);
            }
            int start = ++index;
            int depth = 1;
            while (index < source.length() && depth > 0) {
                char nested = source.charAt(index);
                if (nested == '{') {
                    depth++;
                } else if (nested == '}') {
                    depth--;
                }
                index++;
            }
            if (depth != 0) {
                throw malformed("unbalanced structure initializer in " + label);
            }
            result.add(source.substring(start, index - 1));
        }
        return List.copyOf(result);
    }

    /// Removes line and block comments from a generated C initializer.
    ///
    /// @param source initializer source
    /// @return comment-free text
    private static String removeComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
    }

    /// Parses a decimal integer known not to contain a C prefix.
    ///
    /// @param value decimal text
    /// @param label diagnostic label
    /// @return nonnegative integer
    /// @throws IOException if the value is malformed or out of range
    private static int parseDecimal(String value, String label) throws IOException {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) {
                throw malformed("negative " + label + ": " + value);
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw malformed("invalid " + label + ": " + value, exception);
        }
    }

    /// Parses one nonnegative decimal or hexadecimal C integer literal.
    ///
    /// @param value literal text
    /// @param label diagnostic label
    /// @return parsed integer
    /// @throws IOException if the literal is malformed or out of range
    private static int parseCInteger(String value, String label) throws IOException {
        String normalized = value.trim();
        int radix = 10;
        int offset = 0;
        if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
            radix = 16;
            offset = 2;
        }
        if (offset == normalized.length()) {
            throw malformed("invalid " + label + ": " + value);
        }
        try {
            long parsed = Long.parseLong(normalized.substring(offset), radix);
            if (parsed < 0L || parsed > Integer.MAX_VALUE) {
                throw malformed("out-of-range " + label + ": " + value);
            }
            return (int) parsed;
        } catch (NumberFormatException exception) {
            throw malformed("invalid " + label + ": " + value, exception);
        }
    }

    /// Creates the common ASCII-only single-byte mask.
    ///
    /// @return mask accepting bytes `0x00` through `0x7f`
    private static BitSet asciiSingles() {
        BitSet singles = new BitSet(BYTE_VALUE_COUNT);
        singles.set(0, 0x80);
        return singles;
    }

    /// Creates CP932's direct single-byte mask.
    ///
    /// @return CPython CP932 single-byte acceptance mask
    private static BitSet cp932Singles() {
        BitSet singles = new BitSet(BYTE_VALUE_COUNT);
        singles.set(0, 0x81);
        singles.set(0xa0, 0xe0);
        singles.set(0xfd, 0x100);
        return singles;
    }

    /// Creates Shift-JIS-2004's JIS X 0201 single-byte mask.
    ///
    /// @return CPython Shift-JIS-2004 single-byte acceptance mask
    private static BitSet shiftJis2004Singles() {
        BitSet singles = new BitSet(BYTE_VALUE_COUNT);
        singles.set(0, 0x80);
        singles.set(0xa1, 0xe0);
        return singles;
    }

    /// Creates one immutable output table from a single mask and lead-byte decoder.
    ///
    /// @param name          canonical codec name
    /// @param singles       valid standalone bytes
    /// @param decoder       decoder used when the first byte is not standalone
    /// @param kind          extra-mask kind
    /// @param extra         extra mask
    /// @param extraBitCount meaningful extra-mask bits
    /// @return generated table
    private static MultibyteTable createTable(
            String name,
            BitSet singles,
            DoubleByteDecoder decoder,
            int kind,
            byte @Unmodifiable [] extra,
            int extraBitCount
    ) {
        BitSet pairs = new BitSet(PAIR_BIT_COUNT);
        for (int first = 0; first < BYTE_VALUE_COUNT; first++) {
            for (int second = 0; second < BYTE_VALUE_COUNT; second++) {
                boolean accepted = singles.get(first)
                        ? singles.get(second)
                        : decoder.accepts(first, second);
                if (accepted) {
                    pairs.set((first << Byte.SIZE) | second);
                }
            }
        }
        return new MultibyteTable(
                name,
                toMask(singles, BYTE_VALUE_COUNT),
                toMask(pairs, PAIR_BIT_COUNT),
                kind,
                extra,
                extraBitCount
        );
    }

    /// Tests one Big5-HKSCS double-byte sequence.
    ///
    /// @param first  first byte
    /// @param second second byte
    /// @param big5   base Big5 map
    /// @param hkscs  HKSCS extension map
    /// @return whether CPython's decoder accepts the pair
    private static boolean acceptsBig5Hkscs(
            int first,
            int second,
            DecodeMap big5,
            DecodeMap hkscs
    ) {
        boolean consultBig5 = first < 0xc6
                || first > 0xc8
                || first < 0xc7 && second < 0xa1;
        int pair = (first << Byte.SIZE) | second;
        return consultBig5 && big5.contains(first, second)
                || hkscs.contains(first, second)
                || pair == 0x8862
                || pair == 0x8864
                || pair == 0x88a3
                || pair == 0x88a5;
    }

    /// Tests one CP932 double-byte sequence.
    ///
    /// @param first     first byte
    /// @param second    second byte
    /// @param extension CP932 extension map
    /// @param jisX0208  JIS X 0208 map
    /// @return whether CPython's decoder accepts the pair
    private static boolean acceptsCp932Pair(
            int first,
            int second,
            DecodeMap extension,
            DecodeMap jisX0208
    ) {
        if (extension.contains(first, second)) {
            return true;
        }
        if (((first >= 0x81 && first <= 0x9f)
                || (first >= 0xe0 && first <= 0xea))
                && isShiftJisTrail(second)) {
            int row = first < 0xe0 ? first - 0x81 : first - 0xc1;
            int column = second < 0x80 ? second - 0x40 : second - 0x41;
            row = 2 * row + (column < 0x5e ? 0 : 1) + 0x21;
            column = (column < 0x5e ? column : column - 0x5e) + 0x21;
            return jisX0208.contains(row, column);
        }
        return first >= 0xf0 && first <= 0xf9 && isShiftJisTrail(second);
    }

    /// Tests one CP949 double-byte sequence.
    ///
    /// @param first     first byte
    /// @param second    second byte
    /// @param ksX1001   KS X 1001 map
    /// @param extension CP949 extension map
    /// @return whether CPython's decoder accepts the pair
    private static boolean acceptsCp949Pair(
            int first,
            int second,
            DecodeMap ksX1001,
            DecodeMap extension
    ) {
        return ksX1001.contains(first ^ 0x80, second ^ 0x80)
                || extension.contains(first, second);
    }

    /// Tests one EUC-JIS-2004 two-byte sequence.
    ///
    /// @param first     first byte
    /// @param second    second byte
    /// @param jisX0208  JIS X 0208 map
    /// @param plane1Bmp JIS X 0213 plane-one BMP map
    /// @param plane1Emp JIS X 0213 plane-one supplementary map
    /// @param pairMap   JIS X 0213 paired-code-point map
    /// @return whether CPython's decoder accepts the pair
    private static boolean acceptsEucJis2004Pair(
            int first,
            int second,
            DecodeMap jisX0208,
            DecodeMap plane1Bmp,
            DecodeMap plane1Emp,
            DecodeMap pairMap
    ) {
        if (first == 0x8e) {
            return second >= 0xa1 && second <= 0xdf;
        }
        if (first == 0x8f) {
            return false;
        }
        int row = first ^ 0x80;
        int column = second ^ 0x80;
        return row == 0x21 && column == 0x40
                || row == 0x22 && column == 0x32
                || jisX0208.contains(row, column)
                || plane1Bmp.contains(row, column)
                || plane1Emp.contains(row, column)
                || pairMap.contains(row, column);
    }

    /// Tests one complete EUC-KR two-byte sequence.
    ///
    /// The `a4 d4` prefix begins CPython's eight-byte KS X 1001 make-up sequence and is therefore
    /// incomplete when presented as a two-byte input.
    ///
    /// @param first   first byte
    /// @param second  second byte
    /// @param ksX1001 KS X 1001 map
    /// @return whether CPython's decoder accepts the complete pair
    private static boolean acceptsEucKrPair(int first, int second, DecodeMap ksX1001) {
        return !(first == 0xa4 && second == 0xd4)
                && ksX1001.contains(first ^ 0x80, second ^ 0x80);
    }

    /// Tests one GB18030 two-byte sequence.
    ///
    /// @param first            first byte
    /// @param second           second byte
    /// @param gb2312           GB2312 map
    /// @param gbkExtension     GBK extension map
    /// @param gb18030Extension GB18030 two-byte extension map
    /// @return whether CPython's decoder accepts the pair
    private static boolean acceptsGb18030Pair(
            int first,
            int second,
            DecodeMap gb2312,
            DecodeMap gbkExtension,
            DecodeMap gb18030Extension
    ) {
        if (second >= 0x30 && second <= 0x39) {
            return false;
        }
        return first == 0xa1 && second == 0xaa
                || first == 0xa8 && second == 0x44
                || first == 0xa1 && second == 0xa4
                || gb2312.contains(first ^ 0x80, second ^ 0x80)
                || gbkExtension.contains(first, second)
                || gb18030Extension.contains(first, second);
    }

    /// Tests one Shift-JIS-2004 double-byte sequence.
    ///
    /// @param first     first byte
    /// @param second    second byte
    /// @param jisX0208  JIS X 0208 map
    /// @param plane1Bmp JIS X 0213 plane-one BMP map
    /// @param plane2Bmp JIS X 0213 plane-two BMP map
    /// @param plane1Emp JIS X 0213 plane-one supplementary map
    /// @param plane2Emp JIS X 0213 plane-two supplementary map
    /// @param pairMap   JIS X 0213 paired-code-point map
    /// @return whether CPython's decoder accepts the pair
    private static boolean acceptsShiftJis2004Pair(
            int first,
            int second,
            DecodeMap jisX0208,
            DecodeMap plane1Bmp,
            DecodeMap plane2Bmp,
            DecodeMap plane1Emp,
            DecodeMap plane2Emp,
            DecodeMap pairMap
    ) {
        if (!((first >= 0x81 && first <= 0x9f)
                || (first >= 0xe0 && first <= 0xfc))
                || !isShiftJisTrail(second)) {
            return false;
        }
        int row = first < 0xe0 ? first - 0x81 : first - 0xc1;
        int column = second < 0x80 ? second - 0x40 : second - 0x41;
        row = 2 * row + (column < 0x5e ? 0 : 1);
        column = (column < 0x5e ? column : column - 0x5e) + 0x21;
        if (row < 0x5e) {
            row += 0x21;
            return jisX0208.contains(row, column)
                    || plane1Bmp.contains(row, column)
                    || plane1Emp.contains(row, column)
                    || pairMap.contains(row, column);
        }
        if (row >= 0x67) {
            row += 0x07;
        } else if (row >= 0x63 || row == 0x5f) {
            row -= 0x37;
        } else {
            row -= 0x3d;
        }
        return plane2Bmp.contains(row, column) || plane2Emp.contains(row, column);
    }

    /// Tests one Johab double-byte sequence.
    ///
    /// @param first   first byte
    /// @param second  second byte
    /// @param indexes decoder lookup arrays and sentinels
    /// @param ksX1001 KS X 1001 map used by the non-Hangul branch
    /// @return whether CPython's decoder accepts the pair
    private static boolean acceptsJohabPair(
            int first,
            int second,
            JohabIndexes indexes,
            DecodeMap ksX1001
    ) {
        if (first < 0xd8) {
            int choseong = indexes.choseong()[(first >>> 2) & 0x1f];
            int jungseong = indexes.jungseong()[((first << 3) | (second >>> 5)) & 0x1f];
            int jongseong = indexes.jongseong()[second & 0x1f];
            if (choseong == indexes.none()
                    || jungseong == indexes.none()
                    || jongseong == indexes.none()) {
                return false;
            }
            if (choseong == indexes.fill()) {
                return jungseong == indexes.fill() || jongseong == indexes.fill();
            }
            return jungseong != indexes.fill() || jongseong == indexes.fill();
        }
        if (first == 0xdf
                || first > 0xf9
                || second < 0x31
                || (second >= 0x80 && second <= 0x90)
                || (second & 0x7f) == 0x7f
                || (first == 0xda && second >= 0xa1 && second <= 0xd3)) {
            return false;
        }
        int row = first < 0xe0 ? 2 * (first - 0xd9) : 2 * first - 0x197;
        int column = second < 0x91 ? second - 0x31 : second - 0x43;
        row = row + (column < 0x5e ? 0 : 1) + 0x21;
        column = (column < 0x5e ? column : column - 0x5e) + 0x21;
        return ksX1001.contains(row, column);
    }

    /// Creates the EUC-JIS-2004 mask following an `0x8f` lead byte.
    ///
    /// @param jisX0212  JIS X 0212 map
    /// @param plane2Bmp JIS X 0213 plane-two BMP map
    /// @param plane2Emp JIS X 0213 plane-two supplementary map
    /// @return packed pair mask indexed by the two raw trailing bytes
    private static byte[] createEucJis2004Extra(
            DecodeMap jisX0212,
            DecodeMap plane2Bmp,
            DecodeMap plane2Emp
    ) {
        BitSet pairs = new BitSet(PAIR_BIT_COUNT);
        for (int second = 0; second < BYTE_VALUE_COUNT; second++) {
            for (int third = 0; third < BYTE_VALUE_COUNT; third++) {
                int row = second ^ 0x80;
                int column = third ^ 0x80;
                if (plane2Bmp.contains(row, column)
                        || plane2Emp.contains(row, column)
                        || jisX0212.contains(row, column)) {
                    pairs.set((second << Byte.SIZE) | third);
                }
            }
        }
        return toMask(pairs, PAIR_BIT_COUNT);
    }

    /// Creates the GB18030 four-byte pointer mask from CPython's decoder arithmetic.
    ///
    /// @return packed mask for all syntactically possible pointers
    private static byte[] createGb18030Extra() {
        BitSet pointers = new BitSet(GB18030_POINTER_COUNT);
        for (int first = 0; first < 126; first++) {
            for (int second = 0; second < 10; second++) {
                for (int third = 0; third < 126; third++) {
                    for (int fourth = 0; fourth < 10; fourth++) {
                        int pointer = ((first * 10 + second) * 126 + third) * 10 + fourth;
                        boolean accepted;
                        if (first < 4) {
                            long sequence = (long) (first * 10 + second) * 1260L
                                    + (long) third * 10L
                                    + fourth;
                            accepted = sequence < 39_420L;
                        } else if (first >= 15) {
                            long codePoint = 0x10000L
                                    + (long) ((first - 15) * 10 + second) * 1260L
                                    + (long) third * 10L
                                    + fourth;
                            accepted = codePoint <= 0x10ffffL;
                        } else {
                            accepted = false;
                        }
                        if (accepted) {
                            pointers.set(pointer);
                        }
                    }
                }
            }
        }
        return toMask(pointers, GB18030_POINTER_COUNT);
    }

    /// Tests the discontinuous Shift-JIS trail-byte range.
    ///
    /// @param value byte value
    /// @return whether the value is a syntactically valid trail byte
    private static boolean isShiftJisTrail(int value) {
        return (value >= 0x40 && value <= 0x7e)
                || (value >= 0x80 && value <= 0xfc);
    }

    /// Packs a bit set using the runtime resource's little-bit-order convention.
    ///
    /// @param bits     set bit indexes
    /// @param bitCount exact meaningful bit count
    /// @return fixed-size packed mask
    private static byte[] toMask(BitSet bits, int bitCount) {
        if (bits.length() > bitCount) {
            throw new IllegalArgumentException("Bit set exceeds declared size " + bitCount);
        }
        byte[] mask = new byte[(bitCount + Byte.SIZE - 1) / Byte.SIZE];
        for (int value = bits.nextSetBit(0); value >= 0; value = bits.nextSetBit(value + 1)) {
            mask[value >>> 3] |= (byte) (1 << (value & 7));
        }
        return mask;
    }

    /// Creates a CPython source-format exception.
    ///
    /// @param detail failure detail
    /// @return checked format exception
    private static IOException malformed(String detail) {
        return new IOException("Malformed CPython CJK codec source: " + detail);
    }

    /// Creates a CPython source-format exception with a cause.
    ///
    /// @param detail failure detail
    /// @param cause  parsing cause
    /// @return checked format exception
    private static IOException malformed(String detail, RuntimeException cause) {
        return new IOException("Malformed CPython CJK codec source: " + detail, cause);
    }

    /// Tests a non-standalone leading byte and one following byte.
    @FunctionalInterface
    @NotNullByDefault
    private interface DoubleByteDecoder {
        /// Returns whether the decoder consumes the supplied pair as one complete sequence.
        ///
        /// @param first  first byte
        /// @param second second byte
        /// @return whether the pair is accepted
        boolean accepts(int first, int second);
    }

    /// Stores valid pairs from one CPython generated decode map.
    ///
    /// @param pairs set pair indexes
    @NotNullByDefault
    private record DecodeMap(BitSet pairs) {
        /// Creates a map that owns its bit set.
        private DecodeMap {
        }

        /// Returns whether one pair has a non-`U` mapping entry.
        ///
        /// @param first  first lookup byte
        /// @param second second lookup byte
        /// @return whether a mapping exists
        private boolean contains(int first, int second) {
            if ((first | second) < 0 || first >= BYTE_VALUE_COUNT || second >= BYTE_VALUE_COUNT) {
                return false;
            }
            return pairs.get((first << Byte.SIZE) | second);
        }
    }

    /// Stores the CPython Johab decoder's Hangul index arrays and sentinels.
    ///
    /// @param choseong  initial-consonant indexes
    /// @param jungseong vowel indexes
    /// @param jongseong final-consonant indexes
    /// @param none      invalid index sentinel
    /// @param fill      filler index sentinel
    @NotNullByDefault
    private record JohabIndexes(
            int @Unmodifiable [] choseong,
            int @Unmodifiable [] jungseong,
            int @Unmodifiable [] jongseong,
            int none,
            int fill
    ) {
        /// Creates lookup data whose arrays are owned by the generator.
        private JohabIndexes {
        }
    }

    /// Stores all generated CPython-derived multibyte data.
    ///
    /// @param tables  eight tables in runtime resource order
    /// @param hzPairs HZ shifted GB2312 pair mask
    @NotNullByDefault
    record Result(
            @Unmodifiable List<MultibyteTable> tables,
            byte @Unmodifiable [] hzPairs
    ) {
        /// Creates an immutable result from generator-owned values.
        Result {
            tables = List.copyOf(tables);
            if (tables.size() != 8) {
                throw new IllegalArgumentException("Expected eight multibyte tables");
            }
            if (hzPairs.length != PAIR_BIT_COUNT / Byte.SIZE) {
                throw new IllegalArgumentException("Invalid HZ pair mask length");
            }
        }
    }

    /// Stores one generated KVM1 table.
    ///
    /// @param name          canonical codec name
    /// @param singles       standalone-byte mask
    /// @param pairs         complete two-byte input mask
    /// @param kind          extra-table format discriminator
    /// @param extra         optional sequence mask, empty for kind zero
    /// @param extraBitCount number of meaningful extra bits
    @NotNullByDefault
    record MultibyteTable(
            String name,
            byte @Unmodifiable [] singles,
            byte @Unmodifiable [] pairs,
            int kind,
            byte @Unmodifiable [] extra,
            int extraBitCount
    ) {
        /// Creates one validated table from generator-owned arrays.
        MultibyteTable {
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Empty codec name");
            }
            if (singles.length != BYTE_VALUE_COUNT / Byte.SIZE) {
                throw new IllegalArgumentException("Invalid single-byte mask length for " + name);
            }
            if (pairs.length != PAIR_BIT_COUNT / Byte.SIZE) {
                throw new IllegalArgumentException("Invalid pair mask length for " + name);
            }
            int expectedExtraLength = (extraBitCount + Byte.SIZE - 1) / Byte.SIZE;
            if (extraBitCount < 0 || extra.length != expectedExtraLength) {
                throw new IllegalArgumentException("Invalid extra mask length for " + name);
            }
            if (kind == KIND_NONE && extraBitCount != 0
                    || kind == KIND_EUC_JIS_2004 && extraBitCount != PAIR_BIT_COUNT
                    || kind == KIND_GB18030 && extraBitCount != GB18030_POINTER_COUNT
                    || kind < KIND_NONE
                    || kind > KIND_GB18030) {
                throw new IllegalArgumentException("Invalid extra mask kind for " + name);
            }
        }
    }
}
