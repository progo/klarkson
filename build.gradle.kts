import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.20"
}

group = "me.progo"
version = "1.0"

repositories {
    mavenCentral()
}

val exposedVersion = "0.41.1"

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.0")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.9.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")

    // sqlite and orm
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.xerial:sqlite-jdbc:3.36.0.2")

    implementation("com.inthebacklog:javampd:7.1.0")

    // Coroutine support
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.5.2")

    // Cool, kotlin-idiomatic logging support
    implementation("io.github.microutils:kotlin-logging:2.1.16")
    implementation("ch.qos.logback:logback-classic:1.2.9")
    implementation(kotlin("stdlib-jdk8"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "17"
}

kotlin {
    jvmToolchain(17)
}