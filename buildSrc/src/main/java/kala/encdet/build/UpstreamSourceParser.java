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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/// Parses the restricted Python source forms that define chardet metadata and
/// CPython codec tables.
///
/// This parser does not execute Python. It recognizes only the literal tuple,
/// string, boolean, and identifier forms used by the pinned source archives
/// and rejects unexpected syntax.
@NotNullByDefault
final class UpstreamSourceParser {
    /// Expected number of chardet registry entries.
    private static final int REGISTRY_ENTRY_COUNT = 86;

    /// Expected number of aliases declared directly by chardet.
    private static final int CHARDET_ALIAS_COUNT = 315;

    /// Expected number of entries in CPython's alias dictionary.
    private static final int CPYTHON_ALIAS_COUNT = 341;

    /// Expected number of unique CPython alias targets.
    private static final int CPYTHON_ALIAS_TARGET_COUNT = 99;

    /// Expected number of CPython aliases added to the chardet registry.
    private static final int AUGMENTED_ALIAS_COUNT = 288;

    /// Expected number of strict single-byte decode tables.
    private static final int SINGLE_BYTE_TABLE_COUNT = 64;

    /// Maximum accepted Python source file length.
    private static final int MAXIMUM_SOURCE_BYTES = 1_000_000;

    /// Matches one single-quoted entry in CPython's alias dictionary.
    private static final Pattern CPYTHON_ALIAS_PATTERN = Pattern.compile(
            "(?m)^\\s*'([^'\\\\]+)'\\s*:\\s*'([^'\\\\]+)'\\s*,"
    );

    /// Ordered language tuple names used by chardet's registry.
    private static final String @Unmodifiable [] LANGUAGE_GROUP_NAMES = {
            "_WESTERN",
            "_WESTERN_TR",
            "_CYRILLIC",
            "_CENTRAL_EU",
            "_CENTRAL_EU_NO_RO",
            "_BALTIC",
            "_ARABIC"
    };

    /// Prevents instantiation.
    private UpstreamSourceParser() {
    }

    /// Reads and augments the complete ordered encoding registry.
    ///
    /// @param chardetArchive pinned chardet source ZIP
    /// @param chardetRoot exact chardet ZIP root
    /// @param cpythonArchive pinned CPython source ZIP
    /// @param cpythonRoot exact CPython ZIP root
    /// @return immutable registry entries in upstream order
    /// @throws IOException if a source file is absent or has an unexpected form
    static @Unmodifiable List<RegistryEntry> readRegistry(
            Path chardetArchive,
            String chardetRoot,
            Path cpythonArchive,
            String cpythonRoot
    ) throws IOException {
        String normalizedChardetRoot = validateArchiveRoot(chardetRoot);
        String normalizedCpythonRoot = validateArchiveRoot(cpythonRoot);
        String registrySource;
        try (ZipFile archive = new ZipFile(chardetArchive.toFile())) {
            registrySource = readUtf8Entry(
                    archive,
                    normalizedChardetRoot + "/src/chardet/registry.py"
            );
        }
        @Unmodifiable List<RegistryEntry> baseEntries = parseRegistry(registrySource);
        try (ZipFile archive = new ZipFile(cpythonArchive.toFile())) {
            String aliasesSource = readUtf8Entry(
                    archive,
                    normalizedCpythonRoot + "/Lib/encodings/aliases.py"
            );
            @Unmodifiable Map<String, String> cpythonAliases = parseCpythonAliases(aliasesSource);
            @Unmodifiable Map<String, String> codecNames = readCpythonCodecNames(
                    archive,
                    normalizedCpythonRoot,
                    cpythonAliases
            );
            return augmentAliases(baseEntries, cpythonAliases, codecNames);
        }
    }

