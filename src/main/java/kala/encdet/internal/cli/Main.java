// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal.cli;

import kala.encdet.DetectionResult;
import kala.encdet.Encoding;
import kala.encdet.EncodingDetector;
import kala.encdet.EncodingEra;
import kala.encdet.internal.EncodingRegistry;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/// Implements the `kala-encdet` command-line application.
@NotNullByDefault
public final class Main {
    /// Application version shown by `--version`.
    private static final String VERSION = "0.1.0-SNAPSHOT";

    /// Maximum bytes read from each file or standard input.
    private static final int MAX_BYTES = 200_000;

    /// Human-readable language names keyed by ISO 639 code.
    private static final @Unmodifiable Map<String, String> LANGUAGE_NAMES = Map.ofEntries(
            Map.entry("ar", "arabic"), Map.entry("be", "belarusian"),
            Map.entry("bg", "bulgarian"), Map.entry("br", "breton"),
            Map.entry("cs", "czech"), Map.entry("cy", "welsh"),
            Map.entry("da", "danish"), Map.entry("de", "german"),
            Map.entry("el", "greek"), Map.entry("en", "english"),
            Map.entry("eo", "esperanto"), Map.entry("es", "spanish"),
            Map.entry("et", "estonian"), Map.entry("fa", "farsi"),
            Map.entry("fi", "finnish"), Map.entry("fr", "french"),
            Map.entry("ga", "irish"), Map.entry("gd", "gaelic"),
            Map.entry("he", "hebrew"), Map.entry("hr", "croatian"),
            Map.entry("hu", "hungarian"), Map.entry("id", "indonesian"),
            Map.entry("is", "icelandic"), Map.entry("it", "italian"),
            Map.entry("ja", "japanese"), Map.entry("kk", "kazakh"),
            Map.entry("ko", "korean"), Map.entry("lt", "lithuanian"),
            Map.entry("lv", "latvian"), Map.entry("mk", "macedonian"),
            Map.entry("ms", "malay"), Map.entry("mt", "maltese"),
            Map.entry("nl", "dutch"), Map.entry("no", "norwegian"),
            Map.entry("pl", "polish"), Map.entry("pt", "portuguese"),
            Map.entry("ro", "romanian"), Map.entry("ru", "russian"),
            Map.entry("sk", "slovak"), Map.entry("sl", "slovene"),
            Map.entry("sr", "serbian"), Map.entry("sv", "swedish"),
            Map.entry("tg", "tajik"), Map.entry("th", "thai"),
            Map.entry("tr", "turkish"), Map.entry("uk", "ukrainian"),
            Map.entry("und", "undetermined"), Map.entry("ur", "urdu"),
            Map.entry("vi", "vietnamese"), Map.entry("zh", "chinese")
    );

    /// Prevents instantiation of the command-line entry point.
    private Main() {
    }

    /// Runs the application and terminates the process on a nonzero status.
    ///
    /// @param arguments command-line arguments
    public static void main(String[] arguments) {
        int status = run(arguments, System.in, System.out, System.err);
        if (status != 0) {
            System.exit(status);
        }
    }

    /// Runs the command-line workflow using supplied streams.
    ///
    /// @param arguments command-line arguments
    /// @param input     standard input
    /// @param output    standard output
    /// @param error     standard error
    /// @return process status
    static int run(
            String @Unmodifiable [] arguments,
            InputStream input,
            PrintStream output,
            PrintStream error
    ) {
        Arguments parsed;
        try {
            parsed = Arguments.parse(arguments);
        } catch (CliException exception) {
            error.println("usage: kala-encdet [options] [files ...]");
            error.println("kala-encdet: error: " + exception.getMessage());
            return 2;
        }
        if (parsed.help) {
            printHelp(output);
            return 0;
        }
        if (parsed.version) {
            output.println("kala-encdet " + VERSION);
            return 0;
        }

        if (!parsed.files.isEmpty()) {
            int errors = 0;
            for (String file : parsed.files) {
                byte[] data;
                try (InputStream fileInput = Files.newInputStream(Path.of(file))) {
                    data = fileInput.readNBytes(MAX_BYTES);
                } catch (IOException | RuntimeException exception) {
                    error.println("kala-encdet: " + file + ": " + exception.getMessage());
                    errors++;
                    continue;
                }
                try {
                    DetectionResult result = parsed.toDetector().detect(data);
                    printResult(result, file, parsed.minimal, parsed.language, output);
                } catch (RuntimeException exception) {
                    error.println(
                            "kala-encdet: " + file + ": detection failed: " + exception.getMessage()
                    );
                    errors++;
                }
            }
            return errors == parsed.files.size() ? 1 : 0;
        }

        byte[] data;
        try {
            data = input.readNBytes(MAX_BYTES);
        } catch (IOException exception) {
            error.println("kala-encdet: stdin: detection failed: " + exception.getMessage());
            return 1;
        }
        try {
            DetectionResult result = parsed.toDetector().detect(data);
            printResult(result, "stdin", parsed.minimal, parsed.language, output);
            return 0;
        } catch (RuntimeException exception) {
            error.println("kala-encdet: stdin: detection failed: " + exception.getMessage());
            return 1;
        }
    }

