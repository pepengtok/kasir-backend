plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    application
}

kotlin {
    jvmToolchain(17) // Hanya ini
}

application {
    mainClass.set("com.kasir.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    val ktorVersion    = "2.3.11"
    val exposedVersion = "0.51.0"
    val javaJwtVersion = "4.4.0"

    // Exposed
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")

    // Ktor Server
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion") // <--- KEMBALIKAN KE $ktorVersion
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")

    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")

    // Ktor Client (jika perlu)
    implementation("io.ktor:ktor-client-core:2.3.11")
    implementation("io.ktor:ktor-client-cio:2.3.11")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion") // Menggunakan $ktorVersion

    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktorVersion")
    // Serialization
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")

    // JWT helper
    implementation("com.auth0:java-jwt:$javaJwtVersion")

    // Database & util
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("io.ktor:ktor-server-html-builder:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.11.0")
    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")
}

// ❌ Matikan task 'test' kalau memang tidak dipakai
tasks.named<Test>("test") {
    enabled = false
}
