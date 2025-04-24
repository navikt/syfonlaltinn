import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer

group = "no.nav.syfo"
version = "1.0.0"

val coroutinesVersion = "1.10.2"
val jacksonVersion = "2.18.3"
val kluentVersion = "1.73"
val ktorVersion = "3.1.2"
val logbackVersion = "1.5.18"
val logstashEncoderVersion = "8.1"
val prometheusVersion = "0.16.0"
val mockkVersion = "1.14.0"
val altinnDownloadQueueVersion = "1.2020.10.21-14.38-e6bb56478815"
val altinnPrefillVersion = "1.2020.10.21-14.38-e6bb56478815"
val cxfVersion = "3.6.4"
val jaxwsToolsVersion = "2.3.1"
val javaxActivationVersion = "1.2.0"
val postgresVersion = "42.7.5"
val flywayVersion = "11.7.2"
val hikariVersion = "6.3.0"
val testContainerVersion = "1.21.0"
val digisyfoNarmesteLederVersion = "1.2020.10.07-08.40-90b3ab7bad15"
val commonsValidatorVersion = "1.9.0"
val kotlinVersion = "2.1.20"
val ktfmtVersion = "0.44"
val commonsCodecVersion = "1.18.0"
val junitVersion = "5.12.2"
val kafkaVersion = "3.9.0"

///Due to vulnerabilities
val commonsCollectionsVersion = "3.2.2"
val commonsCompressVersion = "1.27.1"

plugins {
    id("application")
    id("com.diffplug.spotless") version "7.0.3"
    kotlin("jvm") version "2.1.20"
    id("com.gradleup.shadow") version "8.3.6"
}

application {
    mainClass.set("no.nav.syfo.BootstrapKt")
}

repositories {
    mavenCentral()
    maven(url = "https://packages.confluent.io/maven/")
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}


dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:$coroutinesVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    constraints {
        implementation("commons-codec:commons-codec:$commonsCodecVersion") {
            because("override transient from io.ktor:ktor-client-apache")
        }
    }
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    implementation("com.fasterxml.jackson.module:jackson-module-jaxb-annotations:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    implementation("no.nav.tjenestespesifikasjoner:altinn-download-queue-external:$altinnDownloadQueueVersion")
    implementation("no.nav.tjenestespesifikasjoner:altinn-pre-fill:$altinnPrefillVersion")
    implementation("no.nav.tjenestespesifikasjoner:digisyfo-naermesteLeder:$digisyfoNarmesteLederVersion")
    implementation("org.apache.cxf:cxf-rt-frontend-jaxws:$cxfVersion")
    constraints {
        implementation("commons-collections:commons-collections:$commonsCollectionsVersion") {
            because("override transient from org.apache.cxf:cxf-rt-frontend-jaxws")
        }
    }
    implementation("org.apache.cxf:cxf-rt-transports-http:$cxfVersion")
    implementation("com.sun.activation:javax.activation:$javaxActivationVersion")
    implementation("com.sun.xml.ws:jaxws-tools:$jaxwsToolsVersion") {
        exclude(group = "com.sun.xml.ws", module = "policy")
    }

    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    compileOnly("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")


    implementation("org.apache.kafka:kafka_2.12:$kafkaVersion")


    implementation("commons-validator:commons-validator:$commonsValidatorVersion"){
        exclude(group = "commons-collections", module = "commons-collections")
    }

    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation("org.amshove.kluent:kluent:$kluentVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.testcontainers:kafka:$testContainerVersion")
    constraints {
        testImplementation("org.apache.commons:commons-compress:$commonsCompressVersion") {
            because("overrides vulnerable dependency from org.testcontainers:kafka")
        }
    }
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
    }
    testImplementation("org.testcontainers:postgresql:$testContainerVersion")

}

tasks {

    shadowJar {
        mergeServiceFiles {
            setPath("META-INF/services/org.flywaydb.core.extensibility.Plugin")
            setPath("META-INF/cxf")
        }
        transform(ServiceFileTransformer::class.java) {
        include("bus-extensions.txt")
        }
        archiveBaseName.set("app")
        archiveClassifier.set("")
        isZip64 = true
        manifest {
            attributes(
                mapOf(
                    "Main-Class" to "no.nav.syfo.BootstrapKt",
                ),
            )
        }
    }


    test {
        useJUnitPlatform {
        }
        testLogging {
            events("skipped", "failed")
            showStackTraces = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    spotless {
        kotlin { ktfmt(ktfmtVersion).kotlinlangStyle() }
        check {
            dependsOn("spotlessApply")
        }
    }
}
