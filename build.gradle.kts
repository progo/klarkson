import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.30"
}

group = "me.progo"
version = "1.0"

repositories {
    mavenCentral()
}

val exposedVersion = "0.36.1"

dependencies {
    testImplementation(kotlin("test"))

    // sqlite and orm
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.xerial:sqlite-jdbc:3.36.0.2")

    // 6.2.5 requires Java 11
    // https://mvnrepository.com/artifact/org.hihn/javampd
    implementation("org.hihn:javampd:6.1.23")

    // Coroutine support
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.5.2")

    // Cool, kotlin-idiomatic logging support
    implementation("io.github.microutils:kotlin-logging:2.1.16")
    implementation("ch.qos.logback:logback-classic:1.2.9")
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}