    /// Prints one result in reference-compatible detailed or minimal form.
    ///
    /// @param result          detection result
    /// @param label           file name or `stdin`
    /// @param minimal         whether only compact tokens are printed
    /// @param includeLanguage whether language information is printed
    /// @param output          destination stream
    private static void printResult(
            DetectionResult result,
            String label,
            boolean minimal,
            boolean includeLanguage,
            PrintStream output
    ) {
        String encodingText = result.encoding() == null ? "None" : result.encoding().displayName();
        if (minimal) {
            output.println(includeLanguage
                    ? encodingText + " " + languageCode(result)
                    : encodingText);
            return;
        }
        if (includeLanguage) {
            String code = languageCode(result);
            String rawName = LANGUAGE_NAMES.getOrDefault(code, code);
            String name = rawName.isEmpty()
                    ? rawName
                    : rawName.substring(0, 1).toUpperCase(Locale.ROOT) + rawName.substring(1);
            output.println(
                    label + ": " + encodingText + " " + code + " (" + name
                            + ") with confidence " + result.confidence()
            );
        } else {
            output.println(label + ": " + encodingText + " with confidence " + result.confidence());
        }
    }

    /// Returns a result language or the undetermined sentinel.
    ///
    /// @param result detection result
    /// @return ISO language code
    private static String languageCode(DetectionResult result) {
        return result.language() == null ? "und" : result.language();
    }

    /// Prints concise command help.
    ///
    /// @param output destination stream
    private static void printHelp(PrintStream output) {
        output.println("usage: kala-encdet [options] [files ...]");
        output.println("Detect character encoding of files or standard input.");
        output.println();
        output.println("  --minimal                    output only the encoding name");
        output.println("  -l, --language               include detected language");
        output.println("  -e, --encoding-era ERA       modern_web, legacy_iso, legacy_mac,");
        output.println("                                legacy_regional, dos, mainframe, or all");
        output.println("  -i, --include-encodings LIST comma-separated encodings to consider");
        output.println("  -x, --exclude-encodings LIST comma-separated encodings to exclude");
        output.println("  --no-match-encoding NAME     no-match fallback (default: cp1252)");
        output.println("  --empty-input-encoding NAME  empty-input fallback (default: utf-8)");
        output.println("  --version                    show version");
        output.println("  -h, --help                   show this help");
    }

    /// Stores parsed command-line arguments.
    @NotNullByDefault
    private static final class Arguments {
        /// Whether minimal output was requested.
        private boolean minimal;

        /// Whether language output was requested.
        private boolean language;

        /// Whether help was requested.
        private boolean help;

        /// Whether version output was requested.
        private boolean version;

        /// Selected encoding eras.
        private Set<EncodingEra> eras = EnumSet.allOf(EncodingEra.class);

        /// Optional raw include filter.
        private @Nullable Set<String> includeNames;

        /// Optional raw exclude filter.
        private @Nullable Set<String> excludeNames;

        /// Raw no-match fallback.
        private String noMatchName = "cp1252";

        /// Raw empty-input fallback.
        private String emptyInputName = "utf-8";

        /// Positional file names.
        private final List<String> files = new ArrayList<>();

        /// Creates empty default arguments.
        private Arguments() {
        }

