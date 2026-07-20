// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.build;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/// Downloads one immutable source archive into the build's local archive cache.
///
/// An existing file is reused only when its SHA-256 digest matches. Downloads
/// are written to a sibling temporary file and installed atomically after
/// verification, so an interrupted build cannot expose a partial archive.
@NotNullByDefault
@DisableCachingByDefault(because = "The verified archive is already a persistent local cache entry")
public abstract class DownloadPinnedArchive extends DefaultTask {
    /// Maximum time allowed for one complete HTTP exchange.
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(10);

    /// Creates a pinned-archive download task.
    public DownloadPinnedArchive() {
        getOffline().convention(false);
    }

    /// Returns the HTTPS source URI.
    ///
    /// @return source URI property
    @Input
    public abstract Property<String> getSourceUri();

    /// Returns the expected lower-case SHA-256 digest.
    ///
    /// @return expected digest property
    @Input
    public abstract Property<String> getExpectedSha256();

    /// Returns the maximum accepted archive length.
    ///
    /// @return maximum byte count property
    @Input
    public abstract Property<Long> getMaximumBytes();

    /// Returns whether the build forbids network access.
    ///
    /// This affects failure behavior when no valid cached archive exists, but
    /// does not identify the output and is therefore not a task input.
    ///
    /// @return offline-state property
    @Internal
    public abstract Property<Boolean> getOffline();

    /// Returns the persistent verified archive file.
    ///
    /// @return archive output property
    @OutputFile
    public abstract RegularFileProperty getArchiveFile();

