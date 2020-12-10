import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val dusseldorfKtorVersion = "1.4.0.fff48ae"
val ktorVersion = ext.get("ktorVersion").toString()
val kotlinVersion = ext.get("kotlinVersion").toString()

val mockkVersion = "1.10.0"
val jsonassertVersion = "1.5.0"
val junitJupiterVersion = "5.6.2"

val mainClass = "no.nav.k9.SelvbetjeningOppslagKt"

plugins {
    kotlin("jvm") version "1.4.10"
    id("com.github.johnrengelman.shadow") version "6.0.0"
}

buildscript {
    apply("https://raw.githubusercontent.com/navikt/dusseldorf-ktor/fff48ae60a33fb200c2248af44956e93482c69fc/gradle/dusseldorf-ktor.gradle.kts")
}

dependencies {
    implementation ( "no.nav.helse:dusseldorf-ktor-core:$dusseldorfKtorVersion")
    implementation ( "no.nav.helse:dusseldorf-ktor-auth:$dusseldorfKtorVersion")
    implementation ( "no.nav.helse:dusseldorf-ktor-client:$dusseldorfKtorVersion")
    implementation ( "no.nav.helse:dusseldorf-ktor-metrics:$dusseldorfKtorVersion")
    implementation ( "no.nav.helse:dusseldorf-oauth2-client:$dusseldorfKtorVersion")

    // Test
    testImplementation ( "no.nav.helse:dusseldorf-test-support:$dusseldorfKtorVersion")
    testImplementation ("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
    }
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation ("org.skyscreamer:jsonassert:$jsonassertVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
}

repositories {
    maven("https://dl.bintray.com/kotlin/ktor")
    maven("https://kotlin.bintray.com/kotlinx")
    maven("http://packages.confluent.io/maven/")

    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/navikt/dusseldorf-ktor")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }

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

tasks.withType<ShadowJar>{
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
    )
    manifest {
        attributes(
            mapOf(
                "Main-Class" to "no.nav.k9.ApplicationWithMocks"
            )
        )
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "6.6.1"
}

tasks.register("createDependabotFile") {
    doLast {
        mkdir("$projectDir/dependabot")
        val file = File("$projectDir/dependabot/build.gradle")
        file.writeText( "// Do not edit manually! This file was created by the 'createDependabotFile' task defined in the root build.gradle.kts file.\n")
        file.appendText("dependencies {\n")
        project.configurations.getByName("runtimeClasspath").allDependencies
            .filter { it.group != rootProject.name && it.version != null }
            .forEach { file.appendText("    compile '${it.group}:${it.name}:${it.version}'\n") }
        project.configurations.getByName("testRuntimeClasspath").allDependencies
            .filter { it.group != rootProject.name && it.version != null }
            .forEach { file.appendText("    testCompile '${it.group}:${it.name}:${it.version}'\n") }
        file.appendText("}\n")
    }
}