    /// Reads all strict single-byte decode tables from CPython codec modules.
    ///
    /// @param cpythonArchive pinned CPython source ZIP
    /// @param cpythonRoot exact CPython ZIP root
    /// @param registry ordered augmented registry
    /// @return immutable tables in detector registry order
    /// @throws IOException if a codec module is absent or malformed
    static @Unmodifiable List<SingleByteTable> readSingleByteTables(
            Path cpythonArchive,
            String cpythonRoot,
            @Unmodifiable List<RegistryEntry> registry
    ) throws IOException {
        String normalizedRoot = validateArchiveRoot(cpythonRoot);
        ArrayList<SingleByteTable> tables = new ArrayList<>();
        try (ZipFile archive = new ZipFile(cpythonArchive.toFile())) {
            for (RegistryEntry entry : registry) {
                if (entry.multibyte() || entry.name().startsWith("utf-")) {
                    continue;
                }
                int[] mappings;
                if (entry.name().equals("ascii")) {
                    mappings = asciiMappings();
                } else {
                    String moduleName = normalizePythonEncoding(entry.name());
                    String source = readUtf8Entry(
                            archive,
                            normalizedRoot + "/Lib/encodings/" + moduleName + ".py"
                    );
                    mappings = parseDecodingTable(entry.name(), source);
                }
                tables.add(new SingleByteTable(entry.name(), mappings));
            }
        }
        if (tables.size() != SINGLE_BYTE_TABLE_COUNT) {
            throw new IOException(
                    "Expected " + SINGLE_BYTE_TABLE_COUNT + " single-byte codecs but found "
                            + tables.size()
            );
        }
        return List.copyOf(tables);
    }

    /// Parses the ordered `EncodingInfo` calls in chardet's registry module.
    ///
    /// @param source registry module source
    /// @return immutable base registry
    private static @Unmodifiable List<RegistryEntry> parseRegistry(String source) {
        @Unmodifiable Map<String, List<String>> languageGroups = parseLanguageGroups(source);
        int registryStart = source.indexOf("_REGISTRY_ENTRIES = (");
        int registryEnd = source.indexOf("\nREGISTRY:", registryStart);
        if (registryStart < 0 || registryEnd < 0) {
            throw malformed("Unable to locate _REGISTRY_ENTRIES");
        }

        ArrayList<RegistryEntry> entries = new ArrayList<>();
        Set<String> names = new HashSet<>();
        int aliasCount = 0;
        int searchFrom = registryStart;
        while (true) {
            int callStart = source.indexOf("EncodingInfo(", searchFrom);
            if (callStart < 0 || callStart >= registryEnd) {
                break;
            }
            int opening = callStart + "EncodingInfo".length();
            int closing = findMatchingParenthesis(source, opening);
            if (closing >= registryEnd) {
                throw malformed("EncodingInfo call crosses the registry boundary");
            }
            String call = source.substring(opening + 1, closing);
            String name = parseSingleString(extractKeyword(call, "name"), "name");
            @Unmodifiable List<String> aliases = parseTuple(
                    extractKeyword(call, "aliases"),
                    Map.of(),
                    "aliases"
            );
            String eraExpression = extractKeyword(call, "era");
            String eraPrefix = "EncodingEra.";
            if (!eraExpression.startsWith(eraPrefix)
                    || !isIdentifier(eraExpression.substring(eraPrefix.length()))) {
                throw malformed("Invalid era expression for " + name);
            }
            String era = eraExpression.substring(eraPrefix.length());
            String multibyteExpression = extractKeyword(call, "is_multibyte");
            boolean multibyte;
            if (multibyteExpression.equals("True")) {
                multibyte = true;
            } else if (multibyteExpression.equals("False")) {
                multibyte = false;
            } else {
                throw malformed("Invalid is_multibyte expression for " + name);
            }
            @Unmodifiable List<String> languages = parseLanguageExpression(
                    extractKeyword(call, "languages"),
                    languageGroups,
                    name
            );
            if (!names.add(name)) {
                throw malformed("Duplicate registry name " + name);
            }
            aliasCount += aliases.size();
            entries.add(new RegistryEntry(name, era, multibyte, languages, aliases));
            searchFrom = closing + 1;
        }
        if (entries.size() != REGISTRY_ENTRY_COUNT) {
            throw malformed(
                    "Expected " + REGISTRY_ENTRY_COUNT + " registry entries but found "
                            + entries.size()
            );
        }
        if (aliasCount != CHARDET_ALIAS_COUNT) {
            throw malformed(
                    "Expected " + CHARDET_ALIAS_COUNT + " chardet aliases but found " + aliasCount
            );
        }
        return List.copyOf(entries);
    }

    /// Parses the shared language tuples declared before the registry.
    ///
    /// @param source registry module source
    /// @return immutable language tuples keyed by Python identifier
    private static @Unmodifiable Map<String, List<String>> parseLanguageGroups(String source) {
        LinkedHashMap<String, List<String>> groups = new LinkedHashMap<>();
        for (String name : LANGUAGE_GROUP_NAMES) {
            Pattern pattern = Pattern.compile("(?m)^" + Pattern.quote(name) + "\\s*=\\s*\\(");
            Matcher matcher = pattern.matcher(source);
            if (!matcher.find()) {
                throw malformed("Missing language tuple " + name);
            }
            int opening = source.indexOf('(', matcher.start());
            int closing = findMatchingParenthesis(source, opening);
            @Unmodifiable List<String> values = parseTuple(
                    source.substring(opening, closing + 1),
                    groups,
                    name
            );
            groups.put(name, values);
        }
        return Map.copyOf(groups);
    }

