plugins {
    kotlin("jvm") version "1.9.0"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    maven {
        url = uri("https://mvnrepository.com/artifact/org.web3j/core")
    }
}

dependencies {
    implementation("org.web3j:core:4.12.3")
    implementation("net.osslabz:evm-abi-decoder:0.1.0")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.0")
//    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("MainKt")
}