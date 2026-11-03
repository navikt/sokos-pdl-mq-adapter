import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.serialization") version "2.4.21"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("org.jetbrains.kotlinx.kover") version "0.9.11"

    application
}

group "no.nav.sokos"

repositories {
    mavenCentral()

    maven { url = uri("https://packages.confluent.io/maven/") }
}

val ktorVersion = "3.6.0"
val micrometerVersion = "1.17.2"
val kotlinLoggingVersion = "3.0.5"
val logbackVersion = "1.6.6"
val logstashVersion = "9.0"
val kafkaClientsVersion = "8.1.1-ce"
val avroVersion = "1.12.2"
val kafkaAvroSerializerVersion = "8.1.1"
val ibmmqVersion = "10.0.1.0"
val mockkVersion = "1.14.11"
val kotlinVersion = "2.3.10"
val kotestVersion = "6.2.6"

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
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:$logstashVersion")
    implementation("io.github.microutils:kotlin-logging-jvm:$kotlinLoggingVersion")

    // Kafka
    implementation("org.apache.kafka:kafka-clients:$kafkaClientsVersion")
    implementation("org.apache.avro:avro:$avroVersion")
    implementation("io.confluent:kafka-avro-serializer:$kafkaAvroSerializerVersion")
    implementation("at.yawk.lz4:lz4-java:1.12.1")

    // IBM MQ
    implementation("com.ibm.mq:com.ibm.mq.jakarta.client:$ibmmqVersion")

    // Testing
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")

    // Sårbarheter
    implementation(platform("io.netty:netty-bom:4.2.19.Final")) {
        because("Align Netty modules on a patched release")
    }

    constraints {
        implementation("com.google.guava:guava:33.7.2-jre") {
            because("GHSA-xxph-c9ww-hj94 affects Guava versions before 33.7.2")
        }
        implementation("org.xerial.snappy:snappy-java:1.1.10.9") {
            because("Multiple security vulnerabilities affect snappy-java versions before 1.1.10.9")
        }
        implementation("io.opentelemetry:opentelemetry-api:1.67.0") {
            because("GHSA-ccrj-gw8r-854m affects OpenTelemetry API versions before 1.67.0")
        }
        implementation("tools.jackson.core:jackson-databind:3.2.3") {
            because("Multiple versions of jackson-databind has vulnerable dependencies. Affected version >= 3.2.0, <= 3.2.2")
        }
        implementation("com.fasterxml.jackson.core:jackson-databind:2.22.3") {
            because("Multiple versions of jackson-databind has vulnerable dependencies. Affected version >= 2.22.0, < 2.22.2")
        }
        implementation("org.apache.httpcomponents.client5:httpclient5:5.6.3") {
            because("Multiple versions of httpclient5 has vulnerable dependencies. Affected version >= 5.0-alpha1, < 5.6.3")
        }
    }

    modules {
        module("org.lz4:lz4-java") {
            replacedBy("at.yawk.lz4:lz4-java", "Prefer the patched fork for vulnerability fix")
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
