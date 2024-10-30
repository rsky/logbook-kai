plugins {
    `java-library`
    `maven-publish`
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

group = "logbook"
version = "24.10.2"
description = "logbook-kai"
java.sourceCompatibility = JavaVersion.VERSION_21

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
}
