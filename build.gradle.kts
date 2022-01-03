import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val dusseldorfKtorVersion = "3.1.6.7-20bff92"
val ktorVersion = ext.get("ktorVersion").toString()
val kotlinVersion = ext.get("kotlinVersion").toString()
val graphqlKotlinClientVersion = "5.3.1"
val sifTilgangskontrollVersion = "1-58dcba8"

val mockkVersion = "1.12.1"
val jsonassertVersion = "1.5.0"
val fuelVersion = "2.3.1"

val mainClass = "no.nav.k9.SelvbetjeningOppslagKt"

plugins {
    kotlin("jvm") version "1.6.10"
    id("com.github.johnrengelman.shadow") version "7.1.1"
}

buildscript {
    apply("https://raw.githubusercontent.com/navikt/dusseldorf-ktor/20bff92d4f26d6eea4feb56c4f49e728c267868c/gradle/dusseldorf-ktor.gradle.kts")
}

dependencies {
    implementation("no.nav.helse:dusseldorf-ktor-core:$dusseldorfKtorVersion")
    implementation("no.nav.helse:dusseldorf-ktor-jackson:$dusseldorfKtorVersion")
    implementation("no.nav.helse:dusseldorf-ktor-auth:$dusseldorfKtorVersion")
    implementation("no.nav.helse:dusseldorf-ktor-client:$dusseldorfKtorVersion")
    implementation("no.nav.helse:dusseldorf-ktor-metrics:$dusseldorfKtorVersion")
    implementation("no.nav.helse:dusseldorf-oauth2-client:$dusseldorfKtorVersion")
    implementation("com.github.kittinunf.fuel:fuel:$fuelVersion")
    implementation("com.github.kittinunf.fuel:fuel-coroutines:$fuelVersion") {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
    }

    implementation("no.nav.sif.tilgangskontroll:spesification:$sifTilgangskontrollVersion")
    implementation("no.nav.sif.tilgangskontroll:core:$sifTilgangskontrollVersion") {
        exclude(group = "io.projectreactor.netty")
    }

    implementation("com.expediagroup:graphql-kotlin-ktor-client:$graphqlKotlinClientVersion")  {
        exclude("com.expediagroup", "graphql-kotlin-client-serialization")
    }
    implementation("com.expediagroup:graphql-kotlin-client-jackson:$graphqlKotlinClientVersion")

    // Test
    testImplementation("no.nav.helse:dusseldorf-test-support:$dusseldorfKtorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
    }
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation ("org.skyscreamer:jsonassert:$jsonassertVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    implementation(kotlin("stdlib-jdk8"))
}

repositories {
    mavenLocal()

    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/navikt/dusseldorf-ktor")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }

    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/navikt/sif-tilgangskontroll")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }

    mavenCentral()
    maven("https://jitpack.io")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<ShadowJar> {
    archiveBaseName.set("app")
    archiveClassifier.set("")
    manifest {
        attributes(
            mapOf(
                "Main-Class" to mainClass
            )
        )
    }
}

tasks.register<ShadowJar>("shadowJarWithMocks") {
    archiveBaseName.set("app")
    archiveClassifier.set("")
    from(sourceSets.main.get().output, sourceSets.test.get().output)
    configurations = mutableListOf(
        project.configurations.runtimeClasspath.get(),
        project.configurations.testRuntimeClasspath.get()
    ) as List<FileCollection>?
    manifest {
        attributes(
            mapOf(
                "Main-Class" to "no.nav.k9.ApplicationWithMocks"
            )
        )
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "7.2"
}
