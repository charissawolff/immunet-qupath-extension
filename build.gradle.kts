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
    name = "vectra-extension"
    group = "computational-immunology-group"
    version = "0.1.0-SNAPSHOT"
    description = "For the vectra server"
    automaticModule = "io.github.qupath.extension.template"
}

// TODO: Define your dependencies here
dependencies {

    // Main dependencies for most QuPath extensions
    shadow(libs.bundles.qupath)
    shadow(libs.bundles.logging)
    shadow(libs.qupath.fxtras)

    // Third-party libraries used by the extension source.
    // Bundled into the shadow jar so they're available at runtime.
    implementation("org.json:json:20250107")
    implementation("org.apache.sshd:sshd-core:2.14.0")
    implementation("org.controlsfx:controlsfx:11.2.3")

    // Compile-only nullness annotations (not needed at runtime)
    compileOnly("org.checkerframework:checker-qual:3.49.0")

    // For testing
    testImplementation(libs.bundles.qupath)
    testImplementation(libs.junit)

}