    /// Parses one language expression used by an `EncodingInfo` call.
    ///
    /// @param expression Python expression
    /// @param groups previously parsed language tuples
    /// @param encoding encoding name for diagnostics
    /// @return immutable language list
    private static @Unmodifiable List<String> parseLanguageExpression(
            String expression,
            @Unmodifiable Map<String, List<String>> groups,
            String encoding
    ) {
        if (expression.startsWith("(")) {
            return parseTuple(expression, groups, "languages for " + encoding);
        }
        @Nullable List<String> values = groups.get(expression);
        if (values == null) {
            throw malformed("Unknown language tuple " + expression + " for " + encoding);
        }
        return values;
    }

    /// Parses CPython's ordered alias dictionary.
    ///
    /// @param source aliases module source
    /// @return immutable aliases in source order
    private static @Unmodifiable Map<String, String> parseCpythonAliases(String source) {
        LinkedHashMap<String, String> aliases = new LinkedHashMap<>();
        Matcher matcher = CPYTHON_ALIAS_PATTERN.matcher(source);
        while (matcher.find()) {
            String previous = aliases.putIfAbsent(matcher.group(1), matcher.group(2));
            if (previous != null) {
                throw malformed("Duplicate CPython alias " + matcher.group(1));
            }
        }
        if (aliases.size() != CPYTHON_ALIAS_COUNT) {
            throw malformed(
                    "Expected " + CPYTHON_ALIAS_COUNT + " CPython aliases but found "
                            + aliases.size()
            );
        }
        if (new HashSet<>(aliases.values()).size() != CPYTHON_ALIAS_TARGET_COUNT) {
            throw malformed("Unexpected CPython alias target count");
        }
        return java.util.Collections.unmodifiableMap(aliases);
    }

    /// Reads the public codec name returned by every aliased CPython module.
    ///
    /// CPython's encoding search function imports the module selected by
    /// `aliases.py`, then returns the `name` stored in that module's
    /// `CodecInfo`. Chardet recursively resolves that public name rather than
    /// the module identifier, so both values are required to reproduce codec
    /// fallback without executing Python.
    ///
    /// @param archive open pinned CPython source archive
    /// @param cpythonRoot exact CPython ZIP root
    /// @param cpythonAliases alias-to-module mappings
    /// @return immutable module-to-public-codec-name mappings
    /// @throws IOException if a codec module is absent or unreadable
    private static @Unmodifiable Map<String, String> readCpythonCodecNames(
            ZipFile archive,
            String cpythonRoot,
            @Unmodifiable Map<String, String> cpythonAliases
    ) throws IOException {
        TreeSet<String> modules = new TreeSet<>(cpythonAliases.values());
        LinkedHashMap<String, String> codecNames = new LinkedHashMap<>();
        for (String module : modules) {
            if (!isIdentifier(module)) {
                throw malformed("Invalid CPython codec module name " + module);
            }
            String source = readUtf8Entry(
                    archive,
                    cpythonRoot + "/Lib/encodings/" + module + ".py"
            );
            codecNames.put(module, parseCpythonCodecName(module, source));
        }
        if (codecNames.size() != CPYTHON_ALIAS_TARGET_COUNT) {
            throw malformed(
                    "Expected " + CPYTHON_ALIAS_TARGET_COUNT + " CPython codec modules but found "
                            + codecNames.size()
            );
        }
        return java.util.Collections.unmodifiableMap(codecNames);
    }

    /// Parses the `CodecInfo.name` literal returned by one CPython codec module.
    ///
    /// @param module normalized codec module name
    /// @param source codec module source
    /// @return public codec name returned by `codecs.lookup`
    private static String parseCpythonCodecName(String module, String source) {
        int function = source.indexOf("def getregentry(");
        if (function < 0) {
            throw malformed("Missing getregentry function in CPython codec module " + module);
        }
        int constructor = source.indexOf("return codecs.CodecInfo(", function);
        if (constructor < 0) {
            throw malformed("Missing CodecInfo return in CPython codec module " + module);
        }
        int opening = source.indexOf('(', constructor);
        int closing = findMatchingParenthesis(source, opening);
        String arguments = source.substring(opening + 1, closing);
        String name = parseSingleString(
                extractKeyword(arguments, "name"),
                "CodecInfo.name for " + module
        );
        if (name.isEmpty()) {
            throw malformed("Empty CodecInfo.name in CPython codec module " + module);
        }
        return name;
    }