    /// Reuses or downloads and verifies the configured archive.
    @TaskAction
    public final void download() {
        String expected = normalizeDigest(getExpectedSha256().get());
        Path destination = getArchiveFile().get().getAsFile().toPath();
        long maximumBytes = getMaximumBytes().get();
        if (maximumBytes <= 0L) {
            throw new GradleException("maximumBytes must be positive");
        }
        if (isValidCachedArchive(destination, expected, maximumBytes)) {
            return;
        }
        if (getOffline().get()) {
            throw new GradleException(
                    "No verified cached archive is available while Gradle is offline: " + destination
            );
        }

        URI source = URI.create(getSourceUri().get());
        if (!"https".equalsIgnoreCase(source.getScheme())) {
            throw new GradleException("Pinned archives must use HTTPS: " + source);
        }

        Path parent = Objects.requireNonNull(
                destination.getParent(),
                "Archive output must have a parent directory"
        );
        try {
            Files.createDirectories(parent);
        } catch (IOException exception) {
            throw new GradleException("Unable to create archive cache directory: " + parent, exception);
        }

        Path temporary;
        try {
            temporary = Files.createTempFile(parent, destination.getFileName() + ".", ".part");
        } catch (IOException exception) {
            throw new GradleException("Unable to create temporary archive beside " + destination, exception);
        }

        try {
            download(source, temporary, maximumBytes);
            String actual = sha256(temporary);
            if (!expected.equals(actual)) {
                throw new GradleException(
                        "SHA-256 mismatch for " + source + ": expected " + expected + " but found " + actual
                );
            }
            installWithLock(temporary, destination, expected, maximumBytes);
        } finally {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException exception) {
                getLogger().warn("Unable to remove incomplete archive {}", temporary, exception);
            }
        }
    }

    /// Streams an HTTPS response into a bounded temporary file.
    ///
    /// @param source       source URI
    /// @param destination temporary destination
    /// @param maximumBytes maximum accepted byte count
    private static void download(URI source, Path destination, long maximumBytes) {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest request = HttpRequest.newBuilder(source)
                .GET()
                .header("User-Agent", "kala-encoding-detector-build")
                .timeout(REQUEST_TIMEOUT)
                .build();
        HttpResponse.BodyHandler<Path> handler = responseInfo -> {
            long contentLength = contentLength(responseInfo.headers(), source);
            if (contentLength > maximumBytes) {
                throw new GradleException(
                        "Archive from " + source + " exceeds the " + maximumBytes + " byte limit"
                );
            }
            return new BoundedFileSubscriber(destination, maximumBytes, source);
        };
        CompletableFuture<HttpResponse<Path>> future = client.sendAsync(request, handler);
        HttpResponse<Path> response;
        try {
            response = future.get(REQUEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new GradleException("Interrupted while downloading " + source, exception);
        } catch (TimeoutException exception) {
            future.cancel(true);
            throw new GradleException("Timed out while downloading " + source, exception);
        } catch (ExecutionException exception) {
            @Nullable Throwable cause = exception.getCause();
            if (cause instanceof GradleException gradleException) {
                throw gradleException;
            }
            throw new GradleException(
                    "Unable to download " + source,
                    cause == null ? exception : cause
            );
        }
        if (response.statusCode() != 200) {
            throw new GradleException(
                    "Unable to download " + source + ": HTTP " + response.statusCode()
            );
        }
        long contentLength = contentLength(response.headers(), source);
        long total;
        try {
            total = Files.size(destination);
        } catch (IOException exception) {
            throw new GradleException("Unable to inspect archive from " + source, exception);
        }
        if (total > maximumBytes) {
            throw new GradleException(
                    "Archive from " + source + " exceeds the " + maximumBytes + " byte limit"
            );
        }
        if (contentLength >= 0L && total != contentLength) {
            throw new GradleException(
                    "Incomplete archive from " + source + ": expected " + contentLength
                            + " bytes but received " + total
            );
        }
    }

    /// Reads and validates a consistent HTTP content length.
    ///
    /// @param headers response headers
    /// @param source response source for diagnostics
    /// @return parsed byte count, or `-1` when absent
    private static long contentLength(java.net.http.HttpHeaders headers, URI source) {
        @Unmodifiable List<String> values = headers.allValues("Content-Length");
        if (values.isEmpty()) {
            return -1L;
        }
        long expected = parseContentLength(values.get(0), source);
        for (int index = 1; index < values.size(); index++) {
            long current = parseContentLength(values.get(index), source);
            if (current != expected) {
                throw new GradleException("Conflicting Content-Length headers from " + source);
            }
        }
        return expected;
    }

    /// Parses one nonnegative HTTP content length.
    ///
    /// @param value header value
    /// @param source response source for diagnostics
    /// @return parsed byte count
    private static long parseContentLength(String value, URI source) {
        try {
            long length = Long.parseLong(value);
            if (length >= 0L) {
                return length;
            }
        } catch (NumberFormatException exception) {
            throw new GradleException("Invalid Content-Length from " + source + ": " + value, exception);
        }
        throw new GradleException("Invalid Content-Length from " + source + ": " + value);
    }

    /// Tests whether a bounded cache file already has the expected identity.
    ///
    /// @param path cache path
    /// @param expectedSha256 expected lower-case digest
    /// @param maximumBytes maximum accepted byte count
    /// @return whether the cache entry is reusable
    private static boolean isValidCachedArchive(
            Path path,
            String expectedSha256,
            long maximumBytes
    ) {
        try {
            return Files.isRegularFile(path)
                    && Files.size(path) <= maximumBytes
                    && expectedSha256.equals(sha256(path));
        } catch (GradleException | IOException exception) {
            return false;
        }
    }

    /// Installs a verified archive while serializing competing build processes.
    ///
    /// @param temporary verified temporary file
    /// @param destination stable cache path
    /// @param expectedSha256 expected lower-case digest
    /// @param maximumBytes maximum accepted byte count
    private static void installWithLock(
            Path temporary,
            Path destination,
            String expectedSha256,
            long maximumBytes
    ) {
        Path lockPath = destination.resolveSibling(destination.getFileName() + ".lock");
        try (FileChannel channel = FileChannel.open(
                lockPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
        ); FileLock ignored = channel.lock()) {
            if (!isValidCachedArchive(destination, expectedSha256, maximumBytes)) {
                install(temporary, destination);
            }
        } catch (IOException exception) {
            throw new GradleException("Unable to lock archive cache entry " + destination, exception);
        }
    }

    /// Installs a verified temporary file at its stable cache path.
    ///
    /// @param temporary  verified temporary file
    /// @param destination final cache path
    private static void install(Path temporary, Path destination) {
        try {
            Files.move(
                    temporary,
                    destination,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException exception) {
            try {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException fallbackException) {
                throw new GradleException("Unable to install verified archive at " + destination, fallbackException);
            }
        } catch (IOException exception) {
            throw new GradleException("Unable to install verified archive at " + destination, exception);
        }
    }

    /// Computes the lower-case SHA-256 digest of a file.
    ///
    /// @param path file to hash
    /// @return lower-case hexadecimal digest
    private static String sha256(Path path) {
        MessageDigest digest = newDigest();
        try (InputStream input = new DigestInputStream(Files.newInputStream(path), digest)) {
            input.transferTo(OutputStream.nullOutputStream());
        } catch (IOException exception) {
            throw new GradleException("Unable to hash archive " + path, exception);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /// Validates and normalizes one SHA-256 string.
    ///
    /// @param value configured digest
    /// @return lower-case digest
    private static String normalizeDigest(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if (!normalized.matches("[0-9a-f]{64}")) {
            throw new GradleException("Invalid SHA-256 digest: " + value);
        }
        return normalized;
    }

    /// Creates a SHA-256 message digest.
    ///
    /// @return new digest instance
    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }

    /// Writes an HTTP response body to a file while enforcing a byte limit.
    @NotNullByDefault
    private static final class BoundedFileSubscriber implements HttpResponse.BodySubscriber<Path> {
        /// File-writing subscriber supplied by the JDK HTTP client.
        private final HttpResponse.BodySubscriber<Path> delegate;

        /// Maximum accepted byte count.
        private final long maximumBytes;

        /// Source URI used in failure messages.
        private final URI source;

        /// Upstream flow subscription after [#onSubscribe(Flow.Subscription)].
        private @Nullable Flow.Subscription subscription;

        /// Number of body bytes observed so far.
        private long received;

        /// Whether a local size failure has already terminated the delegate.
        private boolean failed;

        /// Creates a bounded subscriber that writes one response body.
        ///
        /// @param destination temporary destination file
        /// @param maximumBytes maximum accepted byte count
        /// @param source response source for diagnostics
        private BoundedFileSubscriber(Path destination, long maximumBytes, URI source) {
            this.delegate = HttpResponse.BodySubscribers.ofFile(
                    destination,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
            this.maximumBytes = maximumBytes;
            this.source = source;
        }

        /// Returns the delegate's completion stage.
        ///
        /// @return response-body file stage
        @Override
        public CompletionStage<Path> getBody() {
            return delegate.getBody();
        }

        /// Records and forwards the upstream subscription.
        ///
        /// @param value upstream subscription
        @Override
        public void onSubscribe(Flow.Subscription value) {
            subscription = value;
            delegate.onSubscribe(value);
        }

        /// Forwards one body chunk unless it would exceed the byte limit.
        ///
        /// @param buffers response body buffers
        @Override
        public void onNext(List<ByteBuffer> buffers) {
            if (failed) {
                return;
            }
            long chunkBytes = 0L;
            for (ByteBuffer buffer : buffers) {
                chunkBytes += buffer.remaining();
            }
            if (chunkBytes > maximumBytes - received) {
                failed = true;
                Flow.Subscription current = Objects.requireNonNull(subscription);
                current.cancel();
                delegate.onError(new IOException(
                        "Archive from " + source + " exceeds the " + maximumBytes + " byte limit"
                ));
                return;
            }
            received += chunkBytes;
            delegate.onNext(buffers);
        }

        /// Forwards an upstream HTTP failure once.
        ///
        /// @param throwable response failure
        @Override
        public void onError(Throwable throwable) {
            if (!failed) {
                delegate.onError(throwable);
            }
        }

        /// Completes the delegate after the bounded body has finished.
        @Override
        public void onComplete() {
            if (!failed) {
                delegate.onComplete();
            }
        }
    }
}
