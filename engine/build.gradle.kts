plugins { alias(libs.plugins.kotlin.jvm) }

kotlin {
    jvmToolchain(17)
    compilerOptions { allWarningsAsErrors = true }
}

dependencies { testImplementation(kotlin("test")) }

tasks.test { useJUnitPlatform() }
