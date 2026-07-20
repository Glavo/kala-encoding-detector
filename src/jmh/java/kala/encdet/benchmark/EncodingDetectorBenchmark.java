// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.benchmark;

import kala.encdet.DetectionResult;
import kala.encdet.EncodingDetector;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/// Measures warmed encoding detection across array, heap-buffer, and direct-buffer inputs.
///
/// The benchmark matrix covers fast ASCII and UTF-8 paths as well as a statistical
/// Windows-1252 path at small, model-window, and maximum default detector sizes.
@NotNullByDefault
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(2)
public class EncodingDetectorBenchmark {
    /// Shared immutable detector used by every benchmark invocation.
    private static final EncodingDetector DETECTOR = EncodingDetector.DEFAULT;

    /// Repeated ASCII input pattern.
    private static final byte @Unmodifiable [] ASCII_PATTERN = (
            "The quick brown fox jumps over the lazy dog. Encoding detection benchmark. "
    ).getBytes(StandardCharsets.US_ASCII);

    /// Repeated valid UTF-8 input pattern.
    private static final byte @Unmodifiable [] UTF_8_PATTERN = (
            "UTF-8 text: Καλημέρα κόσμε. こんにちは世界。 Привет, мир. "
    ).getBytes(StandardCharsets.UTF_8);

    /// Repeated Windows-1252 input pattern with representative high bytes.
    private static final byte @Unmodifiable [] WINDOWS_1252_PATTERN = {
            'C', 'a', 'f', (byte) 0xe9, ' ',
            'd', (byte) 0xe9, 'j', (byte) 0xe0, ' ',
            'v', 'u', ' ', (byte) 0x97, ' ',
            'r', (byte) 0xe9, 's', 'u', 'm', (byte) 0xe9, ',', ' ',
            'n', 'a', (byte) 0xef, 'v', 'e', ' ',
            'f', 'a', (byte) 0xe7, 'a', 'd', 'e', ';', ' ',
            'p', 'i', (byte) 0xf1, 'a', 't', 'a', '.', ' '
    };

    /// Creates a benchmark instance for JMH.
    public EncodingDetectorBenchmark() {
    }

    /// Detects one array-backed input through the `byte[]` API.
    ///
    /// @param state prepared benchmark input
    /// @return highest-ranked detection result
    @Benchmark
    public DetectionResult detectByteArray(InputState state) {
        return DETECTOR.detect(state.data);
    }

    /// Detects one array-backed input through the heap [ByteBuffer] API.
    ///
    /// @param state prepared benchmark input
    /// @return highest-ranked detection result
    @Benchmark
    public DetectionResult detectHeapByteBuffer(InputState state) {
        return DETECTOR.detect(state.heapBuffer);
    }

    /// Detects one off-heap input through the direct [ByteBuffer] API.
    ///
    /// @param state prepared benchmark input
    /// @return highest-ranked detection result
    @Benchmark
    public DetectionResult detectDirectByteBuffer(InputState state) {
        return DETECTOR.detect(state.directBuffer);
    }

    /// Produces the complete candidate list through the `byte[]` API.
    ///
    /// @param state prepared benchmark input
    /// @return immutable ordered detection candidates
    @Benchmark
    public @Unmodifiable List<DetectionResult> detectAllUnfilteredByteArray(InputState state) {
        return DETECTOR.detectAllUnfiltered(state.data);
    }

    /// Returns the fixed byte pattern selected by one JMH parameter.
    ///
    /// @param content input-content parameter
    /// @return immutable nonempty byte pattern
    private static byte @Unmodifiable [] patternFor(String content) {
        return switch (content) {
            case "ASCII" -> ASCII_PATTERN;
            case "UTF_8" -> UTF_8_PATTERN;
            case "WINDOWS_1252" -> WINDOWS_1252_PATTERN;
            default -> throw new IllegalArgumentException("Unknown benchmark content: " + content);
        };
    }

    /// Creates an exact-size input from complete pattern repetitions and ASCII padding.
    ///
    /// Padding instead of truncating the last pattern preserves valid UTF-8 input.
    ///
    /// @param pattern nonempty pattern
    /// @param size positive result length
    /// @return newly allocated input bytes
    private static byte @Unmodifiable [] repeatPattern(
            byte @Unmodifiable [] pattern,
            int size
    ) {
        if (pattern.length == 0) {
            throw new IllegalArgumentException("pattern must not be empty");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be positive");
        }

        byte[] result = new byte[size];
        int offset = 0;
        while (offset <= size - pattern.length) {
            System.arraycopy(pattern, 0, result, offset, pattern.length);
            offset += pattern.length;
        }
        Arrays.fill(result, offset, result.length, (byte) ' ');
        return result;
    }

    /// Holds one immutable input in all three supported storage forms.
    @NotNullByDefault
    @State(Scope.Thread)
    public static class InputState {
        /// Selects the input's byte distribution.
        @Param({"ASCII", "UTF_8", "WINDOWS_1252"})
        public String content = "ASCII";

        /// Selects the exact input length in bytes.
        @Param({"1024", "16384", "200000"})
        public int size = 1024;

        /// Prepared array input.
        private byte @Unmodifiable [] data = new byte[0];

        /// Prepared heap buffer treated as unmodifiable during measurement.
        private @UnmodifiableView ByteBuffer heapBuffer = ByteBuffer.allocate(0);

        /// Prepared direct buffer treated as unmodifiable during measurement.
        private @UnmodifiableView ByteBuffer directBuffer = ByteBuffer.allocateDirect(0);

        /// Creates an initially empty JMH state.
        public InputState() {
        }

        /// Creates the selected input once for the benchmark trial.
        @Setup(Level.Trial)
        public void setUp() {
            data = repeatPattern(patternFor(content), size);
            heapBuffer = ByteBuffer.wrap(data);

            ByteBuffer writableDirectBuffer = ByteBuffer.allocateDirect(data.length);
            writableDirectBuffer.put(data).flip();
            directBuffer = writableDirectBuffer;
        }
    }
}
