import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val dusseldorfKtorVersion = "3.2.3.0-7a92774"
val ktorVersion = "2.3.0"
val graphqlKotlinClientVersion = "6.4.1"
val sifTilgangskontrollVersion = "1-bc530da"
val tokenSupportVersion = "3.1.0"
val mockOauth2ServerVersion = "0.5.8"

val mockkVersion = "1.13.5"
val jsonassertVersion = "1.5.1"
val fuelVersion = "2.3.1"

val mainClass = "no.nav.k9.SelvbetjeningOppslagKt"

plugins {
    kotlin("jvm") version "1.8.21"
    id("org.sonarqube") version "4.1.0.3113"
    jacoco
    id("com.github.johnrengelman.shadow") version "8.1.1"
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

    implementation ("no.nav.security:token-validation-ktor-v2:$tokenSupportVersion")
    testImplementation ("no.nav.security:mock-oauth2-server:$mockOauth2ServerVersion")

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
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
    reports {
        xml.required.set(true)
        csv.required.set(false)
    }
}

sonarqube {
    properties {
        property("sonar.projectKey", "navikt_k9-selvbetjening-oppslag")
        property("sonar.organization", "navikt")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.login", System.getenv("SONAR_TOKEN"))
        property("sonar.sourceEncoding", "UTF-8")
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
    gradleVersion = "8.0.2"
}
