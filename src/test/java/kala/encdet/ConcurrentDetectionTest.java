// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies concurrent use of immutable detector instances and shared model state.
@NotNullByDefault
final class ConcurrentDetectionTest {
    /// Runs many simultaneous statistical detections and checks deterministic results.
    ///
    /// @throws Exception if a worker fails or is interrupted
    @Test
    void concurrentDetectionHasNoSharedStateCorruption() throws Exception {
        byte[] data = (
                "Les élèves français étudient la littérature européenne avec enthousiasme. "
                        + "Après les études, ils préfèrent dîner dans un café."
        ).getBytes(StandardCharsets.ISO_8859_1);
        EncodingDetector detector = EncodingDetector.DEFAULT;
        DetectionResult expected = detector.detect(data);
        int workers = 24;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        try {
            List<Callable<DetectionResult>> tasks = new ArrayList<>(workers);
            for (int index = 0; index < workers; index++) {
                tasks.add(() -> {
                    start.await();
                    DetectionResult result = expected;
                    for (int iteration = 0; iteration < 50; iteration++) {
                        result = detector.detect(data);
                    }
                    return result;
                });
            }
            List<Future<DetectionResult>> futures = new ArrayList<>(workers);
            for (Callable<DetectionResult> task : tasks) {
                futures.add(executor.submit(task));
            }
            start.countDown();
            for (Future<DetectionResult> future : futures) {
                assertEquals(expected, future.get());
            }
        } finally {
            executor.shutdownNow();
        }
    }

    /// Verifies concurrent first use initializes an isolated registry and model store once.
    ///
    /// @throws Exception if isolated class loading or a worker fails
    @Test
    void coldInitializationIsThreadSafe() throws Exception {
        byte[] data = (
                "Les élèves français étudient la littérature européenne avec enthousiasme. "
                        + "Après les études, ils préfèrent dîner dans un café."
        ).getBytes(StandardCharsets.ISO_8859_1);
        String expected = EncodingDetector.DEFAULT.detect(data).toString();
        URL classes = EncodingDetector.class.getProtectionDomain().getCodeSource().getLocation();
        URL resources = mainResourceRoot("kala/encdet/internal/models.bin");
        try (URLClassLoader loader = new URLClassLoader(
                new URL[]{classes, resources},
                ClassLoader.getPlatformClassLoader()
        )) {
            Class<?> detectorClass = Class.forName("kala.encdet.EncodingDetector", false, loader);
            Object defaultDetector = detectorClass.getField("DEFAULT").get(null);
            Method detect = detectorClass.getMethod("detect", byte[].class);
            int workers = 24;
            CountDownLatch start = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(workers);
            try {
                List<Future<String>> futures = new ArrayList<>(workers);
                for (int index = 0; index < workers; index++) {
                    futures.add(executor.submit(() -> {
                        start.await();
                        Object result = detect.invoke(defaultDetector, (Object) data);
                        return result.toString();
                    }));
                }
                start.countDown();
                for (Future<String> future : futures) {
                    assertEquals(expected, future.get());
                }
            } finally {
                executor.shutdownNow();
            }
        }
    }

    /// Returns the classpath root containing one main resource.
    ///
    /// @param name resource name relative to its root
    /// @return main-resource root URL
    private static URL mainResourceRoot(String name) {
        URL resource = ConcurrentDetectionTest.class.getClassLoader().getResource(name);
        if (resource == null) {
            throw new IllegalStateException("Missing main resource: " + name);
        }
        String external = resource.toExternalForm();
        try {
            return java.net.URI.create(external.substring(0, external.length() - name.length()))
                    .toURL();
        } catch (java.net.MalformedURLException exception) {
            throw new IllegalStateException("Invalid main resource URL: " + external, exception);
        }
    }
}
