group = "logbook"
description = "logbook-kai"
version = "24.10.3"

// UpgradeCode (GUID) for Windows Installer
val windowsUpgradeUUID = "880e4493-20fc-4c89-8c5b-01e4b2479b77"

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

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

val jar by tasks.getting(Jar::class) {
    manifest {
        attributes["Main-Class"] = "logbook.internal.Launcher"
        attributes["Implementation-Version"] = version
    }
}

fun archName() = org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentArchitecture().name

task("prePackage", Copy::class) {
    dependsOn("shadowJar")
    mkdir("build/tmp/pkg-input")
    from("build/libs/logbook-kai-${version}-all.jar")
    into("build/tmp/pkg-input")
    rename("logbook-kai-${version}-all.jar", "logbook-kai.jar")
}

task("package", Zip::class) {
    dependsOn("prePackage")
    from("dist-includes").exclude("*/.gitkeep")
    from("build/tmp/pkg-input/logbook-kai.jar")
}

task("macApp", Exec::class) {
    dependsOn("package")
    workingDir(".")
    commandLine(
        "jpackage",
        "@pkg-options/common.txt",
        "@pkg-options/macos.txt",
        "--app-version", version,
        "--type", "app-image"
    )
}

task("macDmg", Exec::class) {
    dependsOn("package")
    workingDir(".")
    commandLine(
        "jpackage",
        "@pkg-options/common.txt",
        "@pkg-options/macos.txt",
        "--app-version", version,
        "--type", "dmg"
    )
}

task("macDmgRelease", Exec::class) {
    dependsOn("macDmg")
    workingDir("build/distributions")
    commandLine(
        "mv", "Logbook-Kai-${version}.dmg", "logbook-kai-${version}-macos-${archName()}.dmg"
    )
}

task("winApp", Exec::class) {
    dependsOn("package")
    workingDir(".")
    commandLine(
        "jpackage",
        "@pkg-options\\common.txt",
        "@pkg-options\\windows.txt",
        "--app-version", version,
        "--type", "app-image"
    )
}

task("winZip", Zip::class) {
    dependsOn("winApp")
    archiveFileName.set("logbook-kai-${version}-windows-${archName()}.zip")
    from("build/distributions/logbook-kai")
}

task("winMsi", Exec::class) {
    // "major.minor.small" でWindows Installerの挙動が変わるので
    // "year.month.day" 形式のバージョニングは不適切かもしれない
    dependsOn("package")
    workingDir(".")
    commandLine(
        "jpackage",
        "@pkg-options\\common.txt",
        "@pkg-options\\windows.txt",
        "--app-version", version,
        "--type", "msi",
        "--win-menu",
        "--win-per-user-install",
        "--win-shortcut-prompt",
        "--win-upgrade-uuid", windowsUpgradeUUID
    )
}

task("winMsiRelease", Exec::class) {
    dependsOn("winMsi")
    workingDir("build/distributions")
    commandLine(
        "ren", "logbook-kai-${version}.msi", "logbook-kai-${version}-windows-${archName()}.msi"
    )
}