        /// Parses supported long and short command-line options.
        ///
        /// @param values raw arguments
        /// @return parsed arguments
        /// @throws CliException for an unknown option, missing value, or invalid era
        private static Arguments parse(String @Unmodifiable [] values) throws CliException {
            Arguments result = new Arguments();
            boolean options = true;
            for (int index = 0; index < values.length; index++) {
                String value = values[index];
                if (options && value.equals("--")) {
                    options = false;
                    continue;
                }
                if (!options || !value.startsWith("-") || value.equals("-")) {
                    result.files.add(value);
                    continue;
                }
                String option = value;
                @Nullable String attached = null;
                int equals = value.indexOf('=');
                if (value.startsWith("--") && equals >= 0) {
                    option = value.substring(0, equals);
                    attached = value.substring(equals + 1);
                }
                switch (option) {
                    case "--minimal" -> result.minimal = true;
                    case "-l", "--language" -> result.language = true;
                    case "-h", "--help" -> result.help = true;
                    case "--version" -> result.version = true;
                    case "-e", "--encoding-era" -> {
                        String argument = attached != null
                                ? attached
                                : nextValue(values, ++index, option);
                        result.eras = parseEra(argument);
                    }
                    case "-i", "--include-encodings" -> {
                        String argument = attached != null
                                ? attached
                                : nextValue(values, ++index, option);
                        result.includeNames = parseEncodingList(argument);
                    }
                    case "-x", "--exclude-encodings" -> {
                        String argument = attached != null
                                ? attached
                                : nextValue(values, ++index, option);
                        result.excludeNames = parseEncodingList(argument);
                    }
                    case "--no-match-encoding" -> result.noMatchName = attached != null
                            ? attached
                            : nextValue(values, ++index, option);
                    case "--empty-input-encoding" -> result.emptyInputName = attached != null
                            ? attached
                            : nextValue(values, ++index, option);
                    default -> throw new CliException("unrecognized argument: " + option);
                }
            }
            return result;
        }

        /// Builds an immutable public detector for one detection call.
        ///
        /// @return configured detector
        private EncodingDetector toDetector() {
            return EncodingDetector.DEFAULT
                    .withEncodingEras(eras)
                    .withIncludedEncodings(resolveEncodingSet(includeNames, "includeEncodings"))
                    .withExcludedEncodings(resolveEncodingSet(excludeNames, "excludeEncodings"))
                    .withNoMatchEncoding(resolveEncoding(noMatchName, "noMatchEncoding"))
                    .withEmptyInputEncoding(resolveEncoding(emptyInputName, "emptyInputEncoding"));
        }

        /// Reads a required option argument.
        ///
        /// @param values raw arguments
        /// @param index  requested value index
        /// @param option option name
        /// @return option value
        /// @throws CliException when no value remains
        private static String nextValue(
                String @Unmodifiable [] values,
                int index,
                String option
        ) throws CliException {
            if (index >= values.length) {
                throw new CliException("argument " + option + ": expected one argument");
            }
            return values[index];
        }

        /// Parses one encoding-era selector.
        ///
        /// @param value lower-case CLI era name
        /// @return selected eras
        /// @throws CliException when the selector is unknown
        private static Set<EncodingEra> parseEra(String value) throws CliException {
            if (value.equals("all")) {
                return EnumSet.allOf(EncodingEra.class);
            }
            try {
                return EnumSet.of(EncodingEra.valueOf(value.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                throw new CliException(
                        "argument --encoding-era: invalid choice: '" + value + "'"
                );
            }
        }

        /// Splits a comma-separated encoding filter and strips surrounding space.
        ///
        /// @param value comma-separated names
        /// @return insertion-ordered raw names
        private static Set<String> parseEncodingList(String value) {
            LinkedHashSet<String> result = new LinkedHashSet<>();
            for (String name : value.split(",", -1)) {
                result.add(name.strip());
            }
            return result;
        }

        /// Resolves an optional set of canonical encoding names or aliases.
        ///
        /// @param values        names to resolve, or `null`
        /// @param parameterName public configuration parameter name
        /// @return resolved encoding identities, or `null`
        /// @throws IllegalArgumentException when a name is unknown
        private static @Nullable Set<Encoding> resolveEncodingSet(
                @Nullable Set<String> values,
                String parameterName
        ) {
            if (values == null) {
                return null;
            }
            LinkedHashSet<Encoding> result = new LinkedHashSet<>(values.size());
            for (String value : values) {
                result.add(resolveEncoding(value, parameterName));
            }
            return result;
        }

        /// Resolves one canonical encoding name or alias.
        ///
        /// @param value         name to resolve
        /// @param parameterName public configuration parameter name
        /// @return resolved encoding identity
        /// @throws IllegalArgumentException when the name is unknown
        private static Encoding resolveEncoding(String value, String parameterName) {
            @Nullable Encoding encoding = EncodingRegistry.lookup(value);
            if (encoding == null) {
                throw new IllegalArgumentException(
                        "Unknown encoding '" + value + "' in " + parameterName
                );
            }
            return encoding;
        }
    }

    /// Reports a command-line syntax error without a stack trace.
    @NotNullByDefault
    private static final class CliException extends Exception {
        /// Creates a syntax error.
        ///
        /// @param message user-facing detail
        private CliException(String message) {
            super(message);
        }
    }
}
