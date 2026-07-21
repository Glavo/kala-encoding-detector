// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.build;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemOperations;
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

import javax.inject.Inject;

import java.io.IOException;

/// Generates all runtime resources required by the detector.
///
/// The task extracts the pinned statistical files and builds the multibyte
/// validity and decoding tables from pinned source archives.
@NotNullByDefault
@CacheableTask
public abstract class GenerateEncodingResources extends DefaultTask {
    /// Creates a runtime-resource generation task.
    public GenerateEncodingResources() {
    }

    /// Returns the verified chardet source archive.
    ///
    /// @return archive input property
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getChardetSourceArchive();

    /// Returns the exact root directory expected inside the source archive.
    ///
    /// @return archive root property
    @Input
    public abstract Property<String> getChardetArchiveRoot();

    /// Returns the verified CPython source archive.
    ///
    /// @return archive input property
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getCpythonSourceArchive();

    /// Returns the exact root directory expected inside the CPython archive.
    ///
    /// @return archive root property
    @Input
    public abstract Property<String> getCpythonArchiveRoot();

    /// Returns the generated classpath resource root.
    ///
    /// @return output directory property
    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    /// Returns the Gradle file-system service used to clear stale generated output.
    ///
    /// @return file-system operations service
    @Inject
    protected abstract FileSystemOperations getFileSystemOperations();

    /// Generates and verifies every runtime resource.
    @TaskAction
    public final void generate() {
        getFileSystemOperations().delete(spec -> spec.delete(getOutputDirectory()));
        try {
            EncodingResourceGenerator.generate(
                    getChardetSourceArchive().get().getAsFile().toPath(),
                    getChardetArchiveRoot().get(),
                    getCpythonSourceArchive().get().getAsFile().toPath(),
                    getCpythonArchiveRoot().get(),
                    getOutputDirectory().get().getAsFile().toPath()
            );
        } catch (IOException | IllegalArgumentException exception) {
            throw new GradleException("Unable to generate encoding detector resources", exception);
        }
    }
}
