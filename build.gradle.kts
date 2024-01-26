import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

val jacksonVersion = "2.16.1"
val jUnitVersion = "5.10.1"

plugins {
    application
    kotlin("jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.github.ben-manes.versions") version "0.51.0"
    id("com.vanniktech.maven.publish") version "0.27.0"
}

application {
    mainClass.set("com.bitkid.itsfranking.ITSFRankingApp")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")

    implementation("com.miglayout:miglayout-swing:11.3")
    implementation("commons-codec:commons-codec:1.16.0")

    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    testImplementation("io.strikt:strikt-core:0.34.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$jUnitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$jUnitVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.1") {
        exclude(group = "junit")
    }
}

repositories {
    mavenCentral()
}

tasks.withType<Test> {
    useJUnitPlatform {
        includeEngines("junit-jupiter")
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}

tasks.withType<ShadowJar> {
    archiveBaseName.set("itsf-ranking")
    archiveClassifier.set("all")
    archiveVersion.set("0.1")
}


publishing {
    repositories {
        maven {
            name = "githubPackages"
            url = URI("https://maven.pkg.github.com/bitkid/itsf-ranking")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

@Suppress("UnstableApiUsage")
mavenPublishing {
    coordinates("com.bitkid", project.name, System.getenv("ITSF_RANKING_RELEASE_NAME") ?: "1.0-SNAPSHOT")
    pom {
        name = "ITSF ranking"
        description = "Simple helper app for handling rankings on the ITSF homepage"
        url = "https://github.com/bitkid/itsf-ranking/"
    }
}