    /// Adds CPython codec keys and module names to the explicit chardet aliases.
    ///
    /// @param baseEntries ordered chardet entries
    /// @param cpythonAliases CPython alias-to-module map
    /// @param codecNames CPython module-to-public-codec-name map
    /// @return immutable augmented entries
    private static @Unmodifiable List<RegistryEntry> augmentAliases(
            @Unmodifiable List<RegistryEntry> baseEntries,
            @Unmodifiable Map<String, String> cpythonAliases,
            @Unmodifiable Map<String, String> codecNames
    ) {
        HashMap<String, RegistryEntry> directLookup = new HashMap<>();
        for (RegistryEntry entry : baseEntries) {
            addDirectLookupName(directLookup, entry, entry.name());
            for (String alias : entry.aliases()) {
                addDirectLookupName(directLookup, entry, alias);
            }
        }

        LinkedHashSet<String> candidates = new LinkedHashSet<>(cpythonAliases.keySet());
        candidates.addAll(cpythonAliases.values());
        HashMap<String, TreeSet<String>> additions = new HashMap<>();
        for (String candidate : candidates) {
            @Nullable RegistryEntry target = resolveChardetName(
                    candidate,
                    directLookup,
                    cpythonAliases,
                    codecNames,
                    new HashSet<>()
            );
            if (target != null) {
                additions.computeIfAbsent(target.name(), ignored -> new TreeSet<>()).add(candidate);
            }
        }

        ArrayList<RegistryEntry> result = new ArrayList<>(baseEntries.size());
        int addedCount = 0;
        for (RegistryEntry entry : baseEntries) {
            ArrayList<String> aliases = new ArrayList<>(entry.aliases());
            Set<String> existing = new HashSet<>();
            existing.add(entry.name().toLowerCase(Locale.ROOT));
            for (String alias : aliases) {
                existing.add(alias.toLowerCase(Locale.ROOT));
            }
            @Nullable TreeSet<String> candidatesForEntry = additions.get(entry.name());
            if (candidatesForEntry != null) {
                for (String candidate : candidatesForEntry) {
                    if (existing.add(candidate.toLowerCase(Locale.ROOT))) {
                        aliases.add(candidate);
                        addedCount++;
                    }
                }
            }
            result.add(new RegistryEntry(
                    entry.name(),
                    entry.era(),
                    entry.multibyte(),
                    entry.languages(),
                    List.copyOf(aliases)
            ));
        }
        if (addedCount != AUGMENTED_ALIAS_COUNT) {
            throw malformed(
                    "Expected " + AUGMENTED_ALIAS_COUNT + " augmented aliases but found "
                            + addedCount
            );
        }
        return List.copyOf(result);
    }

    /// Resolves one name with chardet's exact-first, codec-fallback algorithm.
    ///
    /// Chardet first compares canonical names and declared aliases after only
    /// lower-casing them. It asks CPython to normalize a name only after that
    /// exact pass fails, then recursively resolves `CodecInfo.name`. Keeping
    /// these stages separate is essential for collisions such as `ms_kanji`,
    /// which chardet claims for Shift-JIS even though CPython aliases it to
    /// `cp932`.
    ///
    /// @param value name being resolved
    /// @param directLookup case-folded chardet canonical names and aliases
    /// @param cpythonAliases normalized CPython alias-to-module mappings
    /// @param codecNames CPython module-to-public-codec-name mappings
    /// @param visited case-folded recursive chardet lookup names
    /// @return matching registry entry, or `null` if neither layer recognizes the name
    private static @Nullable RegistryEntry resolveChardetName(
            String value,
            Map<String, RegistryEntry> directLookup,
            @Unmodifiable Map<String, String> cpythonAliases,
            @Unmodifiable Map<String, String> codecNames,
            Set<String> visited
    ) {
        String lowered = value.toLowerCase(Locale.ROOT);
        @Nullable RegistryEntry direct = directLookup.get(lowered);
        if (direct != null) {
            return direct;
        }
        if (!visited.add(lowered)) {
            return null;
        }

        @Nullable String codecName = lookupCpythonCodecName(
                lowered,
                cpythonAliases,
                codecNames
        );
        if (codecName == null || codecName.equals(lowered)) {
            return null;
        }
        return resolveChardetName(
                codecName,
                directLookup,
                cpythonAliases,
                codecNames,
                visited
        );
    }

