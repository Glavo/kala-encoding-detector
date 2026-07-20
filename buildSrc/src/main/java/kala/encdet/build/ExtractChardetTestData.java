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

    /// Returns the complete path, length, and digest inventory.
    ///
    /// @return inventory input property
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInventory();

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
                    getInventory().get().getAsFile().toPath(),
                    getOutputDirectory().get().getAsFile().toPath()
            );
        } catch (IOException | IllegalArgumentException exception) {
            throw new GradleException("Unable to extract chardet test data", exception);
        }
    }
}
