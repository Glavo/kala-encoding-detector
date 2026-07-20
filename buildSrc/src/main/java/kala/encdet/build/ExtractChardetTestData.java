// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.build;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;

/// Extracts the fixed upstream test-data snapshot as generated test resources.
@NotNullByDefault
@CacheableTask
public abstract class ExtractChardetTestData extends DefaultTask {
    /// Creates a corpus extraction task.
    public ExtractChardetTestData() {
    }

    /// Returns the verified test-data source archive.
    ///
    /// @return archive input property
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getSourceArchive();

    /// Returns the exact root directory expected inside the source archive.
    ///
    /// @return archive root property
    @Input
    public abstract Property<String> getArchiveRoot();

    /// Returns the exact expected number of corpus files.
    ///
    /// @return file-count input property
    @Input
    public abstract Property<Integer> getExpectedFileCount();

    /// Returns the exact expected aggregate uncompressed byte length.
    ///
    /// @return aggregate-length input property
    @Input
    public abstract Property<Long> getExpectedTotalBytes();

    /// Returns the expected canonical corpus-tree SHA-256 digest.
    ///
    /// @return lower-case digest input property
    @Input
    public abstract Property<String> getExpectedTreeSha256();

    /// Returns the generated test-resource root.
    ///
    /// @return output directory property
    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    /// Extracts and verifies the complete corpus snapshot.
    @TaskAction
    public final void extract() {
        try {
            TestCorpusExtractor.extract(
                    getSourceArchive().get().getAsFile().toPath(),
                    getArchiveRoot().get(),
                    getExpectedFileCount().get(),
                    getExpectedTotalBytes().get(),
                    getExpectedTreeSha256().get(),
                    getOutputDirectory().get().getAsFile().toPath()
            );
        } catch (IOException | IllegalArgumentException exception) {
            throw new GradleException("Unable to extract chardet test data", exception);
        }
    }
}