    /// Reproduces CPython's encoding-module selection for one normalized lookup.
    ///
    /// CPython consults `aliases.py` using the normalized key (and a dotted-key
    /// fallback), otherwise it imports the normalized name directly. The
    /// imported module's `CodecInfo.name`, not its module identifier, is the
    /// observable result of `codecs.lookup`.
    ///
    /// @param value lower-case codec lookup input
    /// @param cpythonAliases normalized CPython alias-to-module mappings
    /// @param codecNames CPython module-to-public-codec-name mappings
    /// @return public codec name, or `null` when no pinned module exists
    private static @Nullable String lookupCpythonCodecName(
            String value,
            @Unmodifiable Map<String, String> cpythonAliases,
            @Unmodifiable Map<String, String> codecNames
    ) {
        String normalized = normalizePythonEncoding(value);
        @Nullable String module = cpythonAliases.get(normalized);
        if (module == null && normalized.indexOf('.') >= 0) {
            module = cpythonAliases.get(normalized.replace('.', '_'));
        }
        if (module == null) {
            module = normalized;
        }
        return codecNames.get(module);
    }

    /// Adds one case-insensitive chardet name to the direct alias-resolution index.
    ///
    /// @param lookup lower-case name index
    /// @param entry target registry entry
    /// @param value canonical name or alias
    private static void addDirectLookupName(
            Map<String, RegistryEntry> lookup,
            RegistryEntry entry,
            String value
    ) {
        String lowerCase = value.toLowerCase(Locale.ROOT);
        @Nullable RegistryEntry previous = lookup.putIfAbsent(lowerCase, entry);
        if (previous != null && previous != entry) {
            throw malformed("Case-insensitive alias collision for " + value);
        }
    }

    /// Parses one CPython charmap module's `decoding_table` literal.
    ///
    /// @param codec codec name for diagnostics
    /// @param source codec module source
    /// @return 256 byte-to-code-point mappings, using `-1` for undefined bytes
    private static int[] parseDecodingTable(String codec, String source) {
        int assignment = source.indexOf("decoding_table = (");
        if (assignment < 0) {
            throw malformed("Missing decoding_table for " + codec);
        }
        int opening = source.indexOf('(', assignment);
        int closing = findMatchingParenthesis(source, opening);
        String decoded = parseConcatenatedStrings(source.substring(opening, closing + 1), codec);
        int[] codePoints = decoded.codePoints().toArray();
        if (codePoints.length != 256) {
            throw malformed(
                    "Expected 256 decoding entries for " + codec + " but found "
                            + codePoints.length
            );
        }
        for (int index = 0; index < codePoints.length; index++) {
            if (codePoints[index] == 0xfffe) {
                codePoints[index] = -1;
            } else if (!Character.isValidCodePoint(codePoints[index])
                    || codePoints[index] >= Character.MIN_SURROGATE
                    && codePoints[index] <= Character.MAX_SURROGATE) {
                throw malformed("Invalid Unicode scalar in " + codec + " at byte " + index);
            }
        }
        return codePoints;
    }

    /// Creates the strict seven-bit ASCII decode table.
    ///
    /// @return 256 mappings with high bytes marked undefined
    private static int[] asciiMappings() {
        int[] mappings = new int[256];
        for (int value = 0; value < mappings.length; value++) {
            mappings[value] = value < 128 ? value : -1;
        }
        return mappings;
    }

    /// Extracts one keyword value from a restricted `EncodingInfo` call body.
    ///
    /// @param call call body
    /// @param keyword keyword name
    /// @return trimmed source expression
    private static String extractKeyword(String call, String keyword) {
        Pattern pattern = Pattern.compile("(?m)^\\s*" + Pattern.quote(keyword) + "\\s*=\\s*");
        Matcher matcher = pattern.matcher(call);
        if (!matcher.find()) {
            throw malformed("Missing EncodingInfo keyword " + keyword);
        }
        int start = skipTrivia(call, matcher.end());
        if (start >= call.length()) {
            throw malformed("Missing value for EncodingInfo keyword " + keyword);
        }
        char first = call.charAt(start);
        int end;
        if (first == '(') {
            end = findMatchingParenthesis(call, start) + 1;
        } else if (first == '\'' || first == '"') {
            end = parseStringLiteral(call, start).nextIndex();
        } else {
            end = start;
            while (end < call.length()) {
                char current = call.charAt(end);
                if (!(Character.isLetterOrDigit(current) || current == '_' || current == '.')) {
                    break;
                }
                end++;
            }
        }
        if (end == start) {
            throw malformed("Invalid value for EncodingInfo keyword " + keyword);
        }
        return call.substring(start, end);
    }

