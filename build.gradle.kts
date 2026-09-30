import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.4.10"
    kotlin("plugin.spring") version "2.4.10"
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "no.nav"
version = "0.0.1-SNAPSHOT"

val tokenSupportVersion = "6.0.12"
val sifTilgangskontrollVersion = "5.3.2"
// Må følge versjonen sif-tilgangskontroll core er bygget mot
val graphqlKotlinVersion = "8.8.1"
val logstashLogbackEncoderVersion = "9.0"
val mockOauth2ServerVersion = "6.0.2"
val mockkVersion = "1.14.11"
val springMockkVersion = "5.0.1"
val jsonassertVersion = "1.5.3"
val wiremockVersion = "3.13.2"

configurations.all {
    resolutionStrategy {
        force("org.yaml:snakeyaml:2.6")
    }
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-webclient")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-jackson2")
    implementation("io.micrometer:micrometer-registry-prometheus")

    // NAV
    implementation("no.nav.security:token-validation-spring:$tokenSupportVersion")
    implementation("no.nav.security:token-client-spring:$tokenSupportVersion")
    implementation("no.nav.sif.tilgangskontroll:spesification:$sifTilgangskontrollVersion")
    implementation("no.nav.sif.tilgangskontroll:core:$sifTilgangskontrollVersion")

    // GraphQL
    implementation("com.expediagroup:graphql-kotlin-spring-client:$graphqlKotlinVersion")
    implementation("com.expediagroup:graphql-kotlin-client-jackson:$graphqlKotlinVersion")

    // Jackson 2
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j")

    // Diverse
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashLogbackEncoderVersion")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("no.nav.security:token-validation-spring-test:$tokenSupportVersion")
    testImplementation("no.nav.security:mock-oauth2-server:$mockOauth2ServerVersion")
    testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
    testImplementation("org.skyscreamer:jsonassert:$jsonassertVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.ninja-squad:springmockk:$springMockkVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

repositories {
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/navikt/sif-tilgangskontroll")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: "k9-selvbetjening-oppslag"
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks {
    withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.set(listOf("-Xjsr305=strict"))
            jvmTarget.set(JvmTarget.JVM_25)
        }
    }

    withType<Test> {
        useJUnitPlatform()
    }

    bootJar {
        archiveFileName.set("app.jar")
    }

    jar {
        enabled = false
    }

    withType<Wrapper> {
        gradleVersion = "9.3.0"
    }
}
