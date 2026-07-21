// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Verifies bundled resource identities and isolated failure behavior.
@NotNullByDefault
final class ResourceIntegrityTest {
    /// Expected main-resource SHA-256 digests in manifest order.
    private static final @Unmodifiable Map<String, String> EXPECTED_HASHES = expectedHashes();

    /// Verifies every generated or copied main resource has its recorded digest.
    ///
    /// @throws IOException if a resource cannot be read
    @Test
    void bundledResourcesMatchRecordedHashes() throws IOException {
        for (Map.Entry<String, String> entry : EXPECTED_HASHES.entrySet()) {
            assertEquals(entry.getValue(), sha256(resourceBytes(entry.getKey())), entry.getKey());
        }
    }

    /// Verifies encoding metadata has no companion classpath resource.
    @Test
    void registryResourceIsNotBundled() {
        assertNull(
                ResourceIntegrityTest.class.getResource("/kala/encdet/internal/registry.tsv")
        );
    }

    /// Verifies the fixed dense model count and exact IDF table size.
    ///
    /// @throws IOException if a resource cannot be read
    @Test
    void modelHeadersMatchPinnedFormat() throws IOException {
        ByteBuffer models = ByteBuffer.wrap(resourceBytes("models.bin")).order(ByteOrder.BIG_ENDIAN);
        assertEquals(0x434d4432, models.getInt());
        assertEquals(352, models.getInt());
        assertEquals(65_536, resourceBytes("idf.bin").length);
        assertEquals(8_214, resourceBytes("confusion.bin").length);
    }

    /// Verifies missing and empty model resources disable scoring without failing initialization.
    ///
    /// @param directory isolated resource directory
    /// @throws Exception if a class loader or fixture cannot be created
    @Test
    void missingAndEmptyModelsDegradeGracefully(@TempDir Path directory) throws Exception {
        initializeHolderWithMissingResource(
                "kala/encdet/internal/models.bin",
                "kala.encdet.internal.ModelStore$ModelsHolder"
        );
        initializeHolderWithOverride(
                directory,
                "kala/encdet/internal/models.bin",
                new byte[0],
                "kala.encdet.internal.ModelStore$ModelsHolder"
        );
    }

    /// Verifies missing and empty confusion resources disable disambiguation.
    ///
    /// @param directory isolated resource directory
    /// @throws Exception if a class loader or fixture cannot be created
    @Test
    void missingAndEmptyConfusionDataDegradeGracefully(@TempDir Path directory) throws Exception {
        initializeHolderWithMissingResource(
                "kala/encdet/internal/confusion.bin",
                "kala.encdet.internal.ConfusionResolver$Holder"
        );
        initializeHolderWithOverride(
                directory,
                "kala/encdet/internal/confusion.bin",
                new byte[0],
                "kala.encdet.internal.ConfusionResolver$Holder"
        );
    }

    /// Verifies nonempty malformed model data reports a clear installation failure.
    ///
    /// @param directory isolated resource directory
    /// @throws Exception if a class loader or fixture cannot be created
    @Test
    void corruptModelsFailClearly(@TempDir Path directory) throws Exception {
        ExceptionInInitializerError error = assertThrows(
                ExceptionInInitializerError.class,
                () -> initializeHolderWithOverride(
                        directory,
                        "kala/encdet/internal/models.bin",
                        new byte[]{'N', 'O', 'P', 'E'},
                        "kala.encdet.internal.ModelStore$ModelsHolder"
                )
        );
        IllegalStateException cause = assertInstanceOf(IllegalStateException.class, error.getCause());
        assertEquals("Corrupt models.bin: missing CMD2 magic", cause.getMessage());
    }

    /// Verifies nonempty truncated confusion data reports a clear installation failure.
    ///
    /// @param directory isolated resource directory
    /// @throws Exception if a class loader or fixture cannot be created
    @Test
    void corruptConfusionDataFailsClearly(@TempDir Path directory) throws Exception {
        ExceptionInInitializerError error = assertThrows(
                ExceptionInInitializerError.class,
                () -> initializeHolderWithOverride(
                        directory,
                        "kala/encdet/internal/confusion.bin",
                        new byte[]{0, 1},
                        "kala.encdet.internal.ConfusionResolver$Holder"
                )
        );
        IllegalStateException cause = assertInstanceOf(IllegalStateException.class, error.getCause());
        assertEquals("Corrupt confusion.bin: missing name length", cause.getMessage());
    }

