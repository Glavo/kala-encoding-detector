plugins {
    id("java-library")
    id("application")
    id("org.glavo.gradle-wrapper-neo") version "0.2.0"
}

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
