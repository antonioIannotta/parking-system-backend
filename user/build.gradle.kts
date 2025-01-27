val projectGroup: String by project
val projectVersion: String by project
val ktorVersion: String by project
val kotlinVersion: String by project
val mongoDriverVersion: String by project
val logbackVersion: String by project
val jUnitVersion: String by project

plugins {
    kotlin("jvm") version "1.8.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.10"
}

group = projectGroup
version = projectVersion

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("org.mongodb:mongodb-driver-sync:$mongoDriverVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("org.apache.commons:commons-email:1.6.0")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$jUnitVersion")
}

tasks.test {
    useJUnitPlatform()
}
