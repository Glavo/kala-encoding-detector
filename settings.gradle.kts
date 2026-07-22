import java.util.Properties

val projectMetadata = Properties().apply {
    load(
        providers.fileContents(
            layout.settingsDirectory.file("gradle/project.properties")
        ).asText.get().reader()
    )
}
val projectName = requireNotNull(projectMetadata.getProperty("name")) {
    "Missing name in gradle/project.properties"
}
require(projectName.isNotEmpty() && projectName.none(Char::isWhitespace)) {
    "name in gradle/project.properties must be a non-empty value without whitespace"
}

org.apache.tools.ant.DirectoryScanner.removeDefaultExclude("**/.gitignore")

rootProject.name = projectName
