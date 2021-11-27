import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.30"
}

group = "me.progo"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    // 6.2.5 requires Java 11, which we have
    // https://mvnrepository.com/artifact/org.hihn/javampd
    implementation("org.hihn:javampd:6.1.23")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.5.2")

    implementation("io.github.microutils:kotlin-logging:1.12.5")
    // implementation("org.slf4j:slf4j-simple:1.7.5")
    implementation("ch.qos.logback:logback-classic:1.0.13")
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}