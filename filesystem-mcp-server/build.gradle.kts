plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    application
}

application {
    mainClass.set("com.example.filesystemmcpserver.MainKt")
}

tasks.shadowJar {
    mergeServiceFiles()
    archiveClassifier.set("")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:mcp"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.logback.classic)
}
