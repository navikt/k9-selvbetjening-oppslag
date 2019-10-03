import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val dusseldorfKtorVersion = "1.2.4.97e227b"
val cxfVersion = "3.3.3"
val ktorVersion = ext.get("ktorVersion").toString()
val kotlinVersion = ext.get("kotlinVersion").toString()

val tjenestespesifikasjonerVersion = "1.2019.08.16-13.46-35cbdfd492d4"
val junitJupiterVersion = "5.5.2"

val mainClass = "no.nav.k9.SelvbetjeningOppslagKt"

fun tjenestespesifikasjon(name: String) = "no.nav.tjenestespesifikasjoner:$name:$tjenestespesifikasjonerVersion"


plugins {
    kotlin("jvm") version "1.3.50"
    id("com.github.johnrengelman.shadow") version "5.1.0"
}

buildscript {
    apply("https://raw.githubusercontent.com/navikt/dusseldorf-ktor/97e227bcbb622e7843447470dab8f1d643f66327/gradle/dusseldorf-ktor.gradle.kts")
}

dependencies {
    compile ( "no.nav.helse:dusseldorf-ktor-core:$dusseldorfKtorVersion")
    compile ( "no.nav.helse:dusseldorf-ktor-auth:$dusseldorfKtorVersion")
    compile ( "no.nav.helse:dusseldorf-ktor-client:$dusseldorfKtorVersion")
    compile ( "no.nav.helse:dusseldorf-ktor-metrics:$dusseldorfKtorVersion")
    compile ( "no.nav.helse:dusseldorf-oauth2-client:$dusseldorfKtorVersion")

    // Tjenestespesifikasjoner
    compile(tjenestespesifikasjon("person-v3-tjenestespesifikasjon"))

    // Test
    testCompile ( "no.nav.helse:dusseldorf-ktor-test-support:$dusseldorfKtorVersion")
    testCompile ("io.ktor:ktor-server-test-host:1.2.3") {
        exclude(group = "org.eclipse.jetty")
    }
    testCompile("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testCompile("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testCompile ("org.skyscreamer:jsonassert:1.5.0")
}

repositories {
    maven("https://dl.bintray.com/kotlin/ktor")
    maven("https://kotlin.bintray.com/kotlinx")
    maven("http://packages.confluent.io/maven/")

    jcenter()
    mavenLocal()
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
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

tasks.withType<Wrapper> {
    gradleVersion = "5.6.2"
}
