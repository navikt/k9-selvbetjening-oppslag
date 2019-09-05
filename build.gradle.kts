import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer

val dusseldorfKtorVersion = "1.2.3.664b246"
val cxfVersion = "3.3.3"
val tjenestespesifikasjonerVersion = "1.2019.08.16-13.46-35cbdfd492d4"

val mainClass = "no.nav.k9.SelvbetjeningOppslagKt"

fun tjenestespesifikasjon(name: String) = "no.nav.tjenestespesifikasjoner:$name:$tjenestespesifikasjonerVersion"


plugins {
    kotlin("jvm") version "1.3.50"
    id("com.github.johnrengelman.shadow") version "5.1.0"
}

buildscript {
    apply("https://raw.githubusercontent.com/navikt/dusseldorf-ktor/664b2468f6d1a30d77dca88a57eb240e26c32087/gradle/dusseldorf-ktor.gradle.kts")
}

dependencies {
    compile ( "no.nav.helse:dusseldorf-ktor-core:$dusseldorfKtorVersion")
    compile ( "no.nav.helse:dusseldorf-ktor-auth:$dusseldorfKtorVersion")
    compile ( "no.nav.helse:dusseldorf-ktor-client:$dusseldorfKtorVersion")
    compile ( "no.nav.helse:dusseldorf-ktor-metrics:$dusseldorfKtorVersion")
    
    // cxf
    compile("org.apache.cxf:cxf-rt-ws-security:$cxfVersion")
    compile("org.apache.cxf:cxf-rt-ws-policy:$cxfVersion")
    compile("org.apache.cxf:cxf-rt-frontend-jaxws:$cxfVersion")
    compile("org.apache.cxf:cxf-rt-features-logging:$cxfVersion")
    compile("org.apache.cxf:cxf-rt-transports-http:$cxfVersion")

    // Tjenestespesifikasjoner
    compile("com.sun.xml.ws:jaxws-rt:2.3.2")
    compile("javax.activation:activation:1.1.1")

    compile(tjenestespesifikasjon("arbeidsforholdv3-tjenestespesifikasjon"))
    compile(tjenestespesifikasjon("person-v3-tjenestespesifikasjon"))


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

    transform(ServiceFileTransformer::class.java) {
        setPath("META-INF/cxf")
        include("bus-extensions.txt")
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "5.6.1"
}