    /// Parses one expression that must contain exactly one Python string.
    ///
    /// @param expression source expression
    /// @param label diagnostic label
    /// @return decoded string
    private static String parseSingleString(String expression, String label) {
        ParsedString parsed = parseStringLiteral(expression, 0);
        if (skipTrivia(expression, parsed.nextIndex()) != expression.length()) {
            throw malformed("Unexpected syntax after " + label);
        }
        return parsed.value();
    }

    /// Parses a tuple containing strings and optional starred tuple references.
    ///
    /// @param expression parenthesized tuple expression
    /// @param groups tuple references available for expansion
    /// @param label diagnostic label
    /// @return immutable expanded strings
    private static @Unmodifiable List<String> parseTuple(
            String expression,
            @Unmodifiable Map<String, ? extends List<String>> groups,
            String label
    ) {
        if (expression.length() < 2 || expression.charAt(0) != '('
                || findMatchingParenthesis(expression, 0) != expression.length() - 1) {
            throw malformed("Invalid tuple for " + label);
        }
        ArrayList<String> values = new ArrayList<>();
        int index = 1;
        while (true) {
            index = skipTupleSeparators(expression, index);
            if (index >= expression.length() - 1) {
                break;
            }
            boolean starred = expression.charAt(index) == '*';
            if (starred) {
                index = skipTrivia(expression, index + 1);
            }
            char current = expression.charAt(index);
            if (current == '\'' || current == '"') {
                if (starred) {
                    throw malformed("Cannot star a string in " + label);
                }
                ParsedString parsed = parseStringLiteral(expression, index);
                values.add(parsed.value());
                index = parsed.nextIndex();
                continue;
            }
            int end = index;
            while (end < expression.length() && isIdentifierPart(expression.charAt(end))) {
                end++;
            }
            if (end == index) {
                throw malformed("Unexpected tuple syntax in " + label);
            }
            String identifier = expression.substring(index, end);
            @Nullable List<String> referenced = groups.get(identifier);
            if (referenced == null || !starred && expression.charAt(0) == '(') {
                throw malformed("Unknown or unstarred tuple reference " + identifier + " in " + label);
            }
            values.addAll(referenced);
            index = end;
        }
        return List.copyOf(values);
    }

    /// Parses implicitly concatenated Python string literals.
    ///
    /// @param expression parenthesized string sequence
    /// @param label diagnostic label
    /// @return concatenated decoded string
    private static String parseConcatenatedStrings(String expression, String label) {
        StringBuilder builder = new StringBuilder(256);
        int index = 1;
        int literalCount = 0;
        while (true) {
            index = skipTupleSeparators(expression, index);
            if (index >= expression.length() - 1) {
                break;
            }
            char current = expression.charAt(index);
            if (current != '\'' && current != '"') {
                throw malformed("Unexpected decoding_table syntax for " + label);
            }
            ParsedString parsed = parseStringLiteral(expression, index);
            builder.append(parsed.value());
            literalCount++;
            index = parsed.nextIndex();
        }
        if (literalCount == 0) {
            throw malformed("Empty decoding_table for " + label);
        }
        return builder.toString();
    }

