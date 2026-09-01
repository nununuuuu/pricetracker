plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20250517")
    testImplementation(kotlin("test"))
}

kotlin { jvmToolchain(21) }

application { mainClass.set("tw.pricecompare.MainKt") }

tasks.test { useJUnitPlatform() }
