import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val dusseldorfKtorVersion = "6.1.2"
val ktorVersion = "3.1.0"
val graphqlKotlinClientVersion = "8.5.0"
val sifTilgangskontrollVersion = "5.2.0"
val tokenSupportVersion = "5.0.24"
val mockOauth2ServerVersion = "2.1.10"

val mockkVersion = "1.14.0"
val jsonassertVersion = "1.5.3"
val fuelVersion = "2.3.1"

val mainClass = "no.nav.k9.SelvbetjeningOppslagKt"

plugins {
    kotlin("jvm") version "2.1.20"
    id("org.sonarqube") version "6.1.0.5360"
    jacoco
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

configurations.all {
    resolutionStrategy {
        force("org.yaml:snakeyaml:2.4")
    }
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

    implementation ("no.nav.security:token-validation-ktor-v3:$tokenSupportVersion")
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
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/navikt/dusseldorf-ktor")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: "k9-selvbetjening-oppslag"
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "21"
    }

    withType<Test> {
        useJUnitPlatform()
        finalizedBy(jacocoTestReport) // report is always generated after tests run
    }

    jacocoTestReport {
        dependsOn(test) // tests are required to run before generating the report
        reports {
            xml.required.set(true)
            csv.required.set(false)
        }
    }

    withType<ShadowJar> {
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

    register<ShadowJar>("shadowJarWithMocks") {
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

    withType<Wrapper> {
        gradleVersion = "8.5"
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
