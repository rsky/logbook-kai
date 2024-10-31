group = "logbook"
description = "logbook-kai"
version = "24.10.3"

val fatJarFullName = "logbook-kai-${version}-all.jar"
val fatJarShortName = "logbook-kai.jar"

java.sourceCompatibility = JavaVersion.VERSION_21

plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
}

dependencies {
    api(libs.org.glassfish.javax.json)
    api(libs.org.eclipse.jetty.jetty.server)
    api(libs.org.eclipse.jetty.jetty.servlet)
    api(libs.org.eclipse.jetty.jetty.proxy)
    api(libs.org.openjfx.javafx.controls)
    api(libs.org.openjfx.javafx.fxml)
    api(libs.org.openjfx.javafx.media)
    api(libs.org.openjfx.javafx.swing)
    api(libs.org.openjfx.javafx.web)
    api(libs.org.controlsfx.controlsfx)
    api(libs.org.openjdk.nashorn.nashorn.core)
    api(libs.com.fasterxml.jackson.core.jackson.databind)
    api(libs.org.slf4j.slf4j.api)
    api(libs.org.slf4j.slf4j.simple)
    api(libs.ch.qos.logback.logback.classic)
    api(libs.ch.qos.logback.logback.core)
    testImplementation(libs.junit.junit)
    compileOnly(libs.org.projectlombok.lombok)
    annotationProcessor(libs.org.projectlombok.lombok)
    testCompileOnly(libs.org.projectlombok.lombok)
    testAnnotationProcessor(libs.org.projectlombok.lombok)
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
}

val jar by tasks.getting(Jar::class) {
    manifest {
        attributes["Main-Class"] = "logbook.internal.Launcher"
        attributes["Implementation-Version"] = version
    }
}

task("prePackage", Copy::class) {
    dependsOn("shadowJar")
    mkdir("build/pkg-input")
    from("build/libs/${fatJarFullName}")
    into("build/pkg-input")
    rename(fatJarFullName, fatJarShortName)
}

task("package", Zip::class) {
    dependsOn("prePackage")
    from("dist-includes").exclude("*/.gitkeep")
    from("build/pkg-input/${fatJarShortName}")
}

task("cleanDest", Delete::class) {
    delete("dest")
}

task("macApp", Exec::class) {
    dependsOn("prePackage")
    workingDir(".")
    commandLine("jpackage", "@pkg-options/mac-app.txt", "--app-version", version, "--main-jar", fatJarShortName)
}

task("macDmg", Exec::class) {
    dependsOn("prePackage")
    workingDir(".")
    commandLine("jpackage", "@pkg-options/mac-dmg.txt", "--app-version", version, "--main-jar", fatJarShortName)
}

task("winExe", Exec::class) {
    dependsOn("prePackage")
    workingDir(".")
    commandLine("jpackage", "@pkg-options/win-exe.txt", "--app-version", version, "--main-jar", fatJarShortName)
}

task("winMsi", Exec::class) {
    dependsOn("prePackage")
    workingDir(".")
    commandLine("jpackage", "@pkg-options/win-msi.txt", "--app-version", version, "--main-jar", fatJarShortName)
}
