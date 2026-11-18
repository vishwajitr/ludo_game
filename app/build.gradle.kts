import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Release signing comes from a keystore.properties file (see keystore.properties.example). Keep it, and the keystore,
// outside this folder, e.g. ~/.ludo-signing/keystore.properties (storeFile may be an absolute path); a copy in the
// project root also works. Without any, the release build is simply left unsigned.
val keystoreProps = Properties().apply {
    listOf(File(System.getProperty("user.home"), ".ludo-signing/keystore.properties"), rootProject.file("keystore.properties"))
        .firstOrNull { it.exists() }?.inputStream()?.use { load(it) }
}

android {
    namespace = "com.example.ludo"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.vishwajitr.ludo" // namespace (code package) stays com.example.ludo
        minSdk = 26
        targetSdk = 37
        versionCode = providers.gradleProperty("VERSION_CODE").get().toInt()
        versionName = providers.gradleProperty("VERSION_NAME").get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) create("release") {
            storeFile = File(keystoreProps.getProperty("storeFile")).let { if (it.isAbsolute) it else rootProject.file(it.path) }
            storePassword = keystoreProps.getProperty("storePassword")
            keyAlias = keystoreProps.getProperty("keyAlias")
            keyPassword = keystoreProps.getProperty("keyPassword")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions { allWarningsAsErrors = true }
}

dependencies {
    implementation(project(":engine"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.datastore)
    implementation(libs.coroutines.android)
    implementation(libs.core.ktx)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    testImplementation(kotlin("test-junit"))
    testImplementation(libs.junit4)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test)
    androidTestImplementation(libs.espresso.core) // pins the transitive 3.5.0, which breaks on API 37
}
