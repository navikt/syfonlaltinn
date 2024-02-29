import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer

group = "no.nav.syfo"
version = "1.0.0"

val coroutinesVersion = "1.8.0"
val jacksonVersion = "2.16.1"
val kluentVersion = "1.73"
val ktorVersion = "2.3.8"
val logbackVersion = "1.5.1"
val logstashEncoderVersion = "7.4"
val prometheusVersion = "0.16.0"
val smCommonVersion = "2.0.8"
val mockkVersion = "1.13.10"
val altinnDownloadQueueVersion = "1.2020.10.21-14.38-e6bb56478815"
val altinnPrefillVersion = "1.2020.10.21-14.38-e6bb56478815"
val cxfVersion = "3.5.5"
val jaxwsToolsVersion = "2.3.1"
val javaxActivationVersion = "1.2.0"
val postgresVersion = "42.7.2"
val flywayVersion = "10.8.1"
val hikariVersion = "5.1.0"
val testContainerVersion = "1.19.6"
val digisyfoNarmesteLederVersion = "1.2020.10.07-08.40-90b3ab7bad15"
val commonsValidatorVersion = "1.8.0"
val kotlinVersion = "1.9.22"
val confluentVersion = "7.0.1"
val ktfmtVersion = "0.44"
val commonsCodecVersion = "1.16.1"
val snappyJavaVersion = "1.1.10.5"
val junitVersion = "5.10.2"

plugins {
    id("application")
    id("com.diffplug.spotless") version "6.25.0"
    kotlin("jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
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
    implementation("org.apache.cxf:cxf-rt-transports-http:$cxfVersion")
    implementation("com.sun.activation:javax.activation:$javaxActivationVersion")
    implementation("com.sun.xml.ws:jaxws-tools:$jaxwsToolsVersion") {
        exclude(group = "com.sun.xml.ws", module = "policy")
    }

    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    compileOnly("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")
    implementation("no.nav.helse:syfosm-common-kafka:$smCommonVersion")
    constraints {
        implementation("org.xerial.snappy:snappy-java:$snappyJavaVersion") {
            because("override transient from org.apache.kafka:kafka_2.12")
        }
    }
    implementation("no.nav.helse:syfosm-common-networking:$smCommonVersion")
    implementation("commons-validator:commons-validator:$commonsValidatorVersion"){
        exclude(group = "commons-collections", module = "commons-collections")
    }

    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation("org.amshove.kluent:kluent:$kluentVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.testcontainers:kafka:$testContainerVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
    }
    testImplementation("org.testcontainers:postgresql:$testContainerVersion")

}

tasks {

    shadowJar {
        transform(ServiceFileTransformer::class.java) {
        setPath("META-INF/cxf")
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
