plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
}

group = "hu.toliver.matutine"
version = "1.0.0"
application {
    mainClass = "hu.toliver.matutine.ApplicationKt"
}

dependencies {
    api(project(":core"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}

val webDistribution = project(":app:webApp").layout.buildDirectory.dir(
    "dist/wasmJs/productionExecutable",
)

tasks.named<ProcessResources>("processResources") {
    dependsOn(":app:webApp:wasmJsBrowserDistribution")
    from(webDistribution) {
        into("web")
    }
}