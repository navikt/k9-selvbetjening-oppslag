import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.4.20"
    kotlin("plugin.spring") version "2.4.20"
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "no.nav"
version = "0.0.1-SNAPSHOT"

val tokenSupportVersion = "6.0.13"
val sifTilgangskontrollVersion = "6.0.0"
val graphqlKotlinVersion = "10.2.2"
val logstashLogbackEncoderVersion = "9.0"
val mockOauth2ServerVersion = "6.0.5"
val mockkVersion = "1.14.11"
val springMockkVersion = "5.0.1"
val jsonassertVersion = "1.5.3"
val wiremockSpringVersion = "4.4.2"

// CVE-2026-65182, -65905, -68525 (Tomcat) og CVE-2026-68497, -91776, -91777 (Jackson). Fjernes når Spring Boot har tatt dem inn.
extra["tomcat.version"] = "11.0.25"
extra["jackson-bom.version"] = "3.1.7"

configurations.all {
    resolutionStrategy {
        force("org.yaml:snakeyaml:2.7")
    }
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-webclient")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("io.micrometer:micrometer-registry-prometheus")

    // NAV
    implementation("no.nav.security:token-validation-spring:$tokenSupportVersion")
    implementation("no.nav.security:token-client-spring:$tokenSupportVersion")
    implementation("no.nav.sif.tilgangskontroll:spesification:$sifTilgangskontrollVersion")
    implementation("no.nav.sif.tilgangskontroll:core:$sifTilgangskontrollVersion")

    // GraphQL
    implementation("com.expediagroup:graphql-kotlin-spring-client:$graphqlKotlinVersion")
    implementation("com.expediagroup:graphql-kotlin-client-jackson:$graphqlKotlinVersion")

    // Jackson
    implementation("tools.jackson.module:jackson-module-kotlin")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Diverse
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashLogbackEncoderVersion")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("no.nav.security:token-validation-spring-test:$tokenSupportVersion")
    testImplementation("no.nav.security:mock-oauth2-server:$mockOauth2ServerVersion")
    testImplementation("org.wiremock.integrations:wiremock-spring-boot:$wiremockSpringVersion")
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
