import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.serialization") version "2.3.21"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("org.jetbrains.kotlinx.kover") version "0.9.8"

    application
}

group "no.nav.sokos"

repositories {
    mavenCentral()

    maven { url = uri("https://packages.confluent.io/maven/") }
}

val ktorVersion = "3.5.0"
val micrometerVersion = "1.17.0"
val kotlinLoggingVersion = "3.0.5"
val logbackVersion = "1.5.34"
val logstashVersion = "9.0"
val janionVersion = "3.1.12"
val kafkaClientsVersion = "8.1.1-ce"
val avroVersion = "1.12.1"
val kafkaAvroSerializerVersion = "8.1.1"
val ibmmqVersion = "9.4.5.1"
val mockkVersion = "1.14.11"
val kotlinVersion = "2.3.10"
val kotestVersion = "6.1.11"

dependencies {
    // Ktor server
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")

    // Serialization
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

    // Montitorering
    implementation("io.ktor:ktor-server-metrics-micrometer-jvm:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")

    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm:$kotlinLoggingVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    runtimeOnly("org.codehaus.janino:janino:$janionVersion")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:$logstashVersion")
    implementation("io.github.microutils:kotlin-logging-jvm:$kotlinLoggingVersion")

    // Kafka
    implementation("org.apache.kafka:kafka-clients:$kafkaClientsVersion")
    implementation("org.apache.avro:avro:$avroVersion")
    implementation("io.confluent:kafka-avro-serializer:$kafkaAvroSerializerVersion")

    // IBM MQ
    implementation("com.ibm.mq:com.ibm.mq.jakarta.client:$ibmmqVersion")

    // Testing
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
}

configurations.all {
    resolutionStrategy {
        eachDependency {
            if (requested.group == "org.lz4" && requested.name == "lz4-java") {
                useTarget("at.yawk.lz4:lz4-java:1.11.0")
                because("Prefer the patched fork for vulnerability fix")
            }
            if (requested.group == "com.fasterxml.jackson.core" && requested.name == "jackson-core") {
                useVersion("2.21.1")
                because("jackson-core: Number Length Constraint Bypass in Async Parser Leads to Potential DoS Condition. Affected version >= 2.19.0, < 2.21.1")
            }
            if (requested.group == "tools.jackson.core" && requested.name == "jackson-core") {
                useVersion("3.1.1")
                because("Jackson Core: Document length constraint bypass in blocking, async, and DataInput parsers. Affected version >= 3.0.0, <= 3.1.0")
            }
            if (requested.group == "org.bouncycastle" && requested.name == "bcprov-jdk18on") {
                useVersion("1.84")
                because("Bouncy Castle Has Covert Timing Channel Vulnerability. Affected version >= 1.71 < 1.84")
            }
            if (requested.group == "org.bouncycastle" && requested.name == "bcpkix-jdk18on") {
                useVersion("1.84")
                because("Bouncy Castle Has Covert Timing Channel Vulnerability. Affected version >= 1.71 < 1.84")
            }
            if (requested.group == "io.netty" && requested.name == "netty-codec-http") {
                useVersion("4.2.13.Final")
                because(
                    "CVE-2026-42587: Netty HttpContentDecompressor maxAllocation bypass with br/zstd/snappy leads to decompression bomb DoS. Affected version = 4.2.11.Final, patched in >= 4.2.13.Final",
                )
            }
            if (requested.group == "io.netty" && requested.name == "netty-codec-http2") {
                useVersion("4.2.13.Final")
                because(
                    "CVE-2026-42587: Netty HttpContentDecompressor maxAllocation bypass with br/zstd/snappy leads to decompression bomb DoS. Affected version = 4.2.11.Final, patched in >= 4.2.13.Final",
                )
            }
            if (requested.group == "io.netty" && requested.name == "netty-handler") {
                useVersion("4.2.15.Final")
                because(
                    "CVE-2026-44249 and CVE-2026-45416: enforce patched netty-handler version in production runtime classpath",
                )
            }
            if (requested.group == "io.netty" && requested.name == "netty-transport-native-epoll") {
                useVersion("4.2.13.Final")
                because(
                    "CVE-2026-42577: Netty's epoll transport fails to detect and close TCP connections that receive a RST after being half-closed, leading to stale channels that are never cleaned up and, in some code paths, a 100% CPU busy-loop in the event loop thread. Affected version < 4.2.13.Final, patched in >= 4.2.13.Final",
                )
            }
            if (requested.group == "io.opentelemetry" && requested.name == "opentelemetry-api") {
                useVersion("1.62.0")
                because("OpenTelemetry Java SDK has Unbounded Memory Allocation in W3C Baggage Propagation. Affected version <= 1.61.0")
            }
        }
    }
}

application {
    mainClass.set("no.nav.sokos.pdladapter.ApplicationKt")
}

sourceSets {
    main {
        java {
            srcDirs("${layout.buildDirectory.get()}/generated/src/main/kotlin")
        }
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks {

    withType<KotlinCompile>().configureEach {
        dependsOn("ktlintFormat")
    }

    withType<Test>().configureEach {
        useJUnitPlatform()

        testLogging {
            showExceptions = true
            showStackTraces = true
            exceptionFormat = FULL
            events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        }

        reports.forEach { report -> report.required.value(false) }

        finalizedBy(koverHtmlReport)
    }

    ("build") {
        dependsOn("copyPreCommitHook")
    }

    register<Copy>("copyPreCommitHook") {
        from(".scripts/pre-commit")
        into(".git/hooks")
        filePermissions {
            user {
                execute = true
            }
        }
        doFirst {
            println("Installing git hooks...")
        }
        doLast {
            println("Git hooks installed successfully.")
        }
        description = "Copy pre-commit hook to .git/hooks"
        group = "git hooks"
        outputs.upToDateWhen { false }
    }
}
