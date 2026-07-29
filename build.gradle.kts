plugins {
    // Support writing the extension in Groovy (remove this if you don't want to)
    groovy
    // To optionally create a shadow/fat jar that bundle up any non-core dependencies
    id("com.gradleup.shadow") version "8.3.5"
    // QuPath Gradle extension convention plugin
    id("qupath-conventions")
}

// TODO: Configure your extension here (please change the defaults!)
qupathExtension {
    name = "ImmuNet Extension"
    group = "computational-immunology-group"
    version = "0.1.0-SNAPSHOT"
    description = "For the vectra images server"
    automaticModule = "io.github.qupath.extension.template"
}

val osClassifier = when {
    org.gradle.internal.os.OperatingSystem.current().isWindows -> "win"
    org.gradle.internal.os.OperatingSystem.current().isMacOsX -> "mac"
    else -> "linux"
}

dependencies {
    // ...existing...
    testImplementation("org.openjfx:javafx-base:21:$osClassifier")
    testImplementation("org.openjfx:javafx-graphics:21:$osClassifier")
    testImplementation("org.openjfx:javafx-controls:21:$osClassifier")
}



// TODO: Define your dependencies here
dependencies {

    // Main dependencies for most QuPath extensions
    shadow(libs.bundles.qupath)
    shadow(libs.bundles.logging)
    shadow(libs.qupath.fxtras)

    // If you aren't using Groovy, this can be removed
    shadow(libs.bundles.groovy)

    // Third-party libraries used by the extension source.
    // Bundled into the shadow jar so they're available at runtime.
    implementation("org.json:json:20250107")
    implementation("org.apache.sshd:sshd-core:2.14.0")

    // Compile-only nullness annotations (not needed at runtime)
    compileOnly("org.checkerframework:checker-qual:3.49.0")

    // For testing
    testImplementation(libs.bundles.qupath)
    testImplementation(libs.junit)
    testImplementation("org.testfx:testfx-junit5:4.0.18")
    testImplementation("org.testfx:testfx-core:4.0.18")
    testImplementation("org.hamcrest:hamcrest:2.2")

    testImplementation("org.openjfx:javafx-base:21:$osClassifier")
    testImplementation("org.openjfx:javafx-graphics:21:$osClassifier")
    testImplementation("org.openjfx:javafx-controls:21:$osClassifier")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

}