    /// Initializes a private resource holder while hiding its target resource.
    ///
    /// @param resourceName classpath resource to hide
    /// @param holderName   binary holder class name
    /// @throws Exception if class initialization fails unexpectedly
    private static void initializeHolderWithMissingResource(
            String resourceName,
            String holderName
    ) throws Exception {
        URL classes = ModelStore.class.getProtectionDomain().getCodeSource().getLocation();
        try (URLClassLoader loader = new MissingResourceClassLoader(classes, resourceName)) {
            Class.forName(holderName, true, loader);
        }
    }

    /// Initializes a private resource holder with one resource overridden.
    ///
    /// @param directory    isolated resource root
    /// @param resourceName resource path to override
    /// @param data         replacement bytes
    /// @param holderName   binary holder class name
    /// @throws Exception if fixture creation or class initialization fails
    private static void initializeHolderWithOverride(
            Path directory,
            String resourceName,
            byte @Unmodifiable [] data,
            String holderName
    ) throws Exception {
        Path resource = directory.resolve(resourceName);
        Files.createDirectories(resource.getParent());
        Files.write(resource, data);
        URL classes = ModelStore.class.getProtectionDomain().getCodeSource().getLocation();
        try (URLClassLoader loader = new URLClassLoader(
                new URL[]{directory.toUri().toURL(), classes},
                ClassLoader.getPlatformClassLoader()
        )) {
            Class.forName(holderName, true, loader);
        }
    }

    /// Reads one required internal resource.
    ///
    /// @param fileName resource leaf name
    /// @return complete bytes
    /// @throws IOException if the resource cannot be read
    private static byte[] resourceBytes(String fileName) throws IOException {
        String path = "/kala/encdet/internal/" + fileName;
        try (@Nullable InputStream input = ResourceIntegrityTest.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("Missing resource: " + path);
            }
            return input.readAllBytes();
        }
    }

    /// Computes a lower-case SHA-256 digest.
    ///
    /// @param data bytes to hash
    /// @return hexadecimal digest
    private static String sha256(byte @Unmodifiable [] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }

    /// Creates the immutable expected resource digest map.
    ///
    /// @return ordered resource digests
    private static @Unmodifiable Map<String, String> expectedHashes() {
        LinkedHashMap<String, String> hashes = new LinkedHashMap<>();
        hashes.put("confusion.bin", "1c853cd04b400bd2d3772fcaddc4466b32d32db703a7aa415df5a20810bf9ecb");
        hashes.put("hz-validity.bin", "313e548cab6a250d0ae374c9b029475dbe9248632f4940f8aa95f30d7a16c3e2");
        hashes.put("idf.bin", "8306e28aad48cac54db7834d09f49cbbb92d3928ede53c71f2b9b232b586bb8a");
        hashes.put("models.bin", "07eb1dabcf4f8e714f9f866eaa355121bf7b3563dcde0d77ed7c3668de5f75f5");
        hashes.put("multibyte-validity.bin", "cabce96fd96e6bba5fff346a9d6c34bd9a0550f89c91be2a3c7f68ad364cf804");
        hashes.put("single-byte-decode.bin", "63912710247ec04e923f411d7022cfa3bbc3f3af2a5af9c3eaa3c601e65ff030");
        hashes.put("validity.tsv", "f03213c64ec130fc5c00520f8a69753235438e79f64ad4690b0e13d5d8183509");
        return java.util.Collections.unmodifiableMap(hashes);
    }

    /// Loads project classes while hiding one selected resource.
    @NotNullByDefault
    private static final class MissingResourceClassLoader extends URLClassLoader {
        /// Hidden classpath resource name.
        private final String hiddenResource;

        /// Creates an isolated loader with one hidden resource.
        ///
        /// @param classes        compiled main-class root
        /// @param hiddenResource resource name to hide
        private MissingResourceClassLoader(URL classes, String hiddenResource) {
            super(new URL[]{classes}, ClassLoader.getPlatformClassLoader());
            this.hiddenResource = hiddenResource;
        }

        /// Returns no URL for the selected hidden resource.
        ///
        /// @param name resource name
        /// @return located URL, or `null`
        @Override
        public @Nullable URL getResource(String name) {
            return name.equals(hiddenResource) ? null : super.getResource(name);
        }
    }
}
