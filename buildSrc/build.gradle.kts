plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    compileOnly("org.jetbrains:annotations:26.1.0")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 17
}