    /// Parses one ordinary Python string literal.
    ///
    /// @param source containing source text
    /// @param start opening quote index
    /// @return decoded value and first index after the closing quote
    private static ParsedString parseStringLiteral(String source, int start) {
        if (start >= source.length()) {
            throw malformed("Missing Python string literal");
        }
        char quote = source.charAt(start);
        if (quote != '\'' && quote != '"') {
            throw malformed("Expected Python string literal");
        }
        StringBuilder builder = new StringBuilder();
        int index = start + 1;
        while (index < source.length()) {
            char current = source.charAt(index++);
            if (current == quote) {
                return new ParsedString(builder.toString(), index);
            }
            if (current == '\n' || current == '\r') {
                throw malformed("Unterminated Python string literal");
            }
            if (current != '\\') {
                builder.append(current);
                continue;
            }
            if (index >= source.length()) {
                throw malformed("Truncated Python escape sequence");
            }
            char escape = source.charAt(index++);
            switch (escape) {
                case '\\', '\'', '"' -> builder.append(escape);
                case 'n' -> builder.append('\n');
                case 'r' -> builder.append('\r');
                case 't' -> builder.append('\t');
                case 'a' -> builder.append('\u0007');
                case 'b' -> builder.append('\b');
                case 'f' -> builder.append('\f');
                case 'v' -> builder.append('\u000b');
                case 'x' -> {
                    int value = parseFixedHex(source, index, 2);
                    builder.append((char) value);
                    index += 2;
                }
                case 'u' -> {
                    int value = parseFixedHex(source, index, 4);
                    builder.append((char) value);
                    index += 4;
                }
                case 'U' -> {
                    int value = parseFixedHex(source, index, 8);
                    if (!Character.isValidCodePoint(value)) {
                        throw malformed("Invalid Python Unicode escape");
                    }
                    builder.appendCodePoint(value);
                    index += 8;
                }
                default -> {
                    if (escape < '0' || escape > '7') {
                        throw malformed("Unsupported Python escape \\" + escape);
                    }
                    int value = escape - '0';
                    int digits = 1;
                    while (digits < 3 && index < source.length()) {
                        char digit = source.charAt(index);
                        if (digit < '0' || digit > '7') {
                            break;
                        }
                        value = value * 8 + digit - '0';
                        index++;
                        digits++;
                    }
                    builder.append((char) value);
                }
            }
        }
        throw malformed("Unterminated Python string literal");
    }

    /// Parses an exact-width hexadecimal escape payload.
    ///
    /// @param source source text
    /// @param start first hexadecimal digit
    /// @param length required digit count
    /// @return parsed integer
    private static int parseFixedHex(String source, int start, int length) {
        if (start + length > source.length()) {
            throw malformed("Truncated Python hexadecimal escape");
        }
        int value = 0;
        for (int index = start; index < start + length; index++) {
            int digit = Character.digit(source.charAt(index), 16);
            if (digit < 0) {
                throw malformed("Invalid Python hexadecimal escape");
            }
            value = value * 16 + digit;
        }
        return value;
    }

