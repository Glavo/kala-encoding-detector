import kala.encdet.build.DownloadPinnedArchive
import kala.encdet.build.ExtractChardetTestData
import kala.encdet.build.GenerateEncodingResources

plugins {
    id("java-library")
    id("application")
    id("org.glavo.gradle-wrapper-neo") version "0.2.0"
}

val chardetCommit = "e3dfaa1c75256c9d2a06103b566ea92997844f70"
val chardetArchiveSha256 = "a95933ef915caf1a1717d94a5c96b5cc0220b0f576c320ae475f8a3be73751d5"
val cpythonCommit = "c63aec69bd59c55314c06c23f4c22c03de76fe45"
val cpythonArchiveSha256 = "ebe31c63a7e1857bac15f64027d97671732ab7db3379a6601cfd80f08c793ca2"
val testDataCommit = "fa16e9ffde8fd55606e2c7be7423a5fa702cb4a1"
val testDataArchiveSha256 = "a323ac01da5007b41d59cf9e741d862cd14aada832e7182c19be19c1bd984449"

group = "org.glavo"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:26.1.0")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.jetbrains:annotations:26.1.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val downloadChardetSource = tasks.register<DownloadPinnedArchive>("downloadChardetSource") {
    group = "build setup"
    description = "Downloads the pinned chardet source archive"
    sourceUri.set("https://codeload.github.com/chardet/chardet/zip/$chardetCommit")
    expectedSha256.set(chardetArchiveSha256)
    maximumBytes.set(2L * 1024L * 1024L)
    offline.set(gradle.startParameter.isOffline)
    archiveFile.set(
        layout.projectDirectory.file(".gradle/upstream-archives/$chardetArchiveSha256.zip")
    )
}

val downloadCpythonSource = tasks.register<DownloadPinnedArchive>("downloadCpythonSource") {
    group = "build setup"
    description = "Downloads the pinned CPython source archive"
    sourceUri.set("https://codeload.github.com/python/cpython/zip/$cpythonCommit")
    expectedSha256.set(cpythonArchiveSha256)
    maximumBytes.set(40L * 1024L * 1024L)
    offline.set(gradle.startParameter.isOffline)
    archiveFile.set(
        layout.projectDirectory.file(".gradle/upstream-archives/$cpythonArchiveSha256.zip")
    )
}

val generateEncodingResources = tasks.register<GenerateEncodingResources>("generateEncodingResources") {
    group = "build setup"
    description = "Generates deterministic encoding detector resources"
    dependsOn(downloadChardetSource, downloadCpythonSource)
    chardetSourceArchive.set(downloadChardetSource.flatMap { it.archiveFile })
    chardetArchiveRoot.set("chardet-$chardetCommit")
    cpythonSourceArchive.set(downloadCpythonSource.flatMap { it.archiveFile })
    cpythonArchiveRoot.set("cpython-$cpythonCommit")
    outputDirectory.set(layout.buildDirectory.dir("generated-resources/main"))
}

val downloadChardetTestData = tasks.register<DownloadPinnedArchive>("downloadChardetTestData") {
    group = "verification"
    description = "Downloads the pinned chardet test-data archive"
    sourceUri.set("https://codeload.github.com/chardet/test-data/zip/$testDataCommit")
    expectedSha256.set(testDataArchiveSha256)
    maximumBytes.set(32L * 1024L * 1024L)
    offline.set(gradle.startParameter.isOffline)
    archiveFile.set(
        layout.projectDirectory.file(".gradle/upstream-archives/$testDataArchiveSha256.zip")
    )
}

val extractChardetTestData = tasks.register<ExtractChardetTestData>("extractChardetTestData") {
    group = "verification"
    description = "Extracts and verifies the pinned chardet test corpus"
    dependsOn(downloadChardetTestData)
    sourceArchive.set(downloadChardetTestData.flatMap { it.archiveFile })
    archiveRoot.set("test-data-$testDataCommit")
    expectedFileCount.set(2531)
    expectedTotalBytes.set(52_675_437L)
    expectedTreeSha256.set("3d025b519a47ac2ac1eadcb458a28741ef47fa44747b1ff9f64a3d279c7d5d0d")
    outputDirectory.set(layout.buildDirectory.dir("generated-resources/test"))
}

sourceSets {
    main {
        resources.srcDir(generateEncodingResources.flatMap { it.outputDirectory })
    }
    test {
        resources.srcDir(extractChardetTestData.flatMap { it.outputDirectory })
    }
}

application {
    applicationName = "kala-encdet"
    mainModule = "kala.encdet"
    mainClass = "kala.encdet.internal.cli.Main"
    applicationDistribution.from("README.md", "LICENSE", "THIRD_PARTY_NOTICES.md")
}

tasks.jar {
    from(listOf("LICENSE", "THIRD_PARTY_NOTICES.md")) {
        into("META-INF")
    }
    manifest.attributes["Implementation-Version"] = project.version
}

tasks.processResources {
    dependsOn(generateEncodingResources)
    duplicatesStrategy = DuplicatesStrategy.FAIL
}

tasks.processTestResources {
    dependsOn(extractChardetTestData)
    duplicatesStrategy = DuplicatesStrategy.FAIL
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release = 17
}

tasks.test {
    useJUnitPlatform()
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        charSet = "UTF-8"
        addStringOption("Xdoclint:all,-missing", "-quiet")
        tags(
            "apiNote:a:API Note:",
            "implSpec:a:Implementation Requirements:",
            "implNote:a:Implementation Note:"
        )
    }
}