    /// Finds the closing parenthesis matching one opening parenthesis.
    ///
    /// @param source source text
    /// @param opening opening parenthesis index
    /// @return matching closing parenthesis index
    private static int findMatchingParenthesis(String source, int opening) {
        if (opening < 0 || opening >= source.length() || source.charAt(opening) != '(') {
            throw malformed("Expected opening parenthesis");
        }
        int depth = 0;
        char quote = 0;
        boolean escaped = false;
        boolean comment = false;
        for (int index = opening; index < source.length(); index++) {
            char current = source.charAt(index);
            if (comment) {
                if (current == '\n' || current == '\r') {
                    comment = false;
                }
                continue;
            }
            if (quote != 0) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == quote) {
                    quote = 0;
                }
                continue;
            }
            if (current == '#') {
                comment = true;
            } else if (current == '\'' || current == '"') {
                quote = current;
            } else if (current == '(') {
                depth++;
            } else if (current == ')' && --depth == 0) {
                return index;
            }
        }
        throw malformed("Unclosed parenthesized expression");
    }

    /// Skips whitespace and Python comments.
    ///
    /// @param source source text
    /// @param start initial index
    /// @return first non-trivia index
    private static int skipTrivia(String source, int start) {
        int index = start;
        while (index < source.length()) {
            char current = source.charAt(index);
            if (Character.isWhitespace(current)) {
                index++;
            } else if (current == '#') {
                while (index < source.length()
                        && source.charAt(index) != '\n'
                        && source.charAt(index) != '\r') {
                    index++;
                }
            } else {
                break;
            }
        }
        return index;
    }

    /// Skips tuple commas in addition to ordinary Python trivia.
    ///
    /// @param source tuple source
    /// @param start initial index
    /// @return first tuple item index
    private static int skipTupleSeparators(String source, int start) {
        int index = start;
        while (true) {
            index = skipTrivia(source, index);
            if (index < source.length() && source.charAt(index) == ',') {
                index++;
                continue;
            }
            return index;
        }
    }

    /// Normalizes an encoding name with CPython's ASCII name rules.
    ///
    /// @param value encoding name
    /// @return lower-case normalized module name
    private static String normalizePythonEncoding(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        boolean punctuation = false;
        for (int index = 0; index < value.length(); index++) {
            char current = Character.toLowerCase(value.charAt(index));
            if (current >= 'a' && current <= 'z'
                    || current >= '0' && current <= '9'
                    || current == '.') {
                if (punctuation && !builder.isEmpty()) {
                    builder.append('_');
                }
                builder.append(current);
                punctuation = false;
            } else {
                punctuation = true;
            }
        }
        return builder.toString();
    }

    /// Tests whether a string is one nonempty Python identifier.
    ///
    /// @param value candidate identifier
    /// @return whether every character is accepted by the restricted parser
    private static boolean isIdentifier(String value) {
        if (value.isEmpty() || !isIdentifierStart(value.charAt(0))) {
            return false;
        }
        for (int index = 1; index < value.length(); index++) {
            if (!isIdentifierPart(value.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    /// Tests whether a character may start a restricted Python identifier.
    ///
    /// @param value character
    /// @return whether the character is accepted
    private static boolean isIdentifierStart(char value) {
        return value == '_' || value >= 'A' && value <= 'Z' || value >= 'a' && value <= 'z';
    }

    /// Tests whether a character may continue a restricted Python identifier.
    ///
    /// @param value character
    /// @return whether the character is accepted
    private static boolean isIdentifierPart(char value) {
        return isIdentifierStart(value) || value >= '0' && value <= '9';
    }

    /// Reads one exact UTF-8 source entry from a pinned ZIP.
    ///
    /// @param archive open source archive
    /// @param name exact entry name
    /// @return decoded source text
    /// @throws IOException if the entry is absent, oversized, truncated, or malformed UTF-8
    private static String readUtf8Entry(ZipFile archive, String name) throws IOException {
        @Nullable ZipEntry entry = archive.getEntry(name);
        if (entry == null || entry.isDirectory()) {
            throw new IOException("Missing source entry: " + name);
        }
        long size = entry.getSize();
        if (size < 0L || size > MAXIMUM_SOURCE_BYTES) {
            throw new IOException("Invalid source entry length for " + name + ": " + size);
        }
        byte[] bytes = new byte[(int) size];
        try (InputStream input = archive.getInputStream(entry)) {
            int read = input.readNBytes(bytes, 0, bytes.length);
            if (read != bytes.length || input.read() >= 0) {
                throw new IOException("Source entry length differs from ZIP metadata: " + name);
            }
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new IOException("Source entry is not valid UTF-8: " + name, exception);
        }
    }

    /// Validates one configured source-archive root.
    ///
    /// @param value configured root
    /// @return normalized root without a trailing slash
    private static String validateArchiveRoot(String value) {
        String root = value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
        if (root.isEmpty()
                || root.startsWith("/")
                || root.contains("\\")
                || root.contains(":")
                || root.indexOf('\0') >= 0) {
            throw malformed("Invalid archive root: " + value);
        }
        for (String segment : root.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw malformed("Invalid archive root: " + value);
            }
        }
        return root;
    }

    /// Creates one restricted-source format exception.
    ///
    /// @param detail failure detail
    /// @return format exception
    private static IllegalArgumentException malformed(String detail) {
        return new IllegalArgumentException("Malformed pinned Python source: " + detail);
    }

    /// Stores one immutable augmented registry entry.
    ///
    /// @param name canonical encoding name
    /// @param era encoding-era enum constant name
    /// @param multibyte whether the registry marks the encoding as multibyte
    /// @param languages immutable language codes
    /// @param aliases immutable ordered aliases
    @NotNullByDefault
    record RegistryEntry(
            String name,
            String era,
            boolean multibyte,
            @Unmodifiable List<String> languages,
            @Unmodifiable List<String> aliases
    ) {
        /// Creates an entry whose lists are already immutable.
        RegistryEntry {
        }
    }

    /// Stores one immutable strict single-byte decode table.
    ///
    /// @param name canonical codec name
    /// @param mappings immutable byte-to-code-point mappings
    @NotNullByDefault
    record SingleByteTable(String name, int @Unmodifiable [] mappings) {
        /// Creates a table whose mapping array is owned by the generator.
        SingleByteTable {
        }
    }

    /// Stores one decoded Python string and the following source index.
    ///
    /// @param value decoded string
    /// @param nextIndex first source index after the closing quote
    @NotNullByDefault
    private record ParsedString(String value, int nextIndex) {
        /// Creates one parser result.
        private ParsedString {
        }
    }
